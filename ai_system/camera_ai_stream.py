import cv2
import numpy as np
import json
import requests
import mysql.connector
import time
import os
import argparse
from insightface.app import FaceAnalysis

# =========================
# ARGUMENTS
# =========================
parser = argparse.ArgumentParser()
parser.add_argument("--source", type=str, default="0", help="Camera source (0, 1, or URL)")
parser.add_argument("--camera_id", type=int, default=1, help="Camera ID in DB")
args = parser.parse_args()

BACKEND_UPLOAD_URL = "http://localhost:8081/api/upload"
CAMERA_ID = args.camera_id
SOURCE = args.source

if str(SOURCE).isdigit():
    SOURCE = int(SOURCE)

# =========================
# DB CONNECTION
# =========================
db = mysql.connector.connect(
    host="localhost",
    user="root",
    password="root123",
    database="attendance_db"
)
cursor = db.cursor()

# =========================
# IA MODEL
# =========================
print(f"🔄 Chargement du modèle IA pour Caméra {CAMERA_ID} (Source: {SOURCE})...")
app = FaceAnalysis(name='buffalo_l')
app.prepare(ctx_id=0, det_size=(320,320))

# =========================
# CHARGER LES EMPLOYÉS
# =========================
def load_employees():
    cursor.execute("SELECT name, embedding FROM employees WHERE embedding IS NOT NULL")
    data = cursor.fetchall()
    employees = {}
    for name, emb_json in data:
        emb = np.array(json.loads(emb_json))
        emb = emb / np.linalg.norm(emb)
        employees[name] = emb
    return employees

employees = load_employees()

# =========================
# CAPTURE VIDÉO UNIVERSELLE
# =========================
cap = cv2.VideoCapture(SOURCE)

if not cap.isOpened():
    print(f"❌ ERREUR: Impossible d'ouvrir la source: {SOURCE}")
    exit()

print(f"🚀 SYSTÈME IA DÉMARRÉ SUR SOURCE: {SOURCE}")

last_upload_time = 0
UPLOAD_INTERVAL = 1.0 

while True:
    ret, frame = cap.read()
    if not ret:
        print("⚠️ Flux interrompu ou terminé.")
        break

    display_frame = cv2.resize(frame, (640, 480))
    
    faces = app.get(display_frame)

    for face in faces:
        emb = face.embedding
        emb = emb / np.linalg.norm(emb)

        best_name = "INCONNU"
        min_dist = 999

        for name, known_emb in employees.items():
            dist = np.linalg.norm(emb - known_emb)
            if dist < min_dist:
                min_dist = dist
                best_name = name

        if min_dist > 1.1:
            best_name = "INCONNU"

        x1, y1, x2, y2 = map(int, face.bbox)
        color = (0, 255, 0) if best_name != "INCONNU" else (0, 0, 255)
        
        cv2.rectangle(display_frame, (x1, y1), (x2, y2), color, 2)
        cv2.putText(display_frame, best_name, (x1, y1 - 10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, color, 2)

    # 🎥 ENVOI VERS DASHBOARD
    current_time = time.time()
    if current_time - last_upload_time > UPLOAD_INTERVAL:
        cv2.imwrite("temp_stream.jpg", display_frame)
        
        try:
            with open("temp_stream.jpg", "rb") as f:
                files = {'file': f}
                data = {'cameraId': str(CAMERA_ID)}
                requests.post(BACKEND_UPLOAD_URL, files=files, data=data, timeout=5)
                last_upload_time = current_time
        except Exception:
            pass

    # Optionnel: cv2.imshow si besoin en local
    # cv2.imshow(f"Cam {CAMERA_ID}", display_frame)
    if cv2.waitKey(1) == 27:
        break

cap.release()
cv2.destroyAllWindows()
cursor.close()
db.close()
