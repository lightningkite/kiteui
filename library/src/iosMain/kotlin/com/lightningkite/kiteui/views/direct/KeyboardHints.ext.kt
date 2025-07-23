package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.AutoComplete
import com.lightningkite.kiteui.models.KeyboardCase
import com.lightningkite.kiteui.models.KeyboardType
import platform.UIKit.*

val KeyboardCase.ios get() = when (this) {
    KeyboardCase.None -> UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
    KeyboardCase.Letters -> UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters
    KeyboardCase.Words -> UITextAutocapitalizationType.UITextAutocapitalizationTypeWords
    KeyboardCase.Sentences -> UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences
}
val KeyboardType.ios get() = when (this) {
    KeyboardType.Text -> UIKeyboardTypeDefault
    KeyboardType.Integer, KeyboardType.IntegerWithNegative -> UIKeyboardTypeNumberPad
    KeyboardType.Phone -> UIKeyboardTypePhonePad
    KeyboardType.Decimal, KeyboardType.DecimalWithNegative -> UIKeyboardTypeNumbersAndPunctuation
    KeyboardType.Email -> UIKeyboardTypeEmailAddress
}
val AutoComplete?.iosTextContentType get() = when (this) {
    AutoComplete.Email -> UITextContentTypeUsername
    AutoComplete.Password -> UITextContentTypePassword
    AutoComplete.NewPassword -> UITextContentTypeNewPassword
    else -> null
}