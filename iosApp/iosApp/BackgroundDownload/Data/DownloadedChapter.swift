//
//  DownloadedChapter.swift
//  iosApp
//
//  Created by Emilia Lorentsen on 05/11/2025.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct DownloadedChapter: Codable, Identifiable {
    let combinedId: String
    let mangaId: String
    let id: String
    let title: String?
    let volume: String?
    let chapter: String
    let scanlationGroup: String
    let pageNumbers: Int
    let imagesPath: [String]
    var lastReadPage: Int
    var read: Bool
    
    init(
        combinedId: String,
        mangaId: String,
        id: String,
        title: String? = nil,
        volume: String? = nil,
        chapter: String,
        scanlationGroup: String,
        pageNumbers: Int,
        imagesPath: [String],
        lastReadPage: Int = 1,
        read: Bool = false
    ) {
        self.combinedId = combinedId
        self.mangaId = mangaId
        self.id = id
        self.title = title
        self.volume = volume
        self.chapter = chapter
        self.scanlationGroup = scanlationGroup
        self.pageNumbers = pageNumbers
        self.imagesPath = imagesPath
        self.lastReadPage = lastReadPage
        self.read = read
    }
}
