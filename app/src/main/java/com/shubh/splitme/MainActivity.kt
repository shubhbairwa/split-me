package com.shubh.splitme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.ui.MainScreen
import com.shubh.splitme.ui.MainViewModel
import com.shubh.splitme.ui.theme.SplitMeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as SplitMeApplication
            viewModel(factory = MainViewModel.Factory(app.authRepository, app.memberRepository, app.groupRepository)) as MainViewModel

            SplitMeTheme {
                MainScreen()
            }
        }
    }
}
