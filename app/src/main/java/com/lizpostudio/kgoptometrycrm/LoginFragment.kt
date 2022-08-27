package com.lizpostudio.kgoptometrycrm

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.repository.PatientRepository
import com.lizpostudio.kgoptometrycrm.data.source.remote.MyFirebase
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.KGMessage
import com.lizpostudio.kgoptometrycrm.databinding.FragmentLoginBinding
import com.lizpostudio.kgoptometrycrm.search.SearchCostumerFragment
import com.lizpostudio.kgoptometrycrm.search.SearchSalesFragment
import com.lizpostudio.kgoptometrycrm.utils.MessagesListAdapter
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDKey
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDMMYYHRSMIN
import com.lizpostudio.kgoptometrycrm.utils.generateID
import id.xxx.module.view.binding.ktx.viewBinding

class LoginFragment : Fragment() {

    companion object {
        private const val MESSAGES_CHILD = "messages"
        private const val SETTINGS_CHILD = "settings"
        private const val USERS_CHILD = "users"
        private const val DEVICES_CHILD = "devices"
        private const val NEW_CHILD = "new"
        private const val TRUSTED_CHILD = "trusted"
        private const val ADMIN_KEY = "admin"
        private const val USERS_KEY = "user"

        fun defaultFirebaseConfig(edit: SharedPreferences.Editor): Boolean {
            return edit
                .putString(
                    MyFirebase.KEY_FIREBASE_URL,
                    "https://kgoptometrycrm.firebaseio.com"
                )
                .putString(
                    MyFirebase.KEY_PROJECT_NUMBER,
                    "630259719920"
                )
                .putString(
                    MyFirebase.KEY_API_KEY,
                    "AIzaSyBI6-DpeH-ki0jLsQ64E3XVrw00wxG-qQI"
                )
                .putString(
                    MyFirebase.KEY_APPLICATION_ID,
                    "1:630259719920:android:02d8acd58e5fc3ad0d2c35"
                )
                .putString(
                    MyFirebase.KEY_STORAGE_BUCKET,
                    "kgoptometrycrm.appspot.com"
                )
                .putString(
                    MyFirebase.KEY_PROJECT_ID,
                    "kgoptometrycrm"
                )
                .commit()
        }
    }

    private var deviceCode = "NO"
    private var trustedDevice = false
    private var listOfTrustedDevices = MutableLiveData<List<String>>()

    private var userNameAuth = "Guest"
    private var fireApp: FirebaseApp? = null

    private var firebaseDatabase: FirebaseDatabase? = null

    private var messagesReference: DatabaseReference? = null

    //    private var mMessageListener: ChildEventListener? = null
    private var msgListener: ValueEventListener? = null

    //   private var mAdapter: FirebaseRecyclerAdapter<Message, MessageViewHolder>? = null
    private var mFirebaseUser: FirebaseUser? = null
    private var messagesLiveList = MutableLiveData<List<KGMessage>>()
    private var messagesList = mutableListOf<KGMessage>()

    //    private var messagesList = mutableListOf<KGMessage>()
    private val recyclerAdapter = MessagesListAdapter()
    private var isAdmin = MutableLiveData<Boolean>()

    // Recycler View for Messages

    private val binding by viewBinding<FragmentLoginBinding>()

//    private val providers = arrayListOf(
//        AuthUI.IdpConfig.EmailBuilder().build()
//    )

    private fun updateUIOnSuccess() {

        userNameAuth = getUsernameFromEmail(mFirebaseUser!!.email)
// if user logged in - launch listener
        // Log.d(TAG, "Initialize Listener at SUCCESS LOGIN ACTIVITY")
        initReadingMessages()
        //  check if user is admin and save that to shared prefs.
        //  clean-up admin rights in log-out

        readSettingsFromFBAndSaveUserNameAndTypeToStore()
        binding.loginLogoutButton.setImageResource(R.drawable.log_out_36)
        binding.loginLogoutText.text = getString(R.string.logout)
        binding.helloUserText.text = getString(R.string.hello_user, userNameAuth)

        binding.editLoginName.visibility = View.GONE
        binding.containerChangeConfiguration.visibility = View.GONE
        binding.editPassword.visibility = View.GONE
        binding.startButton.visibility = View.VISIBLE
        binding.customersText.visibility = View.VISIBLE
        //  Log.d(TAG, "user = ${mFirebaseUser?.email} === $mFirebaseUser")
    }

