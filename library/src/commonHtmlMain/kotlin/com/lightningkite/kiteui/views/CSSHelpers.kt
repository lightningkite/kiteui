package com.lightningkite.kiteui.views

import kotlin.jvm.JvmInline
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


@JvmInline
value class CSSStyleDeclaration(val record: Record<String> = Record()) {}
var CSSStyleDeclaration.cssText: String?
    get() = record["text"]
    set(value) { record["text"] = value }
var CSSStyleDeclaration.cssFloat: String?
    get() = record["float"]
    set(value) { record["float"] = value }
var CSSStyleDeclaration.alignContent: String?
    get() = record["align-content"]
    set(value) { record["align-content"] = value }
var CSSStyleDeclaration.alignItems: String?
    get() = record["align-items"]
    set(value) { record["align-items"] = value }
var CSSStyleDeclaration.alignSelf: String?
    get() = record["align-self"]
    set(value) { record["align-self"] = value }
var CSSStyleDeclaration.animation: String?
    get() = record["animation"]
    set(value) { record["animation"] = value }
var CSSStyleDeclaration.animationDelay: String?
    get() = record["animation-delay"]
    set(value) { record["animation-delay"] = value }
var CSSStyleDeclaration.animationDirection: String?
    get() = record["animation-direction"]
    set(value) { record["animation-direction"] = value }
var CSSStyleDeclaration.animationDuration: String?
    get() = record["animation-duration"]
    set(value) { record["animation-duration"] = value }
var CSSStyleDeclaration.animationFillMode: String?
    get() = record["animation-fill-mode"]
    set(value) { record["animation-fill-mode"] = value }
var CSSStyleDeclaration.animationIterationCount: String?
    get() = record["animation-iteration-count"]
    set(value) { record["animation-iteration-count"] = value }
var CSSStyleDeclaration.animationName: String?
    get() = record["animation-name"]
    set(value) { record["animation-name"] = value }
var CSSStyleDeclaration.animationPlayState: String?
    get() = record["animation-play-state"]
    set(value) { record["animation-play-state"] = value }
var CSSStyleDeclaration.animationTimingFunction: String?
    get() = record["animation-timing-function"]
    set(value) { record["animation-timing-function"] = value }
var CSSStyleDeclaration.backfaceVisibility: String?
    get() = record["backface-visibility"]
    set(value) { record["backface-visibility"] = value }
var CSSStyleDeclaration.background: String?
    get() = record["background"]
    set(value) { record["background"] = value }
var CSSStyleDeclaration.backgroundAttachment: String?
    get() = record["background-attachment"]
    set(value) { record["background-attachment"] = value }
var CSSStyleDeclaration.backgroundClip: String?
    get() = record["background-clip"]
    set(value) { record["background-clip"] = value }
var CSSStyleDeclaration.backgroundColor: String?
    get() = record["background-color"]
    set(value) { record["background-color"] = value }
var CSSStyleDeclaration.backgroundImage: String?
    get() = record["background-image"]
    set(value) { record["background-image"] = value }
var CSSStyleDeclaration.backgroundOrigin: String?
    get() = record["background-origin"]
    set(value) { record["background-origin"] = value }
var CSSStyleDeclaration.backgroundPosition: String?
    get() = record["background-position"]
    set(value) { record["background-position"] = value }
var CSSStyleDeclaration.backgroundRepeat: String?
    get() = record["background-repeat"]
    set(value) { record["background-repeat"] = value }
var CSSStyleDeclaration.backgroundSize: String?
    get() = record["background-size"]
    set(value) { record["background-size"] = value }
var CSSStyleDeclaration.border: String?
    get() = record["border"]
    set(value) { record["border"] = value }
var CSSStyleDeclaration.borderBottom: String?
    get() = record["border-bottom"]
    set(value) { record["border-bottom"] = value }
var CSSStyleDeclaration.borderBottomColor: String?
    get() = record["border-bottom-color"]
    set(value) { record["border-bottom-color"] = value }
var CSSStyleDeclaration.borderBottomLeftRadius: String?
    get() = record["border-bottom-left-radius"]
    set(value) { record["border-bottom-left-radius"] = value }
var CSSStyleDeclaration.borderBottomRightRadius: String?
    get() = record["border-bottom-right-radius"]
    set(value) { record["border-bottom-right-radius"] = value }
var CSSStyleDeclaration.borderBottomStyle: String?
    get() = record["border-bottom-style"]
    set(value) { record["border-bottom-style"] = value }
