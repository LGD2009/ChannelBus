# ChannelBus
使用协程Channel实现事件总线


1.发送事件
```kotlin
 ChannelBus.instance.send(Events.EVENT_1)
```


2.在Activity或者Fragment里注册
```kotlin
     override fun onCreate(savedInstanceState: Bundle?) {
        ......
        ChannelBus.instance.receive(this) {
            activity_main_text.text = it.name
        }
		......
    }
```
或者
```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
       	......
        ChannelBus.instance.receive(this, Dispatchers.IO) {
            val s = httpRequest()	//IO线程，耗时操作
            withContext(Dispatchers.Main) {	//切回UI线程
                activity_sticky_text.text = s	//更改UI
            }

        }
    }

	//网络请求
    private fun httpRequest(): String {
        val url = URL("https://api.github.com/users/LGD2009")
        val urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.let {
            it.connectTimeout = 5000
            it.requestMethod = "GET"
        }
        urlConnection.connect()
        if (urlConnection.responseCode != 200) {
            return "请求url失败"
        } else {
            val inputStream: InputStream = urlConnection.inputStream
            return inputStream.bufferedReader().use { it.readText() }
        }
    }
```


博客地址：[使用协程Channel实现事件总线](https://juejin.im/post/5e92ef716fb9a03c7d3d05ba)
