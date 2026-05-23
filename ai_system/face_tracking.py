import cv2
import numpy as np
import json
from insightface.app import FaceAnalysis
import mysql.connector
from datetime import datetime
import time
import os
import base64
import argparse
from camera_utils import open_camera, detect_source_type, validate_camera_id
from http_utils import safe_post_json, safe_upload_frame

# =========================
# ARGUMENTS
# =========================
parser = argparse.ArgumentParser()
parser.add_argument("camera_id", type=int, help="Camera ID in DB")
parser.add_argument("source", type=str, help="Camera source (index or URL)")
parser.add_argument("--token", type=str, default=os.environ.get("AI_INTERNAL_TOKEN", "INTERNAL_AI_TOKEN"), help="Internal AI auth token")
parser.add_argument("--headless", action="store_true", help="Run without OpenCV windows")
args = parser.parse_args()

CAMERA_SOURCE = int(args.source) if args.source.isdigit() else args.source
CAMERA_ID = args.camera_id

if not validate_camera_id(CAMERA_ID):
    exit(1)

# =========================
# BACKEND CONFIG
# =========================
BASE_URL = os.environ.get("BACKEND_URL", "http://localhost:8081/api")
ATTENDANCE_URL = f"{BASE_URL}/attendance"
ALERTS_URL = f"{BASE_URL}/alerts/ai-detection"
UPLOAD_URL = f"{BASE_URL}/upload"
TOKEN = args.token

# =========================
# DB CONFIG
# =========================
try:
    db = mysql.connector.connect(
        host="localhost",
        user="root",
        password="root123",
        database="attendance_db"
    )
    cursor = db.cursor()
except Exception as e:
    print(f"[ERROR] DB CONNECTION ERROR: {e}")
    exit()

# =========================
# IA MODELS
# =========================
print(f"[IA] Loading Face AI for Camera {CAMERA_ID}...")
app = FaceAnalysis(name='buffalo_l')
app.prepare(ctx_id=0, det_size=(320, 320))

# =========================
# LOAD EMPLOYEES / USERS
# =========================
def load_employees():
    employees = {}
    cursor.execute("SELECT name, embedding FROM employees WHERE embedding IS NOT NULL")
    emp_rows = cursor.fetchall()
    for name, emb_json in emp_rows:
        emb = np.array(json.loads(emb_json))
        emb = emb / np.linalg.norm(emb)
        employees[name] = emb

    cursor.execute("SELECT email, embedding FROM users WHERE embedding IS NOT NULL AND face_registered = 1")
    user_rows = cursor.fetchall()
    for email, emb_json in user_rows:
        if email not in employees:
            emb = np.array(json.loads(emb_json))
            emb = emb / np.linalg.norm(emb)
            employees[email] = emb

    print(f"[INFO] {len(employees)} known faces loaded ({len(emp_rows)} employees + {len(user_rows)} users)")
    return employees

employees = load_employees()

def get_line_position_percent():
    try:
        cursor.execute("SELECT line_position FROM cameras WHERE id = %s", (CAMERA_ID,))
        row = cursor.fetchone()
        if row and row[0] is not None:
            return max(5, min(int(row[0]), 95))
    except Exception as e:
        print(f"[WARN] Cannot load line position for camera {CAMERA_ID}: {e}")
    return 50

line_position_percent = get_line_position_percent()
print(f"[INFO] FACE ENTRY/EXIT LINE POSITION: {line_position_percent}% vertical")

# =========================
# STATE TRACKING
# =========================
# {track_id: {"state": "LEFT"|"RIGHT", "name": str, "last_seen": float}}
tracking_memory = {}
employee_last_event = {} # {name: {"type": "IN"|"OUT", "time": float}}
unknown_persons = {}

# Anti-duplication cooldown (seconds)
COOLDOWN = 10 
UNKNOWN_MATCH_THRESHOLD = 0.45
UNKNOWN_EXIT_TIMEOUT = 5
UNKNOWN_CLEANUP_TIMEOUT = 300

# State constants
ZONE_LEFT = "LEFT"
ZONE_RIGHT = "RIGHT"
STATE_ACTIVE = "ACTIVE"
STATE_LOST = "LOST"
STATE_EXITED = "EXITED"

# =========================
# HELPERS
# =========================
def send_to_backend(name, status, is_unknown=False, image_base64=None):
    try:
        payload = {
            "employeeName": str(name),
            "status": str(status),
            "unknown": bool(is_unknown),
            "cameraId": CAMERA_ID
        }
        response = safe_post_json(ATTENDANCE_URL, payload, token=TOKEN, timeout=2, retries=2)
        if response.status_code != 200:
            print(f"❌ ATTENDANCE POST FAILED ({response.status_code}): {response.text}")

        if is_unknown and image_base64:
            alert_payload = {
                "type": "UNKNOWN_PERSON",
                "location": f"Camera {CAMERA_ID}",
                "timestamp": datetime.now().isoformat(),
                "imageBase64": image_base64,
                "embeddingHash": str(name),
                "cameraId": CAMERA_ID
            }
            alert_response = safe_post_json(ALERTS_URL, alert_payload, token=TOKEN, timeout=2, retries=2)
            if alert_response.status_code != 200:
                print(f"❌ ALERT POST FAILED ({alert_response.status_code}): {alert_response.text}")

        print(f"🚀 EVENT SENT: {name} -> {status}")
    except Exception as e:
        print(f"❌ BACKEND ERROR: {e}")

def send_unknown_alert(uid, image_base64):
    try:
        alert_payload = {
            "type": "UNKNOWN_PERSON",
            "location": f"Camera {CAMERA_ID}",
            "timestamp": datetime.now().isoformat(),
            "imageBase64": image_base64,
            "embeddingHash": uid,
            "cameraId": CAMERA_ID
        }
        alert_response = safe_post_json(ALERTS_URL, alert_payload, token=TOKEN, timeout=2, retries=2)
        if alert_response.status_code != 200:
            print(f"[ERROR] ALERT POST FAILED ({alert_response.status_code}): {alert_response.text}")
        else:
            print(f"[INFO] UNKNOWN ALERT SENT: {uid}")
    except Exception as e:
        print(f"[ERROR] ALERT BACKEND ERROR: {e}")

def crop_face_b64(frame, x1, y1, x2, y2):
    h, w = frame.shape[:2]
    x1 = max(0, min(x1, w - 1))
    x2 = max(0, min(x2, w - 1))
    y1 = max(0, min(y1, h - 1))
    y2 = max(0, min(y2, h - 1))
    face_img = frame[y1:y2, x1:x2]
    if face_img.size == 0:
        return None
    _, buffer = cv2.imencode('.jpg', face_img)
    return base64.b64encode(buffer).decode('utf-8')

def get_unknown_uid(embedding, current_time):
    for uid, data in list(unknown_persons.items()):
        time_since_seen = current_time - data["last_seen"]
        if time_since_seen > UNKNOWN_CLEANUP_TIMEOUT:
            del unknown_persons[uid]
        elif time_since_seen > UNKNOWN_EXIT_TIMEOUT and data["state"] in [STATE_ACTIVE, STATE_LOST]:
            data["state"] = STATE_EXITED
        elif time_since_seen > 0 and data["state"] == STATE_ACTIVE:
            data["state"] = STATE_LOST

    best_uid = None
    best_sim = -1.0
    for uid, data in unknown_persons.items():
        sim = float(np.dot(embedding, data["embedding"]))
        if sim > best_sim:
            best_sim = sim
            best_uid = uid

    should_alert = False
    if best_uid is not None and best_sim >= UNKNOWN_MATCH_THRESHOLD:
        uid = best_uid
        person = unknown_persons[uid]
        should_alert = person["state"] == STATE_EXITED
        person["embedding"] = embedding
        person["last_seen"] = current_time
        person["state"] = STATE_ACTIVE
    else:
        uid = f"UNKNOWN_{int(current_time)}_{CAMERA_ID}"
        unknown_persons[uid] = {
            "embedding": embedding,
            "last_seen": current_time,
            "state": STATE_ACTIVE
        }
        should_alert = True

    return uid, should_alert

def upload_frame(frame):
    try:
        _, buffer = cv2.imencode('.jpg', frame)
        response = safe_upload_frame(UPLOAD_URL, buffer.tobytes(), CAMERA_ID, token=TOKEN, timeout=1, retries=2)
        if response is not None and response.status_code != 200:
            print(f"[WARN] Upload failed ({response.status_code}): {response.text}")
            if response.status_code == 404:
                print(f"[ERROR] Camera {CAMERA_ID} not found. Exiting old AI process.")
                exit(1)
    except Exception as e:
        print(f"[WARN] Upload failed: {e}")

# =========================
# MAIN LOOP
# =========================
source_type = detect_source_type(CAMERA_SOURCE)
print(f"[INFO] FACE TRACKING SOURCE TYPE: {source_type}")

try:
    cap = open_camera(CAMERA_SOURCE)
    cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
except Exception as e:
    print(f"[ERROR] Unable to open camera source {CAMERA_SOURCE}: {e}")
    exit(1)

last_upload_time = 0
last_db_refresh = time.time()

print(f"[INFO] FACE TRACKING STARTED ON SOURCE: {CAMERA_SOURCE}")

