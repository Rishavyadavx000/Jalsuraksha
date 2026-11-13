#!/usr/bin/env python3
"""Apply the JAL-SURAKSHA supplied artwork as an Android launcher icon."""
from pathlib import Path
import shutil
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: apply_launcher_icon.py <Android-project-root>")

project = Path(sys.argv[1]).resolve()
app = project / "app"
manifest = app / "src/main/AndroidManifest.xml"
source_asset = Path(__file__).with_name("ic_launcher_art.webp")
if not manifest.is_file():
    raise SystemExit(f"AndroidManifest.xml not found under {project}")
if not source_asset.is_file():
    raise SystemExit(f"Launcher artwork not found: {source_asset}")

res = app / "src/main/res"
(res / "drawable-nodpi").mkdir(parents=True, exist_ok=True)
shutil.copy2(source_asset, res / "drawable-nodpi/ic_launcher_art.webp")

(res / "values").mkdir(parents=True, exist_ok=True)
(res / "values/colors.xml").write_text("""<?xml version=\"1.0\" encoding=\"utf-8\"?>
<resources>
    <color name=\"jal_navy\">#FF0B2A4A</color>
    <color name=\"ic_launcher_background\">#FF051A36</color>
</resources>
""", encoding="utf-8")

(res / "mipmap-anydpi").mkdir(parents=True, exist_ok=True)
(res / "mipmap-anydpi/ic_launcher.xml").write_text("""<?xml version=\"1.0\" encoding=\"utf-8\"?>
<layer-list xmlns:android=\"http://schemas.android.com/apk/res/android\">
    <item android:drawable=\"@color/ic_launcher_background\" />
    <item android:drawable=\"@drawable/ic_launcher_art\" />
</layer-list>
""", encoding="utf-8")

(res / "mipmap-anydpi-v26").mkdir(parents=True, exist_ok=True)
(res / "mipmap-anydpi-v26/ic_launcher.xml").write_text("""<?xml version=\"1.0\" encoding=\"utf-8\"?>
<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">
    <background android:drawable=\"@color/ic_launcher_background\" />
    <foreground android:drawable=\"@drawable/ic_launcher_art\" />
</adaptive-icon>
""", encoding="utf-8")

(res / "drawable").mkdir(parents=True, exist_ok=True)
(res / "drawable/ic_launcher_monochrome.xml").write_text("""<?xml version=\"1.0\" encoding=\"utf-8\"?>
<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"
    android:width=\"108dp\" android:height=\"108dp\"
    android:viewportWidth=\"108\" android:viewportHeight=\"108\">
    <path android:fillColor=\"#FFFFFFFF\"
        android:pathData=\"M54,12 C54,12 28,43 28,63 A26,26 0 0,0 80,63 C80,43 54,12 54,12 Z\" />
    <path android:fillColor=\"#FFFFFFFF\" android:pathData=\"M24,76 L84,76 L84,83 L24,83 Z\" />
    <path android:fillColor=\"#FFFFFFFF\"
        android:pathData=\"M31,69 C31,57 40,49 54,49 C68,49 77,57 77,69 L69,69 C69,61 63,57 54,57 C45,57 39,61 39,69 Z\" />
</vector>
""", encoding="utf-8")

(res / "mipmap-anydpi-v33").mkdir(parents=True, exist_ok=True)
(res / "mipmap-anydpi-v33/ic_launcher.xml").write_text("""<?xml version=\"1.0\" encoding=\"utf-8\"?>
<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">
    <background android:drawable=\"@color/ic_launcher_background\" />
    <foreground android:drawable=\"@drawable/ic_launcher_art\" />
    <monochrome android:drawable=\"@drawable/ic_launcher_monochrome\" />
</adaptive-icon>
""", encoding="utf-8")

(project / "docs").mkdir(parents=True, exist_ok=True)
(project / "docs/APP_ICON.md").write_text("""# JAL-SURAKSHA App Icon

The launcher icon uses the artwork supplied for JAL-SURAKSHA, inset on a navy canvas to reduce cropping by Android launcher masks.

- API 24-25: anydpi layer-list fallback.
- API 26+: adaptive icon with navy background and supplied artwork foreground.
- API 33+: monochrome layer for themed launcher icons.
- Manifest launcher icon reference remains @mipmap/ic_launcher.

This change only updates launcher icon resources; it does not change network, authentication, firmware, pump-control or safety behavior.
""", encoding="utf-8")
print(f"Launcher icon applied to {project}")
