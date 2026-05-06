//
//  ChapterRaw.swift
//  iosApp
//
//  Created by Emilia Lorentsen on 05/11/2025.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

// MARK: - ChapterRaw
struct ChapterRaw: Codable {
    let id: String
    let attributes: ChapterAttributes
    let relationships: [ChapterRelationships]
    
    // flattened / computed fields (like your Kotlin data class)
    var title: String? { attributes.title }
    var volume: String? { attributes.volume }
    var chapter: String { attributes.chapter }
    var pageNumbers: Int { attributes.pages }
    var pages: [String] = []
    var scanlationGroup = ""
    
    enum CodingKeys: String, CodingKey {
        case id
        case attributes
        case relationships
    }
}

// MARK: - ChapterAttributes
struct ChapterAttributes: Codable {
    let title: String?
    let volume: String?
    let chapter: String
    let pages: Int
    
    enum CodingKeys: String, CodingKey {
        case title
        case volume
        case chapter
        case pages
    }
}

// MARK: - Custom Decoder handling nulls / missing fields
extension ChapterAttributes {
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // decode optional strings safely
        let title = try? container.decodeIfPresent(String.self, forKey: .title)
        let volume = try? container.decodeIfPresent(String.self, forKey: .volume)
        // if missing or null, default to ""
        let chapter = (try? container.decodeIfPresent(String.self, forKey: .chapter)) ?? ""
        let pages = (try? container.decodeIfPresent(Int.self, forKey: .pages)) ?? 0
        
        self.title = title
        self.volume = volume
        self.chapter = chapter
        self.pages = pages
    }
}

struct ChapterRelationships: Codable {
    let id: String
    let type: String
    
    enum CodingKeys: String, CodingKey {
        case id
        case type
    }
}
