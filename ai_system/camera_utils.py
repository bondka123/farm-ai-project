import cv2
import time

LOCAL_KEYWORDS = {"local", "webcam", "cam", "camera"}


def detect_source_type(source):
    if source is None:
        return "UNKNOWN"

    source_text = str(source).strip().lower()
    if source_text.isdigit() or source_text in LOCAL_KEYWORDS:
        return "LOCAL"
    if source_text.startswith("rtsp://"):
        return "RTSP"
    if source_text.startswith("http://"):
        return "HTTP"
    if source_text.startswith("https://"):
        return "HTTPS"
    return "UNKNOWN"


def open_camera(source, width=1280, height=720, buffer_size=1, warmup_frames=5, timeout_seconds=10):
    source_text = str(source).strip()
    source_candidate = int(source_text) if source_text.isdigit() else source_text
    source_type = detect_source_type(source_text)
    backends = []

    if source_type == "LOCAL":
        if hasattr(cv2, 'CAP_DSHOW'):
            backends.append(cv2.CAP_DSHOW)
        backends.append(cv2.CAP_ANY)
    else:
        if hasattr(cv2, 'CAP_FFMPEG'):
            backends.append(cv2.CAP_FFMPEG)
        backends.append(cv2.CAP_ANY)

    last_error = None
    for backend in backends:
        try:
            if backend is not None:
                cap = cv2.VideoCapture(source_candidate, backend)
            else:
                cap = cv2.VideoCapture(source_candidate)

            if not cap.isOpened():
                cap.release()
                continue

            cap.set(cv2.CAP_PROP_BUFFERSIZE, buffer_size)
            cap.set(cv2.CAP_PROP_FRAME_WIDTH, width)
            cap.set(cv2.CAP_PROP_FRAME_HEIGHT, height)

            connected = False
            start = time.time()
            while time.time() - start < timeout_seconds and not connected:
                ret, frame = cap.read()
                if ret and frame is not None:
                    connected = True
                    break
                time.sleep(0.1)

            if connected:
                return cap

            cap.release()
        except Exception as exc:
            last_error = exc

    raise RuntimeError(f"Unable to open camera source '{source}'{': ' + str(last_error) if last_error else ''}")


def validate_camera_id(camera_id):
    try:
        import mysql.connector

        conn = mysql.connector.connect(
            host="localhost",
            user="root",
            password="root123",
            database="attendance_db"
        )
        cursor = conn.cursor()
        cursor.execute("SELECT id, name, ai_type, url FROM cameras ORDER BY id")
        rows = cursor.fetchall()
        cursor.close()
        conn.close()

        if any(int(row[0]) == int(camera_id) for row in rows):
            return True

        print(f"[ERROR] Camera ID {camera_id} not found in database.")
        if rows:
            print("[INFO] Available cameras:")
            for cam_id, name, ai_type, url in rows:
                print(f"  - ID {cam_id}: {name} | type={ai_type} | source={url}")
        else:
            print("[INFO] No cameras found. Create a camera from the dashboard first.")
        return False
    except Exception as exc:
        print(f"[WARN] Cannot validate camera ID before start: {exc}")
        return True
