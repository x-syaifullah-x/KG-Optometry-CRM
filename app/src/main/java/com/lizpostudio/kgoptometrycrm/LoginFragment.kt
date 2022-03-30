package com.lizpostudio.kgoptometrycrm

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.lizpostudio.kgoptometrycrm.databinding.FragmentLoginBinding
import com.lizpostudio.kgoptometrycrm.firebase.KGMessage
import com.lizpostudio.kgoptometrycrm.utils.MessagesListAdapter
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDKey
import com.lizpostudio.kgoptometrycrm.utils.convertLongToDDMMYYHRSMIN
import com.lizpostudio.kgoptometrycrm.utils.generateID

private const val TAG = "LoginFragment"
private const val RECORDS_CHILD = "records"
private const val MESSAGES_CHILD = "messages"
private const val SETTINGS_CHILD = "settings"
private const val USERS_CHILD = "users"
private const val DEVICES_CHILD = "devices"
private const val NEW_CHILD = "new"
private const val TRUSTED_CHILD = "trusted"
private const val ADMIN_KEY = "admin"
private const val USERS_KEY = "user"
private const val RC_SIGN_IN = 123

private lateinit var app: Application

class LoginFragment :Fragment() {

    private var deviceCode = "NO"
    private var trustedDevice = false
    private var listOfTrustedDevices  = MutableLiveData<List<String>>()

    private var userNameAuth = "Guest"
    private var fireApp :FirebaseApp? = null
//        Log.d(TAG, "fireApp: ${fireApp}")
   private var  firebaseDatabase: FirebaseDatabase? = null
    //    Log.d(TAG, "database: ${database}")
    private var messagesReference: DatabaseReference? =null

    private var mMessageListener: ChildEventListener? = null
    private  var msgListener:ValueEventListener? = null

 //   private var mAdapter: FirebaseRecyclerAdapter<Message, MessageViewHolder>? = null
    private var mFirebaseUser: FirebaseUser? = null
    private var messagesLiveList = MutableLiveData<List<KGMessage>>()
    private  var messagesList = mutableListOf<KGMessage>()
//    private var messagesList = mutableListOf<KGMessage>()
    private val recyclerAdapter = MessagesListAdapter()
    private var isAdmin  = MutableLiveData<Boolean>()

    // Recycler View for Messages

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build()
    )

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
    binding.editPassword.visibility = View.GONE
    binding.startButton.visibility = View.VISIBLE
    binding.customersText.visibility = View.VISIBLE
  //  Log.d(TAG, "user = ${mFirebaseUser?.email} === $mFirebaseUser")
}


    // [START auth_fui_result]
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                 mFirebaseUser = FirebaseAuth.getInstance().currentUser
               userNameAuth = mFirebaseUser?.displayName?:mFirebaseUser?.email?:"Guest"
// if user logged in - launch listener
      //          Log.d(TAG, "Initialize Listener at SUCCESS LOGIN ACTIVITY")
                initReadingMessages()

                binding.loginLogoutButton.setImageResource(R.drawable.log_out_36)
                binding.loginLogoutText.text = getString(R.string.logout)
                binding.helloUserText.text = getString(R.string.hello_user, userNameAuth)
   //             Log.d(TAG, "user = ${mFirebaseUser?.email} === $mFirebaseUser")
                // ...
            } else {

                binding.helloUserText.text = getString(R.string.sign_in_failure)
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }


    private fun signIn(email: String, password: String) {
  //      Log.d(TAG, "signIn:$email")

        // Initialize Firebase Auth
        val auth = Firebase.auth
        // [START sign_in_with_email]
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
             //       Log.d(TAG, "signInWithEmail:success")
                    mFirebaseUser = auth.currentUser

                    updateUIOnSuccess()
                    checkTrustedDevice()
                } else {
                    // If sign in fails, display a message to the user.
                    binding.helloUserText.text = getString(R.string.sign_in_failure)
          //          Log.d(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
        // [END sign_in_with_email]
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)

        binding.lifecycleOwner = this

        val navController = this.findNavController()

        app = requireNotNull(this.activity).application

   /*     val dataSource = PatientsDatabase.getInstance(app).patientsDao


        val viewModelFactory = LoginViewModelFactory(dataSource, app)
        val loginViewModel = ViewModelProvider(this, viewModelFactory).get(LoginViewModel::class.java)*/
        // binding.loginViewModel = loginViewModel

        cleanSearch()

        // =============== Check the User and sign in if null =================

        fireApp =  FirebaseApp.initializeApp(requireContext())
