# KiteUI Rendering Package Research & Plan

## Current State

KiteUI already has a basic 2D canvas abstraction:
- `Canvas` view with `CanvasDelegate` (`library/src/commonMain/kotlin/com/lightningkite/kiteui/views/direct/Canvas.kt`)
- `DrawingContext2D` expect/actual that wraps platform-native 2D APIs:
  - Android: `android.graphics.Canvas`
  - iOS: CoreGraphics (CGContext)
  - JS: HTML5 Canvas 2D Context
  - Swing: `java.awt.Graphics2D`

This existing Canvas is **CPU-based immediate mode 2D** - fine for simple drawing, but not GPU-accelerated for complex scenes.

---

## Goals for New Rendering Package

**Primary use case**: Lightweight game development

**Priority order**:
1. **Skiko (higher priority)**: Rich 2D graphics with GPU acceleration - good for 2D games
2. **OpenGL ES unified API (lower priority)**: Direct GPU access for 3D games, custom shaders

**Requirements**:
- All platforms in sync (Android, iOS, JS, JVM)
- Unified API across platforms (especially for low-level GPU layer)

---

## Option Analysis

### High-Level APIs (Skia-like)

#### Option A: Skiko (JetBrains)
**What it is**: Official Kotlin Multiplatform bindings to Skia (the graphics library behind Chrome, Android, Flutter)

**Platforms**: JVM, Android (API 24+), iOS, macOS, JS/WASM, Linux, Windows

**Graphics backends by platform**:
- macOS: Metal or OpenGL
- iOS/tvOS: Metal only
- Windows/Linux: OpenGL, Vulkan, or Direct3D
- Android: OpenGL ES
- Web: WebGL via WASM

**Maven**: `org.jetbrains.skiko:skiko-awt-runtime-[target]:0.9.39`

**Pros**:
- Powerful, battle-tested (used by Compose Multiplatform)
- Rich 2D API: paths, gradients, shadows, text shaping, image processing
- GPU-accelerated on all platforms
- Active development by JetBrains

**Cons**:
- Large binary size (~15-30MB per platform)
- Complex to integrate with native views (Skiko expects to own the window/layer)
- Adds significant dependency weight

**Integration approach**:
```kotlin
expect class SkiaCanvas(context: RContext) : RView {
    val surface: Readable<SkiaSurface?>
    var delegate: SkiaCanvasDelegate?
}

abstract class SkiaCanvasDelegate {
    open fun draw(canvas: org.jetbrains.skia.Canvas) {}
    // ... similar to CanvasDelegate
}
```

#### Option B: Wrap Existing Platform APIs More Completely

Enhance the existing `DrawingContext2D` to cover more of each platform's native graphics API without adding Skia.

**Pros**:
- No additional binary size
- Uses platform-native rendering (better integration)
- Familiar APIs on each platform

**Cons**:
- Feature parity is difficult (each platform has different capabilities)
- No GPU acceleration on some paths
- More maintenance burden

---

### Low-Level APIs (OpenGL/GPU)

#### Option C: Custom Thin OpenGL Bindings

Create lightweight expect/actual bindings for OpenGL ES 3.0 (lowest common denominator).

**Platform mappings**:
| Platform | API |
|----------|-----|
| Android | OpenGL ES 3.0 (native) |
| iOS | OpenGL ES 3.0 (deprecated) or Metal via ANGLE |
| JS | WebGL 2.0 |
| JVM Desktop | LWJGL OpenGL 3.3 |

**Pros**:
- Minimal binary size
- Full GPU control
- Good for games, shaders, 3D

**Cons**:
- Low-level (need to manage buffers, shaders, state)
- OpenGL ES is deprecated on iOS/macOS (Apple pushing Metal)
- Significant development effort

#### Option D: Use Korender (Compose-integrated 3D engine)
**What it is**: Kotlin Multiplatform 3D engine on OpenGL/ES/WebGL

**Platforms**: Desktop (OpenGL 3.3), Android (GLES 3), Web (WebGL 2)
**Note**: No iOS support yet

**Maven**: `com.github.zakgof:korender:0.6.1`

