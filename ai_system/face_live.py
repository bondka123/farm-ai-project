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

# =========================
# BACKEND CONFIG
# =========================
BACKEND_URL = "http://localhost:8081/api/attendance"
ALERTS_URL = "http://localhost:8081/api/alerts/ai-detection"

# 🔑 🔥 IMPORTANT → MET TON TOKEN ICI
TOKEN = "Bearer TON_TOKEN_ICI"

def send_to_backend(name, status, is_unknown=False, image_path=None):
    try:
        data = {
            "employeeName": str(name),
            "status": str(status),
            "unknown": bool(is_unknown),
            "imagePath": image_path
        }

        headers = {
            "Content-Type": "application/json",
            "Authorization": TOKEN   # 🔥 FIX PRINCIPAL
        }

        print("📤 DATA ENVOYÉE:", data)

        response = requests.post(
            BACKEND_URL,
            json=data,
            headers=headers
        )

        print("✅ STATUS:", response.status_code)
        print("📩 RESPONSE:", response.text)

    except Exception as e:
        print("❌ ERREUR BACKEND:", e)

# =========================
# DB (embeddings)
# =========================
db = mysql.connector.connect(
    host="localhost",
    user="root",
    password="root123",
    database="attendance_db"
)
cursor = db.cursor()

# =========================
# IA
# =========================
app = FaceAnalysis(name='buffalo_l')
app.prepare(ctx_id=0, det_size=(320,320))

# =========================
# LOAD EMPLOYEES
# =========================
def load_employees():
    cursor.execute("SELECT name, embedding FROM employees WHERE embedding IS NOT NULL")
    data = cursor.fetchall()

    employees = {}

    for emp in data:
        name = emp[0]
        emb = np.array(json.loads(emp[1]))
        emb = emb / np.linalg.norm(emb)
        employees[name] = emb

    print(f"✅ {len(employees)} employés chargés")
    return employees

employees = load_employees()

# =========================
# CAMERA
# =========================
cap = cv2.VideoCapture(0)

if not cap.isOpened():
    print("❌ CAMERA ERROR")
    exit()

print("✅ CAMERA OK")

# =========================
# CONFIG
# =========================
line_x = 320
margin = 30
employee_states = {}  # Tracks "INSIDE" or "OUTSIDE"


# =========================
# SMART ALERT SYSTEM CONFIG
# =========================
CAMERA_ID = "Camera_Principale"
EXIT_TIMEOUT = 5      # Seconds without detection to be considered EXITED
CLEANUP_TIMEOUT = 300 # Seconds before removing from memory completely

# State Tracking: Map<faceHash_cameraId, PersonState>
# PersonState: {"state": str, "embedding": arr, "last_seen": float, "last_alert": float}
active_persons = {} 

# Possible States
STATE_NEW = "NEW"
STATE_ACTIVE = "ACTIVE"
STATE_LOST = "LOST"
STATE_EXITED = "EXITED"

UNKNOWN_FOLDER = "unknown_faces"
os.makedirs(UNKNOWN_FOLDER, exist_ok=True)