while True:
    ret, frame = cap.read()
    if not ret or frame is None:
        print("[WARN] Frame read failed, reconnecting...")
        cap.release()
        time.sleep(1)
        try:
            cap = open_camera(CAMERA_SOURCE)
        except Exception as e:
            print(f"[ERROR] Reconnect failed: {e}")
            time.sleep(2)
            continue
        continue

    frame_h, frame_w = frame.shape[:2]
    line_x = int(frame_w * (line_position_percent / 100.0))
    margin = 40 # Dead zone around the line

    faces = app.get(frame)
    current_time = time.time()

    # Refresh employees every 5 minutes
    if current_time - last_db_refresh > 300:
        employees = load_employees()
        line_position_percent = get_line_position_percent()
        last_db_refresh = current_time

    for face in faces:
        emb = face.embedding
        emb = emb / np.linalg.norm(emb)
        
        # Identification
        best_match = "UNKNOWN"
        max_sim = -1.0
        SIMILARITY_THRESHOLD = 0.45

        for name, known_emb in employees.items():
            similarity = np.dot(emb, known_emb)
            if similarity > max_sim:
                max_sim = similarity
                best_match = name
        
        if max_sim < SIMILARITY_THRESHOLD:
            best_match = "UNKNOWN"

        # Tracking Position
        x1, y1, x2, y2 = face.bbox.astype(int)
        center_x = (x1 + x2) // 2
        
        # Determine current zone
        current_zone = None
        if center_x < line_x - margin:
            current_zone = ZONE_LEFT
        elif center_x > line_x + margin:
            current_zone = ZONE_RIGHT

        if best_match == "UNKNOWN":
            track_key, should_alert = get_unknown_uid(emb, current_time)
            best_match = track_key
            if should_alert:
                img_b64 = crop_face_b64(frame, x1, y1, x2, y2)
                if img_b64:
                    send_unknown_alert(track_key, img_b64)
        else:
            track_key = best_match

        # logic for crossing
        if current_zone:
            if track_key in tracking_memory:
                prev_zone = tracking_memory[track_key]["state"]
                
                if prev_zone == ZONE_LEFT and current_zone == ZONE_RIGHT:
                    # LEFT -> RIGHT = ENTRY
                    if not track_key.startswith("UNKNOWN_"):
                        last_event = employee_last_event.get(track_key)
                        if not last_event or (current_time - last_event["time"] > COOLDOWN or last_event["type"] != "IN"):
                            send_to_backend(track_key, "IN")
                            employee_last_event[track_key] = {"type": "IN", "time": current_time}
                    else:
                        img_b64 = crop_face_b64(frame, x1, y1, x2, y2)
                        send_to_backend(track_key, "IN", is_unknown=True, image_base64=img_b64)
                    
                    tracking_memory[track_key]["state"] = ZONE_RIGHT
                
                elif prev_zone == ZONE_RIGHT and current_zone == ZONE_LEFT:
                    # RIGHT -> LEFT = EXIT
                    if not track_key.startswith("UNKNOWN_"):
                        last_event = employee_last_event.get(track_key)
                        if not last_event or (current_time - last_event["time"] > COOLDOWN or last_event["type"] != "OUT"):
                            send_to_backend(track_key, "OUT")
                            employee_last_event[track_key] = {"type": "OUT", "time": current_time}
                    else:
                        send_to_backend(track_key, "OUT", is_unknown=True)
                    
                    tracking_memory[track_key]["state"] = ZONE_LEFT
            else:
                tracking_memory[track_key] = {"state": current_zone, "last_seen": current_time}

        # Visuals
        color = (0, 0, 255) if str(best_match).startswith("UNKNOWN_") else (0, 255, 0)
        cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
        cv2.putText(frame, f"{best_match}", (x1, y1-10), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

    # UI Overlays
    # Line
    cv2.line(frame, (line_x, 0), (line_x, frame_h), (255, 0, 0), 2)
    cv2.line(frame, (line_x - margin, 0), (line_x - margin, frame_h), (200, 200, 200), 1)
    cv2.line(frame, (line_x + margin, 0), (line_x + margin, frame_h), (200, 200, 200), 1)
    cv2.putText(frame, f"ENTRY/EXIT LINE ({line_position_percent}%)", (line_x + 8, 24), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 0, 0), 1)
    
    # Live Badge
    cv2.rectangle(frame, (10, 10), (80, 35), (0, 0, 255), -1)
    cv2.putText(frame, "LIVE", (20, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)
    
    # Cam Info
    cv2.putText(frame, f"CAM: {CAMERA_ID} | FACE AI", (100, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)

    # Periodic Upload (1 FPS for dashboard preview)
    if current_time - last_upload_time > 1.0:
        upload_frame(frame)
        last_upload_time = current_time

    # Show window (for debugging/local)
    if not args.headless:
        cv2.imshow(f"FARM AI - FACE TRACKING CAM {CAMERA_ID}", frame)
        if cv2.waitKey(1) == 27: break
    else:
        # Check for escape every 50 frames to reduce CPU if headless
        if int(time.time() * 10) % 5 == 0:
            if cv2.waitKey(1) == 27: break

cap.release()
cv2.destroyAllWindows()
