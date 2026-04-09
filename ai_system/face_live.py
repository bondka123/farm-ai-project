import cv2
import numpy as np
import json
from insightface.app import FaceAnalysis
import mysql.connector
from datetime import datetime
import time
import os
import requests

# =========================
# BACKEND CONFIG
# =========================
BACKEND_URL = "http://localhost:8080/api/attendance"

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
positions = {}
last_action = {}
cooldown = 3

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
        # UNKNOWN
        # =========================
        if best_match == "UNKNOWN":

            uid = f"UNKNOWN_{int(time.time())}"

            print(f"🆕 UNKNOWN DETECTED: {uid}")

            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"{UNKNOWN_FOLDER}/{uid}_{timestamp}.jpg"

            face_img = frame[y1:y2, x1:x2]
            if face_img.size != 0:
                cv2.imwrite(filename, face_img)

            send_to_backend(uid, "DETECTED", True, filename)

            cv2.putText(frame, uid, (x1,y1-10),
                        cv2.FONT_HERSHEY_SIMPLEX,0.8,(0,0,255),2)

        # =========================
        # KNOWN
        # =========================
        else:

            if best_match in positions:
                old_x = positions[best_match]

                if best_match not in last_action or current_time - last_action[best_match] > cooldown:

                    if old_x < line_x and center_x > line_x:
                        print(f"{best_match} ENTRE")
                        send_to_backend(best_match, "IN")
                        last_action[best_match] = current_time

                    elif old_x > line_x and center_x < line_x:
                        print(f"{best_match} SORTIE")
                        send_to_backend(best_match, "OUT")
                        last_action[best_match] = current_time

            positions[best_match] = center_x

            cv2.putText(frame, best_match, (x1,y1-10),
                        cv2.FONT_HERSHEY_SIMPLEX,0.8,(0,255,0),2)

        color = (0,255,0) if best_match != "UNKNOWN" else (0,0,255)
        cv2.rectangle(frame,(x1,y1),(x2,y2),color,2)

    cv2.line(frame,(line_x,0),(line_x,480),(255,0,0),2)

    cv2.imshow("AI SYSTEM FINAL", frame)

    if cv2.waitKey(1) == 27:
        break

cap.release()
cv2.destroyAllWindows()