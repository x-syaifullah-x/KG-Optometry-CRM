package com.lizpostudio.kgoptometrycrm

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.internal.SafeIterableMap
import androidx.navigation.findNavController
import androidx.savedstate.SavedStateRegistry
import com.google.firebase.database.*
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.RemoteDataSource
import com.lizpostudio.kgoptometrycrm.search.costumer.SearchCostumerFragment
import com.lizpostudio.kgoptometrycrm.search.follow_up.SearchFollowUpFragment
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.SearchRecycleBinFragment
import com.lizpostudio.kgoptometrycrm.search.sales.SearchSalesFragment

class MainActivity : AppCompatActivity() {

    private val userEvent = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val remote = RemoteDataSource.getInstance(this@MainActivity)
            val firebaseAuth = remote.getFirebaseAuth()
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val uid = currentUser.uid
                val dataMap = snapshot.value as? Map<*, *>
                val data = dataMap?.get(uid)
                if (data != null) {
                    val usersEvent = data as? Map<*, *>
                    val type = usersEvent?.get("type")
                    if (type == "logout") {
                        snapshot.ref.child(uid).removeValue()
                        LoginFragment.getDatabaseUserLogin(
                            remote.getFirebaseDatabase(), uid
                        ).removeValue()
                        firebaseAuth.signOut()
                        findNavController(R.id.myNavHostFragment).navigate(R.id.loginFragment)
                    }
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("users_event", error.message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun addUsersEventListener() {
        val remote = RemoteDataSource.getInstance(this);
        remote.getFirebaseDatabase().reference.child("settings")
            .child("users_event")
            .addValueEventListener(userEvent)
    }

    fun removeUsersEventListener() {
        val remote = RemoteDataSource.getInstance(this);
        remote.getFirebaseDatabase().reference.child("settings")
            .child("users_event")
            .removeEventListener(userEvent)
    }

    @SuppressLint("RestrictedApi")
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val parcel = Parcel.obtain()
        parcel.writeValue(outState)
        val bytes = parcel.marshall()
        parcel.recycle()
        val size = bytes.size
        println("onSaveInstanceState: $size of byte")
        if (size >= 100000) {
            outState.clear()
            try {
                val fieldComponents = SavedStateRegistry::class.java.getDeclaredField("components")
                fieldComponents.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val components =
                    fieldComponents.get(savedStateRegistry) as SafeIterableMap<String, SavedStateRegistry.SavedStateProvider>

                @Suppress("INACCESSIBLE_TYPE")
                val it: Iterator<Map.Entry<String, SavedStateRegistry.SavedStateProvider>> =
                    components.iteratorWithAdditions()
                it.forEach {
                    if (it.key.contains("android:support:fragments", true)) {
                        components.remove(it.key)
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        val searchCostumerBy = resources.getStringArray(R.array.search_customer_choices)[0]
        val searchSalesBy = resources.getStringArray(R.array.search_sales_choices)[0]
        val searchFollowUpBy = resources.getStringArray(R.array.search_follow_up_choices)[0]
        val searchRecycleBinBy = resources.getStringArray(R.array.search_recycle_bin_choices)[0]
        val sharedPref = Constants.getSharedPreferences(baseContext)
        val editor = sharedPref.edit()
        searchCustomerReset(editor, searchCostumerBy)
        searchSalesReset(editor, searchSalesBy)
        searchFollowUpReset(editor, searchFollowUpBy)
        searchRecycleBinReset(editor, searchRecycleBinBy)
    }

    private fun searchCustomerReset(editor: SharedPreferences.Editor, searchBy: String) {
        editor.putString(SearchCostumerFragment.KEY_SEARCH_BY, searchBy)
        editor.putString(SearchCostumerFragment.KEY_SEARCH_VALUE, "")
        editor.apply()
    }

    private fun searchSalesReset(editor: SharedPreferences.Editor, searchBy: String) {
        editor.putString(SearchSalesFragment.KEY_SEARCH_BY, searchBy)
        editor.putString(SearchSalesFragment.KEY_SEARCH_VALUE, "")
        editor.apply()
    }

    private fun searchFollowUpReset(editor: SharedPreferences.Editor, searchBy: String) {
        editor.putString(SearchFollowUpFragment.KEY_SEARCH_BY, searchBy)
        editor.putString(SearchFollowUpFragment.KEY_SEARCH_VALUE, "")
        editor.apply()
    }

    private fun searchRecycleBinReset(editor: SharedPreferences.Editor, searchBy: String) {
        editor.putString(SearchRecycleBinFragment.KEY_SEARCH_BY, searchBy)
        editor.putString(SearchRecycleBinFragment.KEY_SEARCH_VALUE, "")
        editor.apply()
    }
}