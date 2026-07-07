package app.gov.uidai.contactlessregistration.ui.registration

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import app.gov.uidai.contactlessregistration.SharedViewModel
import app.gov.uidai.contactlessregistration.model.CLFingerprint
import app.gov.uidai.contactlessregistration.model.FingerCaptureStatus
import app.gov.uidai.contactlessregistration.model.FingerPosition
import app.gov.uidai.contactlessregistration.model.RegistrationUiState
import app.gov.uidai.contactlessregistration.ui.composable.FingerShape
import app.gov.uidai.contactlessregistration.ui.composable.LoadingDialog
import app.gov.uidai.contactlessregistration.ui.theme.AppButton
import app.gov.uidai.contactlessregistration.ui.theme.AttendanceAppTheme
import app.gov.uidai.contactlessregistration.ui.theme.Spacer
import app.gov.uidai.contactlessregistration.usecase.FingerSDKManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class RegistrationFragment : Fragment() {

    private val args: RegistrationFragmentArgs by navArgs()
    private val viewModel: RegistrationViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    @Inject
    lateinit var sdkManager: FingerSDKManager

    // Activity result launcher for SDK
    private val sdkLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        lifecycleScope.launch {
            sdkManager.parseResponse(result.resultCode, result.data)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        sdkManager.setResultListener { result ->
            viewModel.onSDKResult(result)
        }

        // Set UID hash in ViewModel
        viewModel.setUidHash(args.uidHash)

        val txnId = UUID.randomUUID().toString()
        val snackbarHostState = SnackbarHostState()

        val onShowToast: suspend (String) -> Unit = { message ->
            snackbarHostState.showSnackbar(message, withDismissAction = true)
            viewModel.clearError()
        }

        return ComposeView(requireContext()).apply {
            setContent {
                AttendanceAppTheme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val registrationResult by viewModel.registrationResult.collectAsStateWithLifecycle()
                    val sharedUiState by sharedViewModel.uiState.collectAsStateWithLifecycle()

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text("Registration", maxLines = 1)
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        findNavController().navigateUp()
                                    }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Go Back"
                                        )
                                    }
                                }
                            )
                        },
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState, modifier = Modifier
                            )
                        }) { paddingValues ->
                        val keyboardController = LocalSoftwareKeyboardController.current

                        RegistrationScreen(
                            uiState = uiState,
                            onNameChanged = viewModel::onNameChanged,
                            onPhoneNumberChanged = viewModel::onPhoneNumberChanged,
                            onAddFingerprint = {
                                keyboardController?.hide()
                                viewModel.startFingerprintCapture(
                                    fingerPosition = it,
                                    launchSdk = {
                                        sdkManager.captureFingerprint(
                                            activityResultLauncher = sdkLauncher,
                                            purpose = "register"
                                        )
                                    }
                                )
                            },
                            onRemoveFingerprint = viewModel::removeFingerprint,
                            onSaveRegistration = viewModel::registerUser,
                            paddingValues = paddingValues
                        )
                        LoadingDialog(
                            sharedUiState.isLoadingEmbedder,
                            "Initializing Embedder..."
                        )

                        // Handle shared error messages
                        LaunchedEffect(sharedUiState.message) {
                            sharedUiState.message?.let { error ->
                                onShowToast(error)
                            }
                        }

                        // Handle ui error messages
                        LaunchedEffect(uiState.message) {
                            uiState.message?.let { error ->
                                onShowToast(error)
                            }
                        }

                        // Handle registration saved
                        LaunchedEffect(registrationResult) {
                            when (registrationResult) {
                                is RegistrationResult.Success -> {
                                    onShowToast("Registration completed successfully!")
                                    keyboardController?.hide()
                                    findNavController().navigateUp()
                                }

                                is RegistrationResult.Error -> {
                                    val message =
                                        (registrationResult as RegistrationResult.Error).message
                                    onShowToast(message)
                                }

                                else -> {

                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState", "DefaultLocale")
@Composable
fun RegistrationScreen(
    uiState: RegistrationUiState,
    onNameChanged: (String) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onRemoveFingerprint: (FingerPosition) -> Unit,
    onSaveRegistration: () -> Unit,
    onAddFingerprint: (FingerPosition) -> Unit,
    paddingValues: PaddingValues
) {
    var selectedImage by remember { mutableStateOf<FingerPosition?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Personal Information Section
            PersonalInfoSection(
                name = uiState.name,
                phoneNumber = uiState.phoneNumber,
                onNameChanged = onNameChanged,
                onPhoneNumberChanged = onPhoneNumberChanged
            )

            Spacer(16.dp)

            // Fingerprint Section
            FingerprintSection(
                fingerprints = uiState.fingerprints,
                loadingFingerPosition = uiState.loadingFinger,
                fingerUploadStatus = uiState.fingerUploadStatus,
                onAddFingerprint = onAddFingerprint,
                onClickFingerprint = {
                    selectedImage = it
                },
                onRemoveFingerprint = onRemoveFingerprint
            )

            Spacer(16.dp)

            // Save Button
            AppButton(
                text = "Save",
                onClick = onSaveRegistration,
                loadingText = "Saving...",
                enabled = (uiState.canSave && !uiState.isLoading)
            )
        }

        // Selected Image
        selectedImage?.let {
            val bitmap = uiState.fingerprints[it]?.bitmap?.asImageBitmap()!!
            val fingerQuality = uiState.fingerprints[it]?.fingerQuality

            Dialog(onDismissRequest = { selectedImage = null }) {
                Card(shape = MaterialTheme.shapes.extraLarge) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(8.dp)
                        fingerQuality?.let { quality ->
                            quality.attributes.forEach { attribute ->
                                Text(
                                    text = "${attribute.getFormatedName()}: ${
                                        String.format("%.1f", attribute.score)
                                    }",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(4.dp)
                            }
                        }
                        Spacer(4.dp)
                        TextButton(
                            onClick = { selectedImage = null }, modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Okay")
                        }
                    }
                }
            }
        }

        // Overlay loader
        if (uiState.isLoading) {
            Dialog(onDismissRequest = { /*Do Nothing*/ }) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun PersonalInfoSection(
    name: String,
    phoneNumber: String,
    onNameChanged: (String) -> Unit,
    onPhoneNumberChanged: (String) -> Unit
) {
    Column {
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Full Name") },
            placeholder = { Text("Enter your full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(16.dp)

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { newValue ->
                // Only allow digits and limit to reasonable phone number length
                if (newValue.length <= 15 && newValue.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }) {
                    onPhoneNumberChanged(newValue)
                }
            },
            label = { Text("Phone Number") },
            placeholder = { Text("Enter your phone number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerprintSection(
    fingerprints: Map<FingerPosition, CLFingerprint?>,
    loadingFingerPosition: FingerPosition?,
    fingerUploadStatus: Map<FingerPosition, FingerCaptureStatus>,
    onAddFingerprint: (FingerPosition) -> Unit,
    onClickFingerprint: (FingerPosition) -> Unit,
    onRemoveFingerprint: (FingerPosition) -> Unit,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Left", "Right")

    val rightHandFingers = fingerprints.filter { it.key.name.contains("RIGHT") }
    val leftHandFingers = fingerprints.filter { it.key.name.contains("LEFT") }

    Column {

        Text(
            text = "Fingerprints",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // TabRow for switching between tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(title, style = MaterialTheme.typography.bodyLarge)
                            }
                        )
                    }
                }

                // Animated content for switching between tab content
                AnimatedContent(
                    targetState = selectedTabIndex, label = "tabContentAnimation"
                ) { targetTabIndex ->
                    when (targetTabIndex) {
                        0 -> FingerList(
                            items = leftHandFingers,
                            loadingFingerPosition = loadingFingerPosition,
                            fingerUploadStatus = fingerUploadStatus,
                            onAddFingerprint = onAddFingerprint,
                            onRemoveFingerprint = onRemoveFingerprint,
                            onClickFingerprint = onClickFingerprint
                        )

                        1 -> FingerList(
                            items = rightHandFingers,
                            loadingFingerPosition = loadingFingerPosition,
                            fingerUploadStatus = fingerUploadStatus,
                            onAddFingerprint = onAddFingerprint,
                            onRemoveFingerprint = onRemoveFingerprint,
                            onClickFingerprint = onClickFingerprint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FingerList(
    items: Map<FingerPosition, CLFingerprint?>,
    loadingFingerPosition: FingerPosition?,
    fingerUploadStatus: Map<FingerPosition, FingerCaptureStatus>,
    onAddFingerprint: (FingerPosition) -> Unit,
    onClickFingerprint: (FingerPosition) -> Unit,
    onRemoveFingerprint: (FingerPosition) -> Unit
) {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        items.map { item ->
            FingerItem(
                position = item.key,
                isLoading = (item.key == loadingFingerPosition),
                uploadStatus = fingerUploadStatus[item.key] ?: FingerCaptureStatus.NOT_CAPTURED,
                bitmap = item.value?.bitmap,
                minutiaCount = item.value?.fingerQuality?.getMinutia(),
                onClick = { onClickFingerprint(item.key) },
                onRemove = { onRemoveFingerprint(item.key) },
                onAdd = { onAddFingerprint(item.key) })
        }
    }
}

@Composable
fun FingerItem(
    position: FingerPosition,
    isLoading: Boolean,
    uploadStatus: FingerCaptureStatus,
    bitmap: Bitmap?,
    minutiaCount: Double?,
    onAdd: () -> Unit,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isItemAdded = bitmap != null

    // Click policy per state:
    // NOT_CAPTURED -> tap to add
    // CAPTURING / UPLOADING -> busy, no interaction
    // CAPTURED -> already confirmed by backend, no interaction needed
    // PENDING / FAILED -> tap to view details (useful for debugging/retry awareness)
    val isClickable = when (uploadStatus) {
        FingerCaptureStatus.NOT_CAPTURED -> !isLoading
        FingerCaptureStatus.CAPTURING, FingerCaptureStatus.UPLOADING, FingerCaptureStatus.CAPTURED -> false
        FingerCaptureStatus.PENDING, FingerCaptureStatus.FAILED -> isItemAdded
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = isClickable,
                onClick = {
                    when (uploadStatus) {
                        FingerCaptureStatus.NOT_CAPTURED -> onAdd()
                        FingerCaptureStatus.PENDING, FingerCaptureStatus.FAILED -> if (isItemAdded) onClick()
                        else -> {}
                    }
                }
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                uploadStatus == FingerCaptureStatus.UPLOADING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFF9800)
                        )
                    }
                }

                uploadStatus == FingerCaptureStatus.PENDING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Pending sync",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                uploadStatus == FingerCaptureStatus.FAILED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Failed",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                isItemAdded || uploadStatus == FingerCaptureStatus.CAPTURED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Captured",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                uploadStatus == FingerCaptureStatus.CAPTURING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add ${position.name.replace('_', ' ')}"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        val displayText = when {
            uploadStatus == FingerCaptureStatus.UPLOADING -> "Uploading..."
            uploadStatus == FingerCaptureStatus.PENDING -> "Pending sync"
            uploadStatus == FingerCaptureStatus.FAILED -> "Upload failed"
            minutiaCount != null -> "Minutia Count: ${minutiaCount.toInt()}"
            isItemAdded || uploadStatus == FingerCaptureStatus.CAPTURED ->
                position.name.replace('_', ' ')
            !isLoading -> "Add ${position.name.replace('_', ' ')}"
            else -> "Capturing..."
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (minutiaCount != null) {
                Text(
                    text = position.name.replace('_', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}