var CSSStyleDeclaration.borderBottomWidth: String?
    get() = record["border-bottom-width"]
    set(value) { record["border-bottom-width"] = value }
var CSSStyleDeclaration.borderCollapse: String?
    get() = record["border-collapse"]
    set(value) { record["border-collapse"] = value }
var CSSStyleDeclaration.borderColor: String?
    get() = record["border-color"]
    set(value) { record["border-color"] = value }
var CSSStyleDeclaration.borderImage: String?
    get() = record["border-image"]
    set(value) { record["border-image"] = value }
var CSSStyleDeclaration.borderImageOutset: String?
    get() = record["border-image-outset"]
    set(value) { record["border-image-outset"] = value }
var CSSStyleDeclaration.borderImageRepeat: String?
    get() = record["border-image-repeat"]
    set(value) { record["border-image-repeat"] = value }
var CSSStyleDeclaration.borderImageSlice: String?
    get() = record["border-image-slice"]
    set(value) { record["border-image-slice"] = value }
var CSSStyleDeclaration.borderImageSource: String?
    get() = record["border-image-source"]
    set(value) { record["border-image-source"] = value }
var CSSStyleDeclaration.borderImageWidth: String?
    get() = record["border-image-width"]
    set(value) { record["border-image-width"] = value }
var CSSStyleDeclaration.borderLeft: String?
    get() = record["border-left"]
    set(value) { record["border-left"] = value }
var CSSStyleDeclaration.borderLeftColor: String?
    get() = record["border-left-color"]
    set(value) { record["border-left-color"] = value }
var CSSStyleDeclaration.borderLeftStyle: String?
    get() = record["border-left-style"]
    set(value) { record["border-left-style"] = value }
var CSSStyleDeclaration.borderLeftWidth: String?
    get() = record["border-left-width"]
    set(value) { record["border-left-width"] = value }
var CSSStyleDeclaration.borderRadius: String?
    get() = record["border-radius"]
    set(value) { record["border-radius"] = value }
var CSSStyleDeclaration.borderRight: String?
    get() = record["border-right"]
    set(value) { record["border-right"] = value }
var CSSStyleDeclaration.borderRightColor: String?
    get() = record["border-right-color"]
    set(value) { record["border-right-color"] = value }
var CSSStyleDeclaration.borderRightStyle: String?
    get() = record["border-right-style"]
    set(value) { record["border-right-style"] = value }
var CSSStyleDeclaration.borderRightWidth: String?
    get() = record["border-right-width"]
    set(value) { record["border-right-width"] = value }
var CSSStyleDeclaration.borderSpacing: String?
    get() = record["border-spacing"]
    set(value) { record["border-spacing"] = value }
var CSSStyleDeclaration.borderStyle: String?
    get() = record["border-style"]
    set(value) { record["border-style"] = value }
var CSSStyleDeclaration.borderTop: String?
    get() = record["border-top"]
    set(value) { record["border-top"] = value }
var CSSStyleDeclaration.borderTopColor: String?
    get() = record["border-top-color"]
    set(value) { record["border-top-color"] = value }
var CSSStyleDeclaration.borderTopLeftRadius: String?
    get() = record["border-top-left-radius"]
    set(value) { record["border-top-left-radius"] = value }
var CSSStyleDeclaration.borderTopRightRadius: String?
    get() = record["border-top-right-radius"]
    set(value) { record["border-top-right-radius"] = value }
var CSSStyleDeclaration.borderTopStyle: String?
    get() = record["border-top-style"]
    set(value) { record["border-top-style"] = value }
var CSSStyleDeclaration.borderTopWidth: String?
    get() = record["border-top-width"]
    set(value) { record["border-top-width"] = value }
var CSSStyleDeclaration.borderWidth: String?
    get() = record["border-width"]
    set(value) { record["border-width"] = value }
var CSSStyleDeclaration.bottom: String?
    get() = record["bottom"]
    set(value) { record["bottom"] = value }
var CSSStyleDeclaration.boxDecorationBreak: String?
    get() = record["box-decoration-break"]
    set(value) { record["box-decoration-break"] = value }
var CSSStyleDeclaration.boxShadow: String?
    get() = record["box-shadow"]
    set(value) { record["box-shadow"] = value }
var CSSStyleDeclaration.boxSizing: String?
    get() = record["box-sizing"]
    set(value) { record["box-sizing"] = value }
