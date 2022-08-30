package com.lizpostudio.kgoptometrycrm.utils

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.lizpostudio.kgoptometrycrm.R

/**
 * Builds alert dialog with text box to confirm password and returns
 * callback if password was correctly entered
 */
fun actionConfirmDeletion(
    title: String,
    message: String,
    isAdmin: Boolean,
    context: Context,
    checkPassword: Boolean = true,
    onDeleted: (deleteAllowed: Boolean) -> Unit
) {

    val passwordBox = EditText(context)
    passwordBox.textAlignment = View.TEXT_ALIGNMENT_CENTER
    val dialogBuilder = AlertDialog.Builder(context)
    dialogBuilder.setTitle(title)
    dialogBuilder.setMessage(message)
    dialogBuilder.setView(passwordBox)

    if (!checkPassword)
        passwordBox.visibility = View.GONE

    dialogBuilder.setPositiveButton(
        context.getString(R.string.yes_answer)
    ) { _, _ ->
        val textInput = passwordBox.text.toString()
        if (textInput == "Kgopto" || isAdmin || !checkPassword) {
            onDeleted(true)
        } else {
            Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
        }
    }
    dialogBuilder.setNegativeButton(
        context.getString(R.string.no_answer)
    ) { _, _ -> }

    val alertDialog = dialogBuilder.create()
    alertDialog.show()

}