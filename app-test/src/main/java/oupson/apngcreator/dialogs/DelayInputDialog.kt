package oupson.apngcreator.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import oupson.apngcreator.R

class DelayInputDialog(
    private val listener: InputSenderDialogListener?,
    private val value : Int? = null
) : DialogFragment() {
    interface InputSenderDialogListener {
        fun onOK(number: Int?)
        fun onCancel(number: Int?)
    }

    private var mNumberEdit: EditText? = null

    override fun getTheme() = R.style.RoundedCornersDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogLayout: View = View.inflate(context, R.layout.dialog_delay, null)
        mNumberEdit =
            dialogLayout.findViewById<TextInputLayout>(R.id.delay_textInputLayout).editText
        if (value != null)
            mNumberEdit?.setText(value.toString())
        return MaterialAlertDialogBuilder(
            requireContext(),
            R.style.RoundedCornersDialog
        )
            .setTitle(R.string.delay)
            .setView(dialogLayout)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                listener?.onOK(java.lang.String.valueOf(mNumberEdit?.text).toIntOrNull())
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
                listener?.onCancel(java.lang.String.valueOf(mNumberEdit?.text).toIntOrNull())
            }.create()
    }
}