    // [START auth_fui_result]
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == RC_SIGN_IN) {
//            val response = IdpResponse.fromResultIntent(data)
//
//            if (resultCode == Activity.RESULT_OK) {
//                // Successfully signed in
//                mFirebaseUser = FirebaseAuth.getInstance().currentUser
//                userNameAuth = mFirebaseUser?.displayName ?: mFirebaseUser?.email ?: "Guest"
//// if user logged in - launch listener
//                //          Log.d(TAG, "Initialize Listener at SUCCESS LOGIN ACTIVITY")
//                initReadingMessages()
//
//                binding.loginLogoutButton.setImageResource(R.drawable.log_out_36)
//                binding.loginLogoutText.text = getString(R.string.logout)
//                binding.helloUserText.text = getString(R.string.hello_user, userNameAuth)
//                //             Log.d(TAG, "user = ${mFirebaseUser?.email} === $mFirebaseUser")
//                // ...
//            } else {
//
//                binding.helloUserText.text = getString(R.string.sign_in_failure)
//                // Sign in failed. If response is null the user canceled the
//                // sign-in flow using the back button. Otherwise check
//                // response.getError().getErrorCode() and handle the error.
//                // ...
//            }
//        }
//    }

    private fun signIn(email: String, password: String) {
        val firebaseApp = MyFirebase.getInstance(requireContext())
        val auth = FirebaseAuth.getInstance(firebaseApp)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    mFirebaseUser = auth.currentUser
                    updateUIOnSuccess()
                    checkTrustedDevice()
                } else {
                    binding.helloUserText.text = getString(R.string.sign_in_failure)
                    Toast.makeText(
                        requireContext(), "Authentication failed.", Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding.tvChangeFirebaseConfig.setOnClickListener {
            findNavController().navigate(R.id.settingLoginFragment)
        }
        cleanSearch()

        // =============== Check the User and sign in if null =================

        fireApp = MyFirebase.getInstance(requireContext())
        firebaseDatabase = fireApp?.let { Firebase.database(it) }
        messagesReference = firebaseDatabase?.reference?.child(MESSAGES_CHILD)

        // ================  Check if Device is TRUSTED ===================
        checkTrustedDevice()
        mFirebaseUser = FirebaseAuth.getInstance(fireApp!!).currentUser

        if (mFirebaseUser != null) {
            updateUIOnSuccess()

        } else {
            // No user is signed in
            val userLogged = "Guest! Please, log in to continue"

            binding.loginLogoutButton.setImageResource(R.drawable.login_36)
            binding.loginLogoutText.text = getString(R.string.login)
            binding.helloUserText.text = getString(R.string.hello_user, userLogged)
        }

        binding.loginLogoutButton.setOnClickListener {
            //         Log.d(TAG, "login - logout clicked user = $mFirebaseUser")
            if (mFirebaseUser == null) { // log on user
                val userEmail = binding.editLoginName.text.toString() + "@gmail.com"
                val userPass = binding.editPassword.text.toString()

                // WORKAROUND #1
                if (userEmail.isNotBlank() && userPass.isNotBlank()) signIn(userEmail, userPass)

            } else { // log out user
                // remove listeners and clean-up messages

                if (msgListener != null) {
                    messagesReference!!.removeEventListener(msgListener!!)
                }
                recyclerAdapter.submitList(emptyList())
                recyclerAdapter.notifyDataSetChanged()

                signOut()
                mFirebaseUser = null
            }
        }

        isAdmin.observe(viewLifecycleOwner) { isAdmin ->
            isAdmin?.let {
                if (it) {
                    binding.settingsButton.visibility = View.VISIBLE
                } else {
                    binding.settingsButton.visibility = View.GONE
                }
            }

        }

        binding.startButton.setOnClickListener {
            if (trustedDevice) {
                if (mFirebaseUser != null) {
                    findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToDatabaseSearchFragment())
                } else {
                    showPopup(getString(R.string.please_login))
                }
            } else {
                showPopup("Your device is not recognized!\nPlease, contact your administrator to proceed!")
            }
        }

        val itemDecor = DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        val myDecorLine =
            ResourcesCompat.getDrawable(resources, R.drawable.recycler_messages_divider, null)

        myDecorLine?.also {
            itemDecor.setDrawable(it)
        }
        binding.messageRecyclerView.addItemDecoration(itemDecor)
        binding.messageRecyclerView.adapter = recyclerAdapter

        messagesLiveList.observe(viewLifecycleOwner) { mList ->
            mList?.let {

                recyclerAdapter.submitList(it)
                recyclerAdapter.notifyDataSetChanged()

                if (it.size > 5)
                    binding.messageRecyclerView.smoothScrollToPosition(it.size - 1)
            }
        }

        binding.sendButton.setOnClickListener {
            submitMessage()
            binding.messageEditText.setText("")
        }

        binding.settingsButton.setOnClickListener {
            Toast.makeText(context, " Navigate to Admin Settings Screen", Toast.LENGTH_SHORT).show()
        }

        listOfTrustedDevices.observe(viewLifecycleOwner) { trusted ->
            trusted?.let { trustedList ->
                var foundInTrusted = false
                if (trustedList.isNotEmpty()) {
                    for (item in trustedList) {
                        if (item == deviceCode) {
                            foundInTrusted = true
                            break
                        }
                    }
                }
                trustedDevice = foundInTrusted
                val editor = Constants.getSharedPreferences(requireContext()).edit()
                editor.putBoolean("trusted_device", trustedDevice)
                editor.apply()
            }
        }

        return binding.root
    }

    private fun initReadingMessages() {
        if (messagesReference != null) {
            val messagesListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {
                        messagesList.clear()
                        snapshot.children.forEach {
                            var newMessage: KGMessage? = null
                            try {
                                newMessage = it.getValue(KGMessage::class.java)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (newMessage != null) {
                                messagesList.add(
                                    KGMessage(newMessage.author, newMessage.body, newMessage.time)
                                )
                            }
                        }
                        messagesLiveList.value = messagesList
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        context,
                        "Error reading: ${error.toException()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
            msgListener = messagesReference!!.addValueEventListener(messagesListener)

        }
    }

    private fun submitMessage() {
        val body = binding.messageEditText.text.toString()
        val time = convertLongToDDMMYYHRSMIN(System.currentTimeMillis())
        val message = KGMessage(userNameAuth, body, time)

        val key = messagesReference!!.push().key
        messagesReference!!.child("$key").setValue(message)
    }

    private fun getUsernameFromEmail(email: String?): String {

        return if (email!!.contains("@")) {
            email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].uppercase()
        } else {
            email
        }
    }

    private fun signOut() {

        binding.editPassword.setText("")
        FirebaseAuth.getInstance(
            MyFirebase.getInstance(requireContext())
        ).signOut()

        val userLogged = "Guest! Please, log in to continue"
        binding.editLoginName.visibility = View.VISIBLE
        binding.containerChangeConfiguration.visibility = View.VISIBLE
        binding.editPassword.visibility = View.VISIBLE
        binding.startButton.visibility = View.GONE
        binding.customersText.visibility = View.GONE
        binding.settingsButton.visibility = View.GONE

        cleanAdminRight()

        binding.loginLogoutButton.setImageResource(R.drawable.login_36)
        binding.loginLogoutText.text = getString(R.string.login)
        binding.helloUserText.text = getString(R.string.hello_user, userLogged)

    }


    private fun readSettingsFromFBAndSaveUserNameAndTypeToStore() {
        //   Log.d(TAG, " reading settings from FB")
        //      saveUserToLocal(userNameAuth, USERS_KEY)
        val usersFBReference =
            firebaseDatabase?.reference?.child(SETTINGS_CHILD)?.child(USERS_CHILD)
        if (usersFBReference != null) {
            val usersListener = object : ValueEventListener {
                @SuppressLint("DefaultLocale")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        snapshot.children.forEach {
                            if (it.value.toString().equals(userNameAuth, ignoreCase = true)) {
                                if (it.key.toString() == ADMIN_KEY) {
                                    isAdmin.value = true
                                    saveUserToLocal(userNameAuth, ADMIN_KEY)
                                } else {
                                    isAdmin.value = false
                                    saveUserToLocal(userNameAuth, USERS_KEY)
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            usersFBReference.addListenerForSingleValueEvent(usersListener)
        }
    }

    private fun checkTrustedDevice() {
        val sharedPref = activity
            ?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        deviceCode = sharedPref?.getString("device_code", "NO") ?: "NO"
        trustedDevice = sharedPref?.getBoolean("trusted_device", false) ?: false

        if (deviceCode == "NO" && mFirebaseUser != null) {
            // record device code to Firebase
            val timeStamp = System.currentTimeMillis()
            deviceCode = generateID(PatientRepository.getInstance(requireContext()))

            val devicesFBReference = firebaseDatabase!!.reference
                .child(SETTINGS_CHILD)
                .child(DEVICES_CHILD)
                .child(NEW_CHILD)

            val newDevice = "${convertLongToDDKey(timeStamp)}_M_${android.os.Build.MODEL}"

            devicesFBReference.child(newDevice).setValue(deviceCode)

            if (sharedPref != null) {
                val editor = sharedPref.edit()
                editor.putString("device_code", deviceCode)
                editor.apply()
                //             Log.d(TAG, "NEW GENERATED CODE  = $deviceCode, trusted = $trustedDevice")
            }
        }

        // set FireBase Listener fo new code
        checkFireBaseForTrusted()
    }

    private fun checkFireBaseForTrusted() {
        val trustedReference = firebaseDatabase?.reference
            ?.child(SETTINGS_CHILD)
            ?.child(DEVICES_CHILD)
            ?.child(TRUSTED_CHILD)
        if (trustedReference != null) {
            val trustedListener = object : ValueEventListener {
                @SuppressLint("DefaultLocale")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        listOfTrustedDevices.value = snapshot.children.map { it.value.toString() }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            trustedReference.addListenerForSingleValueEvent(trustedListener)
        }
    }


    private fun saveUserToLocal(userName: String, userType: String) {
        val sharedPref = activity
            ?.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putString("user_name", userName)
            editor.putString("admin", userType)
            editor.apply()
        }
    }

    private fun cleanAdminRight() {
        val editor = Constants.getSharedPreferences(requireContext()).edit()
        editor.putString("admin", USERS_KEY)
        editor.apply()
    }

    private fun cleanSearch() {
        val editor = Constants.getSharedPreferences(requireContext()).edit()
        val searchCostumerBy = resources.getStringArray(R.array.search_customer_choices)[0]
        val searchSalesBy = resources.getStringArray(R.array.search_sales_choices)[0]
        editor.putString(SearchCostumerFragment.KEY_SEARCH_BY, searchCostumerBy)
        editor.putString(SearchCostumerFragment.KEY_SEARCH_VALUE, "")
        editor.putString(SearchSalesFragment.KEY_SEARCH_BY, searchSalesBy)
        editor.putString(SearchSalesFragment.KEY_SEARCH_VALUE, "")
        editor.apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (msgListener != null) {
            messagesReference!!.removeEventListener(msgListener!!)
            //      Log.d(TAG, "REMOVE Listener")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showPopup(message: String) {

        //       val app = requireNotNull(this.activity).application
        // inflate the layout of the popup window
        val layoutInflater: LayoutInflater = LayoutInflater.from(requireContext())
        val popupView: View =
            layoutInflater.inflate(R.layout.popup_action_info, binding.mainLayout, false)

        val textItem = popupView.findViewById<TextView>(R.id.popup_text)
        textItem.text = message
        // create the popup window
        val width: Int = LinearLayout.LayoutParams.WRAP_CONTENT
        val height: Int = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it
        val popupWindow = PopupWindow(popupView, width, height, focusable)

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

        // dismiss the popup window when touched
        popupView.setOnTouchListener { _, _ ->
            popupWindow.dismiss()
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = Constants.getSharedPreferences(requireContext())

        val keyFirstInstall = "first_install"

        if (pref.getBoolean(keyFirstInstall, true)) {
            val edit = pref.edit()
            edit.putBoolean(keyFirstInstall, false)
            defaultFirebaseConfig(edit)
        }
    }
}
