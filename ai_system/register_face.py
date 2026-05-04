import cv2
import json
import numpy as np
from insightface.app import FaceAnalysis
import mysql.connector
import sys  # 🔥 IMPORTANT

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
app = FaceAnalysis(name='buffalo_l')
app.prepare(ctx_id=-1, det_size=(320, 320))

# =========================
# LOAD EMBEDDINGS
# =========================
def load_all_embeddings():
    cursor.execute("SELECT id, embedding FROM employees WHERE embedding IS NOT NULL")
    data = cursor.fetchall()

    embeddings = []

    for row in data:
        emp_id = row[0]
        emb = np.array(json.loads(row[1]))
        emb = emb / np.linalg.norm(emb)
        embeddings.append((emp_id, emb))

    return embeddings

# =========================
# CHECK DUPLICATE
# =========================
def is_duplicate(new_emb, db_embeddings, threshold=0.6):
    for emp_id, emb in db_embeddings:
        dist = np.linalg.norm(new_emb - emb)

        print(f"DEBUG: compare avec ID {emp_id} -> distance = {dist:.3f}")

        if dist < threshold:
            return True, emp_id

    return False, None

# =========================
# INPUT USER (AUTO + MANUEL)
# =========================
if len(sys.argv) > 1:
    emp_id = sys.argv[1]
    print(f"OK: Employee ID recu automatiquement: {emp_id}")
else:
    emp_id = input("ENTER Employee ID: ")

# =========================
# CAMERA
# =========================
cap = cv2.VideoCapture(0)

print("PRESS S to capture face | ESC to exit")

while True:
    ret, frame = cap.read()

    if not ret:
        print("ERROR: Camera error")
        break

    faces = app.get(frame)

    if len(faces) > 0:
        face = faces[0]
        x1, y1, x2, y2 = map(int, face.bbox)

        cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
        cv2.putText(frame, f"FACE DETECTED (ID={emp_id})", (x1, y1 - 10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
    else:
        cv2.putText(frame, "NO FACE", (20, 50),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)

    cv2.imshow("Register Face", frame)

    key = cv2.waitKey(1) & 0xFF

    # =========================
    # PRESS S
    # =========================
    if key == ord('s'):

        if len(faces) == 0:
            print("ERROR: No face detected")
            continue

        # 🔥 embedding
        new_emb = faces[0].embedding
        new_emb = new_emb / np.linalg.norm(new_emb)

        # 🔥 load DB
        db_embeddings = load_all_embeddings()

        # 🔥 check duplicate
        duplicate, existing_id = is_duplicate(new_emb, db_embeddings)

        if duplicate:
            print(f"ERROR: FACE ALREADY EXISTS (ID={existing_id})")
            continue

        try:
            # =========================
            # UPDATE EMPLOYEE
            # =========================
            cursor.execute(
                "UPDATE employees SET embedding=%s, face_registered=1 WHERE id=%s",
                (json.dumps(new_emb.tolist()), emp_id)
            )

            # =========================
            # GET USERNAME
            # =========================
            cursor.execute(
                "SELECT name FROM employees WHERE id=%s",
                (emp_id,)
            )
            result = cursor.fetchone()

            if result:
                username = result[0]

                # =========================
                # UPDATE USER
                # =========================
                cursor.execute(
                    "UPDATE users SET face_registered=1 WHERE email=%s",
                    (username,)
                )

            db.commit()

            print("SUCCESS: FACE REGISTERED SUCCESSFULLY")
            print("SUCCESS: USER ACTIVATED")

            break

        except Exception as e:
            print("ERROR: DB ERROR:", e)

    # =========================
    # ESC
    # =========================
    if key == 27:
        break

cap.release()
cv2.destroyAllWindows()