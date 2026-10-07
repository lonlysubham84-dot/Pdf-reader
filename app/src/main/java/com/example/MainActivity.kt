package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderEditorScreen
import com.example.ui.theme.PDFStudioTheme
import com.example.viewmodel.PdfViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming PDF VIEW intent
        handleIntent(intent)

        setContent {
            PDFStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                viewModel.importPdfFromUri(uri, null)
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: PdfViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Crossfade(targetState = uiState.activeDoc, label = "ScreenTransition") { activeDoc ->
        if (activeDoc == null) {
            LibraryScreen(
                viewModel = viewModel,
                onOpenDocument = { doc ->
                    viewModel.openDocument(doc)
                }
            )
        } else {
            ReaderEditorScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.closeDocument()
                }
            )
        }
    }
}
