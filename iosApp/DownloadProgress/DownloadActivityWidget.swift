//
//  DownloadActivityWidget.swift
//  iosApp
//
//  Created by Emilia Lorentsen on 05/11/2025.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI
import ActivityKit
import WidgetKit

@main
struct DownloadActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: DownloadActivityAttributes.self) { context in
            DownloadActivityLiveView(context: context)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.center) {
                    DownloadActivityLiveView(context: context)
                }
            } compactLeading: {
                Text("📚")
            } compactTrailing: {
                Text("\(Int(context.state.progress * 100))%")
            } minimal: {
                Text("⬇️")
            }
        }
    }
}

struct DownloadActivityLiveView: View {
    let context: ActivityViewContext<DownloadActivityAttributes>

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(context.attributes.mangaTitle)
                .font(.headline)
            ProgressView(value: context.state.progress)
                .progressViewStyle(.linear)
            Text(context.state.statusText)
                .font(.caption)
        }
        .padding()
    }
}

struct DownloadActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        var progress: Double
        var statusText: String
    }

    var mangaTitle: String
}
