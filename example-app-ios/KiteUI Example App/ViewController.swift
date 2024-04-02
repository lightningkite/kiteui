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
    }


}

