package mega.privacy.android.app.presentation.contact.invite.contact

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.ContactsDividerDecoration
import mega.privacy.android.app.components.scrollBar.FastScrollerScrollListener
import mega.privacy.android.app.databinding.ActivityInviteContactBinding
import mega.privacy.android.app.databinding.SelectedContactItemBinding
import mega.privacy.android.app.main.InvitationContactInfo
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_MANUAL_INPUT_EMAIL
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_MANUAL_INPUT_PHONE
import mega.privacy.android.app.main.InvitationContactInfo.Companion.createManualInput
import mega.privacy.android.app.main.adapters.InvitationContactsAdapter
import mega.privacy.android.app.main.model.InvitationStatusUiState
import mega.privacy.android.app.presentation.contact.invite.contact.component.ContactInfoListDialog
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.extensions.parcelable
import mega.privacy.android.app.presentation.qrcode.QRCodeComposeActivity
import mega.privacy.android.app.presentation.view.open.camera.confirmation.OpenCameraConfirmationDialogRoute
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.REQUEST_READ_CONTACTS
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.contacts.ContactsFilter.isEmailInContacts
import mega.privacy.android.app.utils.contacts.ContactsFilter.isEmailInPending
import mega.privacy.android.app.utils.contacts.ContactsFilter.isMySelf
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * Invite contact activity.
 */
@AndroidEntryPoint
class InviteContactActivity : PasscodeActivity(), InvitationContactsAdapter.OnItemClickListener {

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: InviteContactViewModel by viewModels()

    private var displayMetrics: DisplayMetrics? = null
    private var actionBar: ActionBar? = null
    private var invitationContactsAdapter: InvitationContactsAdapter? = null

    private var contactsEmailsSelected: MutableList<String> = mutableListOf()
    private var contactsPhoneSelected: MutableList<String> = mutableListOf()

    private var isPermissionGranted = false
    private var isGetContactCompleted = false

    private var shouldShowContactListWithContactInfo by mutableStateOf<InvitationContactInfo?>(null)

    private lateinit var binding: ActivityInviteContactBinding

