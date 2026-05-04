import cv2
import time

print("Testing camera availability...")

# Try different camera indices
for i in range(5):
    print(f"\nTrying camera index {i}...")
    cap = cv2.VideoCapture(i)
    
    if cap.isOpened():
        print(f"  Camera {i} opened successfully")
        
        # Try to read with timeout
        for attempt in range(5):
            ret, frame = cap.read()
            if ret and frame is not None:
                print(f"  Successfully read frame: shape={frame.shape}, dtype={frame.dtype}")
                break
            else:
                print(f"  Attempt {attempt+1}: read failed, retrying...")
                time.sleep(0.2)
        
        cap.release()
    else:
        print(f"  Camera {i} failed to open")

print("\n\nTesting with different APIs on Windows...")
try:
    import platform
    if platform.system() == 'Windows':
        for api_name, api_id in [('DSHOW', cv2.CAP_DSHOW), ('MSMF', cv2.CAP_MSMF)]:
            print(f"\nTrying with {api_name} API (ID={api_id})...")
            cap = cv2.VideoCapture(0, api_id)
            if cap.isOpened():
                ret, frame = cap.read()
                if ret:
                    print(f"  SUCCESS with {api_name}: frame={frame.shape}")
                else:
                    print(f"  Opened but can't read with {api_name}")
                cap.release()
            else:
                print(f"  Failed to open with {api_name}")
except Exception as e:
    print(f"Error: {e}")
