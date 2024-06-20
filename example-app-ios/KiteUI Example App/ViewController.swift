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
        RootSetupIosKt.setup(self, themeReadable: AppKt.appTheme, app: { $0.app() })
        print("UIFontWeight light \(UIFont.Weight.light)")
        print("UIFontWeight medium \(UIFont.Weight.medium)")
        print("UIFontWeight regular \(UIFont.Weight.regular)")
        print("UIFontWeight semibold \(UIFont.Weight.semibold)")
        print("UIFontWeight bold \(UIFont.Weight.bold)")
//        RootSetupIosKt.setup(self, app: { vw in vw.appBase(routes: AutoRoutesKt.AutoRoutes, mainLayout: {_ in
//            vw.iosTest2()
//        }) })
    }


}

