package com.bignerdranch.android.criminalintent.database

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bignerdranch.android.criminalintent.DatePickerFragment
import com.bignerdranch.android.criminalintent.R
import kotlinx.android.synthetic.main.image_dialog.view.*
import java.lang.IllegalStateException
private const val ARG_IMAGE= "image"
private lateinit var imageDialog: ImageView
private  var bitmap: Bitmap? = null
class ImageDialog : DialogFragment() {
    companion object {
        fun newInstance(bitmap: Bitmap): ImageDialog {
            val args = Bundle().apply {
                putParcelable(ARG_IMAGE, bitmap)
            }

            return ImageDialog().apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bitmap = arguments?.getParcelable(ARG_IMAGE)
    }



    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return requireActivity().let {
                val builder = AlertDialog.Builder(it)
                val view = it.layoutInflater.inflate(R.layout.image_dialog,null)
                imageDialog = view.findViewById(R.id.imageview_dialog) as ImageView
                imageDialog.setImageBitmap(bitmap)
                builder.setView(view)
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
    }

}