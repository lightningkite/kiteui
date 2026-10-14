package com.lightningkite.kiteui.locale

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A BCP 47 / [RFC 5646](https://datatracker.ietf.org/doc/html/rfc5646) language tag, e.g. `en-US`, `zh-Hant-TW`,
 * or `de-CH-1996-u-co-phonebk`.
 *
 * Parses the `langtag` production (RFC 5646 §2.1):
 * `language ["-" extlang] ["-" script] ["-" region] *("-" variant) *("-" extension) ["-" privateuse]`,
 * as well as a private-use-only tag (`x-...`).
 *
 * Legacy "grandfathered" tags (e.g. `i-klingon`) don't fit this grammar; their subtags are left blank rather
 * than guessed at. Tags are case-insensitive per the spec and subtags are returned exactly as written.
 */
@JvmInline
@Serializable
public value class LanguageCode(public val asString: String) {

    /** Primary language subtag (ISO 639), e.g. "zh" in "zh-Hant-TW". Blank if the tag has none. */
    public val language: String get() = parse().language

    /** Extended language subtags (ISO 639-3), e.g. ["yue"] in "zh-yue-HK". At most 3, per RFC 5646. */
    public val extlangs: List<String> get() = parse().extlangs

    /** Script subtag (ISO 15924), e.g. "Hant" in "zh-Hant-TW". Null if absent. */
    public val script: String? get() = parse().script

    /** Region subtag (ISO 3166-1 or UN M.49), e.g. "TW" in "zh-Hant-TW" or "419" in "es-419". Null if absent. */
    public val region: String? get() = parse().region

    /** Variant subtags, e.g. ["1996"] in "de-CH-1996". */
    public val variants: List<String> get() = parse().variants

    /** Extension subtags, grouped by their leading singleton, e.g. ["u" to ["co", "phonebk"]] in "de-u-co-phonebk". */
    public val extensions: List<Extension> get() = parse().extensions

    /** Private-use subtags, e.g. ["private"] in both "x-private" and "en-x-private". */
    public val privateUse: List<String> get() = parse().privateUse

    private fun parse(): Parsed {
        val subtags = asString.split('-')
        if (subtags.firstOrNull()?.equals("x", ignoreCase = true) == true) {
            return Parsed(privateUse = subtags.drop(1).takeWhile { privateUseSubtagPattern.matches(it) })
        }

        val language = subtags.firstOrNull()?.takeIf { languagePattern.matches(it) } ?: return Parsed()
        var i = 1

        val extlangs = subtags.drop(i).takeWhile { extlangPattern.matches(it) }.take(3)
        i += extlangs.size

        val script = subtags.getOrNull(i)?.takeIf { scriptPattern.matches(it) }
        if (script != null) i++

        val region = subtags.getOrNull(i)?.takeIf { regionPattern.matches(it) }
        if (region != null) i++

        val variants = subtags.drop(i).takeWhile { variantPattern.matches(it) }
        i += variants.size

        val extensions = mutableListOf<Extension>()
        while (subtags.getOrNull(i)?.let { singletonPattern.matches(it) } == true) {
            val values = subtags.drop(i + 1).takeWhile { extensionSubtagPattern.matches(it) }
            if (values.isEmpty()) break
            extensions.add(Extension(subtags[i][0], values))
            i += 1 + values.size
        }

        val privateUse = if (subtags.getOrNull(i)?.equals("x", ignoreCase = true) == true) {
            subtags.drop(i + 1).takeWhile { privateUseSubtagPattern.matches(it) }
        } else emptyList()

        return Parsed(language, extlangs, script, region, variants, extensions, privateUse)
    }

    /** One `singleton "-" subtag *("-" subtag)` extension sequence, e.g. `u-co-phonebk` -> `Extension('u', ["co", "phonebk"])`. */
    public data class Extension(public val singleton: Char, public val subtags: List<String>)

    private data class Parsed(
        val language: String = "",
        val extlangs: List<String> = emptyList(),
        val script: String? = null,
        val region: String? = null,
        val variants: List<String> = emptyList(),
        val extensions: List<Extension> = emptyList(),
        val privateUse: List<String> = emptyList(),
    )

    public companion object {
        public val enUS: LanguageCode = LanguageCode("en-US")

        // Character classes from the RFC 5646 §2.1 ABNF (case folded; Regex.matches() requires a full match).
        private val languagePattern = Regex("[A-Za-z]{2,8}")
        private val extlangPattern = Regex("[A-Za-z]{3}")
        private val scriptPattern = Regex("[A-Za-z]{4}")
        private val regionPattern = Regex("[A-Za-z]{2}|[0-9]{3}")
        private val variantPattern = Regex("[A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}")
        private val singletonPattern = Regex("[0-9A-WY-Za-wy-z]") // any letter/digit except "x"
        private val extensionSubtagPattern = Regex("[A-Za-z0-9]{2,8}")
        private val privateUseSubtagPattern = Regex("[A-Za-z0-9]{1,8}")
    }
}
