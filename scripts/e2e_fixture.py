"""Local read-only HTTP fixture for Android E2E smoke tests; binds to loopback only."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import base64


class Fixture(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/geomarks":
            payload = b'[ {"photoUUID":"fixture","latitude":0.0,"longitude":0.0} ]'
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)
        elif self.path == "/remote.php/dav/files/fixture/fixture.jpg":
            payload = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/a4sAAAAASUVORK5CYII=")
            self.send_response(200)
            self.send_header("Content-Type", "image/png")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)
        else:
            self.send_error(404)


if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", 18080), Fixture).serve_forever()
