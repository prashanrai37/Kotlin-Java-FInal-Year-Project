package com.fyp.timed.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.fyp.timed.R
import com.fyp.timed.activity.AddNewReminder
import com.fyp.timed.activity.CalendarDayActivity
import com.fyp.timed.database.ReminderDatabase
import com.fyp.timed.databinding.FragmentCalendarBinding
import com.fyp.timed.util.CALENDAR_DATE
import com.fyp.timed.util.dateFormat
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList


class CalendarFragment : Fragment() {

    private lateinit var fragmentCalendarBinding:FragmentCalendarBinding
    lateinit var reminderDatabase:ReminderDatabase
    var eventDay = ArrayList<EventDay>()
    var calendarList = ArrayList<Calendar>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentCalendarBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_calendar,
            container,
            false
        )
        initAll()
        return fragmentCalendarBinding.root
    }

    private fun initAll() {
        reminderDatabase = ReminderDatabase.invoke(requireContext())

        getReminderList()

        fragmentCalendarBinding.fabAddReminder.setOnClickListener{
            val intent = Intent(context, AddNewReminder::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        fragmentCalendarBinding.calendarView.setOnDayClickListener(OnDayClickListener { eventDay ->
            var calendarDay = eventDay.calendar
            for (i in calendarList) {
                if (i.equals(calendarDay)) {
                    var calDate = dateFormat.format(calendarDay.time)
                    val intent = Intent(context, CalendarDayActivity::class.java)
                    intent.putExtra(CALENDAR_DATE, calDate)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }

        })

    }

    private fun getReminderList() {
        lifecycleScope.launch {
            this.coroutineContext.let {
                var reminderTable  = reminderDatabase.addNewReminderDao().fetchAllData()
                for (i in reminderTable){
                    var calendar = Calendar.getInstance()
                    var date = dateFormat.parse(i.date)
                    calendar.time = date
                    calendarList.add(calendar)
                    eventDay.add(
                        EventDay(
                            calendar, R.drawable.ic_baseline_alarm_24, Color.parseColor(
                                "#FD6262"
                            )
                        )
                    )
                }
                fragmentCalendarBinding.calendarView.setEvents(eventDay)

            }
        }
    }

    override fun onResume() {
        getReminderList()
        super.onResume()
    }
}