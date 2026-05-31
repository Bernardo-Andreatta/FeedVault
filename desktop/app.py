import socket
import subprocess
import uuid
from pathlib import Path
from typing import List, Optional

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from file_manager import FileManager

app = FastAPI(title="SecureGallery Desktop", version="1.0.0")
file_manager = FileManager()

UPLOADS_DIR = Path(__file__).parent / "uploads"
THUMBS_DIR  = Path(__file__).parent / "thumbs"
UPLOADS_DIR.mkdir(exist_ok=True)
THUMBS_DIR.mkdir(exist_ok=True)

THUMB_MAX = 400  # max dimension in px


def _make_thumb(src: str, dest: Path, mime: str) -> bool:
    """Generate JPEG thumbnail. Returns True on success."""
    try:
        if mime.startswith("image/"):
            from PIL import Image
            img = Image.open(src)
            img.thumbnail((THUMB_MAX, THUMB_MAX))
            img.convert("RGB").save(str(dest), "JPEG", quality=85, optimize=True)
            return True
        elif mime.startswith("video/"):
            result = subprocess.run(
                [
                    "ffmpeg", "-y", "-i", src,
                    "-vframes", "1",
                    "-vf", f"scale={THUMB_MAX}:-1",
                    str(dest),
                ],
                capture_output=True, timeout=20,
            )
            return dest.exists()
    except Exception:
        pass
    return False

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def startup():
    file_manager.add_folder(str(UPLOADS_DIR), recursive=False, media_only=False)

# ── Helpers ────────────────────────────────────────────────────────────────────

def local_ip() -> str:
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    except Exception:
        return "127.0.0.1"
    finally:
        s.close()


# ── Status ─────────────────────────────────────────────────────────────────────

@app.get("/api/status")
def status():
    return {
        "name": "SecureGallery Desktop",
        "version": "1.0.0",
        "ip": local_ip(),
        "file_count": len(file_manager.get_files()),
        "folder_count": len(file_manager.get_folders()),
    }


# ── Folders ────────────────────────────────────────────────────────────────────

class AddFolderRequest(BaseModel):
    path: str
    recursive: bool = True
    media_only: bool = True


@app.get("/api/folders")
def list_folders():
    return [
        {
            "id": f.id,
            "path": f.path,
            "recursive": f.recursive,
            "mediaOnly": f.media_only,
        }
        for f in file_manager.get_folders()
    ]


@app.post("/api/folders", status_code=201)
def add_folder(req: AddFolderRequest):
    p = Path(req.path)
    if not p.exists():
        raise HTTPException(400, f"Path does not exist: {req.path}")
    if not p.is_dir():
        raise HTTPException(400, f"Path is not a directory: {req.path}")
    folder = file_manager.add_folder(req.path, req.recursive, req.media_only)
    return {
        "id": folder.id,
        "path": folder.path,
        "fileCount": len([f for f in file_manager.get_files() if f.folder_id == folder.id]),
    }


@app.delete("/api/folders/{folder_id}")
def remove_folder(folder_id: str):
    if folder_id not in {f.id for f in file_manager.get_folders()}:
        raise HTTPException(404, "Folder not found")
    file_manager.remove_folder(folder_id)
    return {"ok": True}


@app.post("/api/folders/refresh")
def refresh_folders():
    file_manager.refresh()
    return {"fileCount": len(file_manager.get_files())}


# ── Upload ─────────────────────────────────────────────────────────────────────

@app.post("/api/upload", status_code=201)
async def upload_files(files: List[UploadFile] = File(...)):
    saved = []
    for upload in files:
        data = await upload.read()
        name = upload.filename or f"upload_{uuid.uuid4().hex[:8]}"
        dest = UPLOADS_DIR / name
        counter = 1
        while dest.exists():
            stem = dest.stem
            suffix = dest.suffix
            dest = UPLOADS_DIR / f"{stem}_{counter}{suffix}"
            counter += 1
        dest.write_bytes(data)
        saved.append(dest.name)
    file_manager.refresh()
    return {"count": len(saved), "files": saved}


# ── Files ──────────────────────────────────────────────────────────────────────

@app.get("/api/files")
def list_files(type: Optional[str] = None):
    """type: 'image' | 'video' | None (all)"""
    mime_filter = None
    if type == "image":
        mime_filter = "image/"
    elif type == "video":
        mime_filter = "video/"
    files = file_manager.get_files(mime_filter)
    return [
        {
            "id": f.id,
            "name": f.name,
            "size": f.size,
            "mimeType": f.mime_type,
            "folderId": f.folder_id,
            "uploaded": Path(f.path).parent.resolve() == UPLOADS_DIR.resolve(),
        }
        for f in files
    ]


@app.delete("/api/files/{file_id}")
def delete_file(file_id: str):
    f = file_manager.get_file(file_id)
    if not f:
        raise HTTPException(404, "File not found")
    if Path(f.path).parent.resolve() != UPLOADS_DIR.resolve():
        raise HTTPException(403, "Only uploaded files can be deleted")
    file_manager.delete_file(file_id)
    p = Path(f.path)
    if p.exists():
        p.unlink()
    return {"ok": True}


@app.get("/api/files/{file_id}/thumbnail")
def get_thumbnail(file_id: str):
    f = file_manager.get_file(file_id)
    if not f:
        raise HTTPException(404, "File not found")
    thumb = THUMBS_DIR / f"{file_id}.jpg"
    src = Path(f.path)
    if not src.exists():
        raise HTTPException(410, "Source file gone")
    # Regenerate if source is newer than cached thumb
    if not thumb.exists() or src.stat().st_mtime > thumb.stat().st_mtime:
        if not _make_thumb(str(src), thumb, f.mime_type):
            raise HTTPException(500, "Thumbnail generation failed — ffmpeg missing for video?")
    return FileResponse(str(thumb), media_type="image/jpeg")


@app.get("/api/files/{file_id}/download")
def download_file(file_id: str):
    f = file_manager.get_file(file_id)
    if not f:
        raise HTTPException(404, "File not found")
    if not Path(f.path).exists():
        file_manager.refresh()
        raise HTTPException(410, "File no longer exists on disk")
    return FileResponse(
        path=f.path,
        filename=f.name,
        media_type=f.mime_type,
    )


# ── Web UI ─────────────────────────────────────────────────────────────────────

static_dir = Path(__file__).parent / "static"
app.mount("/", StaticFiles(directory=str(static_dir), html=True), name="static")
