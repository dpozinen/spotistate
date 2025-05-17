package com.spotistate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpotistateApplication

fun main(args: Array<String>) {
    runApplication<SpotistateApplication>(*args)
}
