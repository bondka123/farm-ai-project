import cv2
import json
import numpy as np
import mysql.connector
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from insightface.app import FaceAnalysis
import uvicorn
import os

app_fastapi = FastAPI(title="AI Security Face API")

# Enable CORS for Spring Boot calls if needed (though usually server-to-server)
app_fastapi.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# =========================
# DB CONNECTION
# =========================
def get_db():
    return mysql.connector.connect(
        host="localhost",
        user="root",
        password="root123",
        database="attendance_db"
    )

# =========================
# IA MODEL (BUFFALO_L)
# =========================
print("🔄 Loading InsightFace model...")
face_app = FaceAnalysis(name='buffalo_l')
face_app.prepare(ctx_id=0, det_size=(320, 320))
print("✅ Model loaded")

# =========================
# UTILS
# =========================
def load_all_embeddings():
    db = get_db()
    cursor = db.cursor()
    cursor.execute("SELECT id, embedding FROM employees WHERE embedding IS NOT NULL")
    data = cursor.fetchall()
    
    embeddings = []
    for row in data:
        emp_id = row[0]
        try:
            emb = np.array(json.loads(row[1]))
            emb = emb / np.linalg.norm(emb)
            embeddings.append((emp_id, emb))
        except:
            continue
    
    cursor.close()
    db.close()
    return embeddings

def is_duplicate(new_emb, db_embeddings, threshold=0.6):
    for emp_id, emb in db_embeddings:
        dist = np.linalg.norm(new_emb - emb)
        print(f"🔍 Checking duplicate: dist={dist:.3f}")
        if dist < threshold:
            return True, emp_id
    return False, None

# =========================
# ENDPOINTS
# =========================

@app_fastapi.post("/api/face/register")
def register_face(employeeId: int):
    print(f"📥 RECEIVED REGISTRATION REQUEST FOR ID: {employeeId}")
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        raise HTTPException(status_code=500, detail="Could not open camera")

    print(f"👉 REGISTERING FACE FOR ID: {employeeId}")
    
    face_registered = False
    message = ""
    status = "error"

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        faces = face_app.get(frame)
        
        if len(faces) > 0:
            face = faces[0]
            x1, y1, x2, y2 = map(int, face.bbox)
            cv2.rectangle(frame, (x1, y1), (x2, y2), (156, 39, 176), 2) # Purple color
            cv2.putText(frame, "Press 'S' to Capture", (x1, y1 - 10), 
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (156, 39, 176), 2)
        else:
            cv2.putText(frame, "NO FACE DETECTED", (20, 40), 
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
        
        cv2.imshow("Register Face - AI Security", frame)
        key = cv2.waitKey(1) & 0xFF

        if key == ord('s'):
            if len(faces) == 0:
                print("❌ No face detected")
                continue
            
            new_emb = faces[0].embedding
            new_emb = new_emb / np.linalg.norm(new_emb)
            
            db_embeddings = load_all_embeddings()
            duplicate, existing_id = is_duplicate(new_emb, db_embeddings)

            if duplicate and existing_id != employeeId:
                message = f"Face already exists for employee ID {existing_id}"
                status = "duplicate"
                print(f"❌ {message}")
                break 
            
            try:
                db = get_db()
                cursor = db.cursor()
                
                # Update employee
                cursor.execute(
                    "UPDATE employees SET embedding=%s, face_registered=1 WHERE id=%s",
                    (json.dumps(new_emb.tolist()), employeeId)
                )
                
                # Get email to update user table
                cursor.execute("SELECT email FROM employees WHERE id=%s", (employeeId,))
                result = cursor.fetchone()
                if result:
                    email = result[0]
                    cursor.execute(
                        "UPDATE users SET face_registered=1 WHERE email=%s",
                        (email,)
                    )
                
                db.commit()
                cursor.close()
                db.close()
                face_registered = True
                status = "success"
                message = "Face registered successfully"
                break
            except Exception as e:
                print(f"❌ DB ERROR: {e}")
                message = f"Database error: {str(e)}"
                break

        if key == 27: # ESC
            message = "Registration cancelled by user"
            break

    cap.release()
    cv2.destroyAllWindows()
    
    return {"status": status, "message": message, "employeeId": employeeId}

@app_fastapi.post("/api/face/recognize")
def recognize_face():
    print("📥 RECEIVED RECOGNITION REQUEST")
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        raise HTTPException(status_code=500, detail="Could not open camera")

    db_embeddings = load_all_embeddings()
    result = {"employeeId": None, "confidence": 0.0, "status": "no_face"}

    # Timeout after 10 seconds of no face or no match
    import time
    start_time = time.time()

    while time.time() - start_time < 15:
        ret, frame = cap.read()
        if not ret:
            break

        faces = face_app.get(frame)
        if len(faces) > 0:
            face = faces[0]
            x1, y1, x2, y2 = map(int, face.bbox)
            cv2.rectangle(frame, (x1, y1), (x2, y2), (156, 39, 176), 2)
            
            emb = face.embedding
            emb = emb / np.linalg.norm(emb)
            
            best_match = None
            min_dist = 999
            
            for emp_id, known_emb in db_embeddings:
                dist = np.linalg.norm(emb - known_emb)
                if dist < min_dist:
                    min_dist = dist
                    best_match = emp_id
            
            # Confidence score: 1 - dist/1.5 (approximate)
            confidence = max(0, 1 - (min_dist / 1.5))
            
            if min_dist < 1.0: # Match threshold
                result = {
                    "employeeId": best_match, 
                    "confidence": round(confidence, 2),
                    "status": "success"
                }
                break
            else:
                cv2.putText(frame, "UNKNOWN PERSON", (x1, y1 - 10), 
                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
                result["status"] = "unknown"
        else:
            cv2.putText(frame, "SCANNING...", (20, 40), 
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (156, 39, 176), 2)
        
        cv2.imshow("Face Authentication - Scan", frame)
        if cv2.waitKey(1) == 27:
            result["status"] = "cancelled"
            break

    cap.release()
    cv2.destroyAllWindows()
    return result

@app_fastapi.delete("/api/face/delete/{employeeId}")
def delete_face(employeeId: int):
    try:
        db = get_db()
        cursor = db.cursor()
        
        # Get email
        cursor.execute("SELECT email FROM employees WHERE id=%s", (employeeId,))
        result = cursor.fetchone()
        
        # Reset employee
        cursor.execute(
            "UPDATE employees SET embedding=NULL, face_registered=0 WHERE id=%s",
            (employeeId,)
        )
        
        if result:
            email = result[0]
            # Reset user
            cursor.execute(
                "UPDATE users SET face_registered=0 WHERE email=%s",
                (email,)
            )
            
        db.commit()
        cursor.close()
        db.close()
        return {"status": "success", "message": "Face data deleted"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"DB Error: {str(e)}")

if __name__ == "__main__":
    uvicorn.run(app_fastapi, host="0.0.0.0", port=8000)
