package com.liam.cmp_src

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform