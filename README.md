# kmnd

kmnd is a set of Kotlin Coroutines extensions for JVM's ProcessBuilder, a class that allows launching other programms from Kotlin/JVM world.

## Getting started

This repository is hosted via [jitpack](https://jitpack.io/) since it's by far the easiest delivery method while also being pretty transparent to the developer.

Make sure you have added jitpack to the list of your repositories:

```kotlin
maven("https://jitpack.io")
```

Then simply add the `kmnd` dependency

```kotlin
dependencies {
    compile("com.github.vishna:kmnd:master-SNAPSHOT")
}
```

## Example usage

Ping google's DNS for at least 10 times, if we extend 4 seconds, automatically kill the process

```kotlin
runBlocking {
    withTimeoutOrNull(4000L) {
        "ping 8.8.8.8 -c 10".sh { inputStream, errorStream ->
            inputStream weaveTo System.out
            errorStream weaveTo System.err
        }
    }
}
```

`echo` list of following words to `/bin/bash`

```kotlin
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
```