package com.bignerdranch.android.criminalintent

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACT = 1
private const val MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0
private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks {

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callButton: Button
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        callButton = view.findViewById(R.id.crime_call_suspect) as Button
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime = crime
                    updateUI()
                }
            })
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {

            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // This space intentionally left blank
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                // This one too
            }
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject)
                )
            }.also { intent ->
                val chooserIntent =
                    Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {

            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {
                if (ContextCompat.checkSelfPermission(
                        requireActivity(),
                        Manifest.permission.READ_CONTACTS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.READ_CONTACTS),
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS
                    )
                } else {
                    startActivityForResult(pickContactIntent, REQUEST_CONTACT)
                }
            }

            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(
                    pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            if (resolvedActivity == null) {
                isEnabled = false
            }
        }



        callButton.apply {
            val callIntent: Intent =
                Uri.parse("tel:${crime.suspect?.suspectPhoneNumber}").let { number ->
                    Intent(Intent.ACTION_DIAL, number)
                }
            val packageManager: PackageManager = requireActivity().packageManager
            val activities: List<ResolveInfo> = packageManager.queryIntentActivities(
                callIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            setOnClickListener {
                val isIntentSafe: Boolean = activities.isNotEmpty()
                if (isIntentSafe)
                    Log.d("phone number: ","number is: ${crime.suspect?.suspectPhoneNumber}")
                    startActivity(callIntent)
            }
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_CONTACTS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startActivityForResult(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), REQUEST_CONTACT)
                }
                else{
                    Toast.makeText(requireActivity(),"Cannot Access Contacts without your permission",Toast.LENGTH_SHORT).show()
                }

            }
        }
    }
    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect != null) {
            suspectButton.text = crime.suspect.let { suspect ->
                suspect?.suspectName
            }

            callButton.text = crime.suspect.let { suspect ->
                "call ${suspect?.suspectName}"
            }

        } else {
            suspectButton.text=getString(R.string.crime_suspect_text)
            callButton.apply {
                isEnabled = false
                text=getString(R.string.call_suspect)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                //we have a suspect Object
                crime.suspect = Suspect()
                query1(data)
                query2()

                crimeDetailViewModel.saveCrime(crime)
                suspectButton.text = crime.suspect?.suspectName
                callButton.apply {
                    text = "call ${crime.suspect?.suspectName}"
                    isEnabled = true
                }
            }
        }
    }

    private fun query1(data: Intent) {
        val contactUri: Uri? = data.data
        // Specify which fields you want your query to return values for.
        val queryFields =
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID)
        // Perform your query - the contactUri is like a "where" clause here
        val cursor = requireActivity().contentResolver
            .query(contactUri, queryFields, null, null, null)
        cursor?.use {
            // Double-check that you actually got results
            if (it.count == 0) {
                return
            }

            // Pull out the first column of the first row of data -
            // that is your suspect's name.
            it.moveToFirst()
            crime.suspect?.suspectName =
                it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
            crime.suspect?.suspectId =
                it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))

        }
    }

    private fun query2() {
        val contactUri: Uri? = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        // Specify which fields you want your query to return values for.
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selectionClause = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(crime.suspect?.suspectId)
        val cursor = requireActivity().contentResolver.query(
            contactUri, projection, selectionClause, selectionArgs, null
        )
        cursor?.use {
            if (it.count == 0) {
                Log.d("phone number: ","number is: ${it.count}")
                return
            }
            Log.d("phone number: ","number is: ${it.count}")


            it.moveToFirst()
            crime.suspect?.suspectPhoneNumber =
                it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            Log.d("phone number: ","number is: ${(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))}")



        }
    }


    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspect = if (crime.suspect == null) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(
            R.string.crime_report,
            crime.title, dateString, solvedString, suspect
        )
    }

    companion object {

        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}