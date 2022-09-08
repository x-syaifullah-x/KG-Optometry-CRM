package com.lizpostudio.kgoptometrycrm

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcel
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.search.costumer.SearchCostumerFragment
import com.lizpostudio.kgoptometrycrm.search.follow_up.SearchFollowUpFragment
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.SearchRecycleBinFragment
import com.lizpostudio.kgoptometrycrm.search.sales.SearchSalesFragment

class MainActivity : AppCompatActivity() {

    companion object {
        const val KEY_IS_APP_RESTART = "is_restart"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = Constants.getSharedPreferences(baseContext)
        val isRestart = prefs.getBoolean(KEY_IS_APP_RESTART, false)
        if (isRestart) {
            intent.extras?.clear()
            prefs.edit()
                .putBoolean(KEY_IS_APP_RESTART, false)
                .apply()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val parcel = Parcel.obtain()
        parcel.writeValue(outState)
        val bytes = parcel.marshall()
        parcel.recycle()
        val size = bytes.size
        if (size >= 950000) {
            outState.clear()
            Toast.makeText(
                baseContext,
                "Please restart the app to optimize performance",
                Toast.LENGTH_LONG
            ).show()
            Constants.getSharedPreferences(baseContext)
                .edit()
                .putBoolean(KEY_IS_APP_RESTART, true)
                .apply()
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