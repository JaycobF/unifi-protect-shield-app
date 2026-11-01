"""Desktop interface for the Profile 7 -> 8.1 converter."""
from __future__ import annotations

import itertools
import subprocess
import sys
import threading
from dataclasses import dataclass, field
from pathlib import Path

from PySide6.QtCore import QObject, QPointF, QRectF, QSettings, QSize, Qt, QUrl, Signal
from PySide6.QtGui import (
    QColor, QDesktopServices, QFontDatabase, QIcon, QPainter, QPainterPath, QPalette, QPen, QPixmap,
)
from PySide6.QtWidgets import (
    QApplication, QCheckBox, QDialog, QFileDialog, QFrame, QGridLayout, QHBoxLayout, QLabel, QListWidget,
    QListWidgetItem, QMainWindow, QMessageBox, QPlainTextEdit, QProgressBar, QPushButton,
    QSizePolicy, QStackedWidget, QToolButton, QVBoxLayout, QWidget,
)

from . import APP_NAME, __version__
from .converter import (
    SUPPORTED_SUFFIXES, Cancelled, ConversionError, Converter, ProbeResult, default_output_path, probe,
)
from .tools import IS_MAC, IS_WINDOWS, TOOL_NAMES, find_all, install_hint

# Palette: graphite surfaces with a projector-lamp amber accent.
INK = "#111318"
SURFACE = "#181B22"
RAISED = "#1F232C"
LINE = "#2A2F3A"
TEXT = "#E8EAF0"
MUTED = "#8D93A3"
AMBER = "#F0B43C"
AMBER_INK = "#1C1505"
GOOD = "#4FC48E"
BAD = "#EE6B6B"

STYLESHEET = f"""
QWidget {{ color: {TEXT}; font-size: 13px; }}
QMainWindow, QDialog, #root {{ background: {INK}; }}
QLabel {{ background: transparent; }}
#title {{ font-size: 22px; font-weight: 700; letter-spacing: 0.2px; }}
#subtitle, #muted, #meta {{ color: {MUTED}; }}
#meta {{ font-size: 12px; }}
#sectionLabel {{ color: {MUTED}; font-size: 11px; font-weight: 600; letter-spacing: 1px; }}

QPushButton {{
    background: {RAISED}; border: 1px solid {LINE}; border-radius: 7px;
    padding: 7px 14px; font-weight: 500;
}}
QPushButton:hover {{ border-color: #3A4050; background: #252A35; }}
QPushButton:pressed {{ background: {SURFACE}; }}
QPushButton:focus {{ border-color: {AMBER}; }}
QPushButton:disabled {{ color: #5A6070; border-color: {LINE}; background: {SURFACE}; }}
QPushButton#primary {{
    background: {AMBER}; color: {AMBER_INK}; border: 1px solid {AMBER}; font-weight: 700; padding: 8px 20px;
}}
QPushButton#primary:hover {{ background: #F5C35F; }}
QPushButton#primary:disabled {{ background: #4A4130; color: #8C7F62; border-color: #4A4130; }}
QPushButton#danger {{ background: transparent; color: {BAD}; border-color: #5A2E33; font-weight: 600; }}
QPushButton#danger:hover {{ background: #2A1A1D; }}
QPushButton#link {{ background: transparent; border: none; color: {MUTED}; padding: 4px 6px; }}
QPushButton#link:hover {{ color: {TEXT}; }}
QPushButton#toolsOk {{ background: transparent; border: 1px solid {LINE}; color: {MUTED}; border-radius: 13px; padding: 4px 12px; }}
QPushButton#toolsBad {{ background: #2A1A1D; border: 1px solid #5A2E33; color: {BAD}; border-radius: 13px; padding: 4px 12px; font-weight: 600; }}

QToolButton {{ background: transparent; border: none; color: {MUTED}; border-radius: 6px; padding: 4px 8px; }}
QToolButton:hover {{ background: {RAISED}; color: {TEXT}; }}

#dropzone {{ background: {SURFACE}; border: 2px dashed {LINE}; border-radius: 14px; }}
#dropzone[active="true"] {{ border-color: {AMBER}; background: #1E1C17; }}
#dropTitle {{ font-size: 17px; font-weight: 600; }}

QListWidget {{ background: transparent; border: none; outline: none; }}
QListWidget::item {{ border: none; padding: 0; margin: 0; }}
QListWidget::item:selected, QListWidget::item:hover {{ background: transparent; }}
#row {{ background: {SURFACE}; border: 1px solid {LINE}; border-radius: 10px; }}
#name {{ font-size: 14px; font-weight: 600; }}

#chip {{ border-radius: 10px; padding: 3px 10px; font-size: 11px; font-weight: 700; background: {RAISED}; color: {MUTED}; }}
#chip[state="ready"] {{ color: {TEXT}; }}
#chip[state="running"] {{ background: #3A2F15; color: {AMBER}; }}
#chip[state="done"] {{ background: #173126; color: {GOOD}; }}
#chip[state="failed"] {{ background: #3A1D21; color: {BAD}; }}

QProgressBar {{ background: {RAISED}; border: none; border-radius: 2px; max-height: 4px; min-height: 4px; }}
QProgressBar::chunk {{ background: {AMBER}; border-radius: 2px; }}
QProgressBar[state="done"]::chunk {{ background: {GOOD}; }}
QProgressBar[state="failed"]::chunk {{ background: {BAD}; }}

QPlainTextEdit {{
    background: {SURFACE}; border: 1px solid {LINE}; border-radius: 10px; padding: 8px;
    color: #B9BFCC; selection-background-color: #4A4130;
}}
#footer {{ background: {SURFACE}; border-top: 1px solid {LINE}; }}
QCheckBox {{ color: {TEXT}; spacing: 8px; background: transparent; }}
QCheckBox::indicator {{ width: 16px; height: 16px; border: 1px solid #4A5060; border-radius: 4px; background: {RAISED}; }}
QCheckBox::indicator:hover {{ border-color: {AMBER}; }}
QCheckBox::indicator:checked {{ background: {AMBER}; border-color: {AMBER}; }}
QCheckBox:disabled {{ color: #5A6070; }}
#outputPath {{ color: {TEXT}; }}
QScrollBar:vertical {{ background: transparent; width: 10px; margin: 2px; }}
QScrollBar::handle:vertical {{ background: {LINE}; border-radius: 4px; min-height: 30px; }}
QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical {{ height: 0; }}
QScrollBar::add-page:vertical, QScrollBar::sub-page:vertical {{ background: transparent; }}
QToolTip {{ background: {RAISED}; color: {TEXT}; border: 1px solid {LINE}; padding: 6px; }}
QMessageBox QLabel {{ color: {TEXT}; }}
"""

