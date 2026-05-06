//
//  DownloadSessionDelegate.swift
//  iosApp
//
//  Created by Emilia Lorentsen on 10/11/2025.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

final class DownloadSessionDelegate: NSObject, URLSessionDownloadDelegate {
    weak var owner: BackgroundDownloadService?
    var backgroundCompletionHandler: (() -> Void)?

    // Track continuations per filename
    private var continuations: [String: CheckedContinuation<URL, Error>] = [:]

    func registerContinuation(taskDescription: String, continuation: CheckedContinuation<URL, Error>) {
        continuations[taskDescription] = continuation
    }

    private func resumeContinuation(for fileName: String, with location: URL) {
        continuations[fileName]?.resume(returning: location)
        continuations[fileName] = nil
    }

    func urlSession(_ session: URLSession,
                    downloadTask: URLSessionDownloadTask,
                    didWriteData bytesWritten: Int64,
                    totalBytesWritten: Int64,
                    totalBytesExpectedToWrite: Int64) {
        guard totalBytesExpectedToWrite > 0 else { return }
        let progress = Double(totalBytesWritten) / Double(totalBytesExpectedToWrite)
        Task {
            await owner?.updateProgress(taskId: downloadTask.taskIdentifier, progress: progress)
        }
    }

    func urlSession(_ session: URLSession,
                    downloadTask: URLSessionDownloadTask,
                    didFinishDownloadingTo location: URL) {

        guard let fileName = downloadTask.taskDescription else {
            print("Missing filename for download task \(downloadTask.taskIdentifier)")
            return
        }

        // Move file to Documents folder
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let destination = documents.appendingPathComponent(fileName)

        do {
            let folder = destination.deletingLastPathComponent()
            try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)

            if FileManager.default.fileExists(atPath: destination.path) {
                try FileManager.default.removeItem(at: destination)
            }

            try FileManager.default.moveItem(at: location, to: destination)
            print("Saved downloaded file: \(destination.lastPathComponent)")
        } catch {
            print("Failed to move file \(fileName): \(error)")
        }

        // Notify the actor
        Task {
            await owner?.downloadFinished(taskId: downloadTask.taskIdentifier, location: destination, fileName: fileName)
            // Resume awaiting task if any
            resumeContinuation(for: fileName, with: destination)
        }
    }

    func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
        print("All background events finished for \(session.configuration.identifier ?? "unknown")")
        backgroundCompletionHandler?()
        backgroundCompletionHandler = nil
    }
}
