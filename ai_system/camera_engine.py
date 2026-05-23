import cv2
import time
import threading
import numpy as np
import requests
from datetime import datetime

class CameraEngine:
    def __init__(self, camera_id, source, ai_type, api_url="http://localhost:8081"):
        self.camera_id = camera_id
        self.source = int(source) if str(source).isdigit() else source
        self.ai_type = ai_type
        self.api_url = api_url
        self.running = False
        self.cap = None
        self.thread = None
        self.last_frame = None
        self.frame_width = 0
        self.frame_height = 0
        self.line_x = 0
        self.margin = 30
        
    def initialize_stream(self):
        print(f"🔄 Initializing Stream for Camera {self.camera_id} (Source: {self.source})")
        self.cap = cv2.VideoCapture(self.source)
        if not self.cap.isOpened():
            print(f"❌ Failed to open stream for Camera {self.camera_id}")
            return False
        
        self.frame_width = int(self.cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        self.frame_height = int(self.cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        
        # AUTO 50% LINE
        if self.frame_width > 0:
            self.line_x = self.frame_width // 2
        else:
            self.line_x = 320 # Fallback
            
        return True

    def start(self):
        if self.running:
            return
        
        if not self.initialize_stream():
            return
        
        self.running = True
        self.thread = threading.Thread(target=self._run_loop, daemon=True)
        self.thread.start()
        print(f"✅ Camera {self.camera_id} AI started ({self.ai_type})")

    def stop(self):
        self.running = False
        if self.thread:
            self.thread.join(timeout=2)
        if self.cap:
            self.cap.release()
        print(f"🛑 Camera {self.camera_id} AI stopped")

    def _run_loop(self):
        while self.running:
            ret, frame = self.cap.read()
            if not ret:
                print(f"⚠️ Stream lost for Camera {self.camera_id}. Retrying...")
                time.sleep(2)
                self.initialize_stream()
                continue
            
            # Process frame
            processed_frame = self.process_frame(frame)
            self.last_frame = processed_frame
            
            # Upload frame for realtime dashboard (throttled)
            self.share_realtime_frame(processed_frame)
            
            # Subclasses will implement specific AI logic and data sync
            
        if self.cap:
            self.cap.release()

    def process_frame(self, frame):
        # Override in subclasses
        return frame

    def share_realtime_frame(self, frame):
        # Implementation for uploading frame to backend
        pass

    def send_event(self, sub_path, data):
        try:
            payload = {
                "cameraId": self.camera_id,
                "timestamp": datetime.now().isoformat(),
                **data
            }
            requests.post(f"{self.api_url}/api/ai/events/{sub_path}", json=payload, timeout=2)
        except Exception as e:
            print(f"❌ Error sending event to {sub_path}: {e}")
