package com.mdr.offline.db.adapters

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val byteArrayListAdapter = object : ColumnAdapter<List<ByteArray>, ByteArray> {
    override fun decode(databaseValue: ByteArray): List<ByteArray> {
        val json = databaseValue.decodeToString()
        val listOfStrings = Json.decodeFromString<List<String>>(json)
        return listOfStrings.map { it.encodeToByteArray() }
    }

    override fun encode(value: List<ByteArray>): ByteArray {
        val listOfStrings = value.map { it.decodeToString() }
        val json = Json.encodeToString(listOfStrings)
        return json.encodeToByteArray()
    }
}