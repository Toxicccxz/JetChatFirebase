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

package com.example.compose.jetchat.profile

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.compose.jetchat.FunctionalityNotAvailablePopup
import com.example.compose.jetchat.MainViewModel
import com.example.compose.jetchat.NavActivity
import com.example.compose.jetchat.R
import com.example.compose.jetchat.components.JetchatAppBar
import com.example.compose.jetchat.theme.JetchatTheme
import java.time.LocalDateTime

class ProfileFragment : Fragment() {

    var TAG:String? = null

    private val viewModel: ProfileViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Consider using safe args plugin
        val userId = arguments?.getString("userId")
        viewModel.setUserId(userId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView: View = inflater.inflate(R.layout.fragment_profile, container, false)

        rootView.findViewById<ComposeView>(R.id.toolbar_compose_view).apply {
            var openConversationTime:Long = SystemClock.elapsedRealtime()
            var current:
                    LocalDateTime? = LocalDateTime.now()
            setContent {
                var functionalityNotAvailablePopupShown by remember { mutableStateOf(false) }
                if (functionalityNotAvailablePopupShown) {
                    FunctionalityNotAvailablePopup { functionalityNotAvailablePopupShown = false }
                }

                JetchatTheme {
                    JetchatAppBar(
                        onNavIconPressed = {
                            activityViewModel.openDrawer()
                            var conversationCloseTime = SystemClock.elapsedRealtime()
                            var stayTime = conversationCloseTime - openConversationTime
                            (activity as NavActivity).firebaseAnalytics.logEvent("PROFILE_PAGE", bundleOf(
                                "stay_time_is" to "from $current to ${LocalDateTime.now()}, $stayTime milliseconds total"
                            )) },
                        title = { },
                        actions = {
                            // More icon
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable(onClick = {
                                        functionalityNotAvailablePopupShown = true
                                    })
                                    .padding(horizontal = 12.dp, vertical = 16.dp)
                                    .height(24.dp),
                                contentDescription = stringResource(id = R.string.more_options)
                            )
                        }
                    )
                }
            }
        }

        rootView.findViewById<ComposeView>(R.id.profile_compose_view).apply {
            setContent {
                val userData by viewModel.userData.observeAsState()
                val nestedScrollInteropConnection = rememberNestedScrollInteropConnection()
                TAG = userData?.name
                Log.e("xavier", "ok now im in $TAG\'s profile")

                JetchatTheme {
                    if (userData == null) {
                        ProfileError()
                    } else {
                        ProfileScreen(
                            userData = userData!!,
                            nestedScrollInteropConnection = nestedScrollInteropConnection
                        )
                    }
                }
            }
        }
        return rootView
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("xavier", "ok now i leave $TAG\'s profile")
    }
}
