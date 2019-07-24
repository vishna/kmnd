package dev.vishna.kmnd

import kotlinx.coroutines.*
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class KMNDTests {

    @Test
    fun `handling output from the ping command`() {
        runBlocking {
            withTimeoutOrNull(4000L) {
                "ping 8.8.8.8 -c 10".sh { inputStream, errorStream ->
                    inputStream weaveTo System.out
                    errorStream weaveTo System.err
                }
            }
        }
    }

    @Test
    fun `sending strings to bash using echo`() {
        val words = listOf("one", "two", "\$PATH", "four", "five")

        runBlocking {

            val result = listOf("/bin/bash").execute { outputStream, inputStream, errorStream ->

                launch {
                    outputStream.use {
                        words.forEach { word ->
                            "echo $word\n" weaveToBlocking outputStream
                            outputStream.flush()
                            delay(500)
                        }
                    }
                }

                inputStream weaveTo System.out
                errorStream weaveTo System.err
            }

            result `should be equal to` 0
        }
    }
}