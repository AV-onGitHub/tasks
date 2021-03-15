package org.tasks.dialogs

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.todoroo.andlib.utility.DateUtilities
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.currentTimeMillis
import org.tasks.time.DateTimeUtils.startOfDay

class MyDatePickerDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragment =
            (childFragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) as? MaterialDatePicker<Long>)
                ?: newDatePicker(initial)
                    .let {
                        childFragmentManager
                            .beginTransaction()
                            .add(it, FRAG_TAG_DATE_PICKER)
                            .commit()
                        it
                    }
        with(fragment) {
            addOnPositiveButtonClickListener {
                val dt = DateTime(it, DateTime.UTC)
                selected(dt.year, dt.monthOfYear, dt.dayOfMonth)
            }
            addOnCancelListener { cancel() }
            addOnNegativeButtonClickListener { cancel() }
        }
    }

    private val initial: Long
        get() = arguments?.getLong(MyTimePickerDialog.EXTRA_TIMESTAMP) ?: DateUtilities.now().startOfDay()

    private fun selected(year: Int, month: Int, day: Int) {
        targetFragment?.onActivityResult(
            targetRequestCode,
            RESULT_OK,
            Intent().putExtra(EXTRA_TIMESTAMP, DateTime(year, month, day).millis)
        )
        dismiss()
    }

    private fun cancel() {
        targetFragment?.onActivityResult(targetRequestCode, RESULT_CANCELED, null)
        dismiss()
    }

    companion object {
        const val FRAG_TAG_DATE_PICKER = "frag_date_picker"
        const val EXTRA_TIMESTAMP = "extra_timestamp"

        @JvmStatic
        fun newDatePicker(target: Fragment, rc: Int, initial: Long) =
            MyDatePickerDialog().apply {
                arguments = Bundle().apply {
                    putLong(EXTRA_TIMESTAMP, initial)
                }
                setTargetFragment(target, rc)
            }

        @JvmStatic
        fun newDatePicker(initial: Long) = MaterialDatePicker.Builder.datePicker()
            // TODO: setInputMode for calendar or text
            // TODO: figure out hack for first day of week
            .setSelection(if (initial > 0) initial else currentTimeMillis())
            .build()
    }
}