**Pros**:
- Declarative API that works with Compose
- Full 3D rendering: lighting, shadows, PBR materials, glTF loading
- Active development

**Cons**:
- No iOS support (deal-breaker for KiteUI)
- Heavy dependency for simple use cases
- Tightly coupled to Compose

#### Option E: Use Kool Engine
**What it is**: Multi-backend graphics engine (Vulkan/WebGPU/OpenGL)

**Platforms**: Desktop JVM, Android, JS/WASM
**Note**: No iOS support for now

**Maven**: `de.fabmax.kool:kool-core:0.19.0`

**Pros**:
- Modern backends (Vulkan, WebGPU)
- Full 3D engine with physics
- Kotlin DSL for shaders

**Cons**:
- No iOS support
- Very heavy (full game engine)
- Overkill for most use cases

---

## Recommended Approach

Given KiteUI's design philosophy and the goal of lightweight game development, I recommend:

### Package Structure

```
library-skia/                  # Priority 1: Skiko integration (2D games)
├── src/
│   ├── commonMain/
│   │   └── kotlin/com/lightningkite/kiteui/skia/
│   │       ├── views/
│   │       │   └── SkiaCanvas.kt
│   │       └── models/
│   │           └── SkiaImage.kt (etc.)
│   ├── androidMain/
│   ├── iosMain/
│   ├── jsMain/
│   └── jvmMain/

library-gles/                  # Priority 2: Unified OpenGL ES (3D games)
├── src/
│   ├── commonMain/
│   │   └── kotlin/com/lightningkite/kiteui/gles/
│   │       ├── GL.kt          # OpenGL ES 3.0 API
│   │       └── views/
│   │           └── GLCanvas.kt
│   ├── androidMain/           # Native OpenGL ES 3.0
│   ├── iosMain/               # ANGLE (OpenGL ES → Metal)
│   ├── jsMain/                # WebGL 2 (≈ OpenGL ES 3.0)
│   └── jvmMain/               # LWJGL OpenGL 3.3
```

---

## What is ANGLE?

