package com.lizpostudio.kgoptometrycrm.data.source.remote.firebase

object FirebasePath {

    const val RECORDS = "records"
    const val HISTORY = "history"
    const val DEL_HISTORY = "deleted_records"
    const val SETTINGS = "settings"
    const val USERS = "users"
    const val PRACTITIONERS = "practitioners"

    fun getChildRecords(name: Any) = "$RECORDS/$name"
}