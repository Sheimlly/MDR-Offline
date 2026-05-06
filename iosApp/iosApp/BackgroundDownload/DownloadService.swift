//
//  DownloadService.swift
//  iosApp
//
//  Created by Emilia Lorentsen on 05/11/2025.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import UserNotifications
import ActivityKit
import ComposeApp // your KMM framework module
import DownloadProgressExtension

@MainActor
@objc class DownloadService: NSObject, DownloadServiceProvider {
    
    @objc public static let shared = DownloadService()
    
    private let backgroundService = BackgroundDownloadService.shared
    private let progressNotificationId = "manga_download_progress"
    private var currentActivity: Activity<DownloadActivityAttributes>? = nil
    
    private(set) var currentManga: Manga? = nil

    @objc public func downloadMangaAndChapters(
        db: MDROfflineDatabase,
        manga: Manga,
        chapter: Chapter?
    ) {
        Task.detached(priority: .background) {
            await self.processMangaDownload(db: db, manga: manga, chapter: chapter)
        }
    }

    // MARK: - Internal workflow

    private func processMangaDownload(db: MDROfflineDatabase, manga: Manga, chapter: Chapter? = nil) async {
        do {
            // Check if manga exists
            if (db.mDROfflineDatabaseQueries.selectDownloadedMangaById(id: manga.id).executeAsOneOrNull() != nil) {
                print("Manga already exists in database: \(manga.id)")
            } else {
                // Fetch manga info from API
                try await db.mDROfflineDatabaseQueries.insertDownloadedManga(
                    id: manga.id,
                    title: manga.title,
                    description: manga.description,
                    author: manga.author,
                    coverImage: fetchBytesAsKotlinByteArray(urlString: manga.coverImageUrl!),
                    originalLanguage: manga.originalLanguage,
                    status: manga.status,
                    year: manga.year,
                    state: manga.state,
                    format: manga.format,
                    publicationDemographic: manga.publicationDemographic,
                    contentRating: manga.contentRating,
                    genres: manga.genres
                )
                print("Manga added to database: \(manga.title)")
            }
            
            if(chapter == nil){
                // Fetch all chapter IDs for manga
                let chapters = try await backgroundService.getAllChapters(mangaId: manga.id)
                
                self.currentManga = manga
                await startLiveActivity(for: manga)
                
                // Download each chapter
                for (index, chapter) in chapters.enumerated() {
                    do {
                        let progress = Double(index) / Double(chapters.count)
                        
                        await updateLiveActivityProgress(
                            progress: progress,
                            text: "Downloading chapter \(index+1)/\(chapters.count)"
                        )
                        
                        if(db.mDROfflineDatabaseQueries.selectDownloadedChapterById(combinedId: chapter.combinedId!).executeAsOneOrNull() == nil) {
                            let urls: [String] = await backgroundService.getChapterUrls(chapterId: chapter.id)
                            var paths: [String] = [String]()
                            
                            for(index, url) in urls.enumerated() {
                                let filename = "\(chapter.id)_\(index).png"
                                try await backgroundService.enqueueDownload(urlString: url, fileName: filename)
                                paths.append(filename)
                            }
                            
                            db.mDROfflineDatabaseQueries.insertDownloadedChapter(
                                combinedId: chapter.combinedId!,
                                mangaId: chapter.mangaId!,
                                id: chapter.id,
                                title: chapter.title,
                                volume: chapter.volume,
                                chapter: chapter.chapter,
                                scanlationGroup: chapter.scanlationGroup,
                                pageNumbers: Int64(chapter.pageNumbers),
                                imagesPath: paths
                            )
                        }
                    } catch {
                        print("Failed to download chapter \(chapter.chapter): \(error)")
                    }
                }
                
                await endLiveActivity(success: true)
                await showNotification(title: "Download complete", body: manga.title)
            } else {
                await showNotification(title: "Downloading chapter \(chapter!.chapter)" , body: manga.title)
                
                if(db.mDROfflineDatabaseQueries.selectDownloadedChapterById(combinedId: "\(manga.id)\(chapter!.id)").executeAsOneOrNull() == nil) {
                    let urls: [String] = await backgroundService.getChapterUrls(chapterId: chapter!.id)
                    var paths: [String] = [String]()
                    
                    for(index, url) in urls.enumerated() {
                        let filename = "\(chapter!.id)_\(index).png"
                        try await backgroundService.enqueueDownload(urlString: url, fileName: filename)
                        paths.append(filename)
                    }
                    
                    db.mDROfflineDatabaseQueries.insertDownloadedChapter(
                        combinedId: "\(manga.id)\(chapter!.id)",
                        mangaId: manga.id,
                        id: chapter!.id,
                        title: chapter!.title,
                        volume: chapter!.volume,
                        chapter: chapter!.chapter,
                        scanlationGroup: chapter!.scanlationGroup,
                        pageNumbers: Int64(chapter!.pageNumbers),
                        imagesPath: paths
                    )
                }
                await showNotification(title: "Download complete for chapter: \(chapter!.chapter)", body: manga.title)
            }
        } catch {
            print("Failed to download manga \(manga.title): \(error)")
            await endLiveActivity(success: false)
            await showNotification(title: "Download failed", body: manga.title)
        }
    }