    //work around for android bug - https://issuetracker.google.com/issues/37007605#c10
    private class LinearLayoutManagerWrapper(context: Context) : LinearLayoutManager(context) {
        override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
            try {
                super.onLayoutChildren(recycler, state)
            } catch (e: IndexOutOfBoundsException) {
                Timber.d("IndexOutOfBoundsException in RecyclerView happens")
            }
        }
    }

    /**
     * Called when the activity is starting.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        binding = ActivityInviteContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.composeView.setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                if (uiState.showOpenCameraConfirmation) {
                    OpenCameraConfirmationDialogRoute(
                        onConfirm = {
                            initScanQR()
                            viewModel.onQRScannerInitialized()
                            viewModel.onOpenCameraConfirmationShown()
                        },
                        onDismiss = viewModel::onOpenCameraConfirmationShown
                    )
                }

                shouldShowContactListWithContactInfo?.let {
                    ContactInfoListDialog(
                        contactInfo = it,
                        currentSelectedContactInfo = uiState.selectedContactInformation,
                        onConfirm = { selectedContactInfo ->
                            shouldShowContactListWithContactInfo = null
                            viewModel.updateSelectedContactInfo(selectedContactInfo)
                            controlHighlighted(it.id)
                            refreshComponents(true)
                        },
                        onCancel = {
                            shouldShowContactListWithContactInfo = null
                        }
                    )
                }
            }
        }

        setSupportActionBar(binding.inviteContactToolbar)

        actionBar = supportActionBar
        actionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.title = getString(R.string.invite_contacts)
            setTitleAB()
        }

        setupListeners()

        binding.inviteContactList.apply {
            setClipToPadding(false)
            setItemAnimator(DefaultItemAnimator())
            setLayoutManager(LinearLayoutManagerWrapper(this@InviteContactActivity))
            addItemDecoration(ContactsDividerDecoration(this@InviteContactActivity))

            invitationContactsAdapter = InvitationContactsAdapter(
                this@InviteContactActivity,
                viewModel.filterUiState.value.filteredContacts,
                this@InviteContactActivity,
                megaApi
            )
            setAdapter(invitationContactsAdapter)
        }

        binding.fastScroller.setRecyclerView(binding.inviteContactList)

        binding.inviteContactListEmptyText.setText(R.string.contacts_list_empty_text_loading_share)

        refreshInviteContactButton()

        //orientation changes
        if (savedInstanceState != null) {
            isGetContactCompleted =
                savedInstanceState.getBoolean(KEY_IS_GET_CONTACT_COMPLETED, false)
        }

        if (isGetContactCompleted) {
            savedInstanceState?.let {
                isPermissionGranted = it.getBoolean(KEY_IS_PERMISSION_GRANTED, false)
                shouldShowContactListWithContactInfo = it.parcelable(CURRENT_SELECTED_CONTACT)
            }
            refreshAddedContactsView(true)
            setRecyclersVisibility()
            setTitleAB()
            if (viewModel.allContacts.isNotEmpty()) {
                setEmptyStateVisibility(false)
            } else if (isPermissionGranted) {
                setEmptyStateVisibility(true)
                showEmptyTextView()
            } else {
                setEmptyStateVisibility(true)
                binding.inviteContactListEmptyText.setText(R.string.no_contacts_permissions)
                binding.inviteContactListEmptyImage.visibility = View.VISIBLE
                binding.noPermissionHeader.visibility = View.VISIBLE
            }
        } else {
            queryIfHasReadContactsPermissions()
        }

        collectFlows()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.fabButtonNext.setOnClickListener {
            enableFabButton(false)
            Timber.d("invite Contacts")
            inviteContacts(viewModel.uiState.value.selectedContactInformation)
            Util.hideKeyboard(this, 0)
        }

        binding.typeMailEditText.apply {
            doBeforeTextChanged { _, _, _, _ -> refreshKeyboard() }

            doOnTextChanged { text, start, before, count ->
                Timber.d("onTextChanged: s is $text start: $start before: $before count: $count")
                if (!text.isNullOrBlank()) {
                    val last = text[text.length - 1]
                    if (last == ' ') {
                        val processedString = text.toString().trim()
                        if (isValidEmail(processedString)) {
                            addContactInfo(processedString, TYPE_MANUAL_INPUT_EMAIL)
                            binding.typeMailEditText.text.clear()
                        } else if (isValidPhone(processedString)) {
                            addContactInfo(processedString, TYPE_MANUAL_INPUT_PHONE)
                            binding.typeMailEditText.text.clear()
                        }

                        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            Util.hideKeyboard(this@InviteContactActivity, 0)
                        }
                    } else {
                        Timber.d("Last character is: $last")
                    }
                }

                refreshInviteContactButton()
                viewModel.onSearchQueryChange(binding.typeMailEditText.text.toString())
                refreshKeyboard()
            }

            doAfterTextChanged { refreshKeyboard() }

            setOnEditorActionListener { textView, actionId, event ->
                refreshKeyboard()
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val processedStrong = textView.text.toString().trim()
                    if (processedStrong.isNotBlank()) {
                        binding.typeMailEditText.text.clear()
                        val isEmailValid = isValidEmail(processedStrong)
                        val isPhoneValid = isValidPhone(processedStrong)
                        if (isEmailValid) {
                            val result = checkInputEmail(processedStrong)
                            if (result != null) {
                                Util.hideKeyboard(this@InviteContactActivity, 0)
                                showSnackBar(result)
                                return@setOnEditorActionListener true
                            }
                            addContactInfo(processedStrong, TYPE_MANUAL_INPUT_EMAIL)
                        } else if (isPhoneValid) {
                            addContactInfo(processedStrong, TYPE_MANUAL_INPUT_PHONE)
                        }
                        if (isEmailValid || isPhoneValid) {
                            Util.hideKeyboard(this@InviteContactActivity, 0)
                        } else {
                            Toast.makeText(
                                this@InviteContactActivity,
                                R.string.invalid_input,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnEditorActionListener true
                        }
                        if (!Util.isScreenInPortrait(this@InviteContactActivity)) {
                            Util.hideKeyboard(this@InviteContactActivity, 0)
                        }
                        viewModel.filterContacts(binding.typeMailEditText.text.toString())
                    }
                    Util.hideKeyboard(this@InviteContactActivity, 0)
                    refreshInviteContactButton()
                    return@setOnEditorActionListener true
                }
                if ((event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
                    if (viewModel.uiState.value.selectedContactInformation.isEmpty()) {
                        Util.hideKeyboard(this@InviteContactActivity, 0)
                    } else {
                        inviteContacts(viewModel.uiState.value.selectedContactInformation)
                    }
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            setImeOptions(EditorInfo.IME_ACTION_DONE)
        }

        binding.layoutScanQr.setOnClickListener {
            Timber.d("Scan QR code pressed")
            viewModel.validateCameraAvailability()
        }

        binding.inviteContactList.setOnTouchListener { _: View?, _: MotionEvent ->
            Util.hideKeyboard(this@InviteContactActivity, 0)
            false
        }

        binding.fastScroller.setUpScrollListener(object : FastScrollerScrollListener {
            override fun onScrolled() {
                binding.fabButtonNext.hide()
            }

            override fun onScrolledToTop() {
                binding.fabButtonNext.show()
            }
        })
    }

    private fun collectFlows() {
        collectFlow(
            viewModel
                .uiState
                .map { it.onContactsInitialized }
                .distinctUntilChanged()
        ) { isContactsInitialized ->
            if (isContactsInitialized) {
                onGetContactCompleted()
                viewModel.filterContacts(binding.typeMailEditText.text.toString())
                viewModel.resetOnContactsInitializedState()
            }
        }

        collectFlow(viewModel.filterUiState) {
            refreshList()
            visibilityFastScroller()
        }

        collectFlow(
            viewModel
                .uiState
                .map { it.invitationStatus }
                .filter { it.emails.isNotEmpty() }
                .distinctUntilChanged()
        ) {
            showInvitationsResult(it)
        }

        collectFlow(
            viewModel
                .uiState
                .map { it.shouldInitializeQR }
                .filter { it }
                .distinctUntilChanged()
        ) {
            initScanQR()
            viewModel.onQRScannerInitialized()
        }
    }

    private fun showInvitationsResult(status: InvitationStatusUiState) {
        val result = Intent()
        // If the invitation is successful and the total number of invitations is one.
        if (status.emails.size == 1 && status.totalInvitationSent == 1 && !viewModel.isFromAchievement) {
            showSnackBar(getString(R.string.context_contact_request_sent, status.emails[0]))
        } else {
            val totalFailedInvitations = status.emails.size - status.totalInvitationSent
            // There are failed invitations.
            if (totalFailedInvitations > 0 && !viewModel.isFromAchievement) {
                val requestsSent = resources.getQuantityString(
                    R.plurals.contact_snackbar_invite_contact_requests_sent,
                    status.totalInvitationSent,
                    status.totalInvitationSent
                )
                val requestsNotSent = resources.getQuantityString(
                    R.plurals.contact_snackbar_invite_contact_requests_not_sent,
                    totalFailedInvitations,
                    totalFailedInvitations
                )
                showSnackBar(requestsSent + requestsNotSent)
            } else {
                // All invitations are successfully sent.
                if (!viewModel.isFromAchievement) {
                    showSnackBar(
                        resources.getQuantityString(
                            R.plurals.number_correctly_invite_contact_request,
                            status.emails.size,
                            status.emails.size
                        )
                    )
                } else {
                    // Sent back the result to the InviteFriendsRoute.
                    result.putExtra(KEY_SENT_NUMBER, status.totalInvitationSent)
                }
            }
        }

        Util.hideKeyboard(this@InviteContactActivity, 0)
        Handler(Looper.getMainLooper()).postDelayed({
            if (contactsPhoneSelected.isNotEmpty()) {
                invitePhoneContacts(ArrayList(contactsPhoneSelected))
            }
            setResult(RESULT_OK, result)
            finish()
        }, 2000)
    }

    /**
     * Initialize the contents of the Activity's standard options menu. You should place your menu items in to menu.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")
        menuInflater.inflate(R.menu.activity_invite_contact, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_my_qr -> initMyQr()
            R.id.action_more -> {
                Timber.i("more button clicked - share invitation through other app")
                val message = resources.getString(
                    R.string.invite_contacts_to_start_chat_text_message,
                    viewModel.uiState.value.contactLink
                )
                val sendIntent = Intent().apply {
                    setAction(Intent.ACTION_SEND)
                    putExtra(Intent.EXTRA_TEXT, message)
                    setType(Constants.TYPE_TEXT_PLAIN)
                }
                startActivity(
                    Intent.createChooser(
                        sendIntent,
                        getString(R.string.invite_contact_chooser_title)
                    )
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Called to retrieve per-instance state from an activity before being killed
     */
    override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.putParcelable(CURRENT_SELECTED_CONTACT, shouldShowContactListWithContactInfo)
        outState.putBoolean(KEY_IS_PERMISSION_GRANTED, isPermissionGranted)
        outState.putBoolean(KEY_IS_GET_CONTACT_COMPLETED, isGetContactCompleted)
    }

    override fun onBackPressed() {
        Timber.d("onBackPressed")
        val psaWebBrowser = psaWebBrowser
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return
        finish()
    }

    private fun setTitleAB() {
        Timber.d("setTitleAB")
        actionBar?.subtitle = if (viewModel.uiState.value.selectedContactInformation.isNotEmpty()) {
            resources.getQuantityString(
                R.plurals.general_selection_num_contacts,
                viewModel.uiState.value.selectedContactInformation.size,
                viewModel.uiState.value.selectedContactInformation.size
            )
        } else {
            null
        }
    }

    private fun setRecyclersVisibility() {
        Timber.d("setRecyclersVisibility ${viewModel.allContacts.size}")
        binding.containerListContacts.visibility = if (viewModel.allContacts.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun queryIfHasReadContactsPermissions() {
        Timber.d("queryIfHasReadContactsPermissions")
        if (hasPermissions(this, Manifest.permission.READ_CONTACTS)) {
            isPermissionGranted = true
            prepareToGetContacts()
        } else {
            requestPermission(
                this,
                REQUEST_READ_CONTACTS,
                Manifest.permission.READ_CONTACTS
            )
        }
    }

    private fun prepareToGetContacts() {
        Timber.d("prepareToGetContacts")
        setEmptyStateVisibility(true)
        binding.inviteContactProgressBar.visibility = View.VISIBLE
        viewModel.initializeContacts()
    }

    private fun visibilityFastScroller() {
        Timber.d("visibilityFastScroller")
        binding.fastScroller.setRecyclerView(binding.inviteContactList)
        binding.fastScroller.visibility =
            if (viewModel.allContacts.size < MIN_LIST_SIZE_FOR_FAST_SCROLLER) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun setPhoneAdapterContacts(contacts: List<InvitationContactInfo>) {
        Timber.d("setPhoneAdapterContacts")
        invitationContactsAdapter?.let {
            binding.inviteContactList.post {
                it.setContactData(contacts)
                it.notifyDataSetChanged()
            }
        }
    }

    private fun showEmptyTextView() {
        Timber.d("showEmptyTextView")
        var textToShow = getString(R.string.context_empty_contacts)
        try {
            textToShow = textToShow.replace(
                "[A]", "<font color=\'"
                        + getColorHexString(this, R.color.grey_900_grey_100)
                        + "\'>"
            ).replace("[/A]", "</font>").replace(
                "[B]", "<font color=\'"
                        + getColorHexString(this, R.color.grey_300_grey_600)
                        + "\'>"
            ).replace("[/B]", "</font>")
        } catch (e: Exception) {
            Timber.e(e)
        }

        val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
        binding.inviteContactListEmptyText.text = result
    }

    private fun setEmptyStateVisibility(visible: Boolean) {
        Timber.d("setEmptyStateVisibility")
        if (visible) {
            binding.inviteContactListEmptyImage.visibility = View.VISIBLE
            binding.inviteContactListEmptyText.visibility = View.VISIBLE
            binding.inviteContactListEmptySubtext.visibility = View.GONE
        } else {
            binding.inviteContactListEmptyImage.visibility = View.GONE
            binding.inviteContactListEmptyText.visibility = View.GONE
            binding.inviteContactListEmptySubtext.visibility = View.GONE
        }
    }

    /**
     * Open the [QRCodeComposeActivity]
     */
    fun initScanQR() {
        Timber.d("initScanQR")
        Intent(this, QRCodeComposeActivity::class.java).apply {
            putExtra(Constants.OPEN_SCAN_QR, true)
            startQRActivity(this)
        }
    }

    private fun initMyQr() {
        startQRActivity(Intent(this, QRCodeComposeActivity::class.java))
    }

    private fun startQRActivity(intent: Intent) {
        startActivityForResult(intent, SCAN_QR_FOR_INVITE_CONTACTS)
    }

    private fun refreshKeyboard() {
        Timber.d("refreshKeyboard")
        val imeOptions = binding.typeMailEditText.imeOptions
        binding.typeMailEditText.setImeOptions(EditorInfo.IME_ACTION_DONE)

        val imeOptionsNew = binding.typeMailEditText.imeOptions
        if (imeOptions != imeOptionsNew) {
            val view = currentFocus
            if (view != null) {
                val inputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.restartInput(view)
            }
        }
    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        val result = target != null && Constants.EMAIL_ADDRESS.matcher(target).matches()
        Timber.d("isValidEmail%s", result)
        return result
    }

    private fun isValidPhone(target: CharSequence?): Boolean {
        val result = target != null && Constants.PHONE_NUMBER_REGEX.matcher(target).matches()
        Timber.d("isValidPhone%s", result)
        return result
    }

    private fun checkInputEmail(email: String): String? = when {
        isMySelf(megaApi, email) -> {
            getString(R.string.error_own_email_as_contact)
        }

        isEmailInContacts(megaApi, email) -> {
            getString(R.string.context_contact_already_exists, email)
        }

        isEmailInPending(megaApi, email) -> {
            getString(R.string.invite_not_sent_already_sent, email)
        }

        else -> null
    }

    /**
     * Callback for the result from requesting permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        Timber.d("onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.d("Permission granted")
                isPermissionGranted = true
                prepareToGetContacts()
                binding.noPermissionHeader.visibility = View.GONE
            } else {
                Timber.d("Permission denied")
                setEmptyStateVisibility(true)
                binding.inviteContactListEmptyText.setText(R.string.no_contacts_permissions)
                binding.inviteContactListEmptyImage.visibility = View.VISIBLE
                binding.inviteContactProgressBar.visibility = View.GONE
                binding.noPermissionHeader.visibility = View.VISIBLE
            }
        }
    }

    private fun refreshComponents(shouldScroll: Boolean) {
        refreshAddedContactsView(shouldScroll)
        refreshInviteContactButton()
        //clear input text view after selection
        binding.typeMailEditText.setText("")
        setTitleAB()
    }

    /**
     * Called when a single contact item.
     *
     * @param position The item position in recycler view.
     */
    override fun onItemClick(position: Int) {
        invitationContactsAdapter?.let {
            val contactInfo = it.getItem(position)
            Timber.d("on Item click at %d name is %s", position, contactInfo.getContactName())
            if (contactInfo.hasMultipleContactInfos()) {
                shouldShowContactListWithContactInfo = contactInfo
            } else {
                viewModel.toggleContactHighlightedInfo(contactInfo)
                val singleInvitationContactInfo =
                    viewModel.filterUiState.value.filteredContacts[position]
                if (isContactAdded(singleInvitationContactInfo)) {
                    viewModel.removeSelectedContactInformationByContact(singleInvitationContactInfo)
                    refreshComponents(false)
                } else {
                    viewModel.addSelectedContactInformation(singleInvitationContactInfo)
                    refreshComponents(true)
                }
            }
        }
    }

    private fun refreshHorizontalScrollView() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.scroller.fullScroll(View.FOCUS_RIGHT)
        }, 100)
    }

    private fun onGetContactCompleted() {
        isGetContactCompleted = true
        binding.inviteContactProgressBar.visibility = View.GONE
        refreshList()
        setRecyclersVisibility()
        visibilityFastScroller()

        if (viewModel.allContacts.isNotEmpty()) {
            setEmptyStateVisibility(false)
        } else {
            showEmptyTextView()
        }
    }

    private fun createContactTextView(name: String, viewId: Int): View {
        Timber.d("createTextView contact name is %s", name)
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(
            Util.dp2px(ADDED_CONTACT_VIEW_MARGIN_LEFT.toFloat(), displayMetrics),
            0,
            0,
            0
        )

        val rowView = SelectedContactItemBinding.inflate(layoutInflater)
        with(rowView.root) {
            layoutParams = params
            id = viewId
            isClickable = true
            setOnClickListener { v: View ->
                val invitationContactInfo = viewModel.uiState.value.selectedContactInformation[v.id]
                viewModel.removeSelectedContactInformationAt(viewId)
                if (invitationContactInfo.hasMultipleContactInfos()) {
                    controlHighlighted(invitationContactInfo.id)
                } else {
                    viewModel.toggleContactHighlightedInfo(invitationContactInfo, false)
                }
                refreshAddedContactsView(false)
                refreshInviteContactButton()
                refreshList()
                setTitleAB()
            }
        }

        rowView.contactName.text = name
        return rowView.root
    }

    private fun refreshAddedContactsView(shouldScroll: Boolean) {
        Timber.d("refreshAddedContactsView")
        binding.labelContainer.removeAllViews()
        viewModel.uiState.value.selectedContactInformation.forEachIndexed { index, contact ->
            val displayedLabel = contact.getContactName().ifBlank { contact.displayInfo }
            binding.labelContainer.addView(createContactTextView(displayedLabel, index))
        }
        binding.labelContainer.invalidate()
        if (shouldScroll) {
            refreshHorizontalScrollView()
        } else {
            binding.scroller.clearFocus()
        }
    }

    private fun controlHighlighted(id: Long) {
        var shouldHighlighted = false
        for ((addedId) in viewModel.uiState.value.selectedContactInformation) {
            if (addedId == id) {
                shouldHighlighted = true
                break
            }
        }
        invitationContactsAdapter?.data?.forEach {
            if (it.id == id) {
                viewModel.toggleContactHighlightedInfo(it, shouldHighlighted)
            }
        }
    }

    private fun isContactAdded(invitationContactInfo: InvitationContactInfo): Boolean {
        Timber.d("isContactAdded contact name is %s", invitationContactInfo.getContactName())
        for (addedContact in viewModel.uiState.value.selectedContactInformation) {
            if (viewModel.isTheSameContact(addedContact, invitationContactInfo)) {
                return true
            }
        }
        return false
    }

    private fun refreshList() {
        Timber.d("refresh list")
        setPhoneAdapterContacts(viewModel.filterUiState.value.filteredContacts)
    }

    private fun refreshInviteContactButton() {
        Timber.d("refreshInviteContactButton")
        val stringInEditText = binding.typeMailEditText.text.toString()
        val isStringValidNow = (stringInEditText.isEmpty()
                || isValidEmail(stringInEditText)
                || isValidPhone(stringInEditText))
        enableFabButton(viewModel.uiState.value.selectedContactInformation.isNotEmpty() && isStringValidNow)
    }

    private fun addContactInfo(inputString: String, type: Int) {
        Timber.d("addContactInfo inputString is %s type is %d", inputString, type)
        var info: InvitationContactInfo? = null
        if (type == TYPE_MANUAL_INPUT_EMAIL) {
            info = createManualInput(
                inputString,
                TYPE_MANUAL_INPUT_EMAIL,
                R.color.grey_500_grey_400
            )
        } else if (type == TYPE_MANUAL_INPUT_PHONE) {
            info = createManualInput(
                inputString,
                TYPE_MANUAL_INPUT_PHONE,
                R.color.grey_500_grey_400
            )
        }
        if (info != null) {
            val index = isUserEnteredContactExistInList(info)
            val holder = binding.inviteContactList.findViewHolderForAdapterPosition(index)
            if (index >= 0 && holder != null) {
                holder.itemView.performClick()
            } else if (!isContactAdded(info)) {
                viewModel.addSelectedContactInformation(info)
                refreshAddedContactsView(true)
            }
        }
        setTitleAB()
    }

    private fun inviteContacts(addedContacts: List<InvitationContactInfo>?) {
        // Email/phone contacts to be invited
        contactsEmailsSelected = mutableListOf()
        contactsPhoneSelected = mutableListOf()

        addedContacts?.forEach { contact ->
            if (contact.isEmailContact()) {
                contactsEmailsSelected.add(contact.displayInfo)
            } else {
                contactsPhoneSelected.add(contact.displayInfo)
            }
        }

        if (contactsEmailsSelected.isNotEmpty()) {
            //phone contact will be invited once email done
            viewModel.inviteContactsByEmail(ArrayList(contactsEmailsSelected))
        } else if (contactsPhoneSelected.isNotEmpty()) {
            invitePhoneContacts(ArrayList(contactsPhoneSelected))
            finish()
        } else {
            finish()
        }
    }

    private fun invitePhoneContacts(phoneNumbers: List<String>) {
        Timber.d("invitePhoneContacts")
        val recipient = buildString {
            append("smsto:")
            phoneNumbers.forEach { phone ->
                append(phone)
                append(";")
                Timber.d("setResultPhoneContacts: $phone")
            }
        }
        val smsBody = resources.getString(
            R.string.invite_contacts_to_start_chat_text_message,
            viewModel.uiState.value.contactLink
        )
        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(recipient))
        smsIntent.putExtra("sms_body", smsBody)
        startActivity(smsIntent)
    }

    /**
     * Show a snack bar
     */
    fun showSnackBar(message: String) {
        showSnackbar(Constants.SNACKBAR_TYPE, binding.scroller, message, -1)
    }

    private fun isUserEnteredContactExistInList(userEnteredInfo: InvitationContactInfo): Int {
        if (invitationContactsAdapter == null) return USER_INDEX_NONE_EXIST

        val list = invitationContactsAdapter!!.data
        for (i in list.indices) {
            if (userEnteredInfo.displayInfo.equals(list[i].displayInfo, ignoreCase = true)) {
                return i
            }
        }

        return USER_INDEX_NONE_EXIST
    }

    private fun enableFabButton(enableFabButton: Boolean) {
        Timber.d("enableFabButton: $enableFabButton")
        binding.fabButtonNext.isEnabled = enableFabButton
    }

    companion object {
        internal const val KEY_SENT_NUMBER = "sentNumber"

        private const val SCAN_QR_FOR_INVITE_CONTACTS = 1111
        private const val KEY_IS_PERMISSION_GRANTED = "KEY_IS_PERMISSION_GRANTED"
        private const val KEY_IS_GET_CONTACT_COMPLETED = "KEY_IS_GET_CONTACT_COMPLETED"
        private const val CURRENT_SELECTED_CONTACT = "CURRENT_SELECTED_CONTACT"
        private const val USER_INDEX_NONE_EXIST = -1
        private const val MIN_LIST_SIZE_FOR_FAST_SCROLLER = 20
        private const val ADDED_CONTACT_VIEW_MARGIN_LEFT = 10
    }
}