# =========================
# LOOP
# =========================
while True:
    ret, frame = cap.read()

    if not ret:
        break

    frame = cv2.resize(frame, (640,480))
    faces = app.get(frame)

    for face in faces:
        emb = face.embedding
        emb = emb / np.linalg.norm(emb)

        best_match = "UNKNOWN"
        min_dist = 999

        # 🔍 RECOGNITION
        for person, known_emb in employees.items():
            dist = np.linalg.norm(emb - known_emb)

            if dist < min_dist:
                min_dist = dist
                best_match = person

        if min_dist > 1.1:
            best_match = "UNKNOWN"

        x1,y1,x2,y2 = map(int, face.bbox)
        center_x = (x1 + x2) // 2
        current_time = time.time()

        # =========================
        # UNKNOWN (SMART TRACKING)
        # =========================
        if best_match == "UNKNOWN":
            
            # 1. Update existing tracking states and cleanup
            for p_uid, p_data in list(active_persons.items()):
                time_since_seen = current_time - p_data["last_seen"]
                
                if time_since_seen > CLEANUP_TIMEOUT:
                    del active_persons[p_uid]
                elif time_since_seen > EXIT_TIMEOUT and p_data["state"] in [STATE_ACTIVE, STATE_LOST]:
                    active_persons[p_uid]["state"] = STATE_EXITED
                elif 0 < time_since_seen <= EXIT_TIMEOUT and p_data["state"] == STATE_ACTIVE:
                    active_persons[p_uid]["state"] = STATE_LOST

            # 2. Find if this face is already tracked
            min_unk_dist = 999
            matched_uid = None
            
            for p_uid, p_data in active_persons.items():
                dist = np.linalg.norm(emb - p_data["embedding"])
                if dist < min_unk_dist:
                    min_unk_dist = dist
                    matched_uid = p_uid
            
            trigger_alert = False

            if min_unk_dist < 1.1:
                uid = matched_uid
                person = active_persons[uid]
                person["embedding"] = emb # update to latest
                person["last_seen"] = current_time
                
                if person["state"] == STATE_EXITED:
                    person["state"] = STATE_NEW
                    trigger_alert = True
                else:
                    person["state"] = STATE_ACTIVE
            else:
                uid = f"UNKNOWN_{int(time.time())}_{CAMERA_ID}"
                active_persons[uid] = {
                    "state": STATE_NEW,
                    "embedding": emb,
                    "last_seen": current_time,
                    "last_alert": 0,
                    "line_state": None
                }


                trigger_alert = True

            # 3. Create Alert if rule matched
            if trigger_alert:
                active_persons[uid]["last_alert"] = current_time
                active_persons[uid]["state"] = STATE_ACTIVE
                
                print(f"🆕 NEW ENTRY DETECTED: {uid}")

                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                filename = f"{UNKNOWN_FOLDER}/{uid}_{timestamp}.jpg"

                face_img = frame[y1:y2, x1:x2]
                
                if face_img.size != 0:
                    cv2.imwrite(filename, face_img)
                    
                    # Convert image to base64
                    _, buffer = cv2.imencode('.jpg', face_img)
                    img_base64 = base64.b64encode(buffer).decode('utf-8')
                    
                    # Send to Alert System
                    alert_data = {
                        "type": "UNKNOWN_PERSON",
                        "location": CAMERA_ID,
                        "timestamp": timestamp,
                        "imageBase64": img_base64,
                        "embeddingHash": uid
                    }
                    
                    try:
                        headers = {
                            "Content-Type": "application/json",
                            "Authorization": TOKEN
                        }
                        resp = requests.post(ALERTS_URL, json=alert_data, headers=headers)
                        print("🚨 ALERTE ENVOYÉE:", resp.status_code)
                    except Exception as e:
                        print("❌ ERREUR ALERTE:", e)

            # 4. Universal Line Tracking for Unknowns
            person_data = active_persons[uid]
            if person_data.get("line_state") is None:
                if center_x < line_x - margin:
                    person_data["line_state"] = "OUTSIDE"
                elif center_x > line_x + margin:
                    person_data["line_state"] = "INSIDE"
            else:
                curr_ls = person_data["line_state"]
                if curr_ls == "OUTSIDE" and center_x > line_x + margin:
                    print(f"🕵️ UNKNOWN ENTRE (IN): {uid}")
                    send_to_backend(uid, "IN", is_unknown=True)
                    person_data["line_state"] = "INSIDE"
                elif curr_ls == "INSIDE" and center_x < line_x - margin:
                    print(f"🕵️ UNKNOWN SORTIE (OUT): {uid}")
                    send_to_backend(uid, "OUT", is_unknown=True)
                    person_data["line_state"] = "OUTSIDE"

            # Visual feedback on camera
            state_text = f"{person_data['state']} | {person_data.get('line_state', '...')}"
            cv2.putText(frame, f"{uid} [{state_text}]", (x1, y1 - 10),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)


        # =========================
        # KNOWN
        # =========================
        else:
            # 1. Initialize state if person seen for the first time
            if best_match not in employee_states:
                if center_x < line_x - margin:
                    employee_states[best_match] = "OUTSIDE"
                elif center_x > line_x + margin:
                    employee_states[best_match] = "INSIDE"
                # If inside margin, we wait for a clear position to assign initial state
            
            # 2. Transition Logic (State Machine)
            else:
                current_state = employee_states[best_match]
                
                # Passage GAUCHE -> DROITE (ENTRÉE)
                if current_state == "OUTSIDE" and center_x > line_x + margin:
                    print(f"✅ {best_match} ENTRE (IN)")
                    send_to_backend(best_match, "IN")
                    employee_states[best_match] = "INSIDE"
                
                # Passage DROITE -> GAUCHE (SORTIE)
                elif current_state == "INSIDE" and center_x < line_x - margin:
                    print(f"🚪 {best_match} SORTIE (OUT)")
                    send_to_backend(best_match, "OUT")
                    employee_states[best_match] = "OUTSIDE"

            # Display name and current state
            label = f"{best_match} [{employee_states.get(best_match, '...')}]"
            cv2.putText(frame, label, (x1, y1 - 10),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)


        color = (0,255,0) if best_match != "UNKNOWN" else (0,0,255)
        cv2.rectangle(frame,(x1,y1),(x2,y2),color,2)

    # Draw tracking line and margin zones
    cv2.line(frame, (line_x, 0), (line_x, 480), (255, 0, 0), 2)
    cv2.line(frame, (line_x - margin, 0), (line_x - margin, 480), (200, 200, 200), 1)
    cv2.line(frame, (line_x + margin, 0), (line_x + margin, 480), (200, 200, 200), 1)


    cv2.imshow("AI SYSTEM FINAL", frame)

    if cv2.waitKey(1) == 27:
        break

cap.release()
cv2.destroyAllWindows()
