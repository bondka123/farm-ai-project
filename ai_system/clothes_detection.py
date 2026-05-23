import cv2
import numpy as np
import time
import os
import argparse
from ultralytics import YOLO
from sklearn.cluster import KMeans
from collections import Counter
from camera_utils import open_camera, detect_source_type, validate_camera_id
from http_utils import safe_post_json, safe_upload_frame

# =========================
# ARGUMENTS
# =========================
parser = argparse.ArgumentParser()
parser.add_argument("camera_id", type=int)
parser.add_argument("source", type=str)
parser.add_argument("--token", type=str, default=os.environ.get("AI_INTERNAL_TOKEN", "INTERNAL_AI_TOKEN"), help="Internal AI auth token")
parser.add_argument("--headless", action="store_true", help="Run without OpenCV windows")
args = parser.parse_args()

CAMERA_SOURCE = int(args.source) if args.source.isdigit() else args.source
CAMERA_ID = args.camera_id
HEADLESS = args.headless
TOKEN = args.token

if not validate_camera_id(CAMERA_ID):
    exit(1)

# =========================
# BACKEND CONFIG
# =========================
BASE_URL = os.environ.get("BACKEND_URL", "http://localhost:8081/api")
DEPT_STATUS_URL = f"{BASE_URL}/department-status"
UPLOAD_URL = f"{BASE_URL}/upload"

# =========================
# MODELS
# =========================
model = YOLO("yolov8n.pt")


# =========================
# HELPERS
# =========================
def get_dominant_color(image, k=3):
    if image.size == 0:
        return np.array([255, 255, 255])

    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    pixels = image.reshape((-1, 3))
    pixels = pixels[np.all(pixels > 15, axis=1)]
    if pixels.size == 0:
        pixels = image.reshape((-1, 3))

    clt = KMeans(n_clusters=min(k, len(pixels)), n_init=10)
    clt.fit(pixels)
    counts = Counter(clt.labels_)
    dominant = clt.cluster_centers_[counts.most_common(1)[0][0]]
    return dominant


def detect_helmet(head_region):
    if head_region.size == 0:
        return False

    hsv = cv2.cvtColor(head_region, cv2.COLOR_BGR2HSV)
    yellow_mask = cv2.inRange(hsv, (10, 80, 120), (40, 255, 255))
    white_mask = cv2.inRange(hsv, (0, 0, 180), (180, 50, 255))

    yellow_count = cv2.countNonZero(yellow_mask)
    white_count = cv2.countNonZero(white_mask)
    area = head_region.shape[0] * head_region.shape[1]

    return (yellow_count + white_count) > max(10, area * 0.03)


def classify_role(rgb, head_region):
    r, g, b = rgb
    if r + g + b < 10:
        return "employee"

    hsv = cv2.cvtColor(np.uint8([[rgb]]), cv2.COLOR_RGB2HSV)[0][0]
    h, s, v = int(hsv[0]), int(hsv[1]), int(hsv[2])

    if detect_helmet(head_region):
        return "electricien"

    if r > 210 and g > 210 and b > 210:
        return "medecin"

    if 90 <= h <= 140 and s >= 80 and v >= 50:
        return "employee"

    if (h <= 40 and s >= 80 and v >= 90) or (r > 150 and g > 120 and b < 120):
        return "electricien"

    return "unknown"


def enforce_bounds(value, maximum):
    return max(0, min(value, maximum))


# =========================
# INITIALIZE
# =========================
source_type = detect_source_type(CAMERA_SOURCE)
print(f"[INFO] CLOTHES DETECTION STARTED ON CAM {CAMERA_ID} (SOURCE TYPE={source_type})")

try:
    cap = open_camera(CAMERA_SOURCE)
    cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
except Exception as e:
    print(f"[ERROR] Unable to open camera source {CAMERA_SOURCE}: {e}")
    exit(1)

last_report_time = 0
last_upload_time = 0
REPORT_INTERVAL = 5

print(f"[INFO] CLOTHES DETECTION STARTED ON CAM {CAMERA_ID}")