# Item states
CHECKING, READY, UNSUPPORTED, RUNNING, DONE, FAILED, CANCELLED = (
    "checking", "ready", "unsupported", "running", "done", "failed", "cancelled")
CHIP_STYLE = {CHECKING: "", READY: "ready", UNSUPPORTED: "", RUNNING: "running",
              DONE: "done", FAILED: "failed", CANCELLED: ""}


def _restyle(widget: QWidget) -> None:
    widget.style().unpolish(widget)
    widget.style().polish(widget)


def _human_size(n: int) -> str:
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if n < 1000 or unit == "TB":
            return f"{n:.0f} {unit}" if unit in ("B", "KB") else f"{n:.1f} {unit}"
        n /= 1000
    return ""


def reveal_in_folder(path: Path) -> None:
    if IS_MAC:
        subprocess.run(["open", "-R", str(path)])
    elif IS_WINDOWS:
        subprocess.run(["explorer", f"/select,{path}"])
    else:
        QDesktopServices.openUrl(QUrl.fromLocalFile(str(path.parent)))


def film_pixmap(size: int, color: str) -> QPixmap:
    """A strip of film: the app's icon and the drop-zone illustration."""
    ratio = 2.0
    pm = QPixmap(int(size * ratio), int(size * ratio))
    pm.setDevicePixelRatio(ratio)
    pm.fill(Qt.transparent)
    p = QPainter(pm)
    p.setRenderHint(QPainter.Antialiasing)
    s = size
    body = QRectF(s * 0.12, s * 0.08, s * 0.76, s * 0.84)
    path = QPainterPath()
    path.addRoundedRect(body, s * 0.1, s * 0.1)
    p.setPen(Qt.NoPen)
    p.setBrush(QColor(color))
    p.drawPath(path)
    # sprocket holes
    p.setBrush(QColor(INK))
    hole_w, hole_h = s * 0.08, s * 0.08
    for i in range(5):
        y = body.top() + s * 0.07 + i * (body.height() - s * 0.14 - hole_h) / 4
        p.drawRoundedRect(QRectF(body.left() + s * 0.05, y, hole_w, hole_h), 1.5, 1.5)
        p.drawRoundedRect(QRectF(body.right() - s * 0.05 - hole_w, y, hole_w, hole_h), 1.5, 1.5)
    # frame window with an arrow: 7 becomes 8
    frame = QRectF(body.left() + s * 0.2, body.top() + s * 0.14, body.width() - s * 0.4, body.height() - s * 0.28)
    p.drawRoundedRect(frame, s * 0.04, s * 0.04)
    pen = QPen(QColor(color), max(1.5, s * 0.06), Qt.SolidLine, Qt.RoundCap, Qt.RoundJoin)
    p.setPen(pen)
    cx, cy = frame.center().x(), frame.center().y()
    p.drawLine(QPointF(cx, cy - frame.height() * 0.25), QPointF(cx, cy + frame.height() * 0.25))
    p.drawLine(QPointF(cx - frame.width() * 0.22, cy + frame.height() * 0.05),
               QPointF(cx, cy + frame.height() * 0.25))
    p.drawLine(QPointF(cx + frame.width() * 0.22, cy + frame.height() * 0.05),
               QPointF(cx, cy + frame.height() * 0.25))
    p.end()
    return pm


