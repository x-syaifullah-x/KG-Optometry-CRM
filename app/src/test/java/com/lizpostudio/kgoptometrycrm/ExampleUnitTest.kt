package com.lizpostudio.kgoptometrycrm

import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val data = "a|b|c|".split("|")

        println("size: ${data.size}")

        println(data[2])
    }
}