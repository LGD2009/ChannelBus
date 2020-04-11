package com.ftd.channelbus

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * author GaoPC
 * date 2020-03-22 19:15
 * description
 */
@ExperimentalCoroutinesApi
class BroadcastChannelBus
private constructor(
    private val broadcastChannel: BroadcastChannel<Events> = BroadcastChannel(Channel.BUFFERED)
) : LifecycleObserver {

    private val lifecycleOwnerMap = ConcurrentHashMap<LifecycleOwner, ChannelConsumer>()

    companion object {
        val instance: BroadcastChannelBus by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            BroadcastChannelBus()
        }
    }

    fun send(event: Events) {
        GlobalScope.launch {
            broadcastChannel.send(event)
        }
    }

    fun receive(
        lifecycleOwner: LifecycleOwner,
        context: CoroutineContext = Dispatchers.Main,
        onEvent: suspend (event: Events) -> Unit
    ) {
        lifecycleOwner.lifecycle.addObserver(this)
        val receiveChannel = broadcastChannel.openSubscription()
        val job = GlobalScope.launch(context) {
            for (e in receiveChannel) {
                onEvent(e)
            }
        }
        lifecycleOwnerMap[lifecycleOwner] = ChannelConsumer(context, onEvent, job, receiveChannel)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun remove(lifecycleOwner: LifecycleOwner) {
        lifecycleOwnerMap[lifecycleOwner]?.receiveChannel?.cancel()
        lifecycleOwnerMap[lifecycleOwner]?.job?.cancel()
        lifecycleOwnerMap.remove(lifecycleOwner)
    }

    data class ChannelConsumer(
        val context: CoroutineContext,
        val event: suspend (event: Events) -> Unit,
        val job: Job?,
        val receiveChannel: ReceiveChannel<Events>
    )

}