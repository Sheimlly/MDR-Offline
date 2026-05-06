//
//  DownloadedManga.swift
//  iosApp
//
//  Created by Emilia Lorentsen on 05/11/2025.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct DownloadedManga: Codable, Identifiable {
    let id: String
    let title: String
    let description: String
    let author: String
    let coverImage: Data          // BLOB in DB
    let originalLanguage: String
    let status: String
    let year: String
    let state: String
    let format: String
    let publicationDemographic: String
    let contentRating: String
    let genres: [String]
    
    var lastReadChapter: String?
    var downloadedWholeManga: Bool
    
    init(
        id: String,
        title: String,
        description: String,
        author: String,
        coverImage: Data,
        originalLanguage: String,
        status: String,
        year: String,
        state: String,
        format: String,
        publicationDemographic: String,
        contentRating: String,
        genres: [String],
        lastReadChapter: String? = nil,
        downloadedWholeManga: Bool = false
    ) {
        self.id = id
        self.title = title
        self.description = description
        self.author = author
        self.coverImage = coverImage
        self.originalLanguage = originalLanguage
        self.status = status
        self.year = year
        self.state = state
        self.format = format
        self.publicationDemographic = publicationDemographic
        self.contentRating = contentRating
        self.genres = genres
        self.lastReadChapter = lastReadChapter
        self.downloadedWholeManga = downloadedWholeManga
    }
}
