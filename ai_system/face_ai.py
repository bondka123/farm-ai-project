import cv2
import numpy as np
import json
from insightface.app import FaceAnalysis
import mysql.connector
from datetime import datetime
import time
import os
import requests
import base64
import argparse

# =========================
# ARGUMENTS
# =========================
parser = argparse.ArgumentParser()
parser.add_argument("--source", type=str, default="0")
parser.add_argument("--camera_id", type=str, default="CAM_01")
parser.add_argument("--type", type=str, default="ENTRY") # ENTRY or INTERNAL
args = parser.parse_args()

CAMERA_SOURCE = int(args.source) if args.source.isdigit() else args.source
CAMERA_ID = args.camera_id
CAMERA_TYPE = args.type.upper()

# =========================
# BACKEND CONFIG
# =========================
BACKEND_URL = "http://localhost:8081/api/attendance"
ALERTS_URL = "http://localhost:8081/api/alerts/ai-detection"
ANALYTICS_URL = "http://localhost:8081/api/ai/events"
TOKEN = "Bearer " # Token usually not needed for internal AI reporting if backend allows localhost

def send_to_backend(name, status, is_unknown=False, image_path=None):
    try:
        data = {
            "employeeName": str(name),
            "status": str(status),
            "unknown": bool(is_unknown),
            "imagePath": image_path,
            "cameraId": CAMERA_ID
        }
        requests.post(BACKEND_URL, json=data, timeout=2)
    except Exception as e:
        print("BACKEND ERROR:", e)

def send_analytics(detected, recognized, unknown, entries, exits):
    try:
        payload = {
            "cameraId": CAMERA_ID,
            "cameraType": CAMERA_TYPE,
            "timestamp": datetime.now().isoformat(),
            "detected": int(detected),
            "recognized": int(recognized),
            "unknown": int(unknown),
            "entries": int(entries),
            "exits": int(exits),
            "departmentId": 1 # Default or from args
        }
        requests.post(ANALYTICS_URL, json=payload, timeout=1)
    except Exception as e:
        pass # Don't block for analytics

# =========================
# DB & IA
# =========================
db = mysql.connector.connect(host="localhost", user="root", password="root123", database="attendance_db")
cursor = db.cursor()
app = FaceAnalysis(name='buffalo_l')
app.prepare(ctx_id=-1, det_size=(320,320))

def load_employees():
    cursor.execute("SELECT name, embedding FROM employees WHERE embedding IS NOT NULL")
    data = cursor.fetchall()
    return {emp[0]: np.array(json.loads(emp[1])) / np.linalg.norm(np.array(json.loads(emp[1]))) for emp in data}

employees = load_employees()

# =========================
# LOOP
# =========================
cap = cv2.VideoCapture(CAMERA_SOURCE)
line_x = 320
margin = 30
employee_states = {}
UNKNOWN_FOLDER = "unknown_faces"
os.makedirs(UNKNOWN_FOLDER, exist_ok=True)

print(f"FACE AI STARTED ON {CAMERA_ID}")
last_analytics_time = time.time()

while True:
    ret, frame = cap.read()
    if not ret: break
    frame = cv2.resize(frame, (640,480))
    faces = app.get(frame)
    
    recognized_count = 0
    unknown_count = 0
    entries_this_frame = 0
    exits_this_frame = 0

    for face in faces:
        emb = face.embedding / np.linalg.norm(face.embedding)
        best_match = "UNKNOWN"
        min_dist = 1.1
        
        # ... (recognition logic below)

        for person, known_emb in employees.items():
            dist = np.linalg.norm(emb - known_emb)
            if dist < min_dist:
                min_dist = dist
                best_match = person

        x1,y1,x2,y2 = map(int, face.bbox)
        center_x = (x1 + x2) // 2

        if best_match == "UNKNOWN":
            unknown_count += 1
            # Simple unknown handling for now
            cv2.rectangle(frame, (x1,y1), (x2,y2), (0,0,255), 2)
            cv2.putText(frame, "UNKNOWN", (x1, y1-10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0,0,255), 2)
        else:
            # Tracking logic
            if best_match not in employee_states:
                employee_states[best_match] = "INSIDE" if center_x > line_x else "OUTSIDE"
            else:
                curr = employee_states[best_match]
                if curr == "OUTSIDE" and center_x > line_x + margin:
                    send_to_backend(best_match, "IN")
                    employee_states[best_match] = "INSIDE"
                    entries_this_frame += 1
                elif curr == "INSIDE" and center_x < line_x - margin:
                    send_to_backend(best_match, "OUT")
                    employee_states[best_match] = "OUTSIDE"
                    exits_this_frame += 1
                
                recognized_count += 1

            cv2.rectangle(frame, (x1,y1), (x2,y2), (0,255,0), 2)
            cv2.putText(frame, best_match, (x1, y1-10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0,255,0), 2)

    # Periodic Analytics Envoi (every 3 seconds)
    if time.time() - last_analytics_time > 3:
        send_analytics(len(faces), recognized_count, unknown_count, entries_this_frame, exits_this_frame)
        last_analytics_time = time.time()

    cv2.line(frame, (line_x, 0), (line_x, 480), (255,0,0), 2)
    cv2.imshow(f"FACE AI - {CAMERA_ID}", frame)
    if cv2.waitKey(1) == 27: break

cap.release()
cv2.destroyAllWindows()
