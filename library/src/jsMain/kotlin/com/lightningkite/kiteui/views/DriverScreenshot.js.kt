package com.lightningkite.kiteui.views

import kotlinx.coroutines.await
import kotlin.js.Promise

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
public actual suspend fun Element.driverScreenshot(): String {
    val element = this.native.element ?: throw DriverActionException("Element not yet attached to DOM")

    // modern-screenshot renders DOM to PNG via SVG foreignObject.  Two workarounds are needed:
    //
    // 1. CSSOM visibility: KiteUI inserts theme CSS via CSSStyleSheet.insertRule(), leaving the
    //    <style> element's textContent empty.  modern-screenshot clones <style> by textContent,
    //    so the theme rules are missing from the foreignObject.  Fix: add a temporary <style>
    //    with all CSSOM rules serialized as text.
    //
    // 2. !important overrides: KiteUI's static CSS has !important rules (e.g. flex-basis on
    //    column children) that override the computed styles modern-screenshot inlines on the
    //    clone.  Fix: temporarily strip !important from static CSS rules in-place via CSSOM.
    //
    // Both fixes are non-destructive — the live DOM's computed styles stay intact for
    // modern-screenshot to read, and everything is restored after the screenshot.
    val resultPromise = js("""
        import('https://esm.sh/modern-screenshot@4.4.39').then(function(mod) {
        return (function(rootEl) {
            // Fix 1: Add temporary <style> with CSSOM rules as text
            var genStyle = document.querySelector('style[title="generated-css"]');
            var extraStyle = document.createElement('style');
            if (genStyle && genStyle.sheet) {
                var rulesText = [];
                for (var r = 0; r < genStyle.sheet.cssRules.length; r++) {
                    rulesText.push(genStyle.sheet.cssRules[r].cssText.replace(/\s*!important/g, ''));
                }
                extraStyle.textContent = rulesText.join('\n');
            }
            document.head.appendChild(extraStyle);

            // Fix 2: Strip !important from static CSS rules in-place
            var restored = [];
            for (var s = 0; s < document.styleSheets.length; s++) {
                var sheet = document.styleSheets[s];
                if (sheet === extraStyle.sheet || (genStyle && sheet === genStyle.sheet)) continue;
                try {
                    for (var r = 0; r < sheet.cssRules.length; r++) {
                        var rule = sheet.cssRules[r];
                        if (rule.cssText && rule.cssText.indexOf('!important') !== -1) {
                            var original = rule.cssText;
                            var cleaned = original.replace(/\s*!important/g, '');
                            restored.push({ sheet: sheet, index: r, original: original });
                            sheet.deleteRule(r);
                            sheet.insertRule(cleaned, r);
                        }
                    }
                } catch(e) {}
            }

            // Remove cross-origin <link> stylesheets to avoid CORS errors in cloning
            var removedLinks = [];
            var links = document.querySelectorAll('link[rel="stylesheet"]');
            for (var i = 0; i < links.length; i++) {
                if (links[i].href && links[i].href.indexOf(location.origin) !== 0) {
                    removedLinks.push({ el: links[i], parent: links[i].parentElement, next: links[i].nextSibling });
                    links[i].remove();
                }
            }

            void rootEl.offsetHeight;

            function restore() {
                extraStyle.remove();
                for (var i = restored.length - 1; i >= 0; i--) {
                    try {
                        restored[i].sheet.deleteRule(restored[i].index);
                        restored[i].sheet.insertRule(restored[i].original, restored[i].index);
                    } catch(e) {}
                }
                for (var i = 0; i < removedLinks.length; i++) {
                    var l = removedLinks[i];
                    if (l.next && l.next.parentNode === l.parent) l.parent.insertBefore(l.el, l.next);
                    else l.parent.appendChild(l.el);
                }
            }

            return mod.domToPng(rootEl, { workerUrl: false, font: { skipFonts: true } }).then(function(result) {
                restore();
                return result;
            }).catch(function(err) {
                restore();
                throw err;
            });
        })(element);
        })
    """).unsafeCast<Promise<String>>()

    val dataUrl = resultPromise.await()
    val prefix = "data:image/png;base64,"
    if (!dataUrl.startsWith(prefix)) {
        throw DriverActionException("Unexpected screenshot data URL format")
    }
    return dataUrl.removePrefix(prefix)
}
