//
//  Download_ProgressBundle.swift
//  Download Progress
//
//  Created by Emilia Lorentsen on 05/11/2025.
//  Copyright © 2025 orgName. All rights reserved.
//

import WidgetKit
import SwiftUI

@main
struct Download_ProgressBundle: WidgetBundle {
    var body: some Widget {
        Download_Progress()
        Download_ProgressControl()
        Download_ProgressLiveActivity()
    }
}
