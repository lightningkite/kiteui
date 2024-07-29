//
//  ViewController.swift
//  KiteUI Example App
//
//  Created by Joseph Ivie on 12/14/23.
//

import UIKit
import shared

class ViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        let navigator = ScreenNavigator { AutoRoutesKt.AutoRoutes }
        let dialog = ScreenNavigator { AutoRoutesKt.AutoRoutes }
        RootSetupIosKt.setup(self, themeReadable: AppKt.appTheme, app: { $0.app(navigator: navigator, dialog: dialog) })
//        RootSetupIosKt.setup(self, app: { vw in vw.appBase(routes: AutoRoutesKt.AutoRoutes, mainLayout: {_ in
//            vw.iosTest2()
//        }) })
    }


}

