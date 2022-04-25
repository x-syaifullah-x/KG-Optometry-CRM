package com.lizpostudio.kgoptometrycrm

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.search.SearchCostumerFragment
import com.lizpostudio.kgoptometrycrm.search.SearchSalesFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onDestroy() {
        super.onDestroy()
        val searchCostumerBy = resources.getStringArray(R.array.search_customer_choices)[0]
        val searchSalesBy = resources.getStringArray(R.array.search_sales_choices)[0]
        val sharedPref = Constants.getSharedPreferences(baseContext)
        val editor = sharedPref.edit()
        searchCustomerReset(editor, searchCostumerBy)
        searchSalesReset(editor, searchSalesBy)
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
}