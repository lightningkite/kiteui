# Return-type worker protocol

You are adding **explicit return types** to KiteUI library declarations as part of a migration to
Kotlin explicit API mode. Visibility modifiers are already done in a prior commit — **do not touch
visibility**. Your job is narrow and mechanical-with-judgment: add the missing return type, nothing
else.

## Input
A list of `absolute/path.kt<TAB>comma,separated,1-based,line,numbers`. Each line number points at a
public declaration (a `fun`, `val`, or `var`) that the compiler flagged as missing an explicit
return type.

## What to do for each site
1. Read the whole file first (context + existing imports).
2. At the given declaration, add the return type that the body/expression **actually produces** —
   the type the compiler would infer. Use the natural, idiomatic public type.
   - Function: insert `: Type` after the closing `)` of the parameter list (which may be on a later
     line for multi-line signatures), before `=` or `{`.
   - Property (`val`/`var`): insert `: Type` after the name, before `=` or `get()`.
   - `expect` declarations and interface/abstract members: add the return type the same way.
3. Use **short type names** and reuse imports already present in the file. Add an `import` only if
   the short name is not already resolvable in the file; put new imports with the existing import
   block, alphabetically if that matches the file's existing style.

## Hard rules — keep the diff a pure return-type addition
- Change **only** the return types. No visibility edits, no logic changes, no renames, no
  reformatting, no reordering, no added/removed blank lines, no comment edits.
- Do not convert between `=` (expression) and `{}` (block) bodies.
- If a declaration at a listed line already has an explicit return type, leave it and note it.
- Prefer the API-appropriate declared type: if a function builds and returns a `List` via
  `buildList { }`, declare `List<X>` (not the internal `ArrayList`). When the inferred type is a
  private/internal implementation type but a public supertype is the clear intent, declare the
  public supertype. When unsure, use the exact inferred type.
- Match surrounding code style (comment density, naming, idiom).

## Report back
- Confirm each file is done.
- List any site where the correct type was non-obvious or where you had to add an import, with a
  one-line reason. These get extra scrutiny at the compile gate.
- Do NOT attempt to compile; a central compile gate verifies your batch and any error is fed back.
