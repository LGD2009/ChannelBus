package com.ftd.channelbus

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activity_main_btn.setOnClickListener {
            ChannelBus.instance.send(Events.EVENT_1, true)
        }
        ChannelBus.instance.receive(this.javaClass.name) {
            activity_main_text.text = it.name
        }

        activity_main_to.setOnClickListener {
            startActivity(Intent(this, StickyActivity::class.java))
        }

    }

    override fun onDestroy() {
        ChannelBus.instance.remove(this.javaClass.name)
        super.onDestroy()
    }


}
