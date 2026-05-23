import time
import requests

DEFAULT_TIMEOUT = 5


def build_headers(token=None, content_type=None):
    headers = {}
    if token:
        headers["Authorization"] = token if token.startswith("Bearer ") else f"Bearer {token}"
    if content_type:
        headers["Content-Type"] = content_type
    return headers


def post(url, files=None, json=None, data=None, headers=None, timeout=DEFAULT_TIMEOUT, retries=3, retry_delay=1.0):
    last_exception = None
    for attempt in range(1, retries + 1):
        try:
            response = requests.post(url, files=files, json=json, data=data, headers=headers, timeout=timeout)
            return response
        except requests.RequestException as exc:
            last_exception = exc
            if attempt == retries:
                raise
            time.sleep(retry_delay)
    if last_exception:
        raise last_exception


def safe_upload_frame(url, frame_bytes, camera_id, token=None, timeout=DEFAULT_TIMEOUT, retries=3):
    headers = build_headers(token)
    files = {'file': ('live.jpg', frame_bytes, 'image/jpeg')}
    data = {'cameraId': str(camera_id)}
    return post(url, files=files, data=data, headers=headers, timeout=timeout, retries=retries)


def safe_post_json(url, payload, token=None, timeout=DEFAULT_TIMEOUT, retries=3):
    headers = build_headers(token, content_type='application/json')
    return post(url, json=payload, headers=headers, timeout=timeout, retries=retries)
