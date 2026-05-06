package com.mdr.offline

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object Secrets {
    val CLIENT_ID: String
    val CLIENT_SECRET: String
}