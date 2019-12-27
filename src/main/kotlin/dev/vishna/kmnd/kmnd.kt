package dev.vishna.kmnd

import kotlinx.coroutines.*
import org.apache.commons.lang3.SystemUtils
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.resume

suspend fun List<String>.execute(
    redirectErrorStream: Boolean? = null,
    streamHandler: (suspend (InputStream) -> Unit)
) = execute(redirectErrorStream = redirectErrorStream) { _, inputStream, _ -> streamHandler(inputStream) }

suspend fun List<String>.execute(
    redirectErrorStream: Boolean? = null,
    streamHandler: (suspend (InputStream, InputStream) -> Unit)
) = execute(redirectErrorStream = redirectErrorStream) { _, inputStream, errorStream ->
    streamHandler(
        inputStream,
        errorStream
    )
}

/**
 * In case of empty list, value 126 is returned which is a UNIX code for "Command invoked cannot execute"
 */
suspend fun List<String>.execute(
    redirectErrorStream: Boolean? = false,
    streamHandler: (suspend (OutputStream, InputStream, InputStream) -> Unit)
): Int = coroutineScope {
    suspendCancellableCoroutine<Int> { continuation ->

        if (this@execute.isEmpty()) {
            continuation.resume(126)
            return@suspendCancellableCoroutine
        }

        val builder = ProcessBuilder(this@execute)

        if (redirectErrorStream == true) {
            builder.redirectErrorStream(true)
        }

        val process = builder.start()

        launch(Dispatchers.IO) {
            streamHandler(process.outputStream, process.inputStream, process.errorStream)
            val exitCode = process.waitFor()
            continuation.resume(exitCode)
        }

        continuation.invokeOnCancellation {
            process.destroy()
        }
    }
}

fun String.shList(): List<String> {
    return if (SystemUtils.IS_OS_WINDOWS) {
        listOf("CMD", "/C", this)
    } else {
        listOf("/bin/sh", "-c", this)
    }
}

suspend fun String.sh(
    redirectErrorStream: Boolean = false,
    streamHandler: (suspend (InputStream) -> Unit)
) = shList().execute(redirectErrorStream, streamHandler)

suspend fun String.sh(
    redirectErrorStream: Boolean = false,
    streamHandler: (suspend (InputStream, InputStream) -> Unit)
) = shList().execute(redirectErrorStream, streamHandler)

suspend fun String.sh(
    redirectErrorStream: Boolean = false,
    streamHandler: (suspend (OutputStream, InputStream, InputStream) -> Unit)
) = shList().execute(redirectErrorStream, streamHandler)

suspend infix fun InputStream.weaveToBlocking(outputStream: OutputStream) = withContext(Dispatchers.IO) {

    val bytes = ByteArray(8192)
    while (coroutineContext.isActive) {
        val read = read(bytes)
        if (read == -1 || !coroutineContext.isActive) {
            break
        }
        outputStream.write(bytes, 0, read)
    }

}

suspend infix fun InputStream.weaveTo(outputStream: OutputStream) = coroutineScope {
    launch {
        weaveToBlocking(outputStream)
    }
}

suspend infix fun String.weaveTo(outputStream: OutputStream) {
    this.asInputStream() weaveTo outputStream
}

suspend infix fun String.weaveToBlocking(outputStream: OutputStream) {
    this.asInputStream() weaveToBlocking outputStream
}

fun String.asInputStream(): InputStream = ByteArrayInputStream(this.toByteArray(Charsets.UTF_8))