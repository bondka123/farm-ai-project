import cv2
import json
import numpy as np
from insightface.app import FaceAnalysis
import mysql.connector
import sys
import time
import logging
import warnings

# Suppress all ONNX and InsightFace logging
logging.getLogger().setLevel(logging.ERROR)
warnings.filterwarnings('ignore')

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
    print(f"DATABASE ERROR: {e}", file=sys.stderr)
    sys.exit(1)

# =========================
# IA MODEL
# =========================
app = FaceAnalysis(name='buffalo_l', providers=['CPUExecutionProvider'])
app.prepare(ctx_id=-1, det_size=(320, 320))

# =========================
# LOAD USERS
# =========================
def load_users():
    cursor.execute("SELECT email, embedding FROM users WHERE embedding IS NOT NULL")
    data = cursor.fetchall()
    users = []
    for row in data:
        email = row[0]
        emb = np.array(json.loads(row[1]))
        emb = emb / np.linalg.norm(emb)
        users.append((email, emb))
    return users

users = load_users()
print(f"Loaded {len(users)} users", file=sys.stderr)

# =========================
# OPEN CAMERA (FIXED VERSION)
# =========================
print("SEARCHING - INITIALIZING CAMERA...", file=sys.stderr)

cap = None
for i in range(3):  # try indices 0,1,2
    print(f"Trying camera index {i}...", file=sys.stderr)
    temp = cv2.VideoCapture(i, cv2.CAP_DSHOW)
    time.sleep(1)  # give Windows time to initialize
    if temp.isOpened():
        cap = temp
        print(f"Camera opened at index {i}", file=sys.stderr)
        break

if cap is None or not cap.isOpened():
    print("ERROR_CAMERA", file=sys.stderr)
    sys.exit(1)

# Configure camera to minimize latency
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

# =========================
# TEST CAMERA
# =========================
ret, frame = cap.read()
if not ret or frame is None:
    print("ERROR: Cannot read camera", file=sys.stderr)
    cap.release()
    db.close()
    sys.exit(1)

print("SEARCHING - STARTING FACE RECOGNITION LOGIN...", file=sys.stderr)

# =========================
# RECOGNITION LOOP
# =========================
start_time = time.time()
timeout = 20  # seconds to wait for a matching face

best_id = None
best_dist = float('inf')

while time.time() - start_time < timeout:
    ret, frame = cap.read()
    if not ret:
        continue

    faces = app.get(frame)
    if faces:
        for face in faces:
            emb = face.embedding
            if emb is None:
                continue
            emb = emb / (np.linalg.norm(emb) + 1e-6)
            for email, known_emb in users:
                dist = np.linalg.norm(emb - known_emb)
                if dist < best_dist:
                    best_dist = dist
                    best_id = email
                if dist < 0.6:  # strong match, exit immediately
                    print(best_id)
                    cap.release()
                    db.close()
                    sys.exit(0)
    time.sleep(0.05)

cap.release()

# =========================
# FINAL RESULT
# =========================
if best_id is not None and best_dist < 0.85:
    print(best_id)
    db.close()
    sys.exit(0)

print("NO_MATCH")
db.close()
sys.exit(1)