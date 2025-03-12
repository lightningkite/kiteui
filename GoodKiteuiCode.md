# Writing Good KiteUI Code

## General tips

- Components should:
  - Take a minimal number of parameters that are unique per usage and directly related to the component.
  - Should load their own data.  This makes them easy to debug and work with independently.
  - Should be used more than once.  Single-use components can usually just be inlined for better readability.
- Modifier Order
  - Position > Visibility > Scroll > Theme