var CSSStyleDeclaration.breakAfter: String?
    get() = record["break-after"]
    set(value) { record["break-after"] = value }
var CSSStyleDeclaration.breakBefore: String?
    get() = record["break-before"]
    set(value) { record["break-before"] = value }
var CSSStyleDeclaration.breakInside: String?
    get() = record["break-inside"]
    set(value) { record["break-inside"] = value }
var CSSStyleDeclaration.captionSide: String?
    get() = record["caption-side"]
    set(value) { record["caption-side"] = value }
var CSSStyleDeclaration.clear: String?
    get() = record["clear"]
    set(value) { record["clear"] = value }
var CSSStyleDeclaration.clip: String?
    get() = record["clip"]
    set(value) { record["clip"] = value }
var CSSStyleDeclaration.color: String?
    get() = record["color"]
    set(value) { record["color"] = value }
var CSSStyleDeclaration.columnCount: String?
    get() = record["column-count"]
    set(value) { record["column-count"] = value }
var CSSStyleDeclaration.columnFill: String?
    get() = record["column-fill"]
    set(value) { record["column-fill"] = value }
var CSSStyleDeclaration.columnGap: String?
    get() = record["column-gap"]
    set(value) { record["column-gap"] = value }
var CSSStyleDeclaration.columnRule: String?
    get() = record["column-rule"]
    set(value) { record["column-rule"] = value }
var CSSStyleDeclaration.columnRuleColor: String?
    get() = record["column-rule-color"]
    set(value) { record["column-rule-color"] = value }
var CSSStyleDeclaration.columnRuleStyle: String?
    get() = record["column-rule-style"]
    set(value) { record["column-rule-style"] = value }
var CSSStyleDeclaration.columnRuleWidth: String?
    get() = record["column-rule-width"]
    set(value) { record["column-rule-width"] = value }
var CSSStyleDeclaration.columnSpan: String?
    get() = record["column-span"]
    set(value) { record["column-span"] = value }
var CSSStyleDeclaration.columnWidth: String?
    get() = record["column-width"]
    set(value) { record["column-width"] = value }
var CSSStyleDeclaration.columns: String?
    get() = record["columns"]
    set(value) { record["columns"] = value }
var CSSStyleDeclaration.content: String?
    get() = record["content"]
    set(value) { record["content"] = value }
var CSSStyleDeclaration.counterIncrement: String?
    get() = record["counter-increment"]
    set(value) { record["counter-increment"] = value }
var CSSStyleDeclaration.counterReset: String?
    get() = record["counter-reset"]
    set(value) { record["counter-reset"] = value }
var CSSStyleDeclaration.cursor: String?
    get() = record["cursor"]
    set(value) { record["cursor"] = value }
var CSSStyleDeclaration.direction: String?
    get() = record["direction"]
    set(value) { record["direction"] = value }
var CSSStyleDeclaration.display: String?
    get() = record["display"]
    set(value) { record["display"] = value }
var CSSStyleDeclaration.emptyCells: String?
    get() = record["empty-cells"]
    set(value) { record["empty-cells"] = value }
var CSSStyleDeclaration.filter: String?
    get() = record["filter"]
    set(value) { record["filter"] = value }
var CSSStyleDeclaration.flex: String?
    get() = record["flex"]
    set(value) { record["flex"] = value }
var CSSStyleDeclaration.flexBasis: String?
    get() = record["flex-basis"]
    set(value) { record["flex-basis"] = value }
var CSSStyleDeclaration.flexDirection: String?
    get() = record["flex-direction"]
    set(value) { record["flex-direction"] = value }
var CSSStyleDeclaration.flexFlow: String?
    get() = record["flex-flow"]
    set(value) { record["flex-flow"] = value }
var CSSStyleDeclaration.flexGrow: String?
    get() = record["flex-grow"]
    set(value) { record["flex-grow"] = value }
var CSSStyleDeclaration.flexShrink: String?
    get() = record["flex-shrink"]
    set(value) { record["flex-shrink"] = value }
var CSSStyleDeclaration.flexWrap: String?
    get() = record["flex-wrap"]
    set(value) { record["flex-wrap"] = value }
var CSSStyleDeclaration.font: String?
    get() = record["font"]
    set(value) { record["font"] = value }
var CSSStyleDeclaration.fontFamily: String?
    get() = record["font-family"]
    set(value) { record["font-family"] = value }
