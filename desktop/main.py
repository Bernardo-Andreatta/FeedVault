import socket
import sys
import uvicorn

PORT = 8765


def local_ip() -> str:
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    except Exception:
        return "0.0.0.0"
    finally:
        s.close()


if __name__ == "__main__":
    ip = local_ip()
    print("\nSecureGallery Desktop Server")
    print("=" * 40)
    print(f"  Web UI  : http://localhost:{PORT}")
    print(f"  Phone   : http://{ip}:{PORT}")
    print(f"\n  Enter this in the SecureGallery app: {ip}:{PORT}")
    print("=" * 40)
    print("\nPress Ctrl+C to stop.\n")

    try:
        uvicorn.run("app:app", host="0.0.0.0", port=PORT, reload=False, log_level="warning")
    except KeyboardInterrupt:
        print("\nServer stopped.")
        sys.exit(0)
