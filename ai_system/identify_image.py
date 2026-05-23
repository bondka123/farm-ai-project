import cv2
import json
import numpy as np
from insightface.app import FaceAnalysis
import mysql.connector
import sys
import base64
import logging
import warnings
import os

# Suppress all
logging.getLogger().setLevel(logging.ERROR)
warnings.filterwarnings('ignore')

def identify(image_base64):
    if not image_base64 or len(image_base64) < 10:
        return

    try:
        # 1. Decode fast
        if ',' in image_base64:
            image_base64 = image_base64.split(',')[1]
        img_data = base64.b64decode(image_base64)
        nparr = np.frombuffer(img_data, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if img is None:
            print("INVALID_IMAGE")
            return

        # 2. IA - Use 'buffalo_l' but with a very small detection size for speed
        # or use 'buffalo_s' if the user has it.
        app = FaceAnalysis(name='buffalo_l', providers=['CPUExecutionProvider'])
        # det_size (160, 160) is much faster than (320, 320)
        app.prepare(ctx_id=-1, det_size=(160, 160))
        
        faces = app.get(img)
        if not faces:
            print("NO_FACE")
            return
            
        face = faces[0]
        emb = face.embedding
        emb = emb / (np.linalg.norm(emb) + 1e-6)
        
        # 3. DB
        db = mysql.connector.connect(
            host="localhost",
            user="root",
            password="root123",
            database="attendance_db"
        )
        cursor = db.cursor()
        cursor.execute("SELECT email, embedding FROM users WHERE embedding IS NOT NULL AND face_registered = 1")
        rows = cursor.fetchall()
        print(f"DEBUG: loaded {len(rows)} registered face embeddings", file=sys.stderr)
        
        best_id = None
        max_sim = -1.0
        SIMILARITY_THRESHOLD = 0.45
        
        if not rows:
            print("NO_USERS")
            db.close()
            return
        
        for email, known_emb_str in rows:
            try:
                known_emb = np.array(json.loads(known_emb_str))
                known_emb = known_emb / (np.linalg.norm(known_emb) + 1e-6)
                similarity = np.dot(emb, known_emb)
                if similarity > max_sim:
                    max_sim = similarity
                    best_id = email
            except Exception as e:
                print(f"DEBUG: failed to parse embedding for {email}: {e}", file=sys.stderr)
                continue
        
        print(f"DEBUG: best match={best_id} max_sim={max_sim:.4f}", file=sys.stderr)
        
        if best_id is not None and max_sim >= SIMILARITY_THRESHOLD:
            print(best_id)
        else:
            print("NO_MATCH")
        
        db.close()
            
    except Exception as e:
        print(f"ERROR: {str(e)}", file=sys.stderr)

if __name__ == "__main__":
    input_data = sys.stdin.read().strip()
    identify(input_data)