var CSSStyleDeclaration.fontFeatureSettings: String?
    get() = record["font-feature-settings"]
    set(value) { record["font-feature-settings"] = value }
var CSSStyleDeclaration.fontKerning: String?
    get() = record["font-kerning"]
    set(value) { record["font-kerning"] = value }
var CSSStyleDeclaration.fontLanguageOverride: String?
    get() = record["font-language-override"]
    set(value) { record["font-language-override"] = value }
var CSSStyleDeclaration.fontSize: String?
    get() = record["font-size"]
    set(value) { record["font-size"] = value }
var CSSStyleDeclaration.fontSizeAdjust: String?
    get() = record["font-size-adjust"]
    set(value) { record["font-size-adjust"] = value }
var CSSStyleDeclaration.fontStretch: String?
    get() = record["font-stretch"]
    set(value) { record["font-stretch"] = value }
var CSSStyleDeclaration.fontStyle: String?
    get() = record["font-style"]
    set(value) { record["font-style"] = value }
var CSSStyleDeclaration.fontSynthesis: String?
    get() = record["font-synthesis"]
    set(value) { record["font-synthesis"] = value }
var CSSStyleDeclaration.fontVariant: String?
    get() = record["font-variant"]
    set(value) { record["font-variant"] = value }
var CSSStyleDeclaration.fontVariantAlternates: String?
    get() = record["font-variant-alternates"]
    set(value) { record["font-variant-alternates"] = value }
var CSSStyleDeclaration.fontVariantCaps: String?
    get() = record["font-variant-caps"]
    set(value) { record["font-variant-caps"] = value }
var CSSStyleDeclaration.fontVariantEastAsian: String?
    get() = record["font-variant-east-asian"]
    set(value) { record["font-variant-east-asian"] = value }
var CSSStyleDeclaration.fontVariantLigatures: String?
    get() = record["font-variant-ligatures"]
    set(value) { record["font-variant-ligatures"] = value }
var CSSStyleDeclaration.fontVariantNumeric: String?
    get() = record["font-variant-numeric"]
    set(value) { record["font-variant-numeric"] = value }
var CSSStyleDeclaration.fontVariantPosition: String?
    get() = record["font-variant-position"]
    set(value) { record["font-variant-position"] = value }
var CSSStyleDeclaration.fontWeight: String?
    get() = record["font-weight"]
    set(value) { record["font-weight"] = value }
var CSSStyleDeclaration.hangingPunctuation: String?
    get() = record["hanging-punctuation"]
    set(value) { record["hanging-punctuation"] = value }
var CSSStyleDeclaration.height: String?
    get() = record["height"]
    set(value) { record["height"] = value }
var CSSStyleDeclaration.hyphens: String?
    get() = record["hyphens"]
    set(value) { record["hyphens"] = value }
var CSSStyleDeclaration.imageOrientation: String?
    get() = record["image-orientation"]
    set(value) { record["image-orientation"] = value }
var CSSStyleDeclaration.imageRendering: String?
    get() = record["image-rendering"]
    set(value) { record["image-rendering"] = value }
var CSSStyleDeclaration.imageResolution: String?
    get() = record["image-resolution"]
    set(value) { record["image-resolution"] = value }
var CSSStyleDeclaration.imeMode: String?
    get() = record["ime-mode"]
    set(value) { record["ime-mode"] = value }
var CSSStyleDeclaration.justifyContent: String?
    get() = record["justify-content"]
    set(value) { record["justify-content"] = value }
var CSSStyleDeclaration.left: String?
    get() = record["left"]
    set(value) { record["left"] = value }
var CSSStyleDeclaration.letterSpacing: String?
    get() = record["letter-spacing"]
    set(value) { record["letter-spacing"] = value }
var CSSStyleDeclaration.lineBreak: String?
    get() = record["line-break"]
    set(value) { record["line-break"] = value }
var CSSStyleDeclaration.lineHeight: String?
    get() = record["line-height"]
    set(value) { record["line-height"] = value }
var CSSStyleDeclaration.listStyle: String?
    get() = record["list-style"]
    set(value) { record["list-style"] = value }
var CSSStyleDeclaration.listStyleImage: String?
    get() = record["list-style-image"]
    set(value) { record["list-style-image"] = value }
var CSSStyleDeclaration.listStylePosition: String?
    get() = record["list-style-position"]
    set(value) { record["list-style-position"] = value }
var CSSStyleDeclaration.listStyleType: String?
    get() = record["list-style-type"]
    set(value) { record["list-style-type"] = value }
var CSSStyleDeclaration.margin: String?
    get() = record["margin"]
    set(value) { record["margin"] = value }
var CSSStyleDeclaration.marginBottom: String?
    get() = record["margin-bottom"]
    set(value) { record["margin-bottom"] = value }
var CSSStyleDeclaration.marginLeft: String?
    get() = record["margin-left"]
    set(value) { record["margin-left"] = value }
var CSSStyleDeclaration.marginRight: String?
    get() = record["margin-right"]
    set(value) { record["margin-right"] = value }
var CSSStyleDeclaration.marginTop: String?
    get() = record["margin-top"]
    set(value) { record["margin-top"] = value }
var CSSStyleDeclaration.mark: String?
    get() = record["mark"]
    set(value) { record["mark"] = value }
var CSSStyleDeclaration.markAfter: String?
    get() = record["mark-after"]
    set(value) { record["mark-after"] = value }
var CSSStyleDeclaration.markBefore: String?
    get() = record["mark-before"]
    set(value) { record["mark-before"] = value }
var CSSStyleDeclaration.marks: String?
    get() = record["marks"]
    set(value) { record["marks"] = value }
var CSSStyleDeclaration.marqueeDirection: String?
    get() = record["marquee-direction"]
    set(value) { record["marquee-direction"] = value }
var CSSStyleDeclaration.marqueePlayCount: String?
    get() = record["marquee-play-count"]
    set(value) { record["marquee-play-count"] = value }
var CSSStyleDeclaration.marqueeSpeed: String?
    get() = record["marquee-speed"]
    set(value) { record["marquee-speed"] = value }
var CSSStyleDeclaration.marqueeStyle: String?
    get() = record["marquee-style"]
    set(value) { record["marquee-style"] = value }
var CSSStyleDeclaration.mask: String?
    get() = record["mask"]
    set(value) { record["mask"] = value }
var CSSStyleDeclaration.maskType: String?
    get() = record["mask-type"]
    set(value) { record["mask-type"] = value }
var CSSStyleDeclaration.maxHeight: String?
    get() = record["max-height"]
    set(value) { record["max-height"] = value }
var CSSStyleDeclaration.maxWidth: String?
    get() = record["max-width"]
    set(value) { record["max-width"] = value }
var CSSStyleDeclaration.minHeight: String?
    get() = record["min-height"]
    set(value) { record["min-height"] = value }
var CSSStyleDeclaration.minWidth: String?
    get() = record["min-width"]
    set(value) { record["min-width"] = value }
var CSSStyleDeclaration.navDown: String?
    get() = record["nav-down"]
    set(value) { record["nav-down"] = value }
var CSSStyleDeclaration.navIndex: String?
    get() = record["nav-index"]
    set(value) { record["nav-index"] = value }
var CSSStyleDeclaration.navLeft: String?
    get() = record["nav-left"]
    set(value) { record["nav-left"] = value }
var CSSStyleDeclaration.navRight: String?
    get() = record["nav-right"]
    set(value) { record["nav-right"] = value }
var CSSStyleDeclaration.navUp: String?
    get() = record["nav-up"]
    set(value) { record["nav-up"] = value }
var CSSStyleDeclaration.objectFit: String?
    get() = record["object-fit"]
    set(value) { record["object-fit"] = value }
var CSSStyleDeclaration.objectPosition: String?
    get() = record["object-position"]
    set(value) { record["object-position"] = value }
var CSSStyleDeclaration.opacity: String?
    get() = record["opacity"]
    set(value) { record["opacity"] = value }
var CSSStyleDeclaration.order: String?
    get() = record["order"]
    set(value) { record["order"] = value }
var CSSStyleDeclaration.orphans: String?
    get() = record["orphans"]
    set(value) { record["orphans"] = value }
var CSSStyleDeclaration.outline: String?
    get() = record["outline"]
    set(value) { record["outline"] = value }
var CSSStyleDeclaration.outlineColor: String?
    get() = record["outline-color"]
    set(value) { record["outline-color"] = value }
var CSSStyleDeclaration.outlineOffset: String?
    get() = record["outline-offset"]
    set(value) { record["outline-offset"] = value }
var CSSStyleDeclaration.outlineStyle: String?
    get() = record["outline-style"]
    set(value) { record["outline-style"] = value }
