package com.mach.apps.repostinho

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform