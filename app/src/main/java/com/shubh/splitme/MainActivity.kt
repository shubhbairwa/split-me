package com.shubh.splitme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.ui.MainScreen
import com.shubh.splitme.ui.MainViewModel
import com.shubh.splitme.ui.theme.GlassBackground
import com.shubh.splitme.ui.theme.SplitMeTheme

// AppCompatActivity (not ComponentActivity) so the in-app language choice also applies on Android 12 and older.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as SplitMeApplication
            viewModel(factory = MainViewModel.Factory(app.authRepository, app.memberRepository, app.groupRepository)) as MainViewModel

            SplitMeTheme {
                GlassBackground {
                    MainScreen()
                }
            }
        }
    }
}
