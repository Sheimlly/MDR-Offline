package com.mdr.offline.db.adapters

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val stringListAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> =
        Json.decodeFromString(databaseValue)

    override fun encode(value: List<String>): String =
        Json.encodeToString(value)
}