package com.fyp.timed.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fyp.timed.R
import com.fyp.timed.activity.AlarmActivity
import com.fyp.timed.activity.BootActivity
import com.fyp.timed.database.MySharedPreferences


class MyBroadCastReceiver:BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent == null) {return}
        else {
            if (intent?.action.equals("android.intent.action.BOOT_COMPLETED")) {
                val intent = Intent(context,BootActivity::class.java)
                intent.putExtra(BOOT,BOOT)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                context?.startActivity(intent)
            }else {
                showNotificationAndAlarms(context, intent)
            }

        }
    }



    private fun showNotificationAndAlarms(
        context: Context?,
        intent: Intent?
    ) {
        var reminderTitle = intent?.getStringExtra(REMINDER_TITLE)
        var reportAs = intent?.getStringExtra(SELECTED_REPORTAS)
        var alarmId = intent?.getStringExtra(ALARM_ID)
        var millies = intent?.getStringExtra(MILLIES)

        if (reportAs.equals(ALARM)){

            var mySharedPreferences = MySharedPreferences(context!!)

            if (alarmId != null && millies != null && reminderTitle != null && reportAs != null) {
                mySharedPreferences.setNotiData(alarmId,millies,reminderTitle,reportAs)
            }

            val intent = Intent(context,AlarmActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context?.startActivity(intent)

        }else{
            val notificationId = (0..1000000).random()

            var snoozeIntent = BootActivity().getSnoozeIntent(millies,reminderTitle,alarmId,reportAs,notificationId.toString(),context)

            var builder = NotificationCompat.Builder(context!!, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_alarm_24)
                .setContentTitle("Timed")
                .setAutoCancel(true)
                .setContentText(reminderTitle)
                .addAction(R.drawable.ic_baseline_alarm_24, "SNOOZE", snoozeIntent)
                .setColor(Color.parseColor("#77AE8E"))
            var notificationManager = NotificationManagerCompat.from(context)

            notificationManager.notify(notificationId, builder.build())
        }
    }





}