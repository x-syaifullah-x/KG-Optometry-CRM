package com.lizpostudio.kgoptometrycrm

import org.junit.Test

class ExampleUnitTest {
    private val color = arrayOf(
        "faf9fd", "f3f1fb", "ece8f9", "e4def6", "ded7f4", "d8d0f2", "d4cbf0", "cec4ee", "c8beec", "bfb3e8",
    )

//    "stephy neoh" = THAM

    val a = "|-1.00|-0.50|-0.75|-0.50|155|180|30|31.5|||||||ASHIMO-HE6501-4-5515|28|1.56 UVNEX STEEL |250|||278|THAM|THAM||"
        .split("|")

    @Test
    fun addition_isCorrect() {
        println(a[22])
    }
}