package com.hjinlabs.sengkode.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hjinlabs.sengkode.core.model.repository.StudioRestoreStore
import com.hjinlabs.sengkode.feature.generator.ShareIntake
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity app. The optional ACTION_SEND intake (Phase 4)
 * stages shared text/URLs into the studio through the existing
 * session restore store - the studio consumes it through its tested
 * exactly-once path, so no new QR logic exists here.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var restoreStore: StudioRestoreStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeStageSharedContent(intent)
        setContent {
            com.hjinlabs.sengkode.core.designsystem.SengkodeTheme {
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        maybeStageSharedContent(intent)
    }

    private fun maybeStageSharedContent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
        ShareIntake.stageInto(ShareIntake.parse(shared), restoreStore)
    }
}
