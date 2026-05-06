import requests
import time
import random
from datetime import datetime

URL = "http://localhost:8081/api/ai/events"

# Simulate two cameras: One ENTRY, One INTERNAL
def send_event(cam_id, cam_type, entries, exits, detected):
    data = {
        "cameraId": str(cam_id),
        "cameraType": cam_type,
        "entries": entries,
        "exits": exits,
        "detected": detected,
        "timestamp": datetime.now().isoformat()
    }

    try:
        res = requests.post(URL, json=data, timeout=5)
        print(f"[{datetime.now().strftime('%H:%M:%S')}] Camera {cam_id} ({cam_type}) -> POST {URL}")
        print(f"   Payload: {data}")
        print(f"   Status: {res.status_code}")
    except requests.exceptions.ConnectionError:
        print(f"❌ ERREUR: backend inaccessible sur {URL}. Verifie que Spring Boot tourne.")

if __name__ == "__main__":
    print("🚀 Démarrage du simulateur de caméras AI FARM...")
    
    while True:
        # Simulate ENTRY event (person enters)
        # 30% chance someone enters or exits
        if random.random() < 0.3:
            entries = 1 if random.random() > 0.4 else 0
            exits = 1 if entries == 0 else 0
            send_event(cam_id=1, cam_type="ENTRY", entries=entries, exits=exits, detected=1)
        else:
            # Just send heartbeat
            send_event(cam_id=1, cam_type="ENTRY", entries=0, exits=0, detected=0)

        # Simulate INTERNAL camera
        internal_detected = random.randint(1, 5)
        send_event(cam_id=2, cam_type="INTERNAL", entries=0, exits=0, detected=internal_detected)

        # Wait 2 seconds before next frame analysis
        time.sleep(2)
