package com.lightningkite.kiteui.models

/**
 * Represents standard ARIA roles for accessibility.
 * These roles define the type of UI element or structure for assistive technologies.
 * 
 * Based on WAI-ARIA standard: https://www.w3.org/TR/wai-aria-1.1/#role_definitions
 */
enum class AriaRole {
    // Document Structure
    Article,
    Banner,
    Complementary,
    ContentInfo,
    Definition,
    Directory,
    Document,
    Feed,
    Figure,
    Group,
    Heading,
    Img,
    List,
    ListItem,
    Main,
    Math,
    Navigation,
    Region,
    Row,
    RowGroup,
    RowHeader,
    Separator,
    Toolbar,

    // Widget Roles
    Alert,
    AlertDialog,
    Button,
    Checkbox,
    Dialog,
    GridCell,
    Link,
    Log,
    Marquee,
    Menu,
    MenuBar,
    MenuItem,
    MenuItemCheckbox,
    MenuItemRadio,
    Option,
    ProgressBar,
    Radio,
    RadioGroup,
    ScrollBar,
    SearchBox,
    Slider,
    SpinButton,
    Status,
    Switch,
    Tab,
    TabList,
    TabPanel,
    TextBox,
    Timer,
    Tooltip,
    TreeItem,

    // Live Region Roles
    Application;

    /**
     * Returns the lowercase string representation of the role for use in HTML attributes.
     */
    override fun toString(): String {
        return when (this) {
            MenuItemCheckbox -> "menuitemcheckbox"
            MenuItemRadio -> "menuitemradio"
            ProgressBar -> "progressbar"
            RadioGroup -> "radiogroup"
            SearchBox -> "searchbox"
            SpinButton -> "spinbutton"
            TabList -> "tablist"
            TabPanel -> "tabpanel"
            TreeItem -> "treeitem"
            AlertDialog -> "alertdialog"
            ContentInfo -> "contentinfo"
            GridCell -> "gridcell"
            RowGroup -> "rowgroup"
            RowHeader -> "rowheader"
            else -> name.lowercase()
        }
    }

    companion object {
        /**
         * Converts a string to an AriaRole, ignoring case.
         * Returns null if the string doesn't match any role.
         */
        fun fromString(value: String?): AriaRole? {
            if (value == null) return null

            // Handle special cases
            return when (value.lowercase()) {
                "menuitemcheckbox" -> MenuItemCheckbox
                "menuitemradio" -> MenuItemRadio
                "progressbar" -> ProgressBar
                "radiogroup" -> RadioGroup
                "searchbox" -> SearchBox
                "spinbutton" -> SpinButton
                "tablist" -> TabList
                "tabpanel" -> TabPanel
                "treeitem" -> TreeItem
                "alertdialog" -> AlertDialog
                "contentinfo" -> ContentInfo
                "gridcell" -> GridCell
                "rowgroup" -> RowGroup
                "rowheader" -> RowHeader
                else -> values().find { it.name.equals(value, ignoreCase = true) }
            }
        }
    }
}
