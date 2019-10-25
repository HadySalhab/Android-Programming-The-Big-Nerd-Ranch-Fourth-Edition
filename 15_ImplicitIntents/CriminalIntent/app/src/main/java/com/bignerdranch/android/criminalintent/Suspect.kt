package com.bignerdranch.android.criminalintent

import androidx.room.ColumnInfo

data class Suspect (@ColumnInfo(name="suspect_name") var suspectName:String= "",@ColumnInfo(name="suspect_id")
                    var suspectId:String = "",@ColumnInfo(name="suspect_phonenumber")
                    var suspectPhoneNumber:String = ""
)