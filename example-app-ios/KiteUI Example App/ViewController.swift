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
        RootSetupIosKt.setup(self, app: { $0.app() })
//        RootSetupIosKt.setup(self, app: { vw in vw.appBase(routes: AutoRoutesKt.AutoRoutes, mainLayout: {_ in
//            vw.iosTest2()
//        }) })
    }


}

