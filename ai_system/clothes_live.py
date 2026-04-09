import cv2
import numpy as np
from ultralytics import YOLO
from collections import Counter
import requests
import time
import os
import datetime
from deep_sort_realtime.deepsort_tracker import DeepSort

# =========================
# CONFIG
# =========================
model = YOLO("yolov8n.pt")
tracker = DeepSort(max_age=30)

BACKEND_DEPT_URL = "http://localhost:8080/api/departments/public"
BACKEND_STATUS_URL = "http://localhost:8080/api/department-status"
UPLOAD_URL = "http://localhost:8080/api/upload"

camera_id = 1

UNKNOWN_FOLDER = "unknown_clothes"
os.makedirs(UNKNOWN_FOLDER, exist_ok=True)

COOLDOWN = 5
last_event_time = {}

# =========================
# SAVE IMAGE
# =========================
def save_unknown(img):
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    path = f"{UNKNOWN_FOLDER}/unknown_{timestamp}.jpg"
    cv2.imwrite(path, img)
    print("📸 SAVED:", path)
    return path

# =========================
# SEND IMAGE
# =========================
def send_image(filepath, department_id):
    try:
        files = {'file': open(filepath, 'rb')}
        data = {
            "departmentId": str(department_id),
            "cameraId": str(camera_id)
        }
        res = requests.post(UPLOAD_URL, files=files, data=data)
        print("📤 IMAGE SENT:", filepath, "| STATUS:", res.status_code)
    except Exception as e:
        print("❌ IMAGE ERROR:", e)

# =========================
# SEND STATUS
# =========================
def send_status(data):
    try:
        res = requests.post(BACKEND_STATUS_URL, json=data)
        print("📡 SENT:", data, "| STATUS:", res.status_code)
    except Exception as e:
        print("❌ STATUS ERROR:", e)

# =========================
# GET DEPARTMENTS
# =========================
def get_departments():
    try:
        res = requests.get(BACKEND_DEPT_URL)
        return res.json()
    except:
        print("❌ ERROR FETCH DEPARTMENTS")
        return []

# =========================
# COLOR DETECTION (IMPROVED)
# =========================
def detect_color(img):
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    avg = np.mean(hsv, axis=(0,1))
    h,s,v = avg

    if v > 200 and s < 40:
        return "medecin"

    if 90 < h < 140:
        return "employee"

    if v < 100:
        return "electricien"

    return "unknown"

# =========================
# CAMERA
# =========================
cap = cv2.VideoCapture(0)
print("✅ Camera started")

while True:
    ret, frame = cap.read()
    if not ret:
        break

    results = model(frame)
    detections = []

    for r in results:
        for box in r.boxes:
            cls = int(box.cls[0])

            if model.names[cls] == "person":
                x1,y1,x2,y2 = map(int, box.xyxy[0])
                w = x2 - x1
                h = y2 - y1
                detections.append(([x1, y1, w, h], 1.0, "person"))

    tracks = tracker.update_tracks(detections, frame=frame)

    roles = []
    unknown_detected = False
    unknown_images = []

    for track in tracks:

        if not track.is_confirmed():
            continue

        x1, y1, x2, y2 = map(int, track.to_ltrb())
        person = frame[y1:y2, x1:x2]

        if person.size == 0:
            continue

        h = person.shape[0]
        cloth = person[int(h*0.3):int(h*0.7), :]

        if cloth.size == 0:
            continue

        role = detect_color(cloth)
        print("🧍 ROLE:", role)

        roles.append(role)

        if role == "unknown":
            unknown_detected = True
            filepath = save_unknown(person)
            unknown_images.append(filepath)

        # display
        cv2.rectangle(frame, (x1,y1), (x2,y2), (0,255,0), 2)
        cv2.putText(frame, role, (x1,y1-10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0,255,0), 2)

    # =========================
    # COUNT
    # =========================
    counts = Counter(roles)
    print("📊 COUNTS:", counts)

    # =========================
    # ANALYSE DEPARTMENTS
    # =========================
    departments = get_departments()

    for dept in departments:

        expected = {
            "medecin": dept["doctors"],
            "electricien": dept["electricians"],
            "employee": dept["workers"]
        }

        valid = True
        messages = []

        for role, exp in expected.items():
            actual = counts.get(role, 0)

            if actual < exp:
                valid = False
                messages.append(f"MANQUE {role}: {actual}/{exp}")

            elif actual > exp:
                valid = False
                messages.append(f"TROP {role}: {actual}/{exp}")

        if unknown_detected:
            valid = False
            messages.append("UNKNOWN DETECTED")

        manager_id = None
        if dept.get("manager") and dept["manager"].get("id"):
            manager_id = dept["manager"]["id"]

        key = str(dept["id"])
        now = time.time()

        # 🔥 COOLDOWN CONTROL
        if key not in last_event_time or now - last_event_time[key] > COOLDOWN:

            send_status({
                "departmentId": dept["id"],
                "managerId": manager_id,
                "cameraId": camera_id,
                "status": "valid" if valid else "alert",
                "details": messages,
                "counts": dict(counts)
            })

            # envoyer images unknown
            for img in unknown_images:
                send_image(img, dept["id"])

            last_event_time[key] = now

    cv2.imshow("Camera", frame)

    if cv2.waitKey(1) == 27:
        break

cap.release()
cv2.destroyAllWindows()