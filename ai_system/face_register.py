import cv2
import sys
import time
import json
import numpy as np
import logging

# Suppress all ONNX warnings and standard logging
logging.getLogger().setLevel(logging.ERROR)
import warnings
warnings.filterwarnings('ignore')

from insightface.app import FaceAnalysis

def open_camera():
    # Try multiple camera indices with DSHOW
    for i in range(3):
        cap = cv2.VideoCapture(i, cv2.CAP_DSHOW)
        time.sleep(0.5) # Give camera time to initialize
        if cap.isOpened():
            print(f"DEBUG: Camera opened at index {i}", file=sys.stderr)
            return cap
        cap.release()
    return None

def main():
    try:
        # Initialize insightface
        print("DEBUG: Loading FaceAnalysis model...", file=sys.stderr)
        app = FaceAnalysis(name='buffalo_l', providers=['CPUExecutionProvider'])
        app.prepare(ctx_id=-1, det_size=(320, 320))

        cap = open_camera()
        if cap is None:
            print("ERROR: Camera initialization failed", file=sys.stderr)
            sys.exit(1)

        print("DEBUG: Capturing frame...", file=sys.stderr)
        start_time = time.time()
        timeout = 15
        embedding = None

        while time.time() - start_time < timeout:
            ret, frame = cap.read()
            if not ret:
                continue
            
            faces = app.get(frame)
            if faces:
                # Get the first face
                face = faces[0]
                emb = face.embedding
                if emb is not None:
                    # Normalize embedding
                    emb = emb / (np.linalg.norm(emb) + 1e-6)
                    embedding = emb.tolist()
                    break

        cap.release()

        if embedding is None:
            print("ERROR: No face detected", file=sys.stderr)
            sys.exit(2)

        # Output exactly one line of JSON (the array) to stdout
        print(json.dumps(embedding))
        sys.exit(0)

    except Exception as e:
        print(f"ERROR: {str(e)}", file=sys.stderr)
        if 'cap' in locals() and cap is not None:
            cap.release()
        sys.exit(3)

if __name__ == "__main__":
    main()