while True:
    ret, frame = cap.read()
    if not ret or frame is None:
        print("[WARN] Unable to read frame, reconnecting...")
        cap.release()
        time.sleep(1)
        try:
            cap = open_camera(CAMERA_SOURCE)
        except Exception as e:
            print(f"[ERROR] Reconnect failed: {e}")
            time.sleep(2)
            continue
        continue

    results = model.track(frame, persist=True, classes=[0], verbose=False)
    current_roles = []

    if results and len(results) > 0 and results[0].boxes is not None:
        boxes = results[0].boxes.xyxy.cpu().numpy().astype(int)
        ids = results[0].boxes.id.cpu().numpy().astype(int) if results[0].boxes.id is not None else [None] * len(boxes)

        for box, track_id in zip(boxes, ids):
            x1, y1, x2, y2 = box
            x1, y1, x2, y2 = map(int, [x1, y1, x2, y2])
            x1 = enforce_bounds(x1, frame.shape[1] - 1)
            x2 = enforce_bounds(x2, frame.shape[1] - 1)
            y1 = enforce_bounds(y1, frame.shape[0] - 1)
            y2 = enforce_bounds(y2, frame.shape[0] - 1)

            person = frame[y1:y2, x1:x2]
            if person.size == 0:
                continue

            torso_top = y1 + int((y2 - y1) * 0.2)
            torso_bottom = y1 + int((y2 - y1) * 0.5)
            torso_left = x1 + int((x2 - x1) * 0.25)
            torso_right = x1 + int((x2 - x1) * 0.75)
            torso_top = enforce_bounds(torso_top, frame.shape[0] - 1)
            torso_bottom = enforce_bounds(torso_bottom, frame.shape[0] - 1)
            torso_left = enforce_bounds(torso_left, frame.shape[1] - 1)
            torso_right = enforce_bounds(torso_right, frame.shape[1] - 1)
            torso = frame[torso_top:torso_bottom, torso_left:torso_right]

            head_top = y1
            head_bottom = y1 + int((y2 - y1) * 0.25)
            head_region = frame[head_top:enforce_bounds(head_bottom, frame.shape[0] - 1), x1:enforce_bounds(x2, frame.shape[1] - 1)]

            dominant = get_dominant_color(torso)
            role = classify_role(dominant, head_region)
            label_text = role

            current_roles.append(role)

            color = (255, 255, 255) if role == "medecin" else (255, 100, 0) if role == "employee" else (0, 255, 255)

            cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
            cv2.putText(frame, f"{label_text}", (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

    if time.time() - last_report_time > REPORT_INTERVAL:
        counts = Counter(current_roles)
        payload = {
            "cameraId": CAMERA_ID,
            "counts": {
                "medecin": counts.get("medecin", 0),
                "worker": counts.get("employee", 0) + counts.get("worker", 0),
                "electricien": counts.get("electricien", 0)
            },
            "unknown": counts.get("unknown", 0) > 0,
            "timestamp": time.time()
        }
        try:
            response = safe_post_json(DEPT_STATUS_URL, payload, token=TOKEN, timeout=2, retries=2)
            if response.status_code == 200:
                print(f"[STATUS] REPORT CAM {CAMERA_ID}: {payload['counts']}")
            else:
                print(f"[WARN] Status post failed ({response.status_code}): {response.text}")
        except Exception as e:
            print(f"[WARN] Cannot send status: {e}")
        last_report_time = time.time()

    if time.time() - last_upload_time > 1.0:
        try:
            _, buffer = cv2.imencode('.jpg', frame)
            response = safe_upload_frame(UPLOAD_URL, buffer.tobytes(), CAMERA_ID, token=TOKEN, timeout=2, retries=2)
            if response is not None and response.status_code != 200:
                print(f"[WARN] Upload failed ({response.status_code}): {response.text}")
                if response.status_code == 404:
                    print(f"[ERROR] Camera {CAMERA_ID} not found. Exiting old AI process.")
                    break
        except Exception as e:
            print(f"[WARN] Upload failed: {e}")
        last_upload_time = time.time()

    if not HEADLESS:
        cv2.rectangle(frame, (10, 10), (150, 35), (0, 255, 0), -1)
        cv2.putText(frame, "CLOTHES AI", (20, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 0), 2)
        cv2.imshow(f"FARM AI - CLOTHES DETECTION CAM {CAMERA_ID}", frame)
        if cv2.waitKey(1) == 27:
            break
    else:
        if int(time.time() * 10) % 5 == 0:
            if cv2.waitKey(1) == 27:
                break

cap.release()
cv2.destroyAllWindows()
