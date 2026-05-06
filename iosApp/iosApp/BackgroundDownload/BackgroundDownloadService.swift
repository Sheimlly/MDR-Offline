//
//  BackgroundDownloadService.swift
//  iosApp
//
//  Created by Emilia Lorentsen on 05/11/2025.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import BackgroundTasks
import ComposeApp

// MARK: - Actor (main logic container)
actor BackgroundDownloadService {
    static let shared = BackgroundDownloadService()
        
    private var session: URLSession!
    private var delegate: DownloadSessionDelegate!
    
    private var downloadQueue: [DownloadTaskItem] = []
        private var isDownloading = false

    
    private init() {
        print("BackgroundDownloadService initialized")
        
        delegate = DownloadSessionDelegate()
        delegate.owner = self
        
        let config = URLSessionConfiguration.background(withIdentifier: "com.mangadex.offline.background")
        config.isDiscretionary = false
        config.sessionSendsLaunchEvents = true
        config.allowsCellularAccess = true
        config.allowsExpensiveNetworkAccess = true
        
        // Important: Recreate the session with delegate each launch
        session = URLSession(configuration: config, delegate: delegate, delegateQueue: nil)
    }
    
    // MARK: - API
    
    struct DownloadTaskItem {
        let url: URL
        let fileName: String
    }
    
    func enqueueDownload(urlString: String, fileName: String) async throws -> URL {
        guard let url = URL(string: urlString) else { throw URLError(.badURL) }
        
        return try await withCheckedThrowingContinuation { continuation in
            let task = session.downloadTask(with: url)
            task.taskDescription = fileName
            delegate.registerContinuation(taskDescription: fileName, continuation: continuation)
            task.resume()
        }
    }

    
    private func startNextIfNeeded() async {
        guard !isDownloading, !downloadQueue.isEmpty else { return }
        isDownloading = true
        
        let item = downloadQueue.removeFirst()
        let task = session.downloadTask(with: item.url)
        task.taskDescription = item.fileName
        task.resume()
    }

    
    func handleEventsForBackgroundSession(identifier: String, completionHandler: @escaping () -> Void) {
        print("Handling background events for session \(identifier)")
        // Reconnect the delegate
        session.getAllTasks { tasks in
            print("Reattached to \(tasks.count) tasks for session \(identifier)")
        }
        delegate.backgroundCompletionHandler = completionHandler
    }
    
    func updateProgress(taskId: Int, progress: Double) async {
        print("Progress for task \(taskId): \(Int(progress * 100))%")
    }
    
    func downloadFinished(taskId: Int, location: URL, fileName: String) async {
        print("Download finished")
    }

    // MARK: - Your existing networking + logic
    
    private func retryRequest200<T: Decodable>(
        times: Int = 3,
        delaySeconds: Double = 1.5,
        delaySecondsOn429: Double = 60,
        requestBlock: @escaping () async throws -> (Data, HTTPURLResponse)
    ) async throws -> T {
        
        for attempt in 1..<times {
            do {
                let (data, response) = try await requestBlock()
                
                switch response.statusCode {
                case 200:
                    return try JSONDecoder().decode(T.self, from: data)
                    
                case 429:
                    print("Too many requests (429), retrying in \(delaySecondsOn429)s...")
                    try await Task.sleep(nanoseconds: UInt64(delaySecondsOn429 * 1_000_000_000))
                    
                default:
                    print("Request failed (\(response.statusCode)), retrying (\(attempt)/\(times))...")
                    try await Task.sleep(nanoseconds: UInt64(delaySeconds * 1_000_000_000))
                }
            } catch {
                print("Attempt \(attempt) failed: \(error.localizedDescription)")
                try await Task.sleep(nanoseconds: UInt64(delaySeconds * 1_000_000_000))
            }
        }
        
        let (finalData, finalResponse) = try await requestBlock()
        guard finalResponse.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }
        return try JSONDecoder().decode(T.self, from: finalData)
    }
    
    func getChapterUrls(chapterId: String) async -> [String] {
        do {
            let response: ChapterImagesUrlResponse = try await retryRequest200 {
                guard let url = URL(string: "https://api.mangadex.org/at-home/server/\(chapterId)") else {
                    throw URLError(.badURL)
                }
                
                var request = URLRequest(url: url)
                request.addValue("application/json", forHTTPHeaderField: "Accept")
                
                let (data, response) = try await URLSession.shared.data(for: request)
                guard let httpResponse = response as? HTTPURLResponse else {
                    throw URLError(.badServerResponse)
                }
                
                return (data, httpResponse)
            }
            
            return response.chapter.data.map {
                "\(response.baseUrl)/data/\(response.chapter.hash)/\($0)"
            }
        } catch {
            print("Error getting chapter URLs for \(chapterId): \(error)")
            return []
        }
    }
    
    func fetchChapters(mangaId: String, offset: Int) async throws -> [Chapter] {
        var components = URLComponents(string: "https://api.mangadex.org/chapter")!
        components.queryItems = [
            URLQueryItem(name: "limit", value: "100"),
            URLQueryItem(name: "offset", value: "\(offset)"),
            URLQueryItem(name: "manga", value: mangaId),
            URLQueryItem(name: "translatedLanguage[]", value: "en"),
            URLQueryItem(name: "order[chapter]", value: "asc")
        ]
        
        guard let url = components.url else {
            throw URLError(.badURL)
        }
        
        let (data, response) = try await URLSession.shared.data(from: url)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }
        
        let decoded = try JSONDecoder().decode(ChapterResponse.self, from: data)
        
        var chaptersRaw = decoded.data
        
        for i in 0..<chaptersRaw.count {
            let name = try await getScanlationGroup(chapterRelationships: chaptersRaw[i].relationships)
            chaptersRaw[i].scanlationGroup = name
        }
        
        return mapToChapters(chaptersRaw: chaptersRaw, mangaId: mangaId)
    }
    
    func getAllChapters(mangaId: String) async throws -> [Chapter] {
        var all: [Chapter] = []
        var offset = 0
        while true {
            let batch = try await fetchChapters(mangaId: mangaId, offset: offset)
            if batch.isEmpty { break }
            all += batch
            offset += 100
        }
        
        return all
    }
    
    func saveImage(urlString: String, fileName: String) async throws {
        guard let url = URL(string: urlString) else { throw URLError(.badURL) }
        let (data, _) = try await URLSession.shared.data(from: url)
        
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let fileURL = documents.appendingPathComponent(fileName)
        try data.write(to: fileURL, options: .atomic)
        
        print("Saved image: \(fileURL.path)")
    }
    
    private func getScanlationGroup(chapterRelationships: [ChapterRelationships]) async throws -> String {
        guard let scanlationGroupId = chapterRelationships
            .first(where: { $0.type == "scanlation_group" })?.id else {
            return ""
        }

        let service = ChapterDetailsService()
        return try await service.getScanlationGroupName(scanlationGroupId: scanlationGroupId)
    }
    
    private func mapToChapters(chaptersRaw: [ChapterRaw], mangaId: String) -> [Chapter] {
        chaptersRaw.map {
            Chapter(
                combinedId: "\(mangaId)\($0.id)",
                mangaId: mangaId,
                id: $0.id,
                title: $0.title,
                volume: $0.volume,
                chapter: $0.chapter,
                scanlationGroup: $0.scanlationGroup,
                pageNumbers: Int32($0.pageNumbers),
                pages: nil,
                imagesPath: nil,
                lastReadPage: nil,
                read: false,
                filesDownloaded: false,
            )
        }
    }
    
    private struct ChapterResponse: Codable {
        let data: [ChapterRaw]
    }
    
    struct ChapterImagesUrlResponse: Codable {
        let baseUrl: String
        let chapter: ChapterInfo
        
        struct ChapterInfo: Codable {
            let hash: String
            let data: [String]
        }
    }
}

class ChapterDetailsService {
    
    struct ScanlationGroupResponse: Codable {
        let data: ScanlationGroup
    }
    
    struct ScanlationGroup: Codable {
        let attributes: ScanlationGroupAttributes
    }
    
    struct ScanlationGroupAttributes: Codable {
        let name: String
    }
    
    func getScanlationGroupName(scanlationGroupId: String) async throws -> String {
        let urlString = "https://api.mangadex.org/group/\(scanlationGroupId)"
        guard let url = URL(string: urlString) else {
            throw URLError(.badURL)
        }
        
        let (data, _) = try await URLSession.shared.data(from: url)
        
        let decoded = try JSONDecoder().decode(ScanlationGroupResponse.self, from: data)
        return decoded.data.attributes.name
    }
}
