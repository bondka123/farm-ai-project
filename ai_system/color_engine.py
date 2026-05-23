import cv2
import numpy as np
from ultralytics import YOLO
from deep_sort_realtime.deepsort_tracker import DeepSort
from collections import Counter
from camera_engine import CameraEngine

class ColorEngine(CameraEngine):
    def __init__(self, camera_id, source, api_url="http://localhost:8081"):
        super().__init__(camera_id, source, "COLOR", api_url)
        print("🔄 Loading Role Detection Engine (YOLOv8 + DeepSort)...")
        self.model = YOLO("yolov8n.pt")
        self.tracker = DeepSort(max_age=30)
        self.department_config = self._load_department_config()

    def _load_department_config(self):
        # Fetch department requirements from backend
        return {}

    def detect_role(self, person_img):
        hsv = cv2.cvtColor(person_img, cv2.COLOR_BGR2HSV)
        avg = np.mean(hsv, axis=(0, 1))
        h, s, v = avg
        if v > 200 and s < 40: return "medecin"
        if 90 < h < 140: return "employee"
        if v < 100: return "electricien"
        return "unknown"

    def process_frame(self, frame):
        results = self.model(frame, verbose=False)
        detections = []
        for r in results:
            for box in r.boxes:
                cls = int(box.cls[0])
                if self.model.names[cls] == "person":
                    x1, y1, x2, y2 = map(int, box.xyxy[0])
                    detections.append(([x1, y1, x2-x1, y2-y1], 1.0, "person"))

        tracks = self.tracker.update_tracks(detections, frame=frame)
        roles = []
        
        for track in tracks:
            if not track.is_confirmed(): continue
            x1, y1, x2, y2 = map(int, track.to_ltrb())
            person = frame[max(0,y1):min(frame.shape[0],y2), max(0,x1):min(frame.shape[1],x2)]
            if person.size == 0: continue
            
            # Crop middle part for color detection
            h = person.shape[0]
            cloth = person[int(h*0.3):int(h*0.7), :]
            if cloth.size == 0: continue
            
            role = self.detect_role(cloth)
            roles.append(role)
            
            color = (0, 255, 0)
            cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
            cv2.putText(frame, role.upper(), (x1, y1 - 10), 
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

        counts = Counter(roles)
        # Validation logic against department_config would go here
        
        return frame