[ANGLE](https://github.com/google/angle) (Almost Native Graphics Layer Engine) is Google's **translation layer** that lets you write OpenGL ES code once and run it on any platform:

| Platform | ANGLE translates OpenGL ES to... |
|----------|----------------------------------|
| iOS/macOS | Metal |
| Windows | Direct3D 11 |
| Linux | Vulkan or native OpenGL |
| Android | Native OpenGL ES (passthrough) |

**Why it matters**: Apple deprecated OpenGL ES in 2018 and will eventually remove it. ANGLE lets us write a single OpenGL ES API and have it work on iOS via Metal translation.

**Who uses it**: Chrome, Firefox, Safari all use ANGLE for WebGL. Flutter uses it too.

**Binary impact**: ~5-10MB per platform (native library)

---

## Package 1: library-skia (Priority - 2D Games)

**Purpose**: GPU-accelerated 2D graphics via Skiko (JetBrains' Skia bindings)

**Why Skiko for games**:
- Hardware-accelerated rendering (uses Metal on iOS, OpenGL/Vulkan elsewhere)
- Rich 2D API: sprites, paths, transforms, blend modes, shaders
- Text rendering with proper shaping
- Image loading and manipulation
- Sufficient for most 2D games (platformers, puzzle games, card games, etc.)

### API Design

```kotlin
// View that renders via Skia
expect class SkiaCanvas(context: RContext) : RView {
    var delegate: SkiaCanvasDelegate?
}

abstract class SkiaCanvasDelegate {
    /** Called each frame to draw */
    open fun draw(canvas: org.jetbrains.skia.Canvas, width: Float, height: Float) {}

    /** Input handling (inherited pattern from CanvasDelegate) */
    open fun onPointerDown(id: Int, x: Float, y: Float): Boolean = false
    open fun onPointerMove(id: Int, x: Float, y: Float): Boolean = false
    open fun onPointerUp(id: Int, x: Float, y: Float): Boolean = false
    open fun onKeyDown(key: KeyCode): Boolean = false
    open fun onKeyUp(key: KeyCode): Boolean = false

    /** Request redraw */
    var invalidate: () -> Unit = {}
}

// Convenience: continuous rendering for games
expect class SkiaGameLoop(context: RContext) : RView {
    var delegate: SkiaGameDelegate?
    var targetFps: Int  // default 60
    var running: Boolean
}

abstract class SkiaGameDelegate {
    open fun update(deltaTime: Float) {}  // Called each frame
    open fun draw(canvas: org.jetbrains.skia.Canvas, width: Float, height: Float) {}
    // ... input handlers
}
```

### Platform Implementation Notes

| Platform | Skiko Backend | Surface |
|----------|---------------|---------|
| Android | OpenGL ES / Vulkan | SurfaceView or TextureView |
| iOS | Metal | CAMetalLayer via UIView |
| JS | WebGL (WASM) | HTML Canvas |
| JVM | OpenGL / Metal | AWT Canvas / Swing component |

**Estimated work**: 2-3 weeks
**Binary impact**: +15-30MB per platform (Skia native libraries)

---

## Package 2: library-gles (Lower Priority - 3D Games)

**Purpose**: Unified OpenGL ES 3.0 API across all platforms for 3D rendering and custom shaders

**Why OpenGL ES 3.0**:
- Lowest common denominator that's powerful enough for games
- WebGL 2 is essentially OpenGL ES 3.0
- ANGLE provides translation to Metal/Direct3D
- Well-documented, tons of learning resources

### API Design

```kotlin
// Thin wrapper exposing OpenGL ES 3.0 functions
object GL {
    // Constants
    const val TRIANGLES = 0x0004
    const val ARRAY_BUFFER = 0x8892
    const val STATIC_DRAW = 0x88E4
    // ... all OpenGL ES 3.0 constants

    // Functions (expect/actual per platform)
    expect fun clearColor(r: Float, g: Float, b: Float, a: Float)
    expect fun clear(mask: Int)
    expect fun createShader(type: Int): Int
    expect fun shaderSource(shader: Int, source: String)
    expect fun compileShader(shader: Int)
    expect fun createProgram(): Int
    expect fun attachShader(program: Int, shader: Int)
    expect fun linkProgram(program: Int)
    expect fun useProgram(program: Int)
    expect fun createBuffer(): Int
    expect fun bindBuffer(target: Int, buffer: Int)
    expect fun bufferData(target: Int, data: FloatArray, usage: Int)
    expect fun drawArrays(mode: Int, first: Int, count: Int)
    // ... complete OpenGL ES 3.0 API
}

// View that provides GL context
expect class GLCanvas(context: RContext) : RView {
    var delegate: GLCanvasDelegate?
}

abstract class GLCanvasDelegate {
    /** Called once when GL context is ready */
    open fun onSurfaceCreated() {}

    /** Called when surface size changes */
    open fun onSurfaceChanged(width: Int, height: Int) {}

    /** Called each frame to render */
    open fun onDrawFrame() {}

    /** Called when surface is destroyed */
    open fun onSurfaceDestroyed() {}

    var invalidate: () -> Unit = {}
}
```

### Platform Implementation

| Platform | Implementation |
|----------|----------------|
| Android | GLSurfaceView + native OpenGL ES 3.0 |
| iOS | ANGLE library (OpenGL ES → Metal) + CAEAGLLayer |
| JS | WebGL 2 (direct mapping) |
| JVM | LWJGL + OpenGL 3.3 core profile |

**iOS specifics with ANGLE**:
- Include ANGLE as a CocoaPod or xcframework
- ANGLE provides EGL + OpenGL ES 3.0 API
- Under the hood, translates to Metal

**Estimated work**: 3-4 weeks (more work due to ANGLE integration)
**Binary impact**: ~5-10MB for ANGLE on iOS, minimal on other platforms

---

## Implementation Plan

### Phase 1: library-skia (Skiko Integration)

**Goal**: Get Skiko working on all platforms for 2D game development

#### Step 1.1: Project Setup
- Create `library-skia/` module mirroring `library-lottie/` structure
- Add Skiko dependencies per platform
- Set up CocoaPods for iOS

#### Step 1.2: Core API (commonMain)
- `SkiaCanvas` expect class with delegate pattern
- `SkiaCanvasDelegate` abstract class
- `SkiaGameLoop` for continuous rendering
- Input event handling

#### Step 1.3: Platform Implementations (all in parallel)
- **Android**: SkiaLayer on SurfaceView
- **iOS**: SkiaLayer on UIView with CAMetalLayer
- **JS**: SkiaLayer on HTML Canvas (WASM)
- **JVM**: SkiaLayer on Swing component

#### Step 1.4: Testing & Examples
- Simple drawing example (shapes, images)
- Game loop example (moving sprite)
- Input handling example (touch/mouse, keyboard)

**Deliverable**: Working `SkiaCanvas` and `SkiaGameLoop` on all 4 platforms

---

### Phase 2: library-gles (OpenGL ES Unified API)

**Goal**: Unified OpenGL ES 3.0 API for 3D games

#### Step 2.1: Project Setup
- Create `library-gles/` module
- LWJGL dependency for JVM
- ANGLE CocoaPod/xcframework for iOS

#### Step 2.2: Core API (commonMain)
- `GL` object with all OpenGL ES 3.0 constants and functions
- `GLCanvas` expect class with delegate pattern
- `GLCanvasDelegate` abstract class

#### Step 2.3: Platform Implementations (all in parallel)
- **Android**: GLSurfaceView + native GLES 3.0
- **iOS**: ANGLE + EAGLLayer
- **JS**: WebGL 2 (maps directly)
- **JVM**: LWJGL + OpenGL 3.3 core

#### Step 2.4: Testing & Examples
- Triangle example (vertex buffers, shaders)
- Textured quad example
- Simple 3D cube with perspective

**Deliverable**: Working `GLCanvas` with unified OpenGL ES 3.0 API on all 4 platforms

---

### Phase 3: Game Development Utilities (Optional)

Once both packages are working:

1. **Sprite utilities**: Easy sprite batching, animation frames
2. **Simple 2D physics**: Collision detection helpers
3. **Asset loading**: Unified image/audio loading
4. **Scene graph**: Simple node hierarchy for games

---

## Technical Notes

### Skiko Integration Details

Skiko provides `SkiaLayer` which needs a native surface to render to:

```kotlin
// Android: Create SkiaLayer attached to SurfaceView
val layer = SkiaLayer()
layer.attachTo(surfaceView)
layer.skikoView = object : SkikoView {
    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        delegate?.draw(canvas, width.toFloat(), height.toFloat())
    }
}

// iOS: Create SkiaLayer attached to UIView
let layer = SkiaLayer()
layer.attachTo(view.layer)
// Similar SkikoView implementation

// JS: Create SkiaLayer attached to HTML Canvas
val layer = SkiaLayer()
layer.attachTo(canvasElement)
```

### ANGLE Integration for iOS

ANGLE provides OpenGL ES API that translates to Metal:

```swift
// In iOS native code
import MetalANGLE

// Create EGL display and context
let display = eglGetDisplay(EGL_DEFAULT_DISPLAY)
eglInitialize(display, nil, nil)
// ... standard EGL setup

// Then Kotlin/Native can call through C interop
```

For CocoaPods:
```ruby
pod 'AdjustableAngle', '~> 2.8'  # or build ANGLE from source
```

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Skiko binary size (15-30MB) | Document as optional package; users only include if needed |
| ANGLE iOS complexity | Start with Skiko; ANGLE is lower priority |
| Platform parity issues | Test on all platforms continuously during development |
| Skiko API stability | Skiko is stable (used by Compose); pin version |

---

## Sources

- [Skiko (JetBrains)](https://github.com/JetBrains/skiko) - Kotlin Multiplatform Skia bindings
- [ANGLE (Google)](https://github.com/google/angle) - OpenGL ES to Metal/D3D translation
- [MetalANGLE](https://github.com/kakashidinho/metalangle) - ANGLE fork with Metal backend (now merged into ANGLE)
- [KGL](https://github.com/gergelydaniel/kgl) - Lightweight OpenGL abstraction (dormant, but useful reference)
- [Korender](https://github.com/zakgof/korender) - KMP 3D engine (no iOS, but good API reference)
- [Kool Engine](https://github.com/kool-engine/kool) - Multi-backend engine (no iOS)
