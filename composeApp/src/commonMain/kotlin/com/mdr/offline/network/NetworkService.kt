package com.mdr.offline.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.errors.IOException

open class NetworkService{
    suspend fun <T> retryRequest(
        times: Int = 3,
        delayMs: Long = 750,
        block: suspend () -> T
    ): T {
        repeat(times - 1) {
            try {
                return block()
            } catch (e: IOException) {
                println("Request failed: ${e.message}, retrying...")
                kotlinx.coroutines.delay(delayMs)
            }
        }
        return block()
    }

//    Function created mostly for testing purposes
    suspend inline fun <reified T> retryRequest200(
        times: Int = 3,
        delayMs: Long = 1500,
        delayMsOn429: Long = 60000,
        block: () -> HttpResponse
    ): T {
        repeat(times - 1) {
            try {
                val response = block()
                when(response.status) {
                    HttpStatusCode.OK -> {
                        return response.body() // deserialize here
                    }
                    HttpStatusCode.TooManyRequests -> {
                        println("Request failed with status ${response.status}, retrying...")
                        kotlinx.coroutines.delay(delayMsOn429)
                    }
                    else -> {
                        println("Request failed with status ${response.status}, retrying...")
                    }
                }
            } catch (e: IOException) {
                println("Request failed: ${e.message}, retrying...")
                kotlinx.coroutines.delay(delayMs)
            }
        }

        // last attempt
        val finalResponse = block()

        when(finalResponse.status) {
            HttpStatusCode.OK -> {
                return finalResponse.body() // deserialize here
            }
            HttpStatusCode.TooManyRequests -> {
                println("Request failed with status ${finalResponse.status}, retrying...")
                kotlinx.coroutines.delay(delayMsOn429)
            }
            else -> {
                println("Request failed with status ${finalResponse.status}, retrying...")
            }
        }

        if (block().status == HttpStatusCode.OK) {
            return finalResponse.body()
        } else {
            throw IOException("Request failed with status ${finalResponse.status}")
        }

    }

    val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
}