package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public class HtmlA(context: ElementContext): NativeElement(context) { init { native.tag = "a" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlA(setup: HtmlA.() -> Unit = {}): HtmlA {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlA(context), setup)
}
public class HtmlAbbr(context: ElementContext): NativeElement(context) { init { native.tag = "abbr" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlAbbr(setup: HtmlAbbr.() -> Unit = {}): HtmlAbbr {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlAbbr(context), setup)
}
public class HtmlAddress(context: ElementContext): NativeElement(context) { init { native.tag = "address" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlAddress(setup: HtmlAddress.() -> Unit = {}): HtmlAddress {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlAddress(context), setup)
}
public class HtmlArea(context: ElementContext): NativeElement(context) { init { native.tag = "area" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlArea(setup: HtmlArea.() -> Unit = {}): HtmlArea {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlArea(context), setup)
}
public class HtmlArticle(context: ElementContext): NativeElement(context) { init { native.tag = "article" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlArticle(setup: HtmlArticle.() -> Unit = {}): HtmlArticle {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlArticle(context), setup)
}
public class HtmlAside(context: ElementContext): NativeElement(context) { init { native.tag = "aside" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlAside(setup: HtmlAside.() -> Unit = {}): HtmlAside {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlAside(context), setup)
}
public class HtmlAudio(context: ElementContext): NativeElement(context) { init { native.tag = "audio" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlAudio(setup: HtmlAudio.() -> Unit = {}): HtmlAudio {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlAudio(context), setup)
}
public class HtmlB(context: ElementContext): NativeElement(context) { init { native.tag = "b" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlB(setup: HtmlB.() -> Unit = {}): HtmlB {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlB(context), setup)
}
public class HtmlBase(context: ElementContext): NativeElement(context) { init { native.tag = "base" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlBase(setup: HtmlBase.() -> Unit = {}): HtmlBase {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlBase(context), setup)
}
public class HtmlBdi(context: ElementContext): NativeElement(context) { init { native.tag = "bdi" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlBdi(setup: HtmlBdi.() -> Unit = {}): HtmlBdi {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlBdi(context), setup)
}
public class HtmlBdo(context: ElementContext): NativeElement(context) { init { native.tag = "bdo" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlBdo(setup: HtmlBdo.() -> Unit = {}): HtmlBdo {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlBdo(context), setup)
}
public class HtmlBlockquote(context: ElementContext): NativeElement(context) { init { native.tag = "blockquote" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlBlockquote(setup: HtmlBlockquote.() -> Unit = {}): HtmlBlockquote {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlBlockquote(context), setup)
}
public class HtmlBody(context: ElementContext): NativeElement(context) { init { native.tag = "body" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlBody(setup: HtmlBody.() -> Unit = {}): HtmlBody {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlBody(context), setup)
}
public class HtmlBr(context: ElementContext): NativeElement(context) { init { native.tag = "br" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlBr(setup: HtmlBr.() -> Unit = {}): HtmlBr {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlBr(context), setup)
}
public class HtmlButton(context: ElementContext): NativeElement(context) { init { native.tag = "button" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlButton(setup: HtmlButton.() -> Unit = {}): HtmlButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlButton(context), setup)
}
public class HtmlCanvas(context: ElementContext): NativeElement(context) { init { native.tag = "canvas" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlCanvas(setup: HtmlCanvas.() -> Unit = {}): HtmlCanvas {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlCanvas(context), setup)
}
public class HtmlCaption(context: ElementContext): NativeElement(context) { init { native.tag = "caption" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlCaption(setup: HtmlCaption.() -> Unit = {}): HtmlCaption {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlCaption(context), setup)
}
public class HtmlCite(context: ElementContext): NativeElement(context) { init { native.tag = "cite" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlCite(setup: HtmlCite.() -> Unit = {}): HtmlCite {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlCite(context), setup)
}
public class HtmlCode(context: ElementContext): NativeElement(context) { init { native.tag = "code" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlCode(setup: HtmlCode.() -> Unit = {}): HtmlCode {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlCode(context), setup)
}
public class HtmlCol(context: ElementContext): NativeElement(context) { init { native.tag = "col" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlCol(setup: HtmlCol.() -> Unit = {}): HtmlCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlCol(context), setup)
}
public class HtmlColgroup(context: ElementContext): NativeElement(context) { init { native.tag = "colgroup" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlColgroup(setup: HtmlColgroup.() -> Unit = {}): HtmlColgroup {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlColgroup(context), setup)
}
public class HtmlData(context: ElementContext): NativeElement(context) { init { native.tag = "data" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlData(setup: HtmlData.() -> Unit = {}): HtmlData {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlData(context), setup)
}
public class HtmlDatalist(context: ElementContext): NativeElement(context) { init { native.tag = "datalist" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlDatalist(setup: HtmlDatalist.() -> Unit = {}): HtmlDatalist {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlDatalist(context), setup)
}
public class HtmlDd(context: ElementContext): NativeElement(context) { init { native.tag = "dd" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlDd(setup: HtmlDd.() -> Unit = {}): HtmlDd {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlDd(context), setup)
}
public class HtmlDel(context: ElementContext): NativeElement(context) { init { native.tag = "del" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlDel(setup: HtmlDel.() -> Unit = {}): HtmlDel {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlDel(context), setup)
}
public class HtmlDetails(context: ElementContext): NativeElement(context) { init { native.tag = "details" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlDetails(setup: HtmlDetails.() -> Unit = {}): HtmlDetails {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlDetails(context), setup)
}
public class HtmlDfn(context: ElementContext): NativeElement(context) { init { native.tag = "dfn" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlDfn(setup: HtmlDfn.() -> Unit = {}): HtmlDfn {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlDfn(context), setup)
}
public class HtmlDialog(context: ElementContext): NativeElement(context) { init { native.tag = "dialog" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlDialog(setup: HtmlDialog.() -> Unit = {}): HtmlDialog {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlDialog(context), setup)
}
public class HtmlDiv(context: ElementContext): NativeElement(context) { init { native.tag = "div" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlDiv(setup: HtmlDiv.() -> Unit = {}): HtmlDiv {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlDiv(context), setup)
}
public class HtmlDl(context: ElementContext): NativeElement(context) { init { native.tag = "dl" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlDl(setup: HtmlDl.() -> Unit = {}): HtmlDl {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlDl(context), setup)
}
public class HtmlDt(context: ElementContext): NativeElement(context) { init { native.tag = "dt" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlDt(setup: HtmlDt.() -> Unit = {}): HtmlDt {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlDt(context), setup)
}
public class HtmlEm(context: ElementContext): NativeElement(context) { init { native.tag = "em" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlEm(setup: HtmlEm.() -> Unit = {}): HtmlEm {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlEm(context), setup)
}
public class HtmlEmbed(context: ElementContext): NativeElement(context) { init { native.tag = "embed" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlEmbed(setup: HtmlEmbed.() -> Unit = {}): HtmlEmbed {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlEmbed(context), setup)
}
public class HtmlFieldset(context: ElementContext): NativeElement(context) { init { native.tag = "fieldset" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlFieldset(setup: HtmlFieldset.() -> Unit = {}): HtmlFieldset {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlFieldset(context), setup)
}
public class HtmlFigcaption(context: ElementContext): NativeElement(context) { init { native.tag = "figcaption" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlFigcaption(setup: HtmlFigcaption.() -> Unit = {}): HtmlFigcaption {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlFigcaption(context), setup)
}
public class HtmlFigure(context: ElementContext): NativeElement(context) { init { native.tag = "figure" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlFigure(setup: HtmlFigure.() -> Unit = {}): HtmlFigure {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlFigure(context), setup)
}
public class HtmlFooter(context: ElementContext): NativeElement(context) { init { native.tag = "footer" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlFooter(setup: HtmlFooter.() -> Unit = {}): HtmlFooter {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlFooter(context), setup)
}
public class HtmlForm(context: ElementContext): NativeElement(context) { init { native.tag = "form" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlForm(setup: HtmlForm.() -> Unit = {}): HtmlForm {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlForm(context), setup)
}
public class HtmlH1(context: ElementContext): NativeElement(context) { init { native.tag = "h1" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlH1(setup: HtmlH1.() -> Unit = {}): HtmlH1 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlH1(context), setup)
}
public class HtmlH2(context: ElementContext): NativeElement(context) { init { native.tag = "h2" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlH2(setup: HtmlH2.() -> Unit = {}): HtmlH2 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlH2(context), setup)
}
public class HtmlH3(context: ElementContext): NativeElement(context) { init { native.tag = "h3" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlH3(setup: HtmlH3.() -> Unit = {}): HtmlH3 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlH3(context), setup)
}
public class HtmlH4(context: ElementContext): NativeElement(context) { init { native.tag = "h4" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlH4(setup: HtmlH4.() -> Unit = {}): HtmlH4 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlH4(context), setup)
}
public class HtmlH5(context: ElementContext): NativeElement(context) { init { native.tag = "h5" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlH5(setup: HtmlH5.() -> Unit = {}): HtmlH5 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlH5(context), setup)
}
public class HtmlH6(context: ElementContext): NativeElement(context) { init { native.tag = "h6" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlH6(setup: HtmlH6.() -> Unit = {}): HtmlH6 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlH6(context), setup)
}
public class HtmlHead(context: ElementContext): NativeElement(context) { init { native.tag = "head" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlHead(setup: HtmlHead.() -> Unit = {}): HtmlHead {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlHead(context), setup)
}
public class HtmlHeader(context: ElementContext): NativeElement(context) { init { native.tag = "header" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlHeader(setup: HtmlHeader.() -> Unit = {}): HtmlHeader {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlHeader(context), setup)
}
public class HtmlHgroup(context: ElementContext): NativeElement(context) { init { native.tag = "hgroup" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlHgroup(setup: HtmlHgroup.() -> Unit = {}): HtmlHgroup {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlHgroup(context), setup)
}
public class HtmlHr(context: ElementContext): NativeElement(context) { init { native.tag = "hr" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlHr(setup: HtmlHr.() -> Unit = {}): HtmlHr {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlHr(context), setup)
}
public class HtmlHtml(context: ElementContext): NativeElement(context) { init { native.tag = "html" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlHtml(setup: HtmlHtml.() -> Unit = {}): HtmlHtml {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlHtml(context), setup)
}
public class HtmlI(context: ElementContext): NativeElement(context) { init { native.tag = "i" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlI(setup: HtmlI.() -> Unit = {}): HtmlI {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlI(context), setup)
}
public class HtmlIframe(context: ElementContext): NativeElement(context) { init { native.tag = "iframe" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlIframe(setup: HtmlIframe.() -> Unit = {}): HtmlIframe {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlIframe(context), setup)
}
public class HtmlImg(context: ElementContext): NativeElement(context) { init { native.tag = "img" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlImg(setup: HtmlImg.() -> Unit = {}): HtmlImg {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlImg(context), setup)
}
public class HtmlInput(context: ElementContext): NativeElement(context) { init { native.tag = "input" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlInput(setup: HtmlInput.() -> Unit = {}): HtmlInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlInput(context), setup)
}
public class HtmlIns(context: ElementContext): NativeElement(context) { init { native.tag = "ins" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlIns(setup: HtmlIns.() -> Unit = {}): HtmlIns {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlIns(context), setup)
}
public class HtmlKbd(context: ElementContext): NativeElement(context) { init { native.tag = "kbd" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlKbd(setup: HtmlKbd.() -> Unit = {}): HtmlKbd {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlKbd(context), setup)
}
public class HtmlLabel(context: ElementContext): NativeElement(context) { init { native.tag = "label" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlLabel(setup: HtmlLabel.() -> Unit = {}): HtmlLabel {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlLabel(context), setup)
}
public class HtmlLegend(context: ElementContext): NativeElement(context) { init { native.tag = "legend" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlLegend(setup: HtmlLegend.() -> Unit = {}): HtmlLegend {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlLegend(context), setup)
}
public class HtmlLi(context: ElementContext): NativeElement(context) { init { native.tag = "li" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlLi(setup: HtmlLi.() -> Unit = {}): HtmlLi {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlLi(context), setup)
}
public class HtmlLink(context: ElementContext): NativeElement(context) { init { native.tag = "link" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlLink(setup: HtmlLink.() -> Unit = {}): HtmlLink {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlLink(context), setup)
}
public class HtmlMain(context: ElementContext): NativeElement(context) { init { native.tag = "main" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlMain(setup: HtmlMain.() -> Unit = {}): HtmlMain {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlMain(context), setup)
}
public class HtmlMap(context: ElementContext): NativeElement(context) { init { native.tag = "map" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlMap(setup: HtmlMap.() -> Unit = {}): HtmlMap {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlMap(context), setup)
}
public class HtmlMark(context: ElementContext): NativeElement(context) { init { native.tag = "mark" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlMark(setup: HtmlMark.() -> Unit = {}): HtmlMark {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlMark(context), setup)
}
public class HtmlMenu(context: ElementContext): NativeElement(context) { init { native.tag = "menu" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlMenu(setup: HtmlMenu.() -> Unit = {}): HtmlMenu {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlMenu(context), setup)
}
public class HtmlMeta(context: ElementContext): NativeElement(context) { init { native.tag = "meta" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlMeta(setup: HtmlMeta.() -> Unit = {}): HtmlMeta {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlMeta(context), setup)
}
public class HtmlMeter(context: ElementContext): NativeElement(context) { init { native.tag = "meter" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlMeter(setup: HtmlMeter.() -> Unit = {}): HtmlMeter {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlMeter(context), setup)
}
public class HtmlNav(context: ElementContext): NativeElement(context) { init { native.tag = "nav" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlNav(setup: HtmlNav.() -> Unit = {}): HtmlNav {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlNav(context), setup)
}
public class HtmlNoscript(context: ElementContext): NativeElement(context) { init { native.tag = "noscript" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlNoscript(setup: HtmlNoscript.() -> Unit = {}): HtmlNoscript {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlNoscript(context), setup)
}
public class HtmlObject(context: ElementContext): NativeElement(context) { init { native.tag = "object" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlHtmlObject(setup: HtmlObject.() -> Unit = {}): HtmlObject {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlObject(context), setup)
}
public class HtmlOl(context: ElementContext): NativeElement(context) { init { native.tag = "ol" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlOl(setup: HtmlOl.() -> Unit = {}): HtmlOl {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlOl(context), setup)
}
public class HtmlOptgroup(context: ElementContext): NativeElement(context) { init { native.tag = "optgroup" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlOptgroup(setup: HtmlOptgroup.() -> Unit = {}): HtmlOptgroup {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlOptgroup(context), setup)
}
public class HtmlOption(context: ElementContext): NativeElement(context) { init { native.tag = "option" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlOption(setup: HtmlOption.() -> Unit = {}): HtmlOption {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlOption(context), setup)
}
public class HtmlOutput(context: ElementContext): NativeElement(context) { init { native.tag = "output" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlOutput(setup: HtmlOutput.() -> Unit = {}): HtmlOutput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlOutput(context), setup)
}
public class HtmlP(context: ElementContext): NativeElement(context) { init { native.tag = "p" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlP(setup: HtmlP.() -> Unit = {}): HtmlP {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlP(context), setup)
}
public class HtmlPicture(context: ElementContext): NativeElement(context) { init { native.tag = "picture" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlPicture(setup: HtmlPicture.() -> Unit = {}): HtmlPicture {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlPicture(context), setup)
}
public class HtmlPre(context: ElementContext): NativeElement(context) { init { native.tag = "pre" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlPre(setup: HtmlPre.() -> Unit = {}): HtmlPre {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlPre(context), setup)
}
public class HtmlProgress(context: ElementContext): NativeElement(context) { init { native.tag = "progress" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlProgress(setup: HtmlProgress.() -> Unit = {}): HtmlProgress {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlProgress(context), setup)
}
public class HtmlQ(context: ElementContext): NativeElement(context) { init { native.tag = "q" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlQ(setup: HtmlQ.() -> Unit = {}): HtmlQ {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlQ(context), setup)
}
public class HtmlRp(context: ElementContext): NativeElement(context) { init { native.tag = "rp" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlRp(setup: HtmlRp.() -> Unit = {}): HtmlRp {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlRp(context), setup)
}
public class HtmlRt(context: ElementContext): NativeElement(context) { init { native.tag = "rt" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlRt(setup: HtmlRt.() -> Unit = {}): HtmlRt {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlRt(context), setup)
}
public class HtmlRuby(context: ElementContext): NativeElement(context) { init { native.tag = "ruby" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlRuby(setup: HtmlRuby.() -> Unit = {}): HtmlRuby {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlRuby(context), setup)
}
public class HtmlS(context: ElementContext): NativeElement(context) { init { native.tag = "s" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlS(setup: HtmlS.() -> Unit = {}): HtmlS {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlS(context), setup)
}
public class HtmlSamp(context: ElementContext): NativeElement(context) { init { native.tag = "samp" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlSamp(setup: HtmlSamp.() -> Unit = {}): HtmlSamp {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlSamp(context), setup)
}
public class HtmlScript(context: ElementContext): NativeElement(context) { init { native.tag = "script" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlScript(setup: HtmlScript.() -> Unit = {}): HtmlScript {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlScript(context), setup)
}
public class HtmlSearch(context: ElementContext): NativeElement(context) { init { native.tag = "search" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlSearch(setup: HtmlSearch.() -> Unit = {}): HtmlSearch {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlSearch(context), setup)
}
public class HtmlSection(context: ElementContext): NativeElement(context) { init { native.tag = "section" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlSection(setup: HtmlSection.() -> Unit = {}): HtmlSection {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlSection(context), setup)
}
public class HtmlSelect(context: ElementContext): NativeElement(context) { init { native.tag = "select" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlSelect(setup: HtmlSelect.() -> Unit = {}): HtmlSelect {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlSelect(context), setup)
}
public class HtmlSlot(context: ElementContext): NativeElement(context) { init { native.tag = "slot" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlSlot(setup: HtmlSlot.() -> Unit = {}): HtmlSlot {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlSlot(context), setup)
}
public class HtmlSmall(context: ElementContext): NativeElement(context) { init { native.tag = "small" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlSmall(setup: HtmlSmall.() -> Unit = {}): HtmlSmall {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlSmall(context), setup)
}
public class HtmlSource(context: ElementContext): NativeElement(context) { init { native.tag = "source" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlSource(setup: HtmlSource.() -> Unit = {}): HtmlSource {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlSource(context), setup)
}
public class HtmlSpan(context: ElementContext): NativeElement(context) { init { native.tag = "span" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlSpan(setup: HtmlSpan.() -> Unit = {}): HtmlSpan {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlSpan(context), setup)
}
public class HtmlStrong(context: ElementContext): NativeElement(context) { init { native.tag = "strong" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlStrong(setup: HtmlStrong.() -> Unit = {}): HtmlStrong {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlStrong(context), setup)
}
public class HtmlStyle(context: ElementContext): NativeElement(context) { init { native.tag = "style" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlStyle(setup: HtmlStyle.() -> Unit = {}): HtmlStyle {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlStyle(context), setup)
}
public class HtmlSub(context: ElementContext): NativeElement(context) { init { native.tag = "sub" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlSub(setup: HtmlSub.() -> Unit = {}): HtmlSub {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlSub(context), setup)
}
public class HtmlSummary(context: ElementContext): NativeElement(context) { init { native.tag = "summary" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlSummary(setup: HtmlSummary.() -> Unit = {}): HtmlSummary {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlSummary(context), setup)
}
public class HtmlSup(context: ElementContext): NativeElement(context) { init { native.tag = "sup" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlSup(setup: HtmlSup.() -> Unit = {}): HtmlSup {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlSup(context), setup)
}
public class HtmlTable(context: ElementContext): NativeElement(context) { init { native.tag = "table" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlTable(setup: HtmlTable.() -> Unit = {}): HtmlTable {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlTable(context), setup)
}
public class HtmlTbody(context: ElementContext): NativeElement(context) { init { native.tag = "tbody" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlTbody(setup: HtmlTbody.() -> Unit = {}): HtmlTbody {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlTbody(context), setup)
}
public class HtmlTd(context: ElementContext): NativeElement(context) { init { native.tag = "td" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlTd(setup: HtmlTd.() -> Unit = {}): HtmlTd {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlTd(context), setup)
}
public class HtmlTemplate(context: ElementContext): NativeElement(context) { init { native.tag = "template" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlTemplate(setup: HtmlTemplate.() -> Unit = {}): HtmlTemplate {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlTemplate(context), setup)
}
public class HtmlTextarea(context: ElementContext): NativeElement(context) { init { native.tag = "textarea" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlTextarea(setup: HtmlTextarea.() -> Unit = {}): HtmlTextarea {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlTextarea(context), setup)
}
public class HtmlTfoot(context: ElementContext): NativeElement(context) { init { native.tag = "tfoot" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlTfoot(setup: HtmlTfoot.() -> Unit = {}): HtmlTfoot {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlTfoot(context), setup)
}
public class HtmlTh(context: ElementContext): NativeElement(context) { init { native.tag = "th" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlTh(setup: HtmlTh.() -> Unit = {}): HtmlTh {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlTh(context), setup)
}
public class HtmlThead(context: ElementContext): NativeElement(context) { init { native.tag = "thead" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlThead(setup: HtmlThead.() -> Unit = {}): HtmlThead {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlThead(context), setup)
}
public class HtmlTime(context: ElementContext): NativeElement(context) { init { native.tag = "time" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlTime(setup: HtmlTime.() -> Unit = {}): HtmlTime {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlTime(context), setup)
}
public class HtmlTitle(context: ElementContext): NativeElement(context) { init { native.tag = "title" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlTitle(setup: HtmlTitle.() -> Unit = {}): HtmlTitle {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlTitle(context), setup)
}
public class HtmlTr(context: ElementContext): NativeElement(context) { init { native.tag = "tr" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlTr(setup: HtmlTr.() -> Unit = {}): HtmlTr {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlTr(context), setup)
}
public class HtmlTrack(context: ElementContext): NativeElement(context) { init { native.tag = "track" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlTrack(setup: HtmlTrack.() -> Unit = {}): HtmlTrack {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlTrack(context), setup)
}
public class HtmlU(context: ElementContext): NativeElement(context) { init { native.tag = "u" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlU(setup: HtmlU.() -> Unit = {}): HtmlU {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlU(context), setup)
}
public class HtmlUl(context: ElementContext): NativeElement(context) { init { native.tag = "ul" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlUl(setup: HtmlUl.() -> Unit = {}): HtmlUl {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlUl(context), setup)
}
public class HtmlVar(context: ElementContext): NativeElement(context) { init { native.tag = "var" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlHtmlVar(setup: HtmlVar.() -> Unit = {}): HtmlVar {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlVar(context), setup)
}
public class HtmlVideo(context: ElementContext): NativeElement(context) { init { native.tag = "video" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlVideo(setup: HtmlVideo.() -> Unit = {}): HtmlVideo {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlVideo(context), setup)
}
public class HtmlWbr(context: ElementContext): NativeElement(context) { init { native.tag = "wbr" } }
@OptIn(ExperimentalContracts::class)
@ViewDsl
public inline fun ViewWriter.htmlWbr(setup: HtmlWbr.() -> Unit = {}): HtmlWbr {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HtmlWbr(context), setup)
}