#!/usr/bin/env python3
"""Widen a leaked concrete collection type in a public declaration to its interface.

For each (file, line), replaces the FIRST `: ArrayList<` / `: HashSet<` / `: HashMap<` on that
line (the DECLARED type, which precedes the `=` initializer) with the corresponding interface,
leaving the concrete initializer after `=` untouched. Idempotent and line-local.
"""
import sys

# (path, line, concrete -> interface)
SITES = [
    ("library/src/androidMain/kotlin/com/lightningkite/kiteui/fetch.android.kt", 195, "ArrayList"),
    ("library/src/androidMain/kotlin/com/lightningkite/kiteui/fetch.android.kt", 201, "ArrayList"),
    ("library/src/androidMain/kotlin/com/lightningkite/kiteui/fetch.android.kt", 207, "ArrayList"),
    ("library/src/androidMain/kotlin/com/lightningkite/kiteui/fetch.android.kt", 213, "ArrayList"),
    ("library/src/iosMain/kotlin/com/lightningkite/kiteui/fetch.ios.kt", 220, "ArrayList"),
    ("library/src/iosMain/kotlin/com/lightningkite/kiteui/fetch.ios.kt", 226, "ArrayList"),
    ("library/src/iosMain/kotlin/com/lightningkite/kiteui/fetch.ios.kt", 232, "ArrayList"),
    ("library/src/iosMain/kotlin/com/lightningkite/kiteui/fetch.ios.kt", 238, "ArrayList"),
    ("library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/fetch.jvm.kt", 160, "ArrayList"),
    ("library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/fetch.jvm.kt", 166, "ArrayList"),
    ("library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/fetch.jvm.kt", 172, "ArrayList"),
    ("library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/fetch.jvm.kt", 178, "ArrayList"),
    ("library/src/androidMain/kotlin/com/lightningkite/kiteui/views/direct/ProgrammaticLayout.android.kt", 109, "HashSet"),
    ("library/src/androidMain/kotlin/com/lightningkite/kiteui/views/direct/RawImageView.android.kt", 161, "ArrayList"),
    ("library/src/androidMain/kotlin/com/lightningkite/kiteui/views/direct/RawImageView.android.kt", 305, "ArrayList"),
    ("library/src/commonHtmlMain/kotlin/com/lightningkite/kiteui/views/KiteUiCss.kt", 1214, "HashSet"),
    ("library/src/iosMain/kotlin/com/lightningkite/kiteui/views/direct/FlexLayout.kt", 75, "ArrayList"),
    ("library/src/iosMain/kotlin/com/lightningkite/kiteui/views/direct/FlexLayout.kt", 106, "ArrayList"),
    ("library/src/iosMain/kotlin/com/lightningkite/kiteui/views/direct/LinearLayout.kt", 128, "ArrayList"),
    ("library/src/iosMain/kotlin/com/lightningkite/kiteui/views/direct/LinearLayout.kt", 159, "ArrayList"),
    ("library/src/jsMain/kotlin/com/lightningkite/kiteui/views/NativeElement.commonHtml.js.kt", 39, "ArrayList"),
    ("library/src/jsMain/kotlin/com/lightningkite/kiteui/views/NativeElement.commonHtml.js.kt", 384, "HashSet"),
    ("library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/views/FutureElement.commonHtml.jvmSsr.kt", 33, "ArrayList"),
]
IFACE = {"ArrayList": "MutableList", "HashSet": "MutableSet", "HashMap": "MutableMap"}

def main():
    apply = "--apply" in sys.argv
    bad = 0
    # group by file, edit deepest line first (irrelevant since line-local, but keeps reads simple)
    from collections import defaultdict
    byfile = defaultdict(list)
    for p, ln, c in SITES:
        byfile[p].append((ln, c))
    for path, sites in byfile.items():
        with open(path, encoding="utf-8") as f:
            lines = f.readlines()
        for ln, concrete in sites:
            row = lines[ln-1]
            token = f": {concrete}<"
            if token not in row:
                bad += 1
                print(f"  MISS {path}:{ln} expected {token!r} in: {row.strip()!r}")
                continue
            new = row.replace(token, f": {IFACE[concrete]}<", 1)
            print(f"  ok {path}:{ln}  {concrete}->{IFACE[concrete]}")
            if apply:
                lines[ln-1] = new
        if apply:
            with open(path, "w", encoding="utf-8") as f:
                f.writelines(lines)
    print(f"misses: {bad}")

if __name__ == "__main__":
    main()
