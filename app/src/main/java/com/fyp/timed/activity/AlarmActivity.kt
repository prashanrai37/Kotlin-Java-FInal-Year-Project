package com.fyp.timed.activity

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.fyp.timed.R
import com.fyp.timed.database.MySharedPreferences
import com.fyp.timed.database.ReminderDatabase
import com.fyp.timed.databinding.ActivityAlarmBinding
import com.fyp.timed.util.*
import java.util.*
import android.os.Bundle
import android.util.Log


class AlarmActivity : AppCompatActivity() {

    var reminderAlarmId: Int = 0
    lateinit var r: Ringtone
    lateinit var activityAlarmBinding: ActivityAlarmBinding
    var tag = AlarmActivity::class.simpleName
    var ringtoneUri: Uri? = null
    lateinit var mySharedPreferences: MySharedPreferences
    lateinit var reminderTitle:String
    var reminderMiles:Long = 0
    lateinit var reminderReportAs:String
    lateinit var reminderDatabase:ReminderDatabase
    private var nfcAdapter: NfcAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this)?.let { it }

        if (isDeviceLocked(this)){
            val wind = this.getWindow();

            wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            wind.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            wind.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            wind.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState)
        activityAlarmBinding = DataBindingUtil.setContentView(this,R.layout.activity_alarm)
        initAll()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        var tagFromIntent: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val n = NfcA.get(tagFromIntent)
        val nfc = NfcA.get(tagFromIntent)
        val atqa: ByteArray = n.getAtqa()
        val sak: Short = nfc.getSak()
        nfc.connect()
        val isConnected= nfc.isConnected()

        if(isConnected)
        {
            onDestroy()
        }
    else{
        Log.e("ans", "Not connected")
    }
}

    private fun enableForegroundDispatch(activity: AppCompatActivity, adapter: NfcAdapter?) {

        val intent = Intent(activity.applicationContext, activity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, 0)

        val filters = arrayOfNulls<IntentFilter>(1)
        val techList = arrayOf<Array<String>>()

        filters[0] = IntentFilter()
        with(filters[0]) {
            this?.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
            this?.addCategory(Intent.CATEGORY_DEFAULT)
            try {
                this?.addDataType("text/plain")
            } catch (ex: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException()
            }
        }

        adapter?.enableForegroundDispatch(activity, pendingIntent, filters, techList)
    }

    override fun onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onDestroy()
    }
    private fun initAll() {

        supportActionBar?.hide()

            mySharedPreferences = MySharedPreferences(this)
            reminderDatabase = ReminderDatabase.invoke(this)
            reminderTitle = mySharedPreferences.getReminderTitle()!!
            val calendar = Calendar.getInstance()
            reminderMiles = calendar.timeInMillis
            reminderAlarmId = mySharedPreferences.getAlarmId()!!.toInt()
            reminderReportAs = mySharedPreferences.getReportAs()!!
            activityAlarmBinding.tvTitle.text = reminderTitle
            activityAlarmBinding.tvTime.text = simpleDateFormat.format(Date(reminderMiles))

            try {
                ringtoneUri =
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                r = RingtoneManager.getRingtone(
                    this,
                    ringtoneUri
                )
                r.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            activityAlarmBinding.tvDismiss.setOnClickListener { v ->
                activityAlarmBinding.tvDismiss.visibility = View.INVISIBLE
                activityAlarmBinding.tvHoldTheButton.visibility = View.VISIBLE

            }

            activityAlarmBinding.tvHoldTheButton.setOnLongClickListener { v ->
                r.stop()
                finish()
                return@setOnLongClickListener true
            }

            activityAlarmBinding.tvSnooze.setOnClickListener { view ->
                onScheduleAlarmFor10Minutes(
                    reminderMiles,
                    reminderTitle,
                    reminderAlarmId,
                    reminderReportAs
                )
                r.stop()
                toast("Alarm Snoozed For 10 Minutes")
                finish()
            }


    }

}