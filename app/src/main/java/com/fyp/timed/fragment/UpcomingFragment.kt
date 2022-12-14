package com.fyp.timed.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.fyp.timed.R
import com.fyp.timed.activity.AddNewReminder
import com.fyp.timed.adapter.ReminderListAdapter
import com.fyp.timed.database.MySharedPreferences
import com.fyp.timed.database.ReminderDatabase
import com.fyp.timed.databinding.BottomSheetAllReminderDialogBinding
import com.fyp.timed.databinding.FragmentUpcomingBinding
import com.fyp.timed.model.AddNewReminderTable
import com.fyp.timed.util.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList


class UpcomingFragment : Fragment() {

    private var postPoneValue: String = FIFTEEN_MINUTES
    lateinit var fragmentUpcomingBinding: FragmentUpcomingBinding
    private lateinit var remindeLisAdapter: ReminderListAdapter
    lateinit var reminderDatabase: ReminderDatabase
    lateinit var mySharedPreferences: MySharedPreferences;
    private val reminderTableList: ArrayList<AddNewReminderTable> = ArrayList<AddNewReminderTable>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentUpcomingBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_upcoming, container, false)
        initAll()
        return fragmentUpcomingBinding.root
    }

    private fun initAll() {
        setHasOptionsMenu(true)

        reminderDatabase = ReminderDatabase.invoke(requireContext())
        mySharedPreferences= MySharedPreferences(requireContext())
        remindeLisAdapter = ReminderListAdapter(
            requireContext(),reminderTableList,
            object : ReminderListAdapter.OnClickListener {
                override fun onStarClick(position: Int) {
                    var addNewReminderTable = reminderTableList.get(position)
                    updateReminder(addNewReminderTable)
                }

                override fun onDotsClick(position: Int) {
                    var addNewReminderTable = reminderTableList.get(position)
                    openBottomSheetDialog(addNewReminderTable)
                }

                override fun onMobileClick(position: Int) {
                    var phone = reminderTableList.get(position).title
                    activity?.openDailer(phone!!.substring(5,phone!!.length))
                }

                override fun onItemClick(position: Int) {
                    onEditButtonClicked(reminderTableList.get(position))
                }


            },mySharedPreferences)
        val layoutManager = LinearLayoutManager(context)

        fragmentUpcomingBinding.rvUpcomingReminderList.layoutManager = layoutManager
        fragmentUpcomingBinding.rvUpcomingReminderList.itemAnimator = DefaultItemAnimator()
        fragmentUpcomingBinding.rvUpcomingReminderList.adapter = remindeLisAdapter

        getReminderList()
    }

    private fun getReminderList() {
        lifecycleScope.launch {
            this.coroutineContext.let {
                var reminderTable  = reminderDatabase.addNewReminderDao().fetchDoneReminder(false)

                reminderTableList.clear()

                var cal = Calendar.getInstance()
                cal.add(Calendar.DATE,1)
                var tomorrow = cal.time
                var today = Date()

                for (i in reminderTable){
                    if (System.currentTimeMillis()< dateTimeFormat.parse(i.date+" "+i.time).time
                        && !dateFormat.format(tomorrow).equals(i.date)
                        && !dateFormat.format(today).equals(i.date)){
                        reminderTableList.add(i)
                    }
                }
                if(reminderTableList.isEmpty()){
                    fragmentUpcomingBinding.rvUpcomingReminderList.visibility = View.GONE
                    fragmentUpcomingBinding.llEmptyLayout.visibility = View.VISIBLE
                }else{
                    fragmentUpcomingBinding.rvUpcomingReminderList.visibility = View.VISIBLE
                    fragmentUpcomingBinding.llEmptyLayout.visibility = View.GONE
                }
                remindeLisAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun updateReminder(addNewReminderTable: AddNewReminderTable) {

        lifecycleScope.launch {
            this.coroutineContext.let {
                var isFav:Boolean
                if (addNewReminderTable.isFav){
                    isFav = false
                }else{
                    isFav = true
                }
                addNewReminderTable.isFav = isFav
                var long= reminderDatabase.addNewReminderDao().updateAll(addNewReminderTable)
                if (long>0) {
                    getReminderList()
                }else{
                    activity?.toast("Reminder Added Failed")
                }
            }
        }

    }

    private fun openBottomSheetDialog(addNewReminderTable: AddNewReminderTable) {
        val bottomSheetDialog =
            BottomSheetDialog(requireContext())
        val bottomSheetAllReminderDialogBinding =
            DataBindingUtil.inflate(LayoutInflater.from(bottomSheetDialog.getContext()),R.layout.bottom_sheet_all_reminder_dialog, null,false) as BottomSheetAllReminderDialogBinding
        bottomSheetDialog.setContentView(bottomSheetAllReminderDialogBinding.root)
        bottomSheetDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        BottomSheetBehavior.from(bottomSheetAllReminderDialogBinding.root.parent as View).peekHeight = 1000

        bottomSheetAllReminderDialogBinding.llEdit.setOnClickListener{view ->   bottomSheetDialog.dismiss()
            onEditButtonClicked(addNewReminderTable)
        }

        bottomSheetAllReminderDialogBinding.llDelete.setOnClickListener{view ->
            bottomSheetDialog.dismiss()
            onDeleteButtonClicked(addNewReminderTable)
        }

        bottomSheetAllReminderDialogBinding.llShare.setOnClickListener{view ->
            bottomSheetDialog.dismiss()
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_SUBJECT, "Android Studio Pro")
            intent.putExtra(
                Intent.EXTRA_TEXT,
                addNewReminderTable.title
            )
            intent.type = "text/plain"
            startActivity(intent)
        }

        bottomSheetAllReminderDialogBinding.llCopy.setOnClickListener{view ->
            bottomSheetDialog.dismiss()
            val clipboard: ClipboardManager? =
                activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText("label", addNewReminderTable.title)
            clipboard?.setPrimaryClip(clip)
            activity?.toast("Copied to clipboard")
        }

        bottomSheetAllReminderDialogBinding.llPostPone.setOnClickListener { view ->
            bottomSheetDialog.dismiss()
            onPostPoneButtonClicked(addNewReminderTable)
        }
        bottomSheetAllReminderDialogBinding.llDone.setOnClickListener{view ->
            bottomSheetDialog.dismiss()
            addReminderToDone(addNewReminderTable)
        }
        bottomSheetDialog.show()
    }

    private fun onEditButtonClicked(addNewReminderTable: AddNewReminderTable) {
        val intent = Intent(context, AddNewReminder::class.java)
        intent.putExtra(ADD_REMINDER_TABLE,addNewReminderTable)
        startActivity(intent)
    }


    private fun onDeleteButtonClicked(addNewReminderTable: AddNewReminderTable) {
        val materialDialogBuilder =
            MaterialAlertDialogBuilder(requireContext())
        materialDialogBuilder.setTitle("Delete the task")
        materialDialogBuilder.setMessage("Are you sure you want to delete the task ?")
        materialDialogBuilder.setNeutralButton("Cancel") { dialog, which ->
            // Respond to neutral button press
        }
        materialDialogBuilder.setPositiveButton("Delete") { dialog, which ->
            lifecycleScope.launch {
                this.coroutineContext.let {
                    var long= reminderDatabase.addNewReminderDao().deleteData(addNewReminderTable)
                    if (long>0) {
                        activity?.onCancelAlarm(addNewReminderTable.alarmId)
                        getReminderList()
                    }else{
                        activity?.toast("Delete Fail")
                    }
                }
            }
        }
        materialDialogBuilder.show()

    }

    private fun addReminderToDone(addNewReminderTable: AddNewReminderTable) {

        lifecycleScope.launch {
            this.coroutineContext.let {
                addNewReminderTable.isDone = true
                var long= reminderDatabase.addNewReminderDao().updateAll(addNewReminderTable)
                if (long>0) {
                    activity?.toast("Marked as Done")
                    getReminderList()
                }else{
                    activity?.toast("Reminder Added Failed")
                }
            }
        }

    }

    private fun onPostPoneButtonClicked(addNewReminderTable: AddNewReminderTable) {
        val materialDialogBuilder =
            MaterialAlertDialogBuilder(requireContext())
        materialDialogBuilder.setTitle("PostPone")
        val singleItems = arrayOf(FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR, TOMORROW_AT_10, TOMORROW_AT_14, TOMORROW_AT_18);
        var checkedItem = 0
        materialDialogBuilder.setNeutralButton("Cancel") { dialog, which ->
            // Respond to neutral button press
        }
        materialDialogBuilder.setPositiveButton("Postpone") { dialog, which ->
            updatePostponeReminder(addNewReminderTable,postPoneValue)
        }
        materialDialogBuilder.setSingleChoiceItems(singleItems, checkedItem) { dialog, which ->
            // Respond to item chosen
            postPoneValue = singleItems[which]
        }
        materialDialogBuilder.show()
    }
    private fun updatePostponeReminder(
        addNewReminderTable: AddNewReminderTable,
        postPoneValue: String
    ) {
        var dateTime =dateTimeFormat.parse(addNewReminderTable.date+" "+addNewReminderTable.time)
        var postponeMillies:Long
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateTime.time
        calendar.add(Calendar.DATE,1)
        val tomorrowDate = dateFormat.format(calendar.timeInMillis)
        if (postPoneValue.equals(FIFTEEN_MINUTES)) {
            postponeMillies = dateTime.time + INTERVAL_FIFTEEN_MINUTES
            Log.d(tag, "updatePostponeReminder: FIFTEEN_MINUTES"+postponeMillies)
        }else if (postPoneValue.equals(THIRTY_MINUTES)){
            postponeMillies = dateTime.time + INTERVAL_THIRTY_MINUTES
            Log.d(tag, "updatePostponeReminder: THIRTY_MINUTES"+postponeMillies)
        }else if (postPoneValue.equals(ONE_HOUR)){
            postponeMillies = dateTime.time + INTERVAL_ONE_HOUR
            Log.d(tag, "updatePostponeReminder: "+postponeMillies)
        }else if (postPoneValue.equals(TOMORROW_AT_10)){
            dateTime = dateTime24HourFormat.parse(tomorrowDate+" "+"10:00")
            postponeMillies = dateTime.time
            Log.d(tag, "updatePostponeReminder: "+dateFormat.format(calendar.timeInMillis))
        }else if (postPoneValue.equals(TOMORROW_AT_14)){
            dateTime = dateTime24HourFormat.parse(tomorrowDate+" "+"14:00")
            postponeMillies = dateTime.time
        }else {
            dateTime = dateTime24HourFormat.parse(tomorrowDate+" "+"18:00")
            postponeMillies = dateTime.time
        }
        val date = dateFormat.format(postponeMillies)
        val time = simpleDateFormat.format(postponeMillies)
        lifecycleScope.launch {
            val reminderTitle = addNewReminderTable.title
            val reminderAlarmId = addNewReminderTable.alarmId
            val reminderReportAs = addNewReminderTable.reportAs
            addNewReminderTable.date = date
            addNewReminderTable.time = time
            this.coroutineContext.let {
                var long = reminderDatabase.addNewReminderDao().updateAll(addNewReminderTable)
                if (long > 0) {
                    if (System.currentTimeMillis()< postponeMillies){
                        activity?.onSchedulAlarm(postponeMillies.toLong(),reminderTitle!!,addNewReminderTable.repeat!!,reminderAlarmId.toInt(),reminderReportAs!!)
                        activity?.toast("Postponed by "+postPoneValue)
                    }else{
                        activity?.toast("Postponed by .")
                    }
                    Log.d(tag, "saveReminder: " + postponeMillies)
                    getReminderList()
                } else {
                    activity?.toast("Reminder Updated Failed")
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home, menu)
        val menuItem = menu.findItem(R.id.search)
        val searchView = menuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                remindeLisAdapter.getFilter()!!.filter(newText.toString())
                return true
            }

        })
        super.onCreateOptionsMenu(menu, inflater)
    }

}