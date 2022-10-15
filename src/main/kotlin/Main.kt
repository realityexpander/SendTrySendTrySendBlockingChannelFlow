import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Keep Your Kotlin Flow Alive and Listening With CallbackFlow
// article: https://medium.com/mobile-app-development-publication/keep-your-kotlin-flow-alive-and-listening-with-callbackflow-c95e5dd545a

fun main(args: Array<String>) {
    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
//    println("Program arguments: ${args.joinToString()}")

//    simpleFlow()
//    simpleChannelFlow()
//    simpleChannelFlowKeepAlive()
//    simpleChannelFlowKeepAliveAwaitClose()

    //sendDataFromOutsideLambda()
    //unblockingAwaitingProcess()
    //unblockingAwaitingProcessWithSmallDelay()
    //unblockingAwaitingProcessWithClose()
    //properlyCloseTheChannelExternally()
    //sendingMoreDataAfterChannelClosedWillCrash()

    //sendToChannelUsingTrySendToPreventCrashAfterChannelClosed()
    //sendToChannelWithCleanup()
    //useAwaitCloseAndNotUsingTrySendWillCrash()
    //useAwaitCloseAndNotUsingTrySendHackWithDelayToPreventCrash()

    //trySendIsDifferentThanSendIsDifferentFromTrySendBlocking()
    isTrySendSameAsSend()

}

// Simple flow
fun simpleFlow(): Unit = runBlocking {
    flow {
        for (i in 1..5) emit(i)
    }.collect { println(it) }
}

// Simple channel flow
fun simpleChannelFlow(): Unit = runBlocking {
    channelFlow {
        for (i in 1..5) send(i)
    }.collect { println(it) }

    // Wanted to send more data through the flow but can't!
}

// Keep flow alive
fun simpleChannelFlowKeepAlive(): Unit = runBlocking {
    channelFlow {
        for (i in 1..5) send(i)
        awaitClose()
    }.collect { println(it) }

    // notice the function will not terminate and keep waiting.
}

// Keep flow alive and await close
fun simpleChannelFlowKeepAliveAwaitClose(): Unit = runBlocking {
    channelFlow {
        for (i in 1..5) send(i)
        awaitClose()
    }.collect { println(it) }

    // Note: not putting awaitClose() for callbackFlow will cause the below crash during runtime
}

// Sending data from outside the lambda
fun sendDataFromOutsideLambda(): Unit = runBlocking {

    var sendData: suspend (data: Int) -> Unit = { }

    callbackFlow {
        for (i in 1..5) send(i)
        sendData = { data -> send(data) }
        awaitClose()
    }.collect { println(it) }

    println("Sending 6")
    sendData(6)  // note: 6 is never printed
}

// Unblocking the awaiting process
fun unblockingAwaitingProcess(): Unit = runBlocking {

    var sendData: suspend (data: Int) -> Unit = { }

    launch {
        callbackFlow {
            for (i in 1..5) send(i)
            sendData = { data -> send(data) }
            awaitClose()
        }.collect { println(it) }
    }

    println("Sending 6")  // now unblocked, so this prints.
    sendData(6) // this is not printed bc the process ends before it can be received.
}

fun unblockingAwaitingProcessWithSmallDelay(): Unit = runBlocking {

    var sendData: suspend (data: Int) -> Unit = { }

    launch {
        callbackFlow {
            for (i in 1..5) send(i)
            sendData = { data -> send(data) }
            awaitClose()
        }.collect { println(it) }
    }

    delay(10)
    println("Sending 6")
    sendData(6) // 6 is now emitted & printed

    // the callbackflow is not ended, its just waiting for more data or the `close` instruction.
}

fun properlyCloseTheChannelExternally(): Unit = runBlocking {

    var sendData: suspend (data: Int) -> Unit = { }
    var closeChannel: () -> Unit = { }  // `close` is now externally accessible

    launch {
        callbackFlow {
            for (i in 1..5) send(i)
            sendData = { data -> send(data) }
            closeChannel = { close() }
            awaitClose()
        }.collect { println(it) }
    }

    delay(10)
    println("Sending 6")

    sendData(6)

    closeChannel()  // callbackflow is now closed properly.
}

fun sendingMoreDataAfterChannelClosedWillCrash(): Unit = runBlocking {

    var sendData: suspend (data: Int) -> Unit = { }
    var closeChannel: () -> Unit = { }

    launch {
        callbackFlow {
            for (i in 1..5) send(i)
            sendData = { data -> send(data) }
            closeChannel = { close() }
            awaitClose()
        }.collect { println(it) }
    }

    delay(10)
    println("Sending 6")
    sendData(6)
    closeChannel()

    sendData(7)  // After the channel is closed, this will crash the program.
}

fun sendToChannelUsingTrySendToPreventCrashAfterChannelClosed(): Unit = runBlocking {

    var sendData: (data: Int) -> Unit = { } // no need suspend
    var closeChannel: () -> Unit = { }

    launch {
        callbackFlow {
            for (i in 1..5) send(i)
            sendData = { data -> trySend(data) }  // using trySend instead of send
            closeChannel = { close() }
            awaitClose()
        }.collect { println(it) }
    }

    delay(10)
    println("Sending 6")
    sendData(6)
    closeChannel()

    sendData(7) // will not crash now.
}

fun sendToChannelWithCleanup(): Unit = runBlocking {

    var sendData: (data: Int) -> Unit = { }
    var closeChannel: () -> Unit = { }

    launch {
        callbackFlow {
            for (i in 1..5) trySend(i)
            sendData = { data -> trySend(data) }
            closeChannel = { close() }
            awaitClose {
                sendData = {}       // note: this is the cleanup function
                closeChannel = {}   // note: this is the cleanup function
            }
        }.collect { println(it) }
    }

    delay(10)
    println("Sending 6")
    sendData(6)
    closeChannel()

    sendData(7) // won't be called bc the cleanup function is called.
}

fun useAwaitCloseAndNotUsingTrySendWillCrash(): Unit = runBlocking {

    var sendData: suspend (data: Int) -> Unit = { }
    var closeChannel: () -> Unit = { }

    launch {
        callbackFlow {
            for (i in 1..5) send(i)
            sendData = { data -> send(data) }
            closeChannel = { close() }
            awaitClose {
                sendData = {}
                closeChannel = {}
            }
        }.collect { println(it) }
    }

    delay(10)
    println("Sending 6")
    sendData(6)
    closeChannel()

    // Using `send` instead of `trySend` will crash the program bc
    // the awaitClose is triggered a little slow.
    sendData(7)
}

fun useAwaitCloseAndNotUsingTrySendHackWithDelayToPreventCrash(): Unit = runBlocking {

    var sendData: suspend (data: Int) -> Unit = { }
    var closeChannel: () -> Unit = { }

    launch {
        callbackFlow {
            for (i in 1..5) send(i)
            sendData = { data -> send(data) }
            closeChannel = { close() }
            awaitClose {
                sendData = {}
                closeChannel = {}
            }
        }.collect { println(it) }
    }

    delay(10)
    println("Sending 6")
    sendData(6)
    closeChannel()

    delay(10) // waits for the awaitClose to be triggered.
    sendData(7) // won't crash now, but hacky.
}

fun trySendIsDifferentThanSendIsDifferentFromTrySendBlocking() = runBlocking {

    runBlocking {
        println("Send:")

        channelFlow {
            for (i in 1..10) {
                delay(10)
                send(i)
            }
        }.buffer(0).collect {
            delay(100)
            println(it)
        }

        // output: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
    }

    runBlocking {
        println("\nTrySend:")

        channelFlow {
            for (i in 1..10) {
                delay(10)
                trySend(i)
            }
        }.buffer(0).collect {
            delay(100)
            println(it)
        }

        // output: 1, 10
        // Because the buffer is 0, and the delay is interrupt-able by `trySend`,
        // so it only prints the first and last emissions.
    }

    runBlocking {
        println("\nTrySendBlocking:")

        channelFlow {
            for (i in 1..10) {
                delay(10)
                trySendBlocking(i)
            }
        }.buffer(0).collect {
            delay(100)
            println(it)
        }

        // output: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        // TrySendBlocking is not interrupt-able, so it will print all the emissions.
    }
}

fun isTrySendSameAsSend() {

    val myAbcFlow = flow {
        ('A'..'J').forEach {
            delay(10)
            emit(it)
        }
    }

    val my123Flow = flow {
        (1..10).forEach {
            delay(10)
            emit(it)
        }
    }

    runBlocking {
        print("Send            ")
        my123Flow.channelMergeSend(myAbcFlow).buffer(0).collect {
            delay(100)
            print("$it ")
        }
        println()

        print("TrySendBlocking ")
        my123Flow.channelMergeTryBlocking(myAbcFlow).buffer(0).collect {
            delay(100)
            print("$it ")
        }
        println()

        print("TrySend         ")
        my123Flow.channelMergeTrySend(myAbcFlow).buffer(0).collect {
            delay(100)
            print("$it ")
        }
        println()
    }

//    Send            A 1 B 2 C 3 D 4 E 5 F 6 G 7 H 8 I 9 J 10 // expected order.
//    TrySendBlocking A 1 B C 2 3 D E 4 5 F G 6 7 H I 8 9 J 10 // unexpected order.
//    TrySend         A I   // expected order (only got the first and last emissions).
}

fun <T> Flow<T>.channelMergeSend(other: Flow<T>): Flow<T> =
    channelFlow {
        launch {
            collect { send(it) }
        }
        other.collect { send(it) }
    }

fun <T> Flow<T>.channelMergeTrySend(other: Flow<T>): Flow<T> =
    channelFlow {
        launch {
            collect { trySend(it) }
        }
        other.collect { trySend(it) }
    }

fun <T> Flow<T>.channelMergeTryBlocking(other: Flow<T>): Flow<T> =
    channelFlow {
        launch {
            collect { trySendBlocking(it) }
        }
        other.collect { trySendBlocking(it) }
    }
