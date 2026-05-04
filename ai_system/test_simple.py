import cv2
print(f"OpenCV version: {cv2.__version__}")
print("Attempting to open camera 0...")
cap = cv2.VideoCapture(0)
print(f"Camera opened: {cap.isOpened()}")
if cap.isOpened():
    print("Setting timeout and attempting read...")
    cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
    print("About to read frame...")
    ret, frame = cap.read()
    print(f"Read result: ret={ret}, frame is None={frame is None}")
    if frame is not None:
        print(f"Frame shape: {frame.shape}")
    cap.release()
print("Done")
