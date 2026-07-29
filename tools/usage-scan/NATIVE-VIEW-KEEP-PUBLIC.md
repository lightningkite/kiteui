# Native-view narrowing — the custom-view extension API MUST stay public

Downstream authors build **custom native views** by subclassing the framework's base classes and
registering via `ElementWriter.write`. `library-camera` is a real out-of-tree example and is a
compile gate. So beyond the usual "keep used/floor declarations public," this whole extension
surface stays PUBLIC even if unused by the sample consumers. Internalize ONLY concrete,
self-contained glue for the framework's OWN built-in widgets.

## ALWAYS KEEP PUBLIC (extension API — never internalize)

Core (commonMain `views/`):
- `Element`, `ElementWithChildren`, `ContainerElement`, and their author-facing / `@OverrideOnly` members.
- `NativeElement`, `NativeElementCommonCode` (+ nested `ThemePipeline`, `Step`, `ThemeForElement`, `GetBaseTheme`).
- `NativeContainerElement`, `NativeContainerElementCommonCode`.
- The `InteractiveElement` family: `InteractiveElement`, `ElementWithAction`, `ElementWithSecondaryAction`,
  `NativeInteractiveElement`, `NativeInteractiveContainerElement`, `NativeElementWithAction`,
  `NativeElementWithSecondaryAction`, `NativeContainerElementWithAction`,
  `NativeContainerElementWithSecondaryAction`, and their `nativeSetAction`/`nativeSetSecondaryAction` hooks.
- `ElementWriter` (+ nested `CanAdd*` interfaces), `ViewWriter`, `ElementWriter.write`, `ensureOutermostElement`.
- `ElementContext` (+ `ElementContextCommonCode`, `ChainMap`), platform `ElementContext.activity` (Android) / `ElementContext.controller` (iOS).
- Annotations `@ViewDsl`, `@OverrideOnly`, `@ExperimentalKiteUi`, `@InternalKiteUi`.
- Any `expect`/`actual` declaration (visibility must match; never touch), any `override`, any interface member.
- Every built-in component's `expect class` + its DSL builder function (`button {}`, `separator()`, etc.).

Platform extension surface (KEEP PUBLIC):
- `Element.native` accessor — Android `val Element.native: View`, iOS `val Element.native: UIView`.
- Android `NativeElement`: `native`, `defaultLayoutParams()`, `background`, `updateCorners()`,
  `getBackgroundWithRipple`, `applyThemeWithRipple`.
- iOS `NativeElement`: `native`, `addChildTarget`, `disableBackground`, `sizeConstraints`, `applyBackgroundChanges`.
- Reusable layout INFRASTRUCTURE a custom view/container builds on:
  - Android: `SimplifiedLinearLayout`, `DesiredSizeView`.
  - iOS: `FrameLayout`, `WrapperView`.

## SAFE TO INTERNALIZE (concrete built-in glue only)

Named, self-contained platform helper `View`/`UIView` subclasses that implement ONE built-in widget
and are not extension points — e.g.:
- Android: `NSeparator`, `NSpace`, `FrameLayoutButton`, `TwoWayNestedScrollView`, `TextViewWithGradient`,
  `CoordinatorLayoutWithGestures`, `GlideImageView`, and per-widget private plumbing.
- iOS: `FrameLayoutButton`, `UILabelWithGradient`, `UILabelWithLayerBackground`, `GlassFrameLayout`,
  `TextFieldInput`, and the built-in row/col/scroll engines `FlexLayout`, `ScrollLayout`, `LinearLayout`.
- Internal layout engines only built-ins use: `ProgrammaticLayout`, `CoordinatorFrame`, `pusedoframe`, `sizeThatFits2`.

Only internalize a candidate that is such glue. If a declaration is (or might be) an extension point,
an `actual`, an `override`, an interface member, or reusable layout infrastructure — KEEP PUBLIC.
When in doubt, keep public: the compile gate includes `library-camera` (a real custom view) but it
cannot see other org repos' custom views, so judgment is the real protection.
