package com.example.ui.screens.code

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MorphingLoadingBox
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeViewScreen(
    owner: String,
    repo: String,
    filePath: String,
    fileName: String,
    ref: String?,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val codeState by viewModel.codeViewState.collectAsState()

    BackHandler {
        onBack()
    }

    LaunchedEffect(owner, repo, filePath, ref) {
        viewModel.loadFileContent(owner, repo, filePath, ref)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = filePath,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = PlusJakartaSansFamily
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("code_view_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (codeState is UiState.Success) {
                        IconButton(
                            onClick = {
                                val content = (codeState as UiState.Success<String>).data
                                clipboardManager.setText(AnnotatedString(content))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = codeState) {
                is UiState.Loading -> {
                    MorphingLoadingBox(
                        modifier = Modifier.fillMaxSize(),
                        message = "Loading source file contents...",
                        size = 48.dp
                    )
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Could not load file: ${state.message}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = PlusJakartaSansFamily
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is UiState.Success -> {
                    val code = state.data
                    val lines = code.lines()

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Line numbers column
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                            modifier = Modifier.width(46.dp)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)) {
                                lines.indices.forEach { index ->
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 11.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            textAlign = TextAlign.End
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Code content with horizontal scroll
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 11.5.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
