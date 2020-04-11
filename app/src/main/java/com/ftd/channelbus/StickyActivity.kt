package com.ftd.channelbus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_sticky.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class StickyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sticky)

        activity_sticky_btn.setOnClickListener {
            ChannelBus.instance.send(Events.EVENT_2)
        }

        ChannelBus.instance.receive(this, Dispatchers.IO) {
            println(it)
            val s = httpRequest()
            withContext(Dispatchers.Main) {
                println(Thread.currentThread().name)
                activity_sticky_text.text = s
            }

        }
    }

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

}
