//
//  Download_ProgressLiveActivity.swift
//  Download Progress
//
//  Created by Emilia Lorentsen on 05/11/2025.
//  Copyright © 2025 orgName. All rights reserved.
//

import ActivityKit
import WidgetKit
import SwiftUI

struct Download_ProgressAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        // Dynamic stateful properties about your activity go here!
        var emoji: String
    }

    // Fixed non-changing properties about your activity go here!
    var name: String
}

struct Download_ProgressLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: Download_ProgressAttributes.self) { context in
            // Lock screen/banner UI goes here
            VStack {
                Text("Hello \(context.state.emoji)")
            }
            .activityBackgroundTint(Color.cyan)
            .activitySystemActionForegroundColor(Color.black)

        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded UI goes here.  Compose the expanded UI through
                // various regions, like leading/trailing/center/bottom
                DynamicIslandExpandedRegion(.leading) {
                    Text("Leading")
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text("Trailing")
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text("Bottom \(context.state.emoji)")
                    // more content
                }
            } compactLeading: {
                Text("L")
            } compactTrailing: {
                Text("T \(context.state.emoji)")
            } minimal: {
                Text(context.state.emoji)
            }
            .widgetURL(URL(string: "http://www.apple.com"))
            .keylineTint(Color.red)
        }
    }
}

extension Download_ProgressAttributes {
    fileprivate static var preview: Download_ProgressAttributes {
        Download_ProgressAttributes(name: "World")
    }
}

extension Download_ProgressAttributes.ContentState {
    fileprivate static var smiley: Download_ProgressAttributes.ContentState {
        Download_ProgressAttributes.ContentState(emoji: "😀")
     }
     
     fileprivate static var starEyes: Download_ProgressAttributes.ContentState {
         Download_ProgressAttributes.ContentState(emoji: "🤩")
     }
}

#Preview("Notification", as: .content, using: Download_ProgressAttributes.preview) {
   Download_ProgressLiveActivity()
} contentStates: {
    Download_ProgressAttributes.ContentState.smiley
    Download_ProgressAttributes.ContentState.starEyes
}
