import cv2
import numpy as np
import json
import os
import base64
from datetime import datetime
from insightface.app import FaceAnalysis
import mysql.connector
from camera_engine import CameraEngine

class FaceEngine(CameraEngine):
    def __init__(self, camera_id, source, api_url="http://localhost:8081"):
        super().__init__(camera_id, source, "FACE", api_url)
        print("🔄 Loading FaceRecognition Engine (InsightFace)...")
        self.face_app = FaceAnalysis(name='buffalo_l')
        self.face_app.prepare(ctx_id=0, det_size=(320, 320))
        self.employees = self._load_employees()
        self.employee_states = {}
        self.unknown_folder = "unknown_faces"
        os.makedirs(self.unknown_folder, exist_ok=True)

    def _load_employees(self):
        try:
            db = mysql.connector.connect(
                host="localhost", user="root", password="root123", database="attendance_db"
            )
            cursor = db.cursor()
            cursor.execute("SELECT name, embedding FROM employees WHERE embedding IS NOT NULL")
            data = cursor.fetchall()
            employees = {}
            for name, emb_json in data:
                emb = np.array(json.loads(emb_json))
                emb = emb / np.linalg.norm(emb)
                employees[name] = emb
            cursor.close()
            db.close()
            return employees
        except Exception as e:
            print(f"❌ Error loading employees: {e}")
            return {}

    def process_frame(self, frame):
        # Resize for performance
        display_frame = cv2.resize(frame, (640, 480))
        # Recalculate line for resized frame
        line_x = 320
        
        faces = self.face_app.get(display_frame)
        
        for face in faces:
            emb = face.embedding / np.linalg.norm(face.embedding)
            best_name = "UNKNOWN"
            min_dist = 1.1 # Threshold

            for name, known_emb in self.employees.items():
                dist = np.linalg.norm(emb - known_emb)
                if dist < min_dist:
                    min_dist = dist
                    best_name = name

            x1, y1, x2, y2 = map(int, face.bbox)
            center_x = (x1 + x2) // 2
            
            color = (0, 255, 0) if best_name != "UNKNOWN" else (0, 0, 255)
            
            # Tracking Logic (IN/OUT)
            if best_name != "UNKNOWN":
                if best_name not in self.employee_states:
                    self.employee_states[best_name] = "INSIDE" if center_x > line_x else "OUTSIDE"
                else:
                    curr = self.employee_states[best_name]
                    if curr == "OUTSIDE" and center_x > line_x + self.margin:
                        self.send_event("face", {"employeeName": best_name, "status": "IN"})
                        self.employee_states[best_name] = "INSIDE"
                    elif curr == "INSIDE" and center_x < line_x - self.margin:
                        self.send_event("face", {"employeeName": best_name, "status": "OUT"})
                        self.employee_states[best_name] = "OUTSIDE"
                
                label = f"{best_name} [{self.employee_states[best_name]}]"
            else:
                label = "UNKNOWN PERSON"
                # Handle unknown (crop and save)
                # (Implementation for unknown detection history)
            
            cv2.rectangle(display_frame, (x1, y1), (x2, y2), color, 2)
            cv2.putText(display_frame, label, (x1, y1 - 10), 
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)

        # Draw line
        cv2.line(display_frame, (line_x, 0), (line_x, 480), (255, 0, 0), 2)
        return display_frame
