import cv2
import numpy as np
from ultralytics import YOLO
import requests
import argparse
import time
from sklearn.cluster import KMeans
from collections import Counter

# =========================
# ARGUMENTS
# =========================
parser = argparse.ArgumentParser()
parser.add_argument("--source", type=str, default="0")
parser.add_argument("--camera_id", type=str, default="1")
parser.add_argument("--type", type=str, default="INTERNAL") # INTERNAL or ENTRY
args = parser.parse_args()

CAMERA_SOURCE = int(args.source) if args.source.isdigit() else args.source
CAMERA_ID = args.camera_id
CAMERA_TYPE = args.type.upper()

# =========================
# CONFIG
# =========================
BACKEND_URL = "http://localhost:8081/api/department-status"
ANALYTICS_URL = "http://localhost:8081/api/ai/events"
model = YOLO("yolov8n.pt") # Ensure this file exists in ai_system

def get_dominant_color(image, k=3):
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    image = image.reshape((image.shape[0] * image.shape[1], 3))
    clt = KMeans(n_clusters=k, n_init=10)
    clt.fit(image)
    
    # Get the most frequent color (ignoring very dark/light if possible, but let's keep it simple)
    counts = Counter(clt.labels_)
    center_colors = clt.cluster_centers_
    ordered_colors = [center_colors[i] for i in counts.keys()]
    
    # Sort by frequency
    dominant = center_colors[counts.most_common(1)[0][0]]
    return dominant

def classify_role(rgb):
    r, g, b = rgb
    
    # Simple thresholding for demonstration
    # White: high R, G, B
    if r > 180 and g > 180 and b > 180:
        return "medecin"
    # Blue: high B, lower R/G
    if b > r + 30 and b > g + 30:
        return "worker"
    # Dark/Black: low R, G, B
    if r < 80 and g < 80 and b < 80:
        return "electricien"
    
    return "unknown"

def send_analytics(detected, recognized, unknown):
    try:
        payload = {
            "cameraId": str(CAMERA_ID),
            "cameraType": CAMERA_TYPE,
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
            "detected": int(detected),
            "recognized": int(recognized),
            "unknown": int(unknown),
            "entries": 0,
            "exits": 0,
            "departmentId": 1
        }
        requests.post(ANALYTICS_URL, json=payload, timeout=1)
    except:
        pass

# =========================
# LOOP
# =========================
cap = cv2.VideoCapture(CAMERA_SOURCE)
last_send_time = 0
send_interval = 5 

# Buffer: {track_id: [roles...]}
track_buffer = {}

print(f"🚀 COLOR AI STARTED ON {CAMERA_ID}")

while True:
    ret, frame = cap.read()
    if not ret: break
    
    results = model.track(frame, persist=True, classes=[0], verbose=False)
    
    has_unknown = False
    
    if results[0].boxes.id is not None:
        boxes = results[0].boxes.xyxy.cpu().numpy().astype(int)
        ids = results[0].boxes.id.cpu().numpy().astype(int)
        
        for box, track_id in zip(boxes, ids):
            x1, y1, x2, y2 = box
            h, w = y2 - y1, x2 - x1
            torso = frame[y1 + int(h*0.2):y1 + int(h*0.5), x1 + int(w*0.25):x1 + int(w*0.75)]
            
            if torso.size > 0:
                dom_color = get_dominant_color(torso)
                role = classify_role(dom_color)
                
                # Add to buffer
                if track_id not in track_buffer: track_buffer[track_id] = []
                track_buffer[track_id].append(role)
                if role == "unknown": has_unknown = True
                
                # Visuals
                color = (0, 255, 0)
                if role == "medecin": color = (255, 255, 255)
                elif role == "worker": color = (255, 0, 0)
                elif role == "electricien": color = (50, 50, 50)
                elif role == "unknown": color = (0, 0, 255)
                cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
                cv2.putText(frame, f"ID:{track_id} {role}", (x1, y1-10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)

    # Aggregation & Sending
    if time.time() - last_send_time > send_interval:
        # For each track_id, pick the most frequent role
        final_roles = []
        for tid, roles in track_buffer.items():
            if roles:
                most_common_role = Counter(roles).most_common(1)[0][0]
                final_roles.append(most_common_role)
        
        counts = Counter(final_roles)
        payload = {
            "cameraId": CAMERA_ID,
            "counts": {
                "medecin": counts.get("medecin", 0),
                "worker": counts.get("worker", 0),
                "electricien": counts.get("electricien", 0)
            },
            "unknown": has_unknown,
            "timestamp": time.time()
        }
        
        try:
            requests.post(BACKEND_URL, json=payload, timeout=2)
            print(f"📤 SENT AGGREGATED STATUS (Tracks: {len(track_buffer)}): {payload['counts']}")
            
            # Also send to analytics
            send_analytics(len(track_buffer), len(track_buffer) - (1 if has_unknown else 0), 1 if has_unknown else 0)
            
        except Exception as e:
            print("❌ SEND ERROR:", e)
            
        last_send_time = time.time()
        track_buffer = {} # Reset buffer

    cv2.imshow(f"COLOR AI - {CAMERA_ID}", frame)
    if cv2.waitKey(1) == 27: break

cap.release()
cv2.destroyAllWindows()
