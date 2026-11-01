"""Command-line entry point: python -m dvconvert.cli input.mkv [output.mkv]"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from .converter import Cancelled, ConversionError, Converter, default_output_path, probe
from .tools import find_all


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Convert Dolby Vision Profile 7 to Profile 8.1")
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path, nargs="?")
    args = parser.parse_args(argv)

    tools = find_all()
    missing = [name for name, path in tools.items() if not path]
    if missing:
        print(f"Missing tools: {', '.join(missing)}", file=sys.stderr)
        return 1

    dst = args.output or default_output_path(args.input)
    last = [-1]

    def progress(fraction: float) -> None:
        pct = int(fraction * 100)
        if pct != last[0]:
            last[0] = pct
            print(f"\r{pct:3d}%", end="", flush=True)

    converter = Converter(tools, log=lambda m: print(f"\r{m}"), progress=progress)
    try:
        info = probe(tools["ffprobe"], args.input)
        print(f"{args.input.name}: {info.description}")
        verified = converter.run(args.input, dst, info)
    except ConversionError as exc:
        print(f"\nError: {exc}", file=sys.stderr)
        return 1
    except (Cancelled, KeyboardInterrupt):
        converter.cancel()
        print("\nCancelled", file=sys.stderr)
        return 130
    print(f"\rDone: {dst}" + ("" if verified else " (not verified)"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
