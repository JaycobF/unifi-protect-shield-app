# PyInstaller build spec: pyinstaller --noconfirm dv_converter.spec
# Any executables placed in ./bin (dovi_tool, ffmpeg, ...) are bundled with the app.
import sys
from pathlib import Path

sys.path.insert(0, SPECPATH)
from dvconvert import APP_NAME, __version__  # noqa: E402

bin_dir = Path("bin")
binaries = [(str(p), "bin") for p in bin_dir.glob("*") if p.is_file()] if bin_dir.is_dir() else []

a = Analysis(
    ["main.py"],
    binaries=binaries,
    excludes=["tkinter", "PySide6.QtNetwork", "PySide6.QtQml", "PySide6.QtQuick", "PySide6.QtPdf",
              "PySide6.QtWebEngineCore", "PySide6.QtMultimedia", "PySide6.Qt3DCore"],
)
pyz = PYZ(a.pure)
exe = EXE(
    pyz,
    a.scripts,
    [],
    exclude_binaries=True,
    name=APP_NAME,
    console=False,
    icon="assets/icon.ico" if sys.platform == "win32" else None,
)
coll = COLLECT(exe, a.binaries, a.datas, name=APP_NAME)

if sys.platform == "darwin":
    app = BUNDLE(
        coll,
        name=f"{APP_NAME}.app",
        icon="assets/icon.icns",
        bundle_identifier="app.dvconverter",
        info_plist={
            "CFBundleShortVersionString": __version__,
            "NSHighResolutionCapable": True,
            "LSMinimumSystemVersion": "11.0",
        },
    )