var CSSStyleDeclaration.outlineWidth: String?
    get() = record["outline-width"]
    set(value) { record["outline-width"] = value }
var CSSStyleDeclaration.overflowWrap: String?
    get() = record["overflow-wrap"]
    set(value) { record["overflow-wrap"] = value }
var CSSStyleDeclaration.overflowX: String?
    get() = record["overflow-x"]
    set(value) { record["overflow-x"] = value }
var CSSStyleDeclaration.overflowY: String?
    get() = record["overflow-y"]
    set(value) { record["overflow-y"] = value }
var CSSStyleDeclaration.padding: String?
    get() = record["padding"]
    set(value) { record["padding"] = value }
var CSSStyleDeclaration.paddingBottom: String?
    get() = record["padding-bottom"]
    set(value) { record["padding-bottom"] = value }
var CSSStyleDeclaration.paddingLeft: String?
    get() = record["padding-left"]
    set(value) { record["padding-left"] = value }
var CSSStyleDeclaration.paddingRight: String?
    get() = record["padding-right"]
    set(value) { record["padding-right"] = value }
var CSSStyleDeclaration.paddingTop: String?
    get() = record["padding-top"]
    set(value) { record["padding-top"] = value }
var CSSStyleDeclaration.pageBreakAfter: String?
    get() = record["page-break-after"]
    set(value) { record["page-break-after"] = value }
var CSSStyleDeclaration.pageBreakBefore: String?
    get() = record["page-break-before"]
    set(value) { record["page-break-before"] = value }
var CSSStyleDeclaration.pageBreakInside: String?
    get() = record["page-break-inside"]
    set(value) { record["page-break-inside"] = value }
var CSSStyleDeclaration.perspective: String?
    get() = record["perspective"]
    set(value) { record["perspective"] = value }
var CSSStyleDeclaration.perspectiveOrigin: String?
    get() = record["perspective-origin"]
    set(value) { record["perspective-origin"] = value }
var CSSStyleDeclaration.phonemes: String?
    get() = record["phonemes"]
    set(value) { record["phonemes"] = value }
var CSSStyleDeclaration.position: String?
    get() = record["position"]
    set(value) { record["position"] = value }
var CSSStyleDeclaration.quotes: String?
    get() = record["quotes"]
    set(value) { record["quotes"] = value }
var CSSStyleDeclaration.resize: String?
    get() = record["resize"]
    set(value) { record["resize"] = value }
var CSSStyleDeclaration.rest: String?
    get() = record["rest"]
    set(value) { record["rest"] = value }
var CSSStyleDeclaration.restAfter: String?
    get() = record["rest-after"]
    set(value) { record["rest-after"] = value }
var CSSStyleDeclaration.restBefore: String?
    get() = record["rest-before"]
    set(value) { record["rest-before"] = value }
var CSSStyleDeclaration.right: String?
    get() = record["right"]
    set(value) { record["right"] = value }
var CSSStyleDeclaration.tabSize: String?
    get() = record["tab-size"]
    set(value) { record["tab-size"] = value }
var CSSStyleDeclaration.tableLayout: String?
    get() = record["table-layout"]
    set(value) { record["table-layout"] = value }
var CSSStyleDeclaration.textAlign: String?
    get() = record["text-align"]
    set(value) { record["text-align"] = value }
var CSSStyleDeclaration.textAlignLast: String?
    get() = record["text-align-last"]
    set(value) { record["text-align-last"] = value }
var CSSStyleDeclaration.textCombineUpright: String?
    get() = record["text-combine-upright"]
    set(value) { record["text-combine-upright"] = value }
var CSSStyleDeclaration.textDecoration: String?
    get() = record["text-decoration"]
    set(value) { record["text-decoration"] = value }
var CSSStyleDeclaration.textDecorationColor: String?
    get() = record["text-decoration-color"]
    set(value) { record["text-decoration-color"] = value }
var CSSStyleDeclaration.textDecorationLine: String?
    get() = record["text-decoration-line"]
    set(value) { record["text-decoration-line"] = value }
var CSSStyleDeclaration.textDecorationStyle: String?
    get() = record["text-decoration-style"]
    set(value) { record["text-decoration-style"] = value }
var CSSStyleDeclaration.textIndent: String?
    get() = record["text-indent"]
    set(value) { record["text-indent"] = value }
