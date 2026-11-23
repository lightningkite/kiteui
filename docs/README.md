# KiteUI documentation

KiteUI is a Kotlin Multiplatform UI framework.  You describe views once in a Kotlin DSL; each platform renders them with its own native views (DOM on the web, `View` on Android, `UIView` on iOS, Compose on desktop).  State is fine-grained and reactive, in the style of Solid.js: a view is built once, and only the bindings that read a changed value rerun.

## Learning path

Read these in order the first time.  Each chapter is: what it is, a minimal example, common tasks, gotchas.

1. [Getting started](getting-started.md): dependencies, the app entry point, your first page
2. [Layout](layout.md): `col`, `row`, `frame`, modifiers, sizing, scrolling
3. [Components](components.md): text, buttons, inputs, images, icons
4. [Reactivity](reactivity.md): `Signal`, `remember`, bindings, loading states
5. [Lists](lists.md): `colOf`, `recyclerView`, `windowedList`
6. [Navigation](navigation.md): pages, `@Routable`, the navigator, app shells
7. [Forms](forms.md): labeled fields, drafts, validation
8. [Dialogs](dialogs.md): toasts, confirmations, dialogs, bottom sheets, popovers
9. [Theming](theming.md): semantic themes, built-in themes, custom themes
10. [Server data with ModelCache](model-cache.md): cached, live data from a Lightning Server backend
11. [Error handling](error-handling.md): `Action`, exception handlers, user-facing messages
12. [Testing](testing.md): UI tests on every platform
13. [Platform notes](platform-notes.md): per-platform behavior, native views, elements and lifecycle

## Reference

- [WindowedList](windowed-list.md): the virtualized list for variable-height cells
- [AI driver](ai-driver.md): drive a running app from a CLI or test, by text
- [Telemetry](telemetry.md): traces, crash reports, screen attributes

## Elsewhere

- [Example app](https://kiteui.cs.lightningkite.com/): every component live, with its source (`example-app/`).  Its `docs/` pages are compiled examples.
- [API docs (KDoc)](https://lightningkite-maven.s3.us-west-2.amazonaws.com/com/lightningkite/kiteui/library/docs/index.html)
- [CHANGELOG](../CHANGELOG.md)
- For AI agents: [`.claude/skills/kiteui/`](../.claude/skills/kiteui/SKILL.md) is a condensed version of these docs.
