/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.compose.jetchat

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.DrawerValue.Closed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.compose.jetchat.components.JetchatDrawer
import com.example.compose.jetchat.conversation.BackPressHandler
import com.example.compose.jetchat.conversation.LocalBackPressedDispatcher
import com.example.compose.jetchat.databinding.ContentMainBinding
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Main activity for the app.
 */
class NavActivity : AppCompatActivity() {
    lateinit var firebaseAnalytics: FirebaseAnalytics
    var openFromDrawer:Int = 0
    var openFromProfile:Int = 0
    var messageCount:Int = 0
    private val viewModel: MainViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLifecycleComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = Firebase.analytics
        val sharedPreference:SharedPreferences =  this.getSharedPreferences("count", Context.MODE_PRIVATE)
        val editor:SharedPreferences.Editor = sharedPreference.edit()
        editor.putInt("chat_count", 0)
        editor.apply()

        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(
            ComposeView(this).apply {
                consumeWindowInsets = false
                setContent {
                    CompositionLocalProvider(
                        LocalBackPressedDispatcher provides this@NavActivity.onBackPressedDispatcher
                    ) {
                        val drawerState = rememberDrawerState(initialValue = Closed)
                        val drawerOpen by viewModel.drawerShouldBeOpened
                            .collectAsStateWithLifecycle()

                        if (drawerOpen) {
                            // Open drawer and reset state in VM.
                            LaunchedEffect(Unit) {
                                // wrap in try-finally to handle interruption whiles opening drawer
                                try {
                                    drawerState.open()
                                } finally {
                                    viewModel.resetOpenDrawerAction()
                                }
                            }
                        }

                        // Intercepts back navigation when the drawer is open
                        val scope = rememberCoroutineScope()
                        if (drawerState.isOpen) {
                            BackPressHandler {
                                scope.launch {
                                    drawerState.close()
                                    var drawerCloseTime = SystemClock.elapsedRealtime()
                                    var stayTime = drawerCloseTime - viewModel.drawerOpenTime
                                    firebaseAnalytics.logEvent("DRAWER_TIME", bundleOf(
                                        "stay_time_is" to "from ${viewModel.current} to ${LocalDateTime.now()}, $stayTime milliseconds total"
                                    ))
                                }
                            }
                        }

                        JetchatDrawer(
                            drawerState = drawerState,
                            onChatClicked = {
                                findNavController().popBackStack(R.id.nav_home,false)
                                scope.launch {
                                    drawerState.close()
                                    openFromDrawer++
                                    editor.putInt("conversation_open_from_drawer", openFromDrawer)
                                    editor.apply()
                                    firebaseAnalytics.logEvent("NAVIGATION_COUNT", bundleOf(
                                        "navigation_from_drawer" to "$openFromDrawer"
                                    ))
                                    var drawerCloseTime = SystemClock.elapsedRealtime()
                                    var stayTime = drawerCloseTime - viewModel.drawerOpenTime
                                    Log.e("xavier", "who he is talking to? = $it")
                                    firebaseAnalytics.logEvent("DRAWER_TIME", bundleOf(
                                        "stay_time_is" to "from ${viewModel.current} to ${LocalDateTime.now()}, $stayTime milliseconds total"
                                    ))
                                }
                            },
                            onProfileClicked = {
                                val bundle = bundleOf("userId" to it)
                                findNavController().navigate(R.id.nav_profile, bundle)
                                scope.launch {
                                    drawerState.close()
                                    var drawerCloseTime = SystemClock.elapsedRealtime()
                                    var stayTime = drawerCloseTime - viewModel.drawerOpenTime
                                    firebaseAnalytics.logEvent("DRAWER_TIME", bundleOf(
                                        "stay_time_is" to "from ${viewModel.current} to ${LocalDateTime.now()}, $stayTime milliseconds total"
                                    ))
                                }
                            }
                        ) {
                            AndroidViewBinding(ContentMainBinding::inflate)
                        }
                    }
                }
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController().navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * See https://issuetracker.google.com/142847973
     */
    private fun findNavController(): NavController {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController
    }
}