var CSSStyleDeclaration.textJustify: String?
    get() = record["text-justify"]
    set(value) { record["text-justify"] = value }
var CSSStyleDeclaration.textOrientation: String?
    get() = record["text-orientation"]
    set(value) { record["text-orientation"] = value }
var CSSStyleDeclaration.textOverflow: String?
    get() = record["text-overflow"]
    set(value) { record["text-overflow"] = value }
var CSSStyleDeclaration.textShadow: String?
    get() = record["text-shadow"]
    set(value) { record["text-shadow"] = value }
var CSSStyleDeclaration.textTransform: String?
    get() = record["text-transform"]
    set(value) { record["text-transform"] = value }
var CSSStyleDeclaration.textUnderlinePosition: String?
    get() = record["text-underline-position"]
    set(value) { record["text-underline-position"] = value }
var CSSStyleDeclaration.top: String?
    get() = record["top"]
    set(value) { record["top"] = value }
var CSSStyleDeclaration.transform: String?
    get() = record["transform"]
    set(value) { record["transform"] = value }
var CSSStyleDeclaration.transformOrigin: String?
    get() = record["transform-origin"]
    set(value) { record["transform-origin"] = value }
var CSSStyleDeclaration.transformStyle: String?
    get() = record["transform-style"]
    set(value) { record["transform-style"] = value }
var CSSStyleDeclaration.transition: String?
    get() = record["transition"]
    set(value) { record["transition"] = value }
var CSSStyleDeclaration.transitionDelay: String?
    get() = record["transition-delay"]
    set(value) { record["transition-delay"] = value }
var CSSStyleDeclaration.transitionDuration: String?
    get() = record["transition-duration"]
    set(value) { record["transition-duration"] = value }
var CSSStyleDeclaration.transitionProperty: String?
    get() = record["transition-property"]
    set(value) { record["transition-property"] = value }
var CSSStyleDeclaration.transitionTimingFunction: String?
    get() = record["transition-timing-function"]
    set(value) { record["transition-timing-function"] = value }
var CSSStyleDeclaration.unicodeBidi: String?
    get() = record["unicode-bidi"]
    set(value) { record["unicode-bidi"] = value }
var CSSStyleDeclaration.verticalAlign: String?
    get() = record["vertical-align"]
    set(value) { record["vertical-align"] = value }
var CSSStyleDeclaration.visibility: String?
    get() = record["visibility"]
    set(value) { record["visibility"] = value }
var CSSStyleDeclaration.voiceBalance: String?
    get() = record["voice-balance"]
    set(value) { record["voice-balance"] = value }
var CSSStyleDeclaration.voiceDuration: String?
    get() = record["voice-duration"]
    set(value) { record["voice-duration"] = value }
var CSSStyleDeclaration.voicePitch: String?
    get() = record["voice-pitch"]
    set(value) { record["voice-pitch"] = value }
var CSSStyleDeclaration.voicePitchRange: String?
    get() = record["voice-pitch-range"]
    set(value) { record["voice-pitch-range"] = value }
var CSSStyleDeclaration.voiceRate: String?
    get() = record["voice-rate"]
    set(value) { record["voice-rate"] = value }
var CSSStyleDeclaration.voiceStress: String?
    get() = record["voice-stress"]
    set(value) { record["voice-stress"] = value }
var CSSStyleDeclaration.voiceVolume: String?
    get() = record["voice-volume"]
    set(value) { record["voice-volume"] = value }
var CSSStyleDeclaration.whiteSpace: String?
    get() = record["white-space"]
    set(value) { record["white-space"] = value }
var CSSStyleDeclaration.widows: String?
    get() = record["widows"]
    set(value) { record["widows"] = value }
var CSSStyleDeclaration.width: String?
    get() = record["width"]
    set(value) { record["width"] = value }
var CSSStyleDeclaration.wordBreak: String?
    get() = record["word-break"]
    set(value) { record["word-break"] = value }
var CSSStyleDeclaration.wordSpacing: String?
    get() = record["word-spacing"]
    set(value) { record["word-spacing"] = value }
var CSSStyleDeclaration.wordWrap: String?
    get() = record["word-wrap"]
    set(value) { record["word-wrap"] = value }
var CSSStyleDeclaration.writingMode: String?
    get() = record["writing-mode"]
    set(value) { record["writing-mode"] = value }
var CSSStyleDeclaration.zIndex: String?
    get() = record["z-index"]
    set(value) { record["z-index"] = value }