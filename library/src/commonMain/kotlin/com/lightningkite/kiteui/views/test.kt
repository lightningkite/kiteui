package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.UnselectedSemantic

class L2(private val wraps: NativeElement): Element by wraps {
    var selected = false

    init {
        wraps.appliedStatefulTheming += NativeElementCommonCode.StateTheming {
            if (selected) SelectedSemantic else UnselectedSemantic
        }
    }
}