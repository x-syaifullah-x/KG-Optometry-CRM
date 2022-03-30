package com.lizpostudio.kgoptometrycrm.firebase

/*    private fun copyPasteLocalToFirebase() {
        for (item in allSectionsList) {
                        recordsReference!!.child((item.recordID).toString()).setValue(convertFormToFBRecord(item))
                    }
                    Toast.makeText(context, "Done", Toast.LENGTH_LONG).show()
    }*/
/*    private fun uploadDBtoFireStore() {
        val today = Calendar.getInstance()
        val todayYear = today.get(Calendar.YEAR)
        val todayMonth = today.get(Calendar.MONTH)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        val myActivity = activity

        myActivity?.let{
            val datePickerDialog = DatePickerDialog(
                it,
                { _, year, monthOfYear, dayOfMonth -> // set day of month , month and year value in the edit text
                   val startUploadDate = convertYMDtoTimeMillis(year, monthOfYear, dayOfMonth)
                    Toast.makeText(context, "Uploading  ... ", Toast.LENGTH_SHORT).show()

                    val newList = mutableListOf<Patients>()
                    for (item in allSectionsList) {
                        if (item.dateOfSection > startUploadDate) {
                            newList.add(item)
                        }
                    }

                    var startingID = System.currentTimeMillis()
                    for (item in newList) {
                        recordsReference!!.child((startingID).toString()).setValue(convertFormToFBRecord(item))
                        startingID++
                    }
                    binding.foundItemsText.text = "${newList.size} records exported to Firebase"
                    Toast.makeText(context, "Done", Toast.LENGTH_LONG).show()


                }, todayYear, todayMonth, todayDay
            )
            datePickerDialog.show()


        }
    }*/