    // MARK: - API Calls

    private func fetchMangaDetails(for manga: Manga) async throws -> DownloadedManga {
        
        return DownloadedManga(
            id: manga.id,
            title: manga.title,
            description: manga.description,
            author: manga.author,
            coverImage: Data(),
            originalLanguage: manga.originalLanguage,
            status: manga.status,
            year: manga.year,
            state: manga.state,
            format: manga.format,
            publicationDemographic: manga.publicationDemographic,
            contentRating: manga.contentRating,
            genres: manga.genres
        )
    }
    
    private func fetchBytesAsKotlinByteArray(urlString: String) async throws -> KotlinByteArray {
        guard let url = URL(string: urlString) else {
            throw URLError(.badURL)
        }
        
        let (data, _) = try await URLSession.shared.data(from: url)
        let kotlinByteArray = KotlinByteArray(size: Int32(data.count))
        
        data.withUnsafeBytes { rawBufferPointer in
            guard let baseAddress = rawBufferPointer.baseAddress else { return }
            let pointer = baseAddress.assumingMemoryBound(to: Int8.self)
            for i in 0..<data.count {
                kotlinByteArray.set(index: Int32(i), value: pointer[i])
            }
        }
        
        return kotlinByteArray
    }
    
    private func showNotification(title: String, body: String) async {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )

        do {
            try await UNUserNotificationCenter.current().add(request)
        } catch {
            print("Failed to schedule notification: \(error)")
        }
    }

    // MARK: - Live Activity handling
    private func startLiveActivity(for manga: Manga) async {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            print("Live Activities not enabled on this device")
            return
        }

        let attributes = DownloadActivityAttributes(mangaTitle: manga.title)
        let initialState = DownloadActivityAttributes.ContentState(
            progress: 0.0,
            statusText: "Starting download..."
        )

        let content = ActivityContent(state: initialState, staleDate: nil)

        do {
            currentActivity = try Activity.request(
                attributes: attributes,
                content: content,
                pushType: nil
            )
            print("Live Activity started for \(manga.title)")
        } catch {
            print("Could not start Live Activity: \(error)")
        }
    }

    private func updateLiveActivityProgress(progress: Double, text: String) async {
        if currentActivity == nil {
            print("Live Activity missing — recreating...")
            if let manga = currentManga {
                await startLiveActivity(for: manga)
            }
        }
        
        guard let activity = currentActivity else { return }
        let newState = DownloadActivityAttributes.ContentState(progress: progress, statusText: text)
        let content = ActivityContent(state: newState, staleDate: nil)

        print("Updating Live Activity progress: \(Int(progress * 100))% - \(text)")
        await activity.update(content)
    }

    private func endLiveActivity(success: Bool) async {
        guard let activity = currentActivity else { return }

        let finalState = DownloadActivityAttributes.ContentState(
            progress: 1.0,
            statusText: success ? "Download complete" : "Download failed"
        )
        let content = ActivityContent(state: finalState, staleDate: nil)

        await activity.end(content, dismissalPolicy: .after(Date(timeIntervalSinceNow: 3)))
        currentActivity = nil
    }

}

// MARK: - Notification delegate (for possible future use)

class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationDelegate()

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}

struct DownloadActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        var progress: Double
        var statusText: String
    }

    var mangaTitle: String
}
