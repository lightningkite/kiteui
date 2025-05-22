package com.lightningkite.kiteui.views.direct

/**
 * Interface for input controls that need additional accessibility properties
 * specific to form elements.
 */
interface InputAccessibility {
    /**
     * Indicates that user input is required on the element before a form
     * may be submitted.
     */
    var ariaRequired: Boolean?
}