//        Log.d(TAG, "fireApp: ${fireApp}")
        firebaseDatabase = fireApp?.let { Firebase.database(it) }
        //    Log.d(TAG, "database: ${database}")
        messagesReference = firebaseDatabase?.reference?.child(MESSAGES_CHILD)
        //     Log.d(TAG, "myRef: ${myRef}")

        // ================  Check if Device is TRUSTED ===================
        checkTrustedDevice()
         mFirebaseUser =FirebaseAuth.getInstance().currentUser

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

                if (msgListener !=null) {
                    messagesReference!!.removeEventListener(msgListener!!)
         //           Log.d(TAG, "REMOVE Listener at Log-out ")
                }
                recyclerAdapter.submitList(emptyList())
                recyclerAdapter.notifyDataSetChanged()

          //      Log.d(TAG, "login -logout clicked start to KICK OUT user = $mFirebaseUser")
                signOut()
                mFirebaseUser = null
            }
        }

  isAdmin.observe(viewLifecycleOwner,{ isAdmin ->
      isAdmin?.let {
          if (it) {
              binding.settingsButton.visibility = View.VISIBLE
          } else {
              binding.settingsButton.visibility = View.GONE
          }
      }

  })

/*        binding.kgOptometryMainText.setOnClickListener {
            throw RuntimeException("Test Crash")
        }*/

        binding.startButton.setOnClickListener {

            if (trustedDevice) {
                    if  (mFirebaseUser !=null) {
                        navController.navigate(LoginFragmentDirections.actionLoginFragmentToDatabaseSearchFragment())
                    } else {
                        showPopup(getString(R.string.please_login))
                    }
            }else {
     //           Log.d(TAG, "Your device = $trustedDevice")
                showPopup("Your device is not recognized!\nPlease, contact your administrator to proceed!")
            }

        }

            val itemDecor = DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        val myDecorLine = ResourcesCompat.getDrawable(resources, R.drawable.recycler_messages_divider, null)

        myDecorLine?.also {
            itemDecor.setDrawable(it) }
        binding.messageRecyclerView.addItemDecoration(itemDecor)
        binding.messageRecyclerView.adapter = recyclerAdapter

        messagesLiveList.observe(viewLifecycleOwner, {mList ->
            mList?.let{

                   recyclerAdapter.submitList(it)
                   recyclerAdapter.notifyDataSetChanged()

  //              Log.d(TAG, "messages LIVE list sisze  =  ${it.size}")

                if (it.size>5) binding.messageRecyclerView.smoothScrollToPosition(it.size-1)
            }
        })

        binding.sendButton.setOnClickListener {
            submitMessage()
            binding.messageEditText.setText("")
        }

        binding.settingsButton.setOnClickListener {
            Toast.makeText(context, " Navigate to Admin Settings Screen", Toast.LENGTH_SHORT).show()
        }

        listOfTrustedDevices.observe(viewLifecycleOwner, {trusted->
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
// ==============  SAVE TRUSTED DEVICE VALUE ===========
                val sharedPref = activity?.getSharedPreferences(
                    "kgoptometry",
                    Context.MODE_PRIVATE
                )
                //   Log.d(TAG, " Saving USER TO STORE : userName = $userName, userType = $userType")
                if (sharedPref != null) {
                    val editor = sharedPref.edit()
                    editor.putBoolean("trusted_device", trustedDevice)
           //         Log.d(TAG, "Saaving new value of trusted device = $trustedDevice")
                    editor.apply()
                }
            }
        })

        return binding.root
    }

    private fun initReadingMessages() {

        if (messagesReference != null) {
      //      Log.d(TAG, "Listener STARTED")
            //     var records= mutableListOf<FBRecords>()
            //    val recordsRef = messagesReference!!.child(RECORDS_CHILD)

            val messagesListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {
                        messagesList.clear()
                        var simpleList = ""
                        snapshot.children.forEach {
                            var newMessage:KGMessage? = null
                            try{
                                 newMessage =
                                    it.getValue(KGMessage::class.java)
                            } catch (e:Exception) {
         //                       Log.d(TAG, "ERROR converting messages: $e")
                            }
                                if (newMessage != null) {
                                    messagesList.add(KGMessage(newMessage.author, newMessage.body, newMessage.time))
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
        msgListener =   messagesReference!!.addValueEventListener(messagesListener)

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
            email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].toUpperCase()
        } else {
            email
        }
    }

    private fun signOut() {

        binding.editPassword.setText("")
        val auth = Firebase.auth
        auth.signOut()

        val userLogged = "Guest! Please, log in to continue"
        binding.editLoginName.visibility = View.VISIBLE
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
        val usersFBReference = firebaseDatabase?.reference?.child(SETTINGS_CHILD)?.child(USERS_CHILD)
        if (usersFBReference != null) {
                val usersListener = object : ValueEventListener {
                @SuppressLint("DefaultLocale")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        snapshot.children.forEach {
       //                     Log.d(TAG, " Snapshot child  = ${it.value}, - ${it.key}")
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
                override fun onCancelled(error: DatabaseError) {  }
            }
            usersFBReference.addListenerForSingleValueEvent(usersListener)
        }
    }

    private fun checkTrustedDevice() {
        val sharedPref =  activity?.getSharedPreferences("kgoptometry",
            Context.MODE_PRIVATE)
        deviceCode = sharedPref?.getString("device_code", "NO")?:"NO"
        trustedDevice = sharedPref?.getBoolean("trusted_device", false)?:false

           if (deviceCode == "NO" && mFirebaseUser !=null) {
            // record device code to Firebase
            val timeStamp = System.currentTimeMillis()
            deviceCode = generateID()

            val devicesFBReference = firebaseDatabase!!.reference.child(SETTINGS_CHILD)
                .child(DEVICES_CHILD).child(NEW_CHILD)

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
        val trustedReference = firebaseDatabase?.reference?.child(SETTINGS_CHILD)?.child(DEVICES_CHILD)?.child(TRUSTED_CHILD)
        if (trustedReference != null) {
            val trustedListener = object : ValueEventListener {
                @SuppressLint("DefaultLocale")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
     //                   Log.d(TAG, "Snapshot of trusted devices from firebase = ${snapshot.children.map { it.value.toString()}}")
                        listOfTrustedDevices.value  = snapshot.children.map { it.value.toString() }
                    }
                }
                override fun onCancelled(error: DatabaseError) {  }
            }
            trustedReference.addListenerForSingleValueEvent(trustedListener)
        }
    }


    private fun saveUserToLocal(userName:String, userType:String) {

  //      val app = requireActivity().application
        val sharedPref = activity?.getSharedPreferences(
            "kgoptometry",
            Context.MODE_PRIVATE
        )
     //   Log.d(TAG, " Saving USER TO STORE : userName = $userName, userType = $userType")
        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putString("user_name", userName)
            editor.putString("admin", userType)
            editor.apply()
        }

    }

    private fun cleanAdminRight() {

//        val app = requireNotNull(this.activity).application
        val sharedPref = activity?.getSharedPreferences(
            "kgoptometry",
            Context.MODE_PRIVATE
        )
   //     Log.d(TAG, "Cleaning admin key ---------")
        if (sharedPref != null) {
            val editor = sharedPref.edit()
            editor.putString("admin", USERS_KEY)
            editor.apply()
        }

    }

private fun cleanSearch() {

    val sharedPref = activity?.getSharedPreferences(
        "kgoptometry",
        Context.MODE_PRIVATE
    )

    if (sharedPref != null) {
        val editor = sharedPref.edit()
        editor.putString("searchBy", "NAME")
        editor.putString("searchValue", "")
        editor.apply()
    }

}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (msgListener !=null) {
            messagesReference!!.removeEventListener(msgListener!!)
      //      Log.d(TAG, "REMOVE Listener")
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun showPopup(message: String) {

 //       val app = requireNotNull(this.activity).application
        // inflate the layout of the popup window
        val layoutInflater: LayoutInflater = LayoutInflater.from(app.applicationContext)
        val popupView: View = layoutInflater.inflate(R.layout.popup_action_info, binding.mainLayout, false)

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

}
