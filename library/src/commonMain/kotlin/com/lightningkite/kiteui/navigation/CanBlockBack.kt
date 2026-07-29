package com.lightningkite.kiteui.navigation

public interface CanBlockBack {
    /**
     * @return True if the user should be permitted to navigate away without warning.  False results in a system warning about losing data when navigating away.
     */
    public fun onNavigateAwayAttempt(): Boolean
}
