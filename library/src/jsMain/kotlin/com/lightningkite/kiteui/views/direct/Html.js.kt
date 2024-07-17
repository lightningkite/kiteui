package com.lightningkite.kiteui.views.direct

import org.w3c.dom.*

inline val HtmlA.element: HTMLAnchorElement get() = native.create() as HTMLAnchorElement
inline fun HtmlA.onElement(crossinline action: (HTMLAnchorElement)->Unit) = native.onElement { action(it as HTMLAnchorElement) }
inline val HtmlAbbr.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlAbbr.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlAddress.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlAddress.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlArea.element: HTMLAreaElement get() = native.create() as HTMLAreaElement
inline fun HtmlArea.onElement(crossinline action: (HTMLAreaElement)->Unit) = native.onElement { action(it as HTMLAreaElement) }
inline val HtmlArticle.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlArticle.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlAside.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlAside.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlAudio.element: HTMLAudioElement get() = native.create() as HTMLAudioElement
inline fun HtmlAudio.onElement(crossinline action: (HTMLAudioElement)->Unit) = native.onElement { action(it as HTMLAudioElement) }
inline val HtmlB.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlB.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlBase.element: HTMLBaseElement get() = native.create() as HTMLBaseElement
inline fun HtmlBase.onElement(crossinline action: (HTMLBaseElement)->Unit) = native.onElement { action(it as HTMLBaseElement) }
inline val HtmlBdi.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlBdi.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlBdo.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlBdo.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlBlockquote.element: HTMLQuoteElement get() = native.create() as HTMLQuoteElement
inline fun HtmlBlockquote.onElement(crossinline action: (HTMLQuoteElement)->Unit) = native.onElement { action(it as HTMLQuoteElement) }
inline val HtmlBody.element: HTMLBodyElement get() = native.create() as HTMLBodyElement
inline fun HtmlBody.onElement(crossinline action: (HTMLBodyElement)->Unit) = native.onElement { action(it as HTMLBodyElement) }
inline val HtmlBr.element: HTMLBRElement get() = native.create() as HTMLBRElement
inline fun HtmlBr.onElement(crossinline action: (HTMLBRElement)->Unit) = native.onElement { action(it as HTMLBRElement) }
inline val HtmlButton.element: HTMLButtonElement get() = native.create() as HTMLButtonElement
inline fun HtmlButton.onElement(crossinline action: (HTMLButtonElement)->Unit) = native.onElement { action(it as HTMLButtonElement) }
inline val HtmlCanvas.element: HTMLCanvasElement get() = native.create() as HTMLCanvasElement
inline fun HtmlCanvas.onElement(crossinline action: (HTMLCanvasElement)->Unit) = native.onElement { action(it as HTMLCanvasElement) }
inline val HtmlCaption.element: HTMLTableCaptionElement get() = native.create() as HTMLTableCaptionElement
inline fun HtmlCaption.onElement(crossinline action: (HTMLTableCaptionElement)->Unit) = native.onElement { action(it as HTMLTableCaptionElement) }
inline val HtmlCite.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlCite.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlCode.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlCode.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlCol.element: HTMLTableColElement get() = native.create() as HTMLTableColElement
inline fun HtmlCol.onElement(crossinline action: (HTMLTableColElement)->Unit) = native.onElement { action(it as HTMLTableColElement) }
inline val HtmlColgroup.element: HTMLTableColElement get() = native.create() as HTMLTableColElement
inline fun HtmlColgroup.onElement(crossinline action: (HTMLTableColElement)->Unit) = native.onElement { action(it as HTMLTableColElement) }
inline val HtmlData.element: HTMLDataElement get() = native.create() as HTMLDataElement
inline fun HtmlData.onElement(crossinline action: (HTMLDataElement)->Unit) = native.onElement { action(it as HTMLDataElement) }
inline val HtmlDatalist.element: HTMLDataListElement get() = native.create() as HTMLDataListElement
inline fun HtmlDatalist.onElement(crossinline action: (HTMLDataListElement)->Unit) = native.onElement { action(it as HTMLDataListElement) }
inline val HtmlDd.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlDd.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlDel.element: HTMLModElement get() = native.create() as HTMLModElement
inline fun HtmlDel.onElement(crossinline action: (HTMLModElement)->Unit) = native.onElement { action(it as HTMLModElement) }
inline val HtmlDetails.element: HTMLDetailsElement get() = native.create() as HTMLDetailsElement
inline fun HtmlDetails.onElement(crossinline action: (HTMLDetailsElement)->Unit) = native.onElement { action(it as HTMLDetailsElement) }
inline val HtmlDfn.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlDfn.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlDialog.element: HTMLDialogElement get() = native.create() as HTMLDialogElement
inline fun HtmlDialog.onElement(crossinline action: (HTMLDialogElement)->Unit) = native.onElement { action(it as HTMLDialogElement) }
inline val HtmlDiv.element: HTMLDivElement get() = native.create() as HTMLDivElement
inline fun HtmlDiv.onElement(crossinline action: (HTMLDivElement)->Unit) = native.onElement { action(it as HTMLDivElement) }
inline val HtmlDl.element: HTMLDListElement get() = native.create() as HTMLDListElement
inline fun HtmlDl.onElement(crossinline action: (HTMLDListElement)->Unit) = native.onElement { action(it as HTMLDListElement) }
inline val HtmlDt.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlDt.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlEm.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlEm.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlEmbed.element: HTMLEmbedElement get() = native.create() as HTMLEmbedElement
inline fun HtmlEmbed.onElement(crossinline action: (HTMLEmbedElement)->Unit) = native.onElement { action(it as HTMLEmbedElement) }
inline val HtmlFieldset.element: HTMLFieldSetElement get() = native.create() as HTMLFieldSetElement
inline fun HtmlFieldset.onElement(crossinline action: (HTMLFieldSetElement)->Unit) = native.onElement { action(it as HTMLFieldSetElement) }
inline val HtmlFigcaption.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlFigcaption.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlFigure.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlFigure.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlFooter.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlFooter.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlForm.element: HTMLFormElement get() = native.create() as HTMLFormElement
inline fun HtmlForm.onElement(crossinline action: (HTMLFormElement)->Unit) = native.onElement { action(it as HTMLFormElement) }
inline val HtmlH1.element: HTMLHeadingElement get() = native.create() as HTMLHeadingElement
inline fun HtmlH1.onElement(crossinline action: (HTMLHeadingElement)->Unit) = native.onElement { action(it as HTMLHeadingElement) }
inline val HtmlH2.element: HTMLHeadingElement get() = native.create() as HTMLHeadingElement
inline fun HtmlH2.onElement(crossinline action: (HTMLHeadingElement)->Unit) = native.onElement { action(it as HTMLHeadingElement) }
inline val HtmlH3.element: HTMLHeadingElement get() = native.create() as HTMLHeadingElement
inline fun HtmlH3.onElement(crossinline action: (HTMLHeadingElement)->Unit) = native.onElement { action(it as HTMLHeadingElement) }
inline val HtmlH4.element: HTMLHeadingElement get() = native.create() as HTMLHeadingElement
inline fun HtmlH4.onElement(crossinline action: (HTMLHeadingElement)->Unit) = native.onElement { action(it as HTMLHeadingElement) }
inline val HtmlH5.element: HTMLHeadingElement get() = native.create() as HTMLHeadingElement
inline fun HtmlH5.onElement(crossinline action: (HTMLHeadingElement)->Unit) = native.onElement { action(it as HTMLHeadingElement) }
inline val HtmlH6.element: HTMLHeadingElement get() = native.create() as HTMLHeadingElement
inline fun HtmlH6.onElement(crossinline action: (HTMLHeadingElement)->Unit) = native.onElement { action(it as HTMLHeadingElement) }
inline val HtmlHead.element: HTMLHeadElement get() = native.create() as HTMLHeadElement
inline fun HtmlHead.onElement(crossinline action: (HTMLHeadElement)->Unit) = native.onElement { action(it as HTMLHeadElement) }
inline val HtmlHeader.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlHeader.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlHgroup.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlHgroup.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlHr.element: HTMLHRElement get() = native.create() as HTMLHRElement
inline fun HtmlHr.onElement(crossinline action: (HTMLHRElement)->Unit) = native.onElement { action(it as HTMLHRElement) }
inline val HtmlHtml.element: HTMLHtmlElement get() = native.create() as HTMLHtmlElement
inline fun HtmlHtml.onElement(crossinline action: (HTMLHtmlElement)->Unit) = native.onElement { action(it as HTMLHtmlElement) }
inline val HtmlI.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlI.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlIframe.element: HTMLIFrameElement get() = native.create() as HTMLIFrameElement
inline fun HtmlIframe.onElement(crossinline action: (HTMLIFrameElement)->Unit) = native.onElement { action(it as HTMLIFrameElement) }
inline val HtmlImg.element: HTMLImageElement get() = native.create() as HTMLImageElement
inline fun HtmlImg.onElement(crossinline action: (HTMLImageElement)->Unit) = native.onElement { action(it as HTMLImageElement) }
inline val HtmlInput.element: HTMLInputElement get() = native.create() as HTMLInputElement
inline fun HtmlInput.onElement(crossinline action: (HTMLInputElement)->Unit) = native.onElement { action(it as HTMLInputElement) }
inline val HtmlIns.element: HTMLModElement get() = native.create() as HTMLModElement
inline fun HtmlIns.onElement(crossinline action: (HTMLModElement)->Unit) = native.onElement { action(it as HTMLModElement) }
inline val HtmlKbd.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlKbd.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlLabel.element: HTMLLabelElement get() = native.create() as HTMLLabelElement
inline fun HtmlLabel.onElement(crossinline action: (HTMLLabelElement)->Unit) = native.onElement { action(it as HTMLLabelElement) }
inline val HtmlLegend.element: HTMLLegendElement get() = native.create() as HTMLLegendElement
inline fun HtmlLegend.onElement(crossinline action: (HTMLLegendElement)->Unit) = native.onElement { action(it as HTMLLegendElement) }
inline val HtmlLi.element: HTMLLIElement get() = native.create() as HTMLLIElement
inline fun HtmlLi.onElement(crossinline action: (HTMLLIElement)->Unit) = native.onElement { action(it as HTMLLIElement) }
inline val HtmlLink.element: HTMLLinkElement get() = native.create() as HTMLLinkElement
inline fun HtmlLink.onElement(crossinline action: (HTMLLinkElement)->Unit) = native.onElement { action(it as HTMLLinkElement) }
inline val HtmlMain.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlMain.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlMap.element: HTMLMapElement get() = native.create() as HTMLMapElement
inline fun HtmlMap.onElement(crossinline action: (HTMLMapElement)->Unit) = native.onElement { action(it as HTMLMapElement) }
inline val HtmlMark.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlMark.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlMenu.element: HTMLMenuElement get() = native.create() as HTMLMenuElement
inline fun HtmlMenu.onElement(crossinline action: (HTMLMenuElement)->Unit) = native.onElement { action(it as HTMLMenuElement) }
inline val HtmlMeta.element: HTMLMetaElement get() = native.create() as HTMLMetaElement
inline fun HtmlMeta.onElement(crossinline action: (HTMLMetaElement)->Unit) = native.onElement { action(it as HTMLMetaElement) }
inline val HtmlMeter.element: HTMLMeterElement get() = native.create() as HTMLMeterElement
inline fun HtmlMeter.onElement(crossinline action: (HTMLMeterElement)->Unit) = native.onElement { action(it as HTMLMeterElement) }
inline val HtmlNav.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlNav.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlNoscript.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlNoscript.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlObject.element: HTMLObjectElement get() = native.create() as HTMLObjectElement
inline fun HtmlObject.onElement(crossinline action: (HTMLObjectElement)->Unit) = native.onElement { action(it as HTMLObjectElement) }
inline val HtmlOl.element: HTMLOListElement get() = native.create() as HTMLOListElement
inline fun HtmlOl.onElement(crossinline action: (HTMLOListElement)->Unit) = native.onElement { action(it as HTMLOListElement) }
inline val HtmlOptgroup.element: HTMLOptGroupElement get() = native.create() as HTMLOptGroupElement
inline fun HtmlOptgroup.onElement(crossinline action: (HTMLOptGroupElement)->Unit) = native.onElement { action(it as HTMLOptGroupElement) }
inline val HtmlOption.element: HTMLOptionElement get() = native.create() as HTMLOptionElement
inline fun HtmlOption.onElement(crossinline action: (HTMLOptionElement)->Unit) = native.onElement { action(it as HTMLOptionElement) }
inline val HtmlOutput.element: HTMLOutputElement get() = native.create() as HTMLOutputElement
inline fun HtmlOutput.onElement(crossinline action: (HTMLOutputElement)->Unit) = native.onElement { action(it as HTMLOutputElement) }
inline val HtmlP.element: HTMLParagraphElement get() = native.create() as HTMLParagraphElement
inline fun HtmlP.onElement(crossinline action: (HTMLParagraphElement)->Unit) = native.onElement { action(it as HTMLParagraphElement) }
inline val HtmlPicture.element: HTMLPictureElement get() = native.create() as HTMLPictureElement
inline fun HtmlPicture.onElement(crossinline action: (HTMLPictureElement)->Unit) = native.onElement { action(it as HTMLPictureElement) }
inline val HtmlPre.element: HTMLPreElement get() = native.create() as HTMLPreElement
inline fun HtmlPre.onElement(crossinline action: (HTMLPreElement)->Unit) = native.onElement { action(it as HTMLPreElement) }
inline val HtmlProgress.element: HTMLProgressElement get() = native.create() as HTMLProgressElement
inline fun HtmlProgress.onElement(crossinline action: (HTMLProgressElement)->Unit) = native.onElement { action(it as HTMLProgressElement) }
inline val HtmlQ.element: HTMLQuoteElement get() = native.create() as HTMLQuoteElement
inline fun HtmlQ.onElement(crossinline action: (HTMLQuoteElement)->Unit) = native.onElement { action(it as HTMLQuoteElement) }
inline val HtmlRp.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlRp.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlRt.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlRt.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlRuby.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlRuby.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlS.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlS.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlSamp.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlSamp.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlScript.element: HTMLScriptElement get() = native.create() as HTMLScriptElement
inline fun HtmlScript.onElement(crossinline action: (HTMLScriptElement)->Unit) = native.onElement { action(it as HTMLScriptElement) }
inline val HtmlSearch.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlSearch.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlSection.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlSection.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlSelect.element: HTMLSelectElement get() = native.create() as HTMLSelectElement
inline fun HtmlSelect.onElement(crossinline action: (HTMLSelectElement)->Unit) = native.onElement { action(it as HTMLSelectElement) }
inline val HtmlSlot.element: HTMLSlotElement get() = native.create() as HTMLSlotElement
inline fun HtmlSlot.onElement(crossinline action: (HTMLSlotElement)->Unit) = native.onElement { action(it as HTMLSlotElement) }
inline val HtmlSmall.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlSmall.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlSource.element: HTMLSourceElement get() = native.create() as HTMLSourceElement
inline fun HtmlSource.onElement(crossinline action: (HTMLSourceElement)->Unit) = native.onElement { action(it as HTMLSourceElement) }
inline val HtmlSpan.element: HTMLSpanElement get() = native.create() as HTMLSpanElement
inline fun HtmlSpan.onElement(crossinline action: (HTMLSpanElement)->Unit) = native.onElement { action(it as HTMLSpanElement) }
inline val HtmlStrong.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlStrong.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlStyle.element: HTMLStyleElement get() = native.create() as HTMLStyleElement
inline fun HtmlStyle.onElement(crossinline action: (HTMLStyleElement)->Unit) = native.onElement { action(it as HTMLStyleElement) }
inline val HtmlSub.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlSub.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlSummary.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlSummary.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlSup.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlSup.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlTable.element: HTMLTableElement get() = native.create() as HTMLTableElement
inline fun HtmlTable.onElement(crossinline action: (HTMLTableElement)->Unit) = native.onElement { action(it as HTMLTableElement) }
inline val HtmlTbody.element: HTMLTableSectionElement get() = native.create() as HTMLTableSectionElement
inline fun HtmlTbody.onElement(crossinline action: (HTMLTableSectionElement)->Unit) = native.onElement { action(it as HTMLTableSectionElement) }
inline val HtmlTd.element: HTMLTableCellElement get() = native.create() as HTMLTableCellElement
inline fun HtmlTd.onElement(crossinline action: (HTMLTableCellElement)->Unit) = native.onElement { action(it as HTMLTableCellElement) }
inline val HtmlTemplate.element: HTMLTemplateElement get() = native.create() as HTMLTemplateElement
inline fun HtmlTemplate.onElement(crossinline action: (HTMLTemplateElement)->Unit) = native.onElement { action(it as HTMLTemplateElement) }
inline val HtmlTextarea.element: HTMLTextAreaElement get() = native.create() as HTMLTextAreaElement
inline fun HtmlTextarea.onElement(crossinline action: (HTMLTextAreaElement)->Unit) = native.onElement { action(it as HTMLTextAreaElement) }
inline val HtmlTfoot.element: HTMLTableSectionElement get() = native.create() as HTMLTableSectionElement
inline fun HtmlTfoot.onElement(crossinline action: (HTMLTableSectionElement)->Unit) = native.onElement { action(it as HTMLTableSectionElement) }
inline val HtmlTh.element: HTMLTableCellElement get() = native.create() as HTMLTableCellElement
inline fun HtmlTh.onElement(crossinline action: (HTMLTableCellElement)->Unit) = native.onElement { action(it as HTMLTableCellElement) }
inline val HtmlThead.element: HTMLTableSectionElement get() = native.create() as HTMLTableSectionElement
inline fun HtmlThead.onElement(crossinline action: (HTMLTableSectionElement)->Unit) = native.onElement { action(it as HTMLTableSectionElement) }
inline val HtmlTime.element: HTMLTimeElement get() = native.create() as HTMLTimeElement
inline fun HtmlTime.onElement(crossinline action: (HTMLTimeElement)->Unit) = native.onElement { action(it as HTMLTimeElement) }
inline val HtmlTitle.element: HTMLTitleElement get() = native.create() as HTMLTitleElement
inline fun HtmlTitle.onElement(crossinline action: (HTMLTitleElement)->Unit) = native.onElement { action(it as HTMLTitleElement) }
inline val HtmlTr.element: HTMLTableRowElement get() = native.create() as HTMLTableRowElement
inline fun HtmlTr.onElement(crossinline action: (HTMLTableRowElement)->Unit) = native.onElement { action(it as HTMLTableRowElement) }
inline val HtmlTrack.element: HTMLTrackElement get() = native.create() as HTMLTrackElement
inline fun HtmlTrack.onElement(crossinline action: (HTMLTrackElement)->Unit) = native.onElement { action(it as HTMLTrackElement) }
inline val HtmlU.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlU.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlUl.element: HTMLUListElement get() = native.create() as HTMLUListElement
inline fun HtmlUl.onElement(crossinline action: (HTMLUListElement)->Unit) = native.onElement { action(it as HTMLUListElement) }
inline val HtmlVar.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlVar.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }
inline val HtmlVideo.element: HTMLVideoElement get() = native.create() as HTMLVideoElement
inline fun HtmlVideo.onElement(crossinline action: (HTMLVideoElement)->Unit) = native.onElement { action(it as HTMLVideoElement) }
inline val HtmlWbr.element: HTMLElement get() = native.create() as HTMLElement
inline fun HtmlWbr.onElement(crossinline action: (HTMLElement)->Unit) = native.onElement { action(it as HTMLElement) }