class ElidedLabel(QLabel):
    """A label that shortens long file names in the middle instead of growing."""

    def __init__(self, text: str = "", parent: QWidget | None = None):
        super().__init__(text, parent)
        self.setSizePolicy(QSizePolicy.Ignored, QSizePolicy.Preferred)

    def paintEvent(self, _event):
        p = QPainter(self)
        p.setPen(self.palette().color(self.foregroundRole()))
        elided = self.fontMetrics().elidedText(self.text(), Qt.ElideMiddle, self.width())
        p.drawText(self.rect(), Qt.AlignLeft | Qt.AlignVCenter, elided)


@dataclass
class QueueItem:
    id: int
    path: Path
    size: int
    state: str = CHECKING
    info: ProbeResult | None = None
    message: str = ""
    output: Path | None = None
    progress: float = 0.0
    row: "FileRow | None" = field(default=None, repr=False)
    list_item: QListWidgetItem | None = field(default=None, repr=False)


class FileRow(QFrame):
    remove_requested = Signal(int)
    reveal_requested = Signal(int)

    def __init__(self, item: QueueItem):
        super().__init__()
        self.item_id = item.id
        self.setObjectName("row")

        self.name = ElidedLabel(item.path.name)
        self.name.setObjectName("name")
        self.name.setToolTip(str(item.path))
        self.meta = ElidedLabel(_human_size(item.size))
        self.meta.setObjectName("meta")

        self.chip = QLabel()
        self.chip.setObjectName("chip")
        self.reveal = QToolButton(text="Show file")
        self.reveal.setCursor(Qt.PointingHandCursor)
        self.reveal.clicked.connect(lambda: self.reveal_requested.emit(self.item_id))
        self.reveal.hide()
        self.remove = QToolButton(text="✕")
        self.remove.setToolTip("Remove from list")
        self.remove.setCursor(Qt.PointingHandCursor)
        self.remove.clicked.connect(lambda: self.remove_requested.emit(self.item_id))

        self.bar = QProgressBar()
        self.bar.setRange(0, 1000)
        self.bar.setTextVisible(False)
        self.bar.hide()

        text = QVBoxLayout()
        text.setSpacing(3)
        text.addWidget(self.name)
        text.addWidget(self.meta)

        top = QHBoxLayout()
        top.setSpacing(10)
        top.addLayout(text, 1)
        top.addWidget(self.chip, 0, Qt.AlignVCenter)
        top.addWidget(self.reveal, 0, Qt.AlignVCenter)
        top.addWidget(self.remove, 0, Qt.AlignVCenter)

        outer = QVBoxLayout(self)
        outer.setContentsMargins(16, 12, 12, 12)
        outer.setSpacing(10)
        outer.addLayout(top)
        outer.addWidget(self.bar)

    def update_from(self, item: QueueItem, busy: bool) -> None:
        meta = [_human_size(item.size)]
        if item.info:
            if item.info.resolution:
                meta.append(item.info.resolution)
            meta.append(item.info.description)
            if item.info.dv_profile == 7:
                meta.append("dual layer" if item.info.el_present else "single layer")
        if item.state == DONE and item.output:
            meta = [f"Saved as {item.output.name}"]
        elif item.state in (FAILED, UNSUPPORTED) and item.message:
            meta.append(item.message)
        self.meta.setText("  ·  ".join(meta))
        self.meta.setToolTip(item.message or "")
        self.meta.update()

        labels = {CHECKING: "Checking…", READY: "Ready", RUNNING: f"{int(item.progress * 100)}%",
                  DONE: "Profile 8.1", FAILED: "Failed", CANCELLED: "Cancelled"}
        if item.state == DONE and item.output and ".P8+P7" in item.output.name:
            labels[DONE] = "8.1 + 7"
        if item.state == UNSUPPORTED:
            labels[UNSUPPORTED] = "Already 8" if (item.info and item.info.dv_profile == 8) else "Can't convert"
        self.chip.setText(labels[item.state])
        self.chip.setProperty("state", CHIP_STYLE[item.state])
        _restyle(self.chip)

        show_bar = item.state in (RUNNING, DONE, FAILED, CANCELLED) and item.progress > 0
        self.bar.setVisible(show_bar or item.state == RUNNING)
        self.bar.setValue(int(item.progress * 1000))
        self.bar.setProperty("state", CHIP_STYLE.get(item.state, ""))
        _restyle(self.bar)

        self.reveal.setVisible(item.state == DONE)
        self.remove.setVisible(not (busy and item.state in (READY, RUNNING)))


class DropZone(QFrame):
    browse_requested = Signal()

    def __init__(self):
        super().__init__()
        self.setObjectName("dropzone")
        self.setProperty("active", False)
        icon = QLabel()
        icon.setPixmap(film_pixmap(56, AMBER))
        icon.setAlignment(Qt.AlignCenter)
        title = QLabel("Drop Dolby Vision files here")
        title.setObjectName("dropTitle")
        title.setAlignment(Qt.AlignCenter)
        sub = QLabel("Profile 7 MKV, MP4 or M2TS files are converted to Profile 8.1.\n"
                     "Audio, subtitles and chapters are kept as they are.")
        sub.setObjectName("muted")
        sub.setAlignment(Qt.AlignCenter)
        browse = QPushButton("Choose files…")
        browse.setCursor(Qt.PointingHandCursor)
        browse.clicked.connect(self.browse_requested.emit)

        lay = QVBoxLayout(self)
        lay.setContentsMargins(32, 40, 32, 40)
        lay.setSpacing(12)
        lay.addStretch(1)
        lay.addWidget(icon)
        lay.addSpacing(4)
        lay.addWidget(title)
        lay.addWidget(sub)
        lay.addSpacing(8)
        lay.addWidget(browse, 0, Qt.AlignCenter)
        lay.addStretch(1)

    def set_active(self, active: bool) -> None:
        self.setProperty("active", active)
        _restyle(self)


class ToolsDialog(QDialog):
    def __init__(self, settings: QSettings, parent: QWidget | None = None):
        super().__init__(parent)
        self.settings = settings
        self.setWindowTitle("Conversion tools")
        self.setMinimumWidth(600)

        intro = QLabel("DV Converter uses four free tools to do the work. "
                       "It finds them automatically when they're installed in the usual places.")
        intro.setWordWrap(True)
        intro.setObjectName("muted")

        self.grid = QGridLayout()
        self.grid.setHorizontalSpacing(14)
        self.grid.setVerticalSpacing(6)
        self.status: dict[str, QLabel] = {}
        self.hints: dict[str, QLabel] = {}
        for row, name in enumerate(TOOL_NAMES):
            label = QLabel(name)
            label.setObjectName("name")
            status = ElidedLabel()
            status.setObjectName("meta")
            hint = QLabel(install_hint(name))
            hint.setObjectName("meta")
            hint.setWordWrap(True)
            hint.setOpenExternalLinks(True)
            hint.setTextInteractionFlags(Qt.TextBrowserInteraction)
            locate = QPushButton("Locate…")
            locate.clicked.connect(lambda _=False, n=name: self._locate(n))
            self.grid.addWidget(label, row * 2, 0)
            self.grid.addWidget(status, row * 2, 1)
            self.grid.addWidget(locate, row * 2, 2)
            self.grid.addWidget(hint, row * 2 + 1, 1, 1, 2)
            self.status[name] = status
            self.hints[name] = hint
        self.grid.setColumnStretch(1, 1)

        done = QPushButton("Done")
        done.setObjectName("primary")
        done.clicked.connect(self.accept)
        buttons = QHBoxLayout()
        buttons.addStretch(1)
        buttons.addWidget(done)

        lay = QVBoxLayout(self)
        lay.setContentsMargins(24, 22, 24, 20)
        lay.setSpacing(16)
        lay.addWidget(intro)
        lay.addLayout(self.grid)
        lay.addLayout(buttons)
        self.refresh()

    def overrides(self) -> dict[str, str]:
        return {n: self.settings.value(f"tools/{n}", "") for n in TOOL_NAMES}

    def refresh(self) -> None:
        found = find_all(self.overrides())
        for name, path in found.items():
            if path:
                self.status[name].setText(f"✓  {path}")
                self.status[name].setStyleSheet(f"color: {GOOD};")
                self.hints[name].hide()
            else:
                self.status[name].setText("Not found")
                self.status[name].setStyleSheet(f"color: {BAD};")
                self.hints[name].show()
            self.status[name].setToolTip(path or "")

    def _locate(self, name: str) -> None:
        filt = f"{name}.exe ({name}.exe)" if IS_WINDOWS else ""
        path, _ = QFileDialog.getOpenFileName(self, f"Locate {name}", str(Path.home()), filt)
        if path:
            self.settings.setValue(f"tools/{name}", path)
            self.refresh()


class Bridge(QObject):
    """Carries worker-thread events to the GUI thread."""
    probed = Signal(int, object, str)
    progress = Signal(int, float)
    log = Signal(str)
    item_finished = Signal(int, str, str, object)
    all_finished = Signal()


class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle(APP_NAME)
        self.resize(880, 640)
        self.setMinimumSize(780, 480)
        self.setAcceptDrops(True)

        self.settings = QSettings("DVConverter", "DV Converter")
        self.items: dict[int, QueueItem] = {}
        self.ids = itertools.count(1)
        self.busy = False
        self.converter: Converter | None = None
        self.worker: threading.Thread | None = None
        self.stop_requested = False
        self.tools: dict[str, str | None] = {}
        out = self.settings.value("output_dir", "")
        self.output_dir: Path | None = Path(out) if out and Path(out).is_dir() else None
        self.keep_original = self.settings.value("keep_original", False, type=bool)

        self.bridge = Bridge()
        self.bridge.probed.connect(self._on_probed)
        self.bridge.progress.connect(self._on_progress)
        self.bridge.log.connect(self._append_log)
        self.bridge.item_finished.connect(self._on_item_finished)
        self.bridge.all_finished.connect(self._on_all_finished)

        self._build()
        self._refresh_tools()
        self._refresh()

    # -- layout ----------------------------------------------------------

    def _build(self) -> None:
        root = QWidget()
        root.setObjectName("root")
        self.setCentralWidget(root)

        title = QLabel(APP_NAME)
        title.setObjectName("title")
        subtitle = QLabel("Dolby Vision Profile 7  →  Profile 8.1")
        subtitle.setObjectName("subtitle")
        heading = QVBoxLayout()
        heading.setSpacing(2)
        heading.addWidget(title)
        heading.addWidget(subtitle)
        logo = QLabel()
        logo.setPixmap(film_pixmap(40, AMBER))
        self.tools_button = QPushButton()
        self.tools_button.setCursor(Qt.PointingHandCursor)
        self.tools_button.clicked.connect(self._open_tools)

        header = QHBoxLayout()
        header.setSpacing(12)
        header.addWidget(logo)
        header.addLayout(heading)
        header.addStretch(1)
        header.addWidget(self.tools_button, 0, Qt.AlignVCenter)

        self.dropzone = DropZone()
        self.dropzone.browse_requested.connect(self._browse)

        self.count_label = QLabel()
        self.count_label.setObjectName("sectionLabel")
        add = QPushButton("Add files…")
        add.clicked.connect(self._browse)
        self.clear_button = QPushButton("Clear finished")
        self.clear_button.setObjectName("link")
        self.clear_button.clicked.connect(self._clear_finished)
        list_bar = QHBoxLayout()
        list_bar.addWidget(self.count_label)
        list_bar.addStretch(1)
        list_bar.addWidget(self.clear_button)
        list_bar.addWidget(add)

        self.list = QListWidget()
        self.list.setSpacing(4)
        self.list.setSelectionMode(QListWidget.NoSelection)
        self.list.setFocusPolicy(Qt.NoFocus)
        self.list.setVerticalScrollMode(QListWidget.ScrollPerPixel)

        list_page = QWidget()
        lp = QVBoxLayout(list_page)
        lp.setContentsMargins(0, 0, 0, 0)
        lp.setSpacing(10)
        lp.addLayout(list_bar)
        lp.addWidget(self.list, 1)

        self.stack = QStackedWidget()
        self.stack.addWidget(self.dropzone)
        self.stack.addWidget(list_page)

        self.log_view = QPlainTextEdit()
        self.log_view.setReadOnly(True)
        mono = QFontDatabase.systemFont(QFontDatabase.FixedFont)
        mono.setPointSize(max(mono.pointSize() - 1, 9))
        self.log_view.setFont(mono)
        self.log_view.setMaximumBlockCount(5000)
        self.log_view.setFixedHeight(160)
        self.log_view.hide()

        body = QVBoxLayout()
        body.setContentsMargins(28, 24, 28, 16)
        body.setSpacing(18)
        body.addLayout(header)
        body.addWidget(self.stack, 1)
        body.addWidget(self.log_view)

        # footer
        save_label = QLabel("SAVE TO")
        save_label.setObjectName("sectionLabel")
        self.output_label = ElidedLabel()
        self.output_label.setObjectName("outputPath")
        self.output_label.setMinimumWidth(120)
        change = QPushButton("Change…")
        change.setObjectName("link")
        change.clicked.connect(self._choose_output)
        self.reset_output = QPushButton("Use source folder")
        self.reset_output.setObjectName("link")
        self.reset_output.clicked.connect(self._reset_output)
        self.keep_box = QCheckBox("Keep Profile 7 in the same file")
        self.keep_box.setChecked(self.keep_original)
        self.keep_box.setToolTip(
            "Adds the original Profile 7 video as a second video track. Profile 8.1 stays the\n"
            "default track, so the Shield and most players play it; players that support\n"
            "Profile 7 can switch to the other video track. The file is almost twice as big.")
        self.keep_box.toggled.connect(self._set_keep_original)
        self.log_toggle = QPushButton("Show log")
        self.log_toggle.setObjectName("link")
        self.log_toggle.clicked.connect(self._toggle_log)
        self.convert_button = QPushButton("Convert")
        self.convert_button.setObjectName("primary")
        self.convert_button.setCursor(Qt.PointingHandCursor)
        self.convert_button.clicked.connect(self._convert_or_stop)
        self.convert_button.setMinimumWidth(130)
        for fixed in (self.convert_button, self.keep_box, self.log_toggle):
            fixed.setSizePolicy(QSizePolicy.Fixed, QSizePolicy.Fixed)

        footer = QFrame()
        footer.setObjectName("footer")
        fl = QHBoxLayout(footer)
        fl.setContentsMargins(28, 14, 28, 14)
        fl.setSpacing(10)
        fl.addWidget(save_label)
        fl.addWidget(self.output_label, 1)
        fl.addWidget(change)
        fl.addWidget(self.reset_output)
        fl.addSpacing(12)
        fl.addWidget(self.keep_box)
        fl.addSpacing(4)
        fl.addWidget(self.log_toggle)
        fl.addWidget(self.convert_button)

        outer = QVBoxLayout(root)
        outer.setContentsMargins(0, 0, 0, 0)
        outer.setSpacing(0)
        outer.addLayout(body, 1)
        outer.addWidget(footer)

    # -- state -----------------------------------------------------------

    def _refresh(self) -> None:
        items = list(self.items.values())
        self.stack.setCurrentIndex(1 if items else 0)
        ready = [i for i in items if i.state == READY]
        n = len(items)
        self.count_label.setText(f"{n} FILE{'S' if n != 1 else ''}")
        self.clear_button.setVisible(any(i.state in (DONE, UNSUPPORTED, FAILED, CANCELLED) for i in items)
                                     and not self.busy)

        if self.busy:
            self.convert_button.setText("Stop")
            self.convert_button.setObjectName("danger")
            self.convert_button.setEnabled(not self.stop_requested)
        else:
            self.convert_button.setObjectName("primary")
            self.convert_button.setText(f"Convert {len(ready)} file{'s' if len(ready) != 1 else ''}"
                                        if ready else "Convert")
            self.convert_button.setEnabled(bool(ready))
        _restyle(self.convert_button)

        if self.output_dir:
            self.output_label.setText(str(self.output_dir))
            self.output_label.setToolTip(str(self.output_dir))
        else:
            self.output_label.setText("Same folder as each original file")
            self.output_label.setToolTip("")
        self.output_label.update()
        self.reset_output.setVisible(self.output_dir is not None)
        self.keep_box.setEnabled(not self.busy)

        for item in items:
            if item.row:
                item.row.update_from(item, self.busy)

    def _refresh_tools(self) -> None:
        overrides = {n: self.settings.value(f"tools/{n}", "") for n in TOOL_NAMES}
        self.tools = find_all(overrides)
        missing = [n for n, p in self.tools.items() if not p]
        if missing:
            self.tools_button.setText(f"{len(missing)} tool{'s' if len(missing) > 1 else ''} missing")
            self.tools_button.setObjectName("toolsBad")
            self.tools_button.setToolTip("Missing: " + ", ".join(missing))
        else:
            self.tools_button.setText("Tools ready")
            self.tools_button.setObjectName("toolsOk")
            self.tools_button.setToolTip("\n".join(f"{n}: {p}" for n, p in self.tools.items()))
        _restyle(self.tools_button)

    def _open_tools(self) -> None:
        ToolsDialog(self.settings, self).exec()
        self._refresh_tools()
        for item in self.items.values():
            if item.state == FAILED and item.info is None:
                self._start_probe(item)
        self._refresh()

    # -- adding files ----------------------------------------------------

    def _browse(self) -> None:
        exts = " ".join(f"*{s}" for s in sorted(SUPPORTED_SUFFIXES))
        start = self.settings.value("last_dir", str(Path.home()))
        paths, _ = QFileDialog.getOpenFileNames(self, "Choose Dolby Vision files", start,
                                                f"Video files ({exts});;All files (*)")
        if paths:
            self.settings.setValue("last_dir", str(Path(paths[0]).parent))
            self.add_files([Path(p) for p in paths])

    def add_files(self, paths: list[Path]) -> None:
        known = {i.path.resolve() for i in self.items.values()}
        for path in paths:
            if path.is_dir():
                self.add_files(sorted(p for p in path.iterdir() if p.suffix.lower() in SUPPORTED_SUFFIXES))
                continue
            if not path.is_file() or path.resolve() in known:
                continue
            known.add(path.resolve())
            item = QueueItem(id=next(self.ids), path=path, size=path.stat().st_size)
            row = FileRow(item)
            row.remove_requested.connect(self._remove)
            row.reveal_requested.connect(lambda i: reveal_in_folder(self.items[i].output))
            lw = QListWidgetItem()
            lw.setSizeHint(QSize(0, 78))
            self.list.addItem(lw)
            self.list.setItemWidget(lw, row)
            item.row, item.list_item = row, lw
            self.items[item.id] = item
            self._start_probe(item)
        self._refresh()

    def _start_probe(self, item: QueueItem) -> None:
        item.state = CHECKING
        item.message = ""
        ffprobe = self.tools.get("ffprobe")
        if not ffprobe:
            item.state = FAILED
            item.message = "ffprobe is missing. Click the tools button at the top right."
            return

        def work():
            try:
                self.bridge.probed.emit(item.id, probe(ffprobe, item.path), "")
            except Exception as exc:  # noqa: BLE001 - report anything to the row
                self.bridge.probed.emit(item.id, None, str(exc))

        threading.Thread(target=work, daemon=True).start()

    def _on_probed(self, item_id: int, info: ProbeResult | None, error: str) -> None:
        item = self.items.get(item_id)
        if not item:
            return
        item.info = info
        if error:
            item.state, item.message = FAILED, error
        else:
            reason = info.unsupported_reason()
            item.state, item.message = (UNSUPPORTED, reason) if reason else (READY, "")
        self._refresh()

    def _remove(self, item_id: int) -> None:
        item = self.items.pop(item_id, None)
        if item and item.list_item:
            self.list.takeItem(self.list.row(item.list_item))
        self._refresh()

    def _clear_finished(self) -> None:
        for item in list(self.items.values()):
            if item.state in (DONE, UNSUPPORTED, FAILED, CANCELLED):
                self._remove(item.id)

    # -- drag and drop ---------------------------------------------------

    def dragEnterEvent(self, event):
        if event.mimeData().hasUrls():
            event.acceptProposedAction()
            self.dropzone.set_active(True)

    def dragLeaveEvent(self, _event):
        self.dropzone.set_active(False)

    def dropEvent(self, event):
        self.dropzone.set_active(False)
        paths = [Path(u.toLocalFile()) for u in event.mimeData().urls() if u.isLocalFile()]
        self.add_files(paths)

    # -- output folder & log ---------------------------------------------

    def _choose_output(self) -> None:
        start = str(self.output_dir or Path.home())
        folder = QFileDialog.getExistingDirectory(self, "Save converted files to", start)
        if folder:
            self.output_dir = Path(folder)
            self.settings.setValue("output_dir", folder)
            self._refresh()

    def _reset_output(self) -> None:
        self.output_dir = None
        self.settings.setValue("output_dir", "")
        self._refresh()

    def _set_keep_original(self, checked: bool) -> None:
        self.keep_original = checked
        self.settings.setValue("keep_original", checked)

    def _toggle_log(self) -> None:
        visible = not self.log_view.isVisible()
        self.log_view.setVisible(visible)
        self.log_toggle.setText("Hide log" if visible else "Show log")

    def _append_log(self, text: str) -> None:
        self.log_view.appendPlainText(text)

    # -- conversion ------------------------------------------------------

    def _convert_or_stop(self) -> None:
        if self.busy:
            self.stop_requested = True
            if self.converter:
                self.converter.cancel()
            self._refresh()
            return

        self._refresh_tools()
        if not all(self.tools.values()):
            self._open_tools()
            if not all(self.tools.values()):
                return
        queue = [i for i in self.items.values() if i.state == READY]
        if not queue:
            return
        self.busy = True
        self.stop_requested = False
        self._refresh()
        tools = dict(self.tools)
        out_dir = self.output_dir
        self.worker = threading.Thread(target=self._run_queue, args=(queue, tools, out_dir, self.keep_original),
                                       daemon=True)
        self.worker.start()

    def _run_queue(self, queue: list[QueueItem], tools: dict[str, str], out_dir: Path | None,
                   keep_original: bool) -> None:
        """Worker thread: converts each queued item in turn."""
        for item in queue:
            if self.stop_requested:
                break
            dst = default_output_path(item.path, out_dir, keep_original)
            self.bridge.progress.emit(item.id, 0.0)
            self.bridge.log.emit(f"\n▶ {item.path.name}")
            self.converter = Converter(
                tools, log=self.bridge.log.emit,
                progress=lambda f, i=item.id: self.bridge.progress.emit(i, f),
                keep_original=keep_original)
            try:
                verified = self.converter.run(item.path, dst, item.info)
                note = "" if verified else "Converted, but the Profile 8 tag couldn't be confirmed."
                self.bridge.log.emit(f"✓ Saved {dst}")
                self.bridge.item_finished.emit(item.id, DONE, note, dst)
            except Cancelled:
                self.bridge.log.emit("Stopped.")
                self.bridge.item_finished.emit(item.id, CANCELLED, "", None)
            except ConversionError as exc:
                self.bridge.log.emit(f"✕ {exc}")
                self.bridge.item_finished.emit(item.id, FAILED, str(exc), None)
            except Exception as exc:  # noqa: BLE001 - keep the queue alive
                self.bridge.log.emit(f"✕ Unexpected error: {exc!r}")
                self.bridge.item_finished.emit(item.id, FAILED, f"Unexpected error: {exc}", None)
        self.converter = None
        self.bridge.all_finished.emit()

    def _on_progress(self, item_id: int, fraction: float) -> None:
        item = self.items.get(item_id)
        if not item:
            return
        item.state = RUNNING
        item.progress = fraction
        if item.row:
            item.row.update_from(item, True)

    def _on_item_finished(self, item_id: int, state: str, message: str, output) -> None:
        item = self.items.get(item_id)
        if not item:
            return
        item.state, item.message, item.output = state, message, output
        if state == DONE:
            item.progress = 1.0
        elif state == CANCELLED:
            item.state, item.progress = READY, 0.0  # can be converted again
        self._refresh()

    def _on_all_finished(self) -> None:
        self.busy = False
        self.stop_requested = False
        done = sum(1 for i in self.items.values() if i.state == DONE)
        failed = sum(1 for i in self.items.values() if i.state == FAILED)
        self._append_log(f"\nFinished: {done} converted, {failed} failed.")
        if failed and not self.log_view.isVisible():
            self._toggle_log()
        self._refresh()
        QApplication.alert(self)

    def closeEvent(self, event):
        if self.busy:
            answer = QMessageBox.question(
                self, "Stop converting?",
                "A conversion is still running. Quitting stops it and deletes the unfinished file.",
                QMessageBox.Discard | QMessageBox.Cancel, QMessageBox.Cancel)
            if answer != QMessageBox.Discard:
                event.ignore()
                return
            self.stop_requested = True
            if self.converter:
                self.converter.cancel()
            if self.worker:
                self.worker.join(timeout=15)  # let it delete temporary files
        event.accept()


def main() -> int:
    QApplication.setApplicationName(APP_NAME)
    QApplication.setApplicationVersion(__version__)
    app = QApplication(sys.argv)
    app.setStyle("Fusion")
    font = app.font()
    if IS_WINDOWS:
        font.setFamily("Segoe UI")
    app.setFont(font)
    palette = app.palette()
    palette.setColor(QPalette.Link, QColor(AMBER))
    app.setPalette(palette)
    app.setStyleSheet(STYLESHEET)
    app.setWindowIcon(QIcon(film_pixmap(256, AMBER)))
    window = MainWindow()
    window.show()
    window.add_files([Path(a) for a in sys.argv[1:] if Path(a).exists()])
    return app.exec()
