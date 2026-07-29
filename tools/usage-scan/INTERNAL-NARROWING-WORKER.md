# Internal-narrowing worker protocol

You are reducing the public API surface of the KiteUI library (a Kotlin Multiplatform UI framework
for app developers) by changing `public` to `internal` on declarations that are implementation
details, NOT part of the intended public API. Every public declaration currently has an explicit
`public` modifier (a prior mechanical pass added them). Your job is JUDGMENT: decide, per
declaration, whether an app developer using this framework would legitimately use it.

## The decision, per `public` declaration
Ask: **"Does it make sense for this to be public API?"**
- **Keep `public`** if an app author would reasonably reference/call it: pages & navigation, view
  components & DSL builders, modifiers, reactive primitives, theme/color/model types, public
  extension functions on those, anything with KDoc describing usage, anything that reads as a
  deliberate API. When in doubt, KEEP PUBLIC — over-internalizing breaks consumers; err toward public.
- **Change to `internal`** only when it's clearly an implementation detail: internal state holders,
  private-ish helpers, platform glue, caches, things obviously meant only for the library's own use,
  declarations with names/comments signalling internals.

## HARD constraint — the forced-public floor
The file `tools/usage-scan/forced-public-floor.txt` lists declarations proven to be used by external
downstream repos. **Never internalize a declaration whose fully-qualified name is in that file.**
Keys look like `CALL:com.lightningkite.kiteui.<pkg>.<name>` or `CLASS:...`, with property accessors
as `<get-name>`/`<set-name>`. Before internalizing, grep the floor for the declaration's name; if it
appears for this declaration, keep it public. (The floor is a lower bound — judgment still keeps
other genuine API public too.)

## Syntactic rules — do NOT create compile errors
- **Never** add `internal` to: an `override`; a member of an `interface`; an `abstract`/open member
  that overrides or is meant to be overridden across modules; an `actual` declaration (its visibility
  must match the `expect`); an `enum` entry; a `companion object`'s members that back a public const.
- **Do not** internalize a type (class/interface/typealias) that appears in the signature (param,
  return, supertype, receiver) of any declaration that remains `public` — that exposes an internal
  type in public API and is a compile error. If unsure whether a type is used in public signatures,
  KEEP IT PUBLIC.
- Only change the visibility keyword. No other edits — no logic, no reordering, no reformatting.

## Output discipline
- Change `public` → `internal` in place (replace the modifier only).
- Keep a running list of what you internalized and WHY (one line each), and separately note any
  declaration you were tempted to internalize but kept public due to the floor or uncertainty.
- Do NOT compile; a central `:library` strict compile + consumer compile verifies your work and any
  breakage is fed back to you for revert.

Bias reminder: this pass is reversible via the compiler for in-repo consumers but NOT for external
repos — so when judgment is genuinely uncertain, keeping `public` is the safe choice.
