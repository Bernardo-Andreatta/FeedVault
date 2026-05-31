import uuid
import mimetypes
from pathlib import Path
from dataclasses import dataclass, field
from typing import Dict, List, Optional

IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".heic", ".heif"}
VIDEO_EXTS = {".mp4", ".mkv", ".mov", ".avi", ".webm", ".m4v", ".3gp", ".wmv"}
MEDIA_EXTS = IMAGE_EXTS | VIDEO_EXTS


@dataclass
class SharedFolder:
    id: str
    path: str
    recursive: bool
    media_only: bool


@dataclass
class SharedFile:
    id: str
    name: str
    path: str
    size: int
    mime_type: str
    folder_id: str


class FileManager:
    def __init__(self):
        self._folders: Dict[str, SharedFolder] = {}
        self._files: Dict[str, SharedFile] = {}

    # ── Folders ────────────────────────────────────────────────────────────

    def add_folder(self, path: str, recursive: bool = True, media_only: bool = True) -> SharedFolder:
        p = Path(path).resolve()
        folder = SharedFolder(
            id=str(uuid.uuid4()),
            path=str(p),
            recursive=recursive,
            media_only=media_only,
        )
        self._folders[folder.id] = folder
        self._index_folder(folder)
        return folder

    def remove_folder(self, folder_id: str):
        self._folders.pop(folder_id, None)
        self._files = {fid: f for fid, f in self._files.items() if f.folder_id != folder_id}

    def get_folders(self) -> List[SharedFolder]:
        return list(self._folders.values())

    def refresh(self):
        self._files.clear()
        for folder in self._folders.values():
            self._index_folder(folder)

    # ── Files ──────────────────────────────────────────────────────────────

    def get_files(self, mime_filter: Optional[str] = None) -> List[SharedFile]:
        files = list(self._files.values())
        if mime_filter:
            files = [f for f in files if f.mime_type.startswith(mime_filter)]
        return sorted(files, key=lambda f: f.name.lower())

    def get_file(self, file_id: str) -> Optional[SharedFile]:
        return self._files.get(file_id)

    def delete_file(self, file_id: str) -> Optional[SharedFile]:
        """Remove from index and return the record (caller handles disk deletion)."""
        return self._files.pop(file_id, None)

    # ── Internal ───────────────────────────────────────────────────────────

    def _index_folder(self, folder: SharedFolder):
        p = Path(folder.path)
        if not p.exists():
            return
        pattern = "**/*" if folder.recursive else "*"
        for entry in p.glob(pattern):
            if not entry.is_file():
                continue
            if folder.media_only and entry.suffix.lower() not in MEDIA_EXTS:
                continue
            file_id = str(uuid.uuid5(uuid.NAMESPACE_URL, str(entry.resolve())))
            mime, _ = mimetypes.guess_type(str(entry))
            self._files[file_id] = SharedFile(
                id=file_id,
                name=entry.name,
                path=str(entry.resolve()),
                size=entry.stat().st_size,
                mime_type=mime or "application/octet-stream",
                folder_id=folder.id,
            )
