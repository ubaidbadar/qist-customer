#!/usr/bin/env python3
"""Verify that committed artwork uses lossless PNG or vector resources.

Uses only the Python standard library. Android vector drawables remain scalable;
PNG is lossless at its existing resolution, not resolution-independent.
"""

from __future__ import annotations

import struct
import subprocess
import sys
import xml.etree.ElementTree as ET
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
# These formats can be lossy or restrict colors. Require PNG/vector artwork here.
DISALLOWED_RASTER = {".jpg", ".jpeg", ".webp", ".gif", ".avif", ".heic", ".heif"}
VECTOR_ROOTS = {"vector", "adaptive-icon", "layer-list", "selector", "shape", "inset", "ripple"}
VALID_DEPTHS = {
    0: {1, 2, 4, 8, 16},
    2: {8, 16},
    3: {1, 2, 4, 8},
    4: {8, 16},
    6: {8, 16},
}


def png_dimensions(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if not data.startswith(PNG_SIGNATURE):
        raise ValueError("not a PNG file")
    offset = len(PNG_SIGNATURE)
    header = None
    compressed = bytearray()
    seen_end = False
    first = True
    while offset < len(data):
        if len(data) - offset < 12:
            raise ValueError("incomplete PNG chunk")
        length = struct.unpack_from(">I", data, offset)[0]
        chunk = data[offset + 4:offset + 8]
        end = offset + 12 + length
        if end > len(data):
            raise ValueError("truncated PNG chunk")
        payload = data[offset + 8:end - 4]
        expected_crc = struct.unpack_from(">I", data, end - 4)[0]
        if zlib.crc32(chunk + payload) & 0xFFFFFFFF != expected_crc:
            raise ValueError(f"CRC mismatch in {chunk!r}")
        if first and chunk != b"IHDR":
            raise ValueError("IHDR must be first")
        if chunk == b"IHDR":
            if not first or length != 13:
                raise ValueError("invalid IHDR")
            width, height, depth, color, compression, filtering, interlace = struct.unpack(">IIBBBBB", payload)
            if not (width and height and depth in VALID_DEPTHS.get(color, set())):
                raise ValueError("invalid dimensions or color format")
            if compression != 0 or filtering != 0 or interlace not in (0, 1):
                raise ValueError("unsupported PNG encoding")
            header = (width, height)
        elif chunk == b"IDAT":
            compressed.extend(payload)
        elif chunk == b"IEND":
            if length or end != len(data):
                raise ValueError("invalid PNG end")
            seen_end = True
        offset = end
        first = False
        if seen_end:
            break
    if header is None or not compressed or not seen_end:
        raise ValueError("missing PNG image data or end chunk")
    try:
        zlib.decompress(compressed)
    except zlib.error as error:
        raise ValueError("invalid compressed PNG image data") from error
    return header


def vector_resource(path: Path) -> None:
    element = ET.parse(path).getroot()
    local_name = element.tag.rsplit("}", 1)[-1]
    if path.suffix == ".svg":
        if local_name != "svg":
            raise ValueError("not an SVG vector")
        if any(node.tag.rsplit("}", 1)[-1] == "image" for node in element.iter()):
            raise ValueError("embedded raster image in SVG")
    elif local_name not in VECTOR_ROOTS:
        raise ValueError(f"unexpected drawable root element: {local_name}")


def tracked_paths() -> list[Path]:
    output = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT)
    return [ROOT / p.decode("utf-8") for p in output.split(b"\0") if p]


def main() -> int:
    images = 0
    vectors = 0
    errors = []
    for path in tracked_paths():
        relative = path.relative_to(ROOT).as_posix()
        ext = path.suffix.lower()
        try:
            if ext in DISALLOWED_RASTER:
                raise ValueError("lossy-capable artwork format; use PNG or vector instead")
            if ext == ".png":
                png_dimensions(path)
                images += 1
            elif ext == ".svg" or (
                ext == ".xml"
                and "/src/main/res/" in relative
                and ("/drawable/" in relative or "/mipmap-anydpi" in relative)
            ):
                vector_resource(path)
                vectors += 1
        except (ValueError, ET.ParseError, OSError) as error:
            errors.append(f"{relative}: {error}")
    if errors or not images or not vectors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        if not images or not vectors:
            print("ERROR: expected PNG artwork and vector resources", file=sys.stderr)
        return 1
    print(f"Lossless artwork verified: {images} PNG files and {vectors} vector XML/SVG resources.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
