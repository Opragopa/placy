"""Local read-only HTTP fixture for Android E2E smoke tests; binds to loopback only."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import struct
import zlib


def photo_png():
    width, height = 200, 100
    def chunk(name, data):
        return struct.pack(">I", len(data)) + name + data + struct.pack(">I", zlib.crc32(name + data) & 0xffffffff)
    rows = (b"\x00" + bytes((235, 25, 25)) * width) * height
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(rows)) + chunk(b"IEND", b""))


class Fixture(BaseHTTPRequestHandler):
    writes = 0

    def do_POST(self):
        type(self).writes += 1
        self.send_error(405)

    def do_PUT(self):
        type(self).writes += 1
        self.send_error(405)

    def do_GET(self):
        if self.path == "/geomarks":
            payload = b'[ {"photoUUID":"fixture","latitude":0.0,"longitude":0.0} ]'
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)
        elif self.path == "/remote.php/dav/files/fixture/fixture.jpg":
            payload = photo_png()
            self.send_response(200)
            self.send_header("Content-Type", "image/png")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)
        elif self.path == "/test-events":
            payload = str(type(self).writes).encode("ascii")
            self.send_response(200)
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)
        else:
            self.send_error(404)


if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", 18080), Fixture).serve_forever()
