package app.gov.uidai.contactlessregistration.ui.userinfo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import app.gov.uidai.contactlessregistration.R
import app.gov.uidai.contactlessregistration.SharedViewModel
import app.gov.uidai.contactlessregistration.model.FingerPosition
import app.gov.uidai.contactlessregistration.model.User
import app.gov.uidai.contactlessregistration.model.UserInfoUiState
import app.gov.uidai.contactlessregistration.ui.composable.LoadingDialog
import app.gov.uidai.contactlessregistration.ui.theme.AttendanceAppTheme
import app.gov.uidai.contactlessregistration.ui.theme.BoxButton
import app.gov.uidai.contactlessregistration.ui.theme.ButtonContentPadding
import app.gov.uidai.contactlessregistration.ui.theme.ButtonTextStyle
import app.gov.uidai.contactlessregistration.ui.theme.CheckboxRow
import app.gov.uidai.contactlessregistration.ui.theme.Spacer
import app.gov.uidai.contactlessregistration.usecase.FingerSDKManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class UserInfoFragment : Fragment() {

    private val args: UserInfoFragmentArgs by navArgs()
    private val viewModel: UserInfoViewModel by viewModels()
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

    private val uploadLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("UserInfoFragment", "Uploaded Uri: $uri")
        viewModel.onUploadImage(uri)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sdkManager.setResultListener { result ->
            viewModel.onSDKResult(result)
        }

        // Set UID hash in ViewModel
        viewModel.setUidHash(args.uidHash)

        val snackbarHostState = SnackbarHostState()

        val onShowToast: suspend (String) -> Unit = { message ->
            snackbarHostState.showSnackbar(message, withDismissAction = true)
            viewModel.clearError()
        }

        return ComposeView(requireContext()).apply {
            setContent {
                AttendanceAppTheme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val sharedUiState by sharedViewModel.uiState.collectAsStateWithLifecycle()

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        "User - ${viewModel.currentUIDHash.take(10)}",
                                        maxLines = 1
                                    )
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
                                hostState = snackbarHostState
                            )
                        }
                    ) { paddingValues ->
                        UserInfoScreen(
                            uiState = uiState,
                            onCaptureImage = {
                                viewModel.startCapture()
                                sdkManager.captureFingerprint(
                                    activityResultLauncher = sdkLauncher,
                                    purpose = "validation"
                                )
                            },
                            onUploadJp2Image = {
                                viewModel.startUpload()
                                uploadLauncher.launch("image/jp2")
                            },
                            onClearMatchingResult = viewModel::clearMatchingResult,
                            onSavePreferenceChange = viewModel::onSavePreferenceChange,
                            retryLoadUserInfo = viewModel::retryLoadUserInfo,
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
                        LaunchedEffect(uiState.errorMessage) {
                            uiState.errorMessage?.let { error ->
                                onShowToast(error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserInfoScreen(
    uiState: UserInfoUiState,
    onCaptureImage: () -> Unit,
    onUploadJp2Image: () -> Unit,
    onClearMatchingResult: () -> Unit,
    onSavePreferenceChange: (Boolean) -> Unit,
    retryLoadUserInfo: () -> Unit,
    paddingValues: PaddingValues
) {
    Box(
        modifier = Modifier.padding(paddingValues)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            when {
                uiState.isLoadingUserData -> {
                    LoadingSection()
                }

                uiState.user != null -> {
                    UserDetailsCard(user = uiState.user)

                    Spacer(32.dp)

                    Text(
                        text = "Fingerprint Matching",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    MatchingSection(
                        isCapturing = uiState.isCapturingImage,
                        isUploading = uiState.isUploadingImage,
                        saveImageAfterCapture = uiState.saveImageAfterCapture,
                        onCaptureImage = onCaptureImage,
                        onUploadJp2Image = onUploadJp2Image,
                        onClearResult = onClearMatchingResult,
                        matchingResults = uiState.matchingResults,
                        onSavePreferenceChange = onSavePreferenceChange,
                        showFingerResult = {}
                    )
                }

                else -> {
                    ErrorSection(onRetry = retryLoadUserInfo)
                }
            }
        }
    }
}

@Composable
fun LoadingSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )

        Spacer(16.dp)

        Text(
            text = "Loading user information...",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun UserDetailsCard(
    user: User
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Name",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(16.dp)

                Column {
                    Text(
                        text = "Full Name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Phone Number
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(16.dp)

                Column {
                    Text(
                        text = "Phone Number",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = user.phoneNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Fingerprint Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_fingerprint),
                    contentDescription = "Fingerprints",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(16.dp)

                Column {
                    Text(
                        text = "Total Registered Fingerprints",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${user.fingerprintCount} fingerprints",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MatchingSection(
    isCapturing: Boolean,
    isUploading: Boolean,
    saveImageAfterCapture: Boolean,
    matchingResults: Map<FingerPosition, Float>?,
    onCaptureImage: () -> Unit,
    onUploadJp2Image: () -> Unit,
    onClearResult: () -> Unit,
    onSavePreferenceChange: (Boolean) -> Unit,
    showFingerResult: (FingerPosition) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            BoxButton(
                text = "Capture Fingerprint",
                onClick = onCaptureImage,
                icon = ImageVector.vectorResource(R.drawable.ic_camera),
                isLoading = isCapturing,
                enabled = !isCapturing && !isUploading,
                loadingText = "Capturing...",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            Text(
                text = "OR",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )

            BoxButton(
                text = "Upload Fingerprint (.jp2)",
                onClick = onUploadJp2Image,
                icon = ImageVector.vectorResource(R.drawable.ic_upload),
                isLoading = isUploading,
                enabled = !isCapturing && !isUploading,
                loadingText = "Uploading...",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        CheckboxRow(
            checked = saveImageAfterCapture,
            onCheckedChange = onSavePreferenceChange,
            text = "Save captured image to the gallery",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(8.dp)

        AnimatedVisibility(
            visible = matchingResults != null
        ) {
            matchingResults?.let { results ->
                MatchingResultCard(
                    matchingResults = results,
                    onClickResult = showFingerResult
                )
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchingResultCard(
    matchingResults: Map<FingerPosition, Float>,
    onClickResult: (FingerPosition) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = mutableListOf<String>()
    val separatedResults = mutableListOf<Map<FingerPosition, Float>>()
    val bestMatch = matchingResults.maxBy { it.value }

    matchingResults.filter { it.key.name.contains("LEFT") }.also {
        if (!it.isEmpty()) {
            tabs.add("Left")
            separatedResults.add(it)
        }
    }
    matchingResults.filter { it.key.name.contains("RIGHT") }.also {
        if (!it.isEmpty()) {
            tabs.add("Right")
            separatedResults.add(it)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Matching Scores",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {

                        Text(
                            text = "Best Match",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = bestMatch.key.name.replace('_', ' '),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        text = bestMatch.value.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                    )
                }

                // TabRow for switching between tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clip(
                            RoundedCornerShape(
                                topEnd = 12.dp,
                                topStart = 12.dp
                            )
                        )
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
                    MatchingList(
                        items = separatedResults[targetTabIndex],
                        onClickResult = onClickResult
                    )
                }
            }
        }
    }
}

@Composable
fun MatchingList(
    items: Map<FingerPosition, Float>,
    onClickResult: (FingerPosition) -> Unit
) {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        items.map { item ->
            MatchingItem(
                position = item.key,
                matchingScore = item.value,
                onClick = { onClickResult(item.key) }
            )
        }
    }
}

@Composable
fun MatchingItem(
    position: FingerPosition,
    matchingScore: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image Placeholder or Loaded Image
        Box(
            modifier = Modifier
                .size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            // Otherwise, show add button
            Icon(
                painter = painterResource(R.drawable.ic_fingerprint),
                contentDescription = null,
                tint = scoreToColor(matchingScore),
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(8.dp)

        Column {
            Text(
                text = position.name.replace('_', ' '),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = "$matchingScore",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun scoreToColor(score: Float): Color {
    val clamped = score.coerceIn(0f, 300f)
    val base = when {
        clamped <= 200f -> {
            // 0 → 750 : Red → Yellow
            val fraction = clamped / 200f
            lerp(Color.Red, Color.Yellow, fraction)
        }

        else -> {
            // 750 → 1500 : Yellow → Green
            val fraction = (clamped - 200f) / 100f
            lerp(Color.Yellow, Color.Green, fraction)
        }
    }
    // Desaturate a bit and lighten slightly
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(base.toArgb(), hsv)
    hsv[1] *= 0.6f // reduce saturation
    hsv[2] = (hsv[2] * 0.9f).coerceAtMost(1f) // slightly dim
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun ErrorSection(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Error",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(16.dp)

        Text(
            text = "Unable to load user information",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(8.dp)

        Text(
            text = "Please check your connection and try again",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(24.dp)

        Button(onClick = onRetry, contentPadding = ButtonContentPadding) {
            Text("Retry", style = ButtonTextStyle)
        }
    }
}
