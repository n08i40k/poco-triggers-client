package ru.n08i40k.poco.triggers.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.daemon.DaemonBridge

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        p1?.action // suppress warning

        if (!DaemonBridge.started)
            Application.INSTANCE.init()
    }
}