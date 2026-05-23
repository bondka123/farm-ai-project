import cv2
import json
import numpy as np
import sys
import os
import base64
import time
import mysql.connector
from insightface.app import FaceAnalysis
import onnxruntime as ort

# =========================
# CONFIG & GPU DETECTION
# =========================
providers = ort.get_available_providers()
# Requirement 3: GPU Auto Detection
ctx_id = 0 if 'CUDAExecutionProvider' in providers else -1
print(f"DEBUG: Providers: {providers} | Using ctx_id: {ctx_id}", file=sys.stderr)

# Requirement 1: Load InsightFace Model ONLY ONCE
app = FaceAnalysis(name='buffalo_l', providers=providers)
# Requirement 2: Increase Detection Quality (320, 320)
app.prepare(ctx_id=ctx_id, det_size=(320, 320))

# =========================
# DB CONFIG
# =========================
def get_db():
    return mysql.connector.connect(
        host="localhost",
        user="root",
        password="root123",
        database="attendance_db"
    )

# Requirement 11: Cache Registered Embeddings
cached_embeddings = [] # List of (email, embedding)

def refresh_cache():
    global cached_embeddings
    try:
        db = get_db()
        cursor = db.cursor()
        cursor.execute("SELECT email, embedding FROM users WHERE embedding IS NOT NULL AND face_registered = 1")
        rows = cursor.fetchall()
        
        new_cache = []
        for email, emb_str in rows:
            emb = np.array(json.loads(emb_str))
            emb = emb / (np.linalg.norm(emb) + 1e-6)
            new_cache.append((email, emb))
        
        cached_embeddings = new_cache
        db.close()
        print(f"DEBUG: Cache refreshed. {len(cached_embeddings)} users loaded.", file=sys.stderr)
    except Exception as e:
        print(f"ERROR: DB Refresh failed: {e}", file=sys.stderr)

# Requirement 7: Save Debug Frames
DEBUG_DIR = "debug_frames"
os.makedirs(DEBUG_DIR, exist_ok=True)

# Initial load
refresh_cache()

# =========================
# MAIN SERVICE LOOP
# =========================
print("FACE_ENGINE_READY", flush=True)

while True:
    line = sys.stdin.readline()
    if not line:
        break
    
    try:
        data = json.loads(line)
        action = data.get("action")
        
        if action == "refresh":
            refresh_cache()
            print(json.dumps({"status": "success", "message": "cache_refreshed"}), flush=True)
            continue
            
        if action == "identify":
            img_b64 = data.get("image")
            if not img_b64:
                print(json.dumps({"status": "error", "message": "MISSING_IMAGE"}), flush=True)
                continue
                
            # Decode image
            if ',' in img_b64: img_b64 = img_b64.split(',')[1]
            img_data = base64.b64decode(img_b64)
            nparr = np.frombuffer(img_data, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            
            if img is None:
                print(json.dumps({"status": "error", "message": "INVALID_IMAGE"}), flush=True)
                continue
            
            # Requirement 7: Save Debug Frame
            debug_path = os.path.join(DEBUG_DIR, f"frame_{int(time.time()*1000)}.jpg")
            cv2.imwrite(debug_path, img)

            # Start timer for logs (Requirement 17)
            start_time = time.time()
            
            faces = app.get(img)
            proc_time = (time.time() - start_time) * 1000
            
            # Requirement 8: DO NOT return UNKNOWN when no face
            if not faces:
                print(json.dumps({
                    "status": "error", 
                    "message": "NO_FACE",
                    "proc_ms": proc_time
                }), flush=True)
                continue
            
            face = faces[0]
            
            # Requirement 9: Add Face Size Validation
            bbox = face.bbox.astype(int)
            w = bbox[2] - bbox[0]
            if w < 80:
                print(json.dumps({
                    "status": "error", 
                    "message": "FACE_TOO_SMALL",
                    "width": int(w),
                    "proc_ms": proc_time
                }), flush=True)
                continue
            
            # Requirement 10: Improve Matching Logic (Cosine Similarity)
            emb = face.embedding
            emb = emb / (np.linalg.norm(emb) + 1e-6)
            
            # Requirement 2: Verify Login Embeddings (Logs)
            print(f"DEBUG: Face detected. Embedding len: {len(emb)}", file=sys.stderr)
            print(f"DEBUG: First 5 values: {emb[:5]}", file=sys.stderr)

            best_id = None
            max_sim = -1.0
            # Requirement 10: Recommended threshold 0.65 for security
            SIMILARITY_THRESHOLD = 0.65 
            
            for email, known_emb in cached_embeddings:
                sim = np.dot(emb, known_emb)
                if sim > max_sim:
                    max_sim = sim
                    best_id = email
            
            print(f"DEBUG: Best match: {best_id} | Similarity: {max_sim:.4f}", file=sys.stderr)

            if best_id is not None and max_sim >= SIMILARITY_THRESHOLD:
                # Requirement 17: Logs
                print(json.dumps({
                    "status": "success", 
                    "email": best_id,
                    "similarity": float(max_sim),
                    "proc_ms": proc_time
                }), flush=True)
            else:
                # Requirement 8: Only return UNKNOWN if face detected but no match
                print(json.dumps({
                    "status": "error", 
                    "message": "UNKNOWN",
                    "similarity": float(max_sim),
                    "proc_ms": proc_time
                }), flush=True)

    except Exception as e:
        print(json.dumps({"status": "error", "message": str(e)}), flush=True)
