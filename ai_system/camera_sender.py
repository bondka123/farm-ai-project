import cv2
import requests

URL = "http://localhost:8081/api/upload"

camera_id = 1  # ⚠️ METS LE BON ID

cap = cv2.VideoCapture(0)

while True:
    ret, frame = cap.read()
    if not ret:
        break

    cv2.imwrite("temp.jpg", frame)

    with open("temp.jpg", "rb") as f:
        files = {'file': f}
        data = {'cameraId': str(camera_id)}

        try:
            res = requests.post(URL, files=files, data=data, timeout=10)
        except requests.exceptions.ConnectionError:
            print(f"ERREUR: backend inaccessible sur {URL}. Verifie que Spring Boot tourne sur le bon port.")
            break

        print("STATUS:", res.status_code)
        print("REPONSE:", res.text)

    if cv2.waitKey(1000) == 27:
        break

cap.release()
cv2.destroyAllWindows()
