# KiteUI Coding Guidelines

## Deprecated Functions and Their Replacements

### Input Fields
- Use `textInput` instead of `textField` (deprecated)
- Use `numberInput` instead of `numberField` (deprecated)

## View Modifiers

### Alignment
- Use `align(horizontal, vertical)` instead of `gravity(horizontal, vertical)` (deprecated)
- Use `centered` as a shortcut for `align(Align.Center, Align.Center)`

### Visibility
- Use `shownWhen(condition = { ... })` instead of `onlyWhen(condition = { ... })` (deprecated)

## Reactivity

### Accessing Signal Values
- Use direct invocation syntax `signal()` instead of `signal.await()` (deprecated)
- Example: `text { reactive { content = "Value: " + mySignal() } }`

### Reactive Binding
- Two ways to handle reactivity:
  1. Using `reactive { content = "signal = ${signal()}" }`
  2. Using `::content { "signal = ${signal()}" }`

### Creating Derived Signals
- Use `remember { ... }` for creating derived signals (`shared` is the deprecated older name)
- Example: `val derived = remember { signalA() + signalB() }`

## View Construction

### Prefix vs Postfix Style
Both styles are valid, choose based on readability in context:
- Prefix style (using dash): `important - button { text("Hello World") }`
- Postfix style (using 'in'): `button { text("Hello World") } in important`

### Nesting Components
When nesting components with multiple modifiers, use the postfix style with 'in' for better readability:
```kotlin
glassFrame {
    centered - text("Glass Frame")
} in align(Align.Center, Align.Center) in sizeConstraints(width = 200.px, height = 100.px)
```

## Best Practices

### Card Usage
When adding text to a card, prefer the postfix style:
```kotlin
text("Content") in card
```
Instead of:
```kotlin
card - text("Content")
```

### String Concatenation
When displaying property values in text, use string concatenation instead of string templates:
```kotlin
content = "Value: " + property().toString()
```