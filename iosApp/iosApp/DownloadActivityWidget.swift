//
//  DownloadActivityWidget.swift
//  iosApp
//
//  Created by Emilia Lorentsen on 05/11/2025.
//  Copyright © 2025 orgName. All rights reserved.
//

import ActivityKit
import WidgetKit
import SwiftUI

struct DownloadActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: DownloadActivityAttributes.self) { context in
            // Lock Screen / Dynamic Island expanded view
            VStack(alignment: .leading, spacing: 8) {
                Text(context.attributes.mangaTitle)
                    .font(.headline)
                    .bold()
                ProgressView(value: context.state.progress)
                    .progressViewStyle(.linear)
                Text(context.state.statusText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.center) {
                    VStack {
                        Text(context.attributes.mangaTitle)
                            .font(.subheadline)
                        ProgressView(value: context.state.progress)
                            .progressViewStyle(.linear)
                    }
                }
            } compactLeading: {
                Image(systemName: "book")
            } compactTrailing: {
                Text("\(Int(context.state.progress * 100))%")
                    .font(.caption2)
            } minimal: {
                Image(systemName: "arrow.down.circle")
            }
        }
    }
}

struct DownloadActivityWidgetBundle: WidgetBundle {
    var body: some Widget {
        DownloadActivityWidget()
    }
}
