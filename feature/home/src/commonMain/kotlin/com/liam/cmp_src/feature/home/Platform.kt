package com.liam.cmp_src.feature.home

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform