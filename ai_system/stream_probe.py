import cv2
import sys
import json
import argparse


def open_capture(source):
    try:
        if isinstance(source, str) and source.isdigit():
            index = int(source)
            cap = cv2.VideoCapture(index, cv2.CAP_DSHOW)
            if cap.isOpened():
                return cap
            cap.release()
            return cv2.VideoCapture(index)

        return cv2.VideoCapture(source)
    except Exception:
        return cv2.VideoCapture(source)


def probe_stream(url):
    try:
        source = url.strip()
        cap = open_capture(source)

        if not cap.isOpened():
            return {"healthy": False, "error": "Camera Device Busy or URL Invalid (Could not open VideoCapture)"}

        cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)

        for _ in range(5):
            ret, frame = cap.read()
            if not ret or frame is None:
                cap.release()
                return {"healthy": False, "error": "Could not read frames from source"}

        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        fps = int(cap.get(cv2.CAP_PROP_FPS))
        cap.release()

        return {
            "healthy": True,
            "resolution": f"{width}x{height}",
            "fps": fps if fps > 0 else 30
        }

    except Exception as e:
        return {"healthy": False, "error": str(e)}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", type=str, required=True)
    args = parser.parse_args()

    result = probe_stream(args.url)
    print(json.dumps(result))
