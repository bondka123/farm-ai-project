import cv2
import json
import numpy as np
from insightface.app import FaceAnalysis
import mysql.connector
import sys
import time
import platform
import threading

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
app = FaceAnalysis(name='buffalo_l')
app.prepare(ctx_id=-1, det_size=(320, 320))


def open_camera(index=0):
    cap = cv2.VideoCapture(index)
    if cap.isOpened():
        return cap

    if platform.system() == 'Windows':
        for api in [cv2.CAP_DSHOW, cv2.CAP_MSMF, cv2.CAP_VFW]:
            try:
                cap = cv2.VideoCapture(index, api)
                if cap.isOpened():
                    return cap
            except Exception:
                pass

    return cap


def load_employees():
    cursor.execute("SELECT id, embedding FROM employees WHERE embedding IS NOT NULL")
    data = cursor.fetchall()
    employees = []
    for row in data:
        emp_id = row[0]
        emb = np.array(json.loads(row[1]))
        emb = emb / np.linalg.norm(emb)
        employees.append((emp_id, emb))
    return employees

employees = load_employees()
print(f"Loaded {len(employees)} employees", file=sys.stderr)

# =========================
# RECOGNITION
# =========================
print("SEARCHING - INITIALIZING CAMERA...", file=sys.stderr)

# Try to open camera with timeout
camera_opened = threading.Event()
camera_obj = [None]

def open_camera_thread():
    try:
        cap = open_camera(0)
        camera_obj[0] = cap
        camera_opened.set()
    except Exception as e:
        print(f"ERROR: Camera open failed: {e}", file=sys.stderr)
        camera_opened.set()

thread = threading.Thread(target=open_camera_thread, daemon=True)
thread.start()
thread.join(timeout=5)  # Wait 5 seconds for camera to open

if not camera_opened.is_set():
    print("ERROR: Camera initialization timeout", file=sys.stderr)
    sys.exit(1)

cap = camera_obj[0]
if cap is None or not cap.isOpened():
    print("ERROR_CAMERA", file=sys.stderr)
    sys.exit(1)

# Set camera properties to avoid hangs
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)  # Minimize buffer size
cap.set(cv2.CAP_PROP_FPS, 30)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

# Try to read a test frame
print("SEARCHING - WARMING UP CAMERA...", file=sys.stderr)
frame = None
for attempt in range(10):
    ret, frame = cap.read()
    if ret and frame is not None:
        break
    time.sleep(0.1)

if frame is None:
    print("ERROR: Cannot read from camera", file=sys.stderr)
    cap.release()
    db.close()
    sys.exit(1)

frame_min = np.min(frame)
frame_max = np.max(frame)
frame_mean = np.mean(frame)
print(f"DEBUG: Camera ready - frame shape={frame.shape}, dtype={frame.dtype}, min={frame_min}, max={frame_max}, mean={frame_mean:.1f}", file=sys.stderr)

start_time = time.time()
timeout = 20  # 20 seconds to recognize

print("SEARCHING - STARTING FACE RECOGNITION LOGIN...", file=sys.stderr)

frame_count = 0
best_overall_id = None
best_overall_dist = float('inf')
debug_interval = 5  # Print debug every 5 frames

while time.time() - start_time < timeout:
    ret, frame = cap.read()
    frame_count += 1
    
    if not ret or frame is None:
        print("ERROR_CAMERA_READ", file=sys.stderr)
        break

    # Debug frame validity every 5 frames
    if frame_count % debug_interval == 1:
        frame_min = np.min(frame)
        frame_max = np.max(frame)
        frame_mean = np.mean(frame)
        print(f"DEBUG: Frame {frame_count} - min={frame_min}, max={frame_max}, mean={frame_mean:.1f}", file=sys.stderr)

    # Try to detect faces
    try:
        faces = app.get(frame)
    except Exception as e:
        print(f"ERROR: Face detection failed - {e}", file=sys.stderr)
        break

    if faces is not None and len(faces) > 0:
        if frame_count % debug_interval == 1:
            print(f"DEBUG: Frame {frame_count} - Found {len(faces)} face(s)", file=sys.stderr)
        
        # Process each detected face
        for face in faces:
            try:
                # Skip faces with very low confidence (if available)
                emb = face.embedding
                if emb is None or len(emb) == 0:
                    continue
                    
                # Normalize embedding
                emb = emb / (np.linalg.norm(emb) + 1e-6)

                # Compare with all known employees
                for emp_id, known_emb in employees:
                    dist = np.linalg.norm(emb - known_emb)
                    # Keep track of best match ever found
                    if dist < 1.0 and dist < best_overall_dist:
                        best_overall_dist = dist
                        best_overall_id = emp_id
                        print(f"DEBUG: Found better match - ID={emp_id}, distance={dist:.4f}", file=sys.stderr)
                        # If we find a very good match, exit immediately
                        if dist < 0.6:
                            print(best_overall_id)
                            cap.release()
                            db.close()
                            sys.exit(0)
            except Exception as e:
                print(f"ERROR: Face processing failed - {e}", file=sys.stderr)
                continue
    else:
        if frame_count % debug_interval == 1:
            print(f"DEBUG: Frame {frame_count} - No faces detected", file=sys.stderr)

    time.sleep(0.1)

cap.release()

# If we found a reasonable match (within tolerance), accept it
if best_overall_id is not None and best_overall_dist < 0.85:
    print(f"DEBUG: Accepting match - ID={best_overall_id}, distance={best_overall_dist:.4f}", file=sys.stderr)
    print(best_overall_id)
    db.close()
    sys.exit(0)

print(f"NO_MATCH_FOUND (frames={frame_count}, best_dist={best_overall_dist:.4f})", file=sys.stderr)
db.close()
sys.exit(1)
