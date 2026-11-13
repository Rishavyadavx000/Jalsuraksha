#!/usr/bin/env python3
"""Apply the v0.3.0 HTTP 401 behavior correction to the extracted CI source.

Firmware 1.0 keeps GET /api/status unauthenticated. Only POST /api/cmd
uses the control token. Do not map a status GET 401 to token rejection.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_status_http401.py <Android-project-root>")

project = Path(sys.argv[1])
target = project / "app/src/main/java/com/jalsuraksha/app/data/DeviceApi.kt"
if not target.is_file():
    raise SystemExit(f"DeviceApi.kt not found: {target}")

source = target.read_text(encoding="utf-8")
start = source.find("suspend fun fetchStatus(")
end = source.find("suspend fun fetchEvents(", start)
if start < 0 or end < 0:
    raise SystemExit("Could not locate fetchStatus() block in DeviceApi.kt")

block = source[start:end]
old = "                raw.code == 401 -> ApiOutcome.Failure(ApiError.AuthRequired)\n"
if old not in block:
    if "raw.code == 401" not in block and "raw.code == 401" not in source:
        print("Status 401 behavior already corrected; no patch needed.")
        raise SystemExit(0)
    raise SystemExit("Unexpected fetchStatus() 401 mapping; inspect source before patching.")

new = (
    "                // Firmware 1.0 leaves GET /api/status public; a 401 here is an HTTP "
    "endpoint error, not a control-token rejection.\n"
)
updated_block = block.replace(old, "", 1)
updated = source[:start] + updated_block + source[end:]
target.write_text(updated, encoding="utf-8")
print("Patched fetchStatus(): HTTP 401 now maps to HttpError; POST /api/cmd token handling is unchanged.")
