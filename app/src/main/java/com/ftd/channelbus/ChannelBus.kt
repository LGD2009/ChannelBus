package com.ftd.channelbus

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * author GaoPC
 * date 2019-12-19 16:52
 * description
 */
class ChannelBus private constructor() : LifecycleObserver {

    private val channel: Channel<Events> = Channel(Channel.BUFFERED)
    private val consumerMap = ConcurrentHashMap<String, ChannelConsumer>()
    private val lifecycleOwnerMap = ConcurrentHashMap<LifecycleOwner, ChannelConsumer>()

    private val stickyEventsList = mutableListOf<Events>()

    companion object {
        val instance: ChannelBus by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ChannelBus()
        }
    }

    /**
     * 对存储的消费者发送事件
     */
    init {
        GlobalScope.launch {
            for (e in channel) {
                consumerMap.entries.forEach {
                    it.value.jobList.add(launch(it.value.context) {
                        it.value.event(e)
                    })
                }
                lifecycleOwnerMap.entries.forEach {
                    it.value.jobList.add(launch(it.value.context) {
                        it.value.event(e)
                    })
                }
            }
        }
    }


    /**
     * [event] 需要发送的事件
     * [isSticky]  是否是粘性事件
     *
     * 如果是粘性事件，保存起来
     */
    fun send(event: Events, isSticky: Boolean = false) {
        GlobalScope.launch {
            if (isSticky) {
                stickyEventsList.add(event)
            }
            channel.send(event)
        }
    }

    /**
     * [key] 存储消费者时的key
     * [context]  处理事件默认主线程
     * [onEvent]  消费事件的lambada方法
     *
     */
    fun receive(
        key: String,
        context: CoroutineContext = Dispatchers.Main,
        onEvent: suspend (event: Events) -> Unit
    ) {
        consumerMap[key] = ChannelConsumer(context, onEvent)
    }

    fun receive(
        lifecycleOwner: LifecycleOwner,
        context: CoroutineContext = Dispatchers.Main,
        onEvent: suspend (event: Events) -> Unit
    ) {
        lifecycleOwner.lifecycle.addObserver(this)
        lifecycleOwnerMap[lifecycleOwner] = ChannelConsumer(context, onEvent)
    }

    /**
     *   添加新的消费者时，发送粘性事件
     */
    fun receiveSticky(
        key: String,
        context: CoroutineContext = Dispatchers.Main,
        onEvent: suspend (event: Events) -> Unit
    ) {
        consumerMap[key] = ChannelConsumer(context, onEvent)
        stickyEventsList.forEach { e ->
            consumerMap[key]?.jobList?.add(GlobalScope.launch(context) {
                onEvent(e)
            })
        }
    }

    fun receiveSticky(
        lifecycleOwner: LifecycleOwner,
        context: CoroutineContext = Dispatchers.Main,
        onEvent: suspend (event: Events) -> Unit
    ) {
        lifecycleOwner.lifecycle.addObserver(this)
        lifecycleOwnerMap[lifecycleOwner] = ChannelConsumer(context, onEvent)
        stickyEventsList.forEach { e ->
            lifecycleOwnerMap[lifecycleOwner]?.jobList?.add(GlobalScope.launch(context) {
                onEvent(e)
            })
        }
    }

    //移除粘性事件
    fun removeStickEvent(event: Events) {
        stickyEventsList.remove(event)
    }

    //移除消费者
    fun remove(key: String) {
        consumerMap[key]?.jobList?.forEach {
            it.cancel()
        }
        consumerMap.remove(key)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun remove(lifecycleOwner: LifecycleOwner) {
        lifecycleOwnerMap[lifecycleOwner]?.jobList?.forEach {
            it.cancel()
        }
        lifecycleOwnerMap.remove(lifecycleOwner)
    }

    /**
     * [context] 消费事件时的 CoroutineContext
     * [event]  消费事件的lambada方法
     * [jobList] 消费事件的协程job列表
     *
     * 事件消费者，用来保存事件的消费方法和消费时所在的协程上下文
     */
    data class ChannelConsumer(
        val context: CoroutineContext,
        val event: suspend (event: Events) -> Unit,
        var jobList: MutableList<Job> = mutableListOf()
    )

}

