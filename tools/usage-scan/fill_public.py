#!/usr/bin/env python3
"""Fill explicit `public` visibility at every 'Visibility must be specified' warning site.

Reads a Kotlin compiler warning log (`w: file://PATH:LINE:COL Visibility must be specified...`)
and inserts `public ` at each (line, col). Insertions are within a line and never add lines, so
all other warning coordinates for the same file stay valid regardless of order.

Modes:
  --check : report any site whose target column is not the start of an identifier/keyword/@,
            without modifying files. Run this first.
  --apply : perform the insertions.
"""
import re, sys, collections

WARN_RE = re.compile(r"^w: file://([^:]+):(\d+):(\d+) Visibility must be specified")

def parse(logpath):
    sites = collections.defaultdict(list)  # path -> list[(line,col)]
    with open(logpath, encoding="utf-8", errors="replace") as f:
        for ln in f:
            m = WARN_RE.match(ln)
            if m:
                sites[m.group(1)].append((int(m.group(2)), int(m.group(3))))
    return sites

def main():
    mode = sys.argv[1]  # --check | --apply
    log = sys.argv[2]
    sites = parse(log)
    total = sum(len(v) for v in sites.values())
    print(f"{total} visibility sites across {len(sites)} files")
    suspicious = 0
    for path, coords in sites.items():
        with open(path, encoding="utf-8") as f:
            lines = f.readlines()
        # apply deepest-column-first per line so earlier insertions don't shift later columns
        for (line, col) in sorted(coords, key=lambda c: (-c[0], -c[1])):
            row = lines[line-1]
            idx = col-1
            ch = row[idx] if idx < len(row) else ""
            # leading token must start with a letter, underscore, backtick, or @
            if not (ch.isalpha() or ch in "_`@"):
                suspicious += 1
                if suspicious <= 40:
                    print(f"  SUSPICIOUS {path}:{line}:{col} -> {row[idx:idx+20]!r}")
                continue
            if mode == "--apply":
                lines[line-1] = row[:idx] + "public " + row[idx:]
        if mode == "--apply":
            with open(path, "w", encoding="utf-8") as f:
                f.writelines(lines)
    print(f"suspicious sites: {suspicious}")

if __name__ == "__main__":
    main()
