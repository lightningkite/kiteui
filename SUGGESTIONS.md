# KiteUI Suggestions

<!-- by Claude — Collected from patterns seen across ls-kiteui-starter, etzgo, instaclub, open-ticketer, lightning-time -->

## Relative Time Formatting

Every project using KiteUI with timestamps ends up writing its own `Instant.toRelativeTimeString()` (e.g. "5 minutes ago", "2 days ago", "just now"). This should be a built-in utility in KiteUI since it's universally needed for any app showing time-based content.

Proposed API:
```kotlin
fun Instant.toRelativeTimeString(): String
// "just now", "5 minutes ago", "2 hours ago", "yesterday", "3 days ago", "Jan 15"
```
