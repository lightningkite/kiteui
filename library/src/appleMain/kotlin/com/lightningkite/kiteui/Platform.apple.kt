package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color

actual val Platform.Companion.current: Platform
    get() = Platform.iOS



// https://developer.apple.com/documentation/technotes/tn3105-customizing-uistatusbar-syle
actual fun setStatusBarColor(color: Color) {

}


// possible solutions:

//override var preferredStatusBarStyle: UIStatusBarStyle {
//    return .lightContent // Set light content for better visibility on dark background
//}
//
//override func viewDidLoad() {
//    super.viewDidLoad()
//
//    // Set the navigation bar background color (if using a navigation controller)
//    navigationController?.navigationBar.barTintColor = UIColor.red
//
//    // Create and set the background view for the status bar
//    let statusBarView = UIView()
//    if let statusBarFrame = UIApplication.shared.windows.first?.windowScene?.statusBarManager?.statusBarFrame {
//        statusBarView.frame = statusBarFrame
//        statusBarView.backgroundColor = UIColor.red // Set your desired color
//        view.addSubview(statusBarView)
//    }
//}
