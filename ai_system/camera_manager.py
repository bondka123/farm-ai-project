import sys
import argparse
from face_engine import FaceEngine
from color_engine import ColorEngine

def main():
    parser = argparse.ArgumentParser(description="FARM AI Camera Instance")
    parser.add_argument("--camera_id", type=int, required=True)
    parser.add_argument("--source", type=str, required=True)
    parser.add_argument("--mode", type=str, choices=["FACE", "COLOR"], required=True)
    args = parser.parse_args()

    print(f"🚀 Starting Camera {args.camera_id} in {args.mode} mode...")
    
    if args.mode == "FACE":
        engine = FaceEngine(args.camera_id, args.source)
    else:
        engine = ColorEngine(args.camera_id, args.source)

    try:
        engine.start()
        # Keep main thread alive
        import time
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        engine.stop()
        print("Bye!")

if __name__ == "__main__":
    main()
