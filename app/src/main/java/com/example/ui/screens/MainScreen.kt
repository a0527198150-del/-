package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.database.ApprovedChannel
import com.example.data.network.YoutubeVideo

// Colors for our elegant "Professional Polish - סינון קודש" Theme
val ScreenBg = Color(0xFFF3F4F9)       // Professional Light gray-blue
val CardBg = Color(0xFFFFFFFF)         // White
val RoyalBlue = Color(0xFF001452)      // Deep Primary dark blue
val LightBlue = Color(0xFFDDE1FF)      // Warm blue highlight
val LightBlueBorder = Color(0xFFBBC3FF) // Border color for highlights
val DarkText = Color(0xFF1B1B1F)       // Active text / dark accent
val MutedText = Color(0xFF44474E)      // Slate gray secondary text/icons
val BorderColor = Color(0xFFE1E2EC)    // Standard border color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val approvedChannels by viewModel.approvedChannels.collectAsStateWithLifecycle()
    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val selectedVideo by viewModel.selectedVideo.collectAsStateWithLifecycle()
    val videosState by viewModel.videosState.collectAsStateWithLifecycle()
    
    val isSupervisor by viewModel.isSupervisorAuthenticated.collectAsStateWithLifecycle()
    val showPinPrompt by viewModel.showPinPrompt.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // Handle toast messages
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    // Capture system back press to return back inside the app
    BackHandler(enabled = screenState != ScreenState.CHANNELS) {
        when (screenState) {
            ScreenState.PLAYER -> viewModel.selectChannel(selectedChannel!!)
            ScreenState.CHANNEL_VIDEOS -> viewModel.navigateToChannels()
            ScreenState.SUPERVISOR -> viewModel.navigateToChannels()
            else -> viewModel.navigateToChannels()
        }
    }

    // Material 3 Custom Theme Wrapper (Light Theme to match Professional Polish)
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = RoyalBlue,
            onPrimary = Color.White,
            secondary = LightBlue,
            background = ScreenBg,
            surface = CardBg,
            onBackground = DarkText,
            onSurface = DarkText
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(19.dp))
                                    .background(LightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield, 
                                    contentDescription = "סינון",
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = when (screenState) {
                                        ScreenState.CHANNELS -> "סינון קודש"
                                        ScreenState.CHANNEL_VIDEOS -> selectedChannel?.customName ?: "סרטונים מאושרים"
                                        ScreenState.PLAYER -> selectedVideo?.title ?: "נגן כשר"
                                        ScreenState.SUPERVISOR -> "מצב מפקח - ניהול סינון"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "סביבה מוגנת ומבוקרת",
                                    fontSize = 11.sp,
                                    color = MutedText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    actions = {
                        // Supervisor indicator button
                        IconButton(
                            onClick = {
                                if (isSupervisor) {
                                    if (screenState == ScreenState.SUPERVISOR) {
                                        viewModel.navigateToChannels()
                                    } else {
                                        viewModel.navigateToSupervisor()
                                    }
                                } else {
                                    viewModel.navigateToSupervisor()
                                }
                            },
                            modifier = Modifier.testTag("supervisor_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isSupervisor) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = if (isSupervisor) "מצב מפקח פעיל" else "כניסה למצב מפקח",
                                tint = if (isSupervisor) RoyalBlue else MutedText
                            )
                        }
                        
                        if (isSupervisor) {
                            IconButton(
                                onClick = { viewModel.logoutSupervisor() },
                                modifier = Modifier.testTag("supervisor_logout")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Logout,
                                    contentDescription = "יציאה ממצב מפקח",
                                    tint = Color.Red
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CardBg,
                        titleContentColor = DarkText,
                        actionIconContentColor = MutedText
                    )
                )
            },
            containerColor = ScreenBg,
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(ScreenBg)
            ) {
                when (screenState) {
                    ScreenState.CHANNELS -> ChannelsGridScreen(
                        channels = approvedChannels,
                        isSupervisorMode = isSupervisor,
                        onChannelClick = { viewModel.selectChannel(it) },
                        onEnterSupervisorClick = { viewModel.navigateToSupervisor() }
                    )
                    
                    ScreenState.CHANNEL_VIDEOS -> ChannelVideosScreen(
                        channel = selectedChannel!!,
                        videosState = videosState,
                        onBackClick = { viewModel.navigateToChannels() },
                        onVideoClick = { viewModel.playVideo(it) }
                    )
                    
                    ScreenState.PLAYER -> VideoPlayerScreen(
                        video = selectedVideo!!,
                        onBackClick = { viewModel.selectChannel(selectedChannel!!) }
                    )
                    
                    ScreenState.SUPERVISOR -> SupervisorPanelScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.navigateToChannels() }
                    )
                }

                // PIN entry modal prompt
                if (showPinPrompt) {
                    PinPromptDialog(
                        errorMessage = pinError,
                        onDismiss = { viewModel.closePinPrompt() },
                        onSubmit = { viewModel.authenticateSupervisor(it) }
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. CHANNELS LIST / GRID SCREEN
// ==========================================
@Composable
fun ChannelsGridScreen(
    channels: List<ApprovedChannel>,
    isSupervisorMode: Boolean,
    onChannelClick: (ApprovedChannel) -> Unit,
    onEnterSupervisorClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kosher Header Card in LightBlue
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LightBlue),
            border = BorderStroke(1.dp, LightBlueBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "הלימוד היומי שלך בסינון קודש",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = RoyalBlue,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "הגישה באפליקציה זו מוגבלת אך ורק לערוצי התורה והחיזוק שאושרו בפיקוח. שאר התכנים ברשת חסומים בצורה מוחלטת.",
                    fontSize = 13.sp,
                    color = RoyalBlue.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                
                if (!isSupervisorMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "לשינוי או הוספת ערוצים, פנה למשגיח או לחץ על סמל המנעול למעלה.",
                        fontSize = 12.sp,
                        color = RoyalBlue.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .background(RoyalBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "מצב מפקח פעיל! באפשרותך להוסיף ולמחוק ערוצים כעת.",
                            fontSize = 12.sp,
                            color = RoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "ערוצים מאושרים (${channels.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText
            )
        }

        if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.WifiOff,
                        contentDescription = "אין ערוצים",
                        tint = MutedText,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "אין ערוצים מאושרים כרגע.",
                        color = DarkText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "היכנס למצב מפקח על ידי הקשת קוד והוסף ערוצים.",
                        color = MutedText,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onEnterSupervisorClick,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Text("התחבר כמפקח להוספה", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(channels) { channel ->
                    ChannelGridItem(
                        channel = channel,
                        onClick = { onChannelClick(channel) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelGridItem(
    channel: ApprovedChannel,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("channel_card_${channel.channelId}")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant circle with channel initial letter (school/study theme)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFF1F0F4))
                    .border(1.dp, BorderColor, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.customName.take(1),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = channel.customName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = DarkText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = channel.description.ifBlank { "ערוץ מאושר ומסונן" },
                fontSize = 11.sp,
                color = MutedText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
                modifier = Modifier.height(28.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "צפייה",
                    tint = RoyalBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "צפה בשיעורים",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue
                )
            }
        }
    }
}

// ==========================================
// 2. VIDEOS LIST / SCREEN
// ==========================================
@Composable
fun ChannelVideosScreen(
    channel: ApprovedChannel,
    videosState: VideosUiState,
    onBackClick: () -> Unit,
    onVideoClick: (YoutubeVideo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back Navigation Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("back_to_channels_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack, 
                    contentDescription = "חזרה",
                    tint = RoyalBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("לרשימת הערוצים", color = RoyalBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Selected Channel Detail Title Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(LightBlue.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(channel.customName.take(1), color = RoyalBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(channel.customName, fontWeight = FontWeight.Bold, color = DarkText, fontSize = 15.sp)
                    Text("מזהה ערוץ: ${channel.channelId}", fontSize = 10.sp, color = MutedText)
                }
            }
        }

        // Live Feed Videos List
        when (videosState) {
            is VideosUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = RoyalBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("מייבא שיעורי תורה אחרונים מהערוץ...", color = DarkText, fontSize = 14.sp)
                    }
                }
            }
            
            is VideosUiState.Error -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "שגיאה",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = videosState.message,
                            color = DarkText,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            is VideosUiState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(videosState.videos) { video ->
                        VideoListItem(
                            video = video,
                            onClick = { onVideoClick(video) }
                        )
                    }
                }
            }
            
            is VideosUiState.Idle -> {
                // Should not happen as load is triggered in selectChannel
            }
        }
    }
}

@Composable
fun VideoListItem(
    video: YoutubeVideo,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("video_item_${video.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 75.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // We use Coil to load the actual YouTube thumbnail image
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = "תמונה מקדימה לשיעור",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Play Icon overlay
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Video Title & Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DarkText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = "תאריך",
                        tint = MutedText,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "ערוץ מאושר • שיעור למידה",
                        fontSize = 11.sp,
                        color = MutedText
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. KOSHER SANDBOX VIDEO PLAYER
// ==========================================
@Composable
fun VideoPlayerScreen(
    video: YoutubeVideo,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("back_to_videos_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack, 
                    contentDescription = "חזרה",
                    tint = RoyalBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("חזרה לרשימת השיעורים", color = RoyalBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Title and Video metadata Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkText,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ערוץ: ${video.channelName} • צפייה כשרה ללא הסחות דעת",
                    fontSize = 11.sp,
                    color = MutedText
                )
            }
        }

        // The sandboxed HTML player
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            KosherVideoPlayer(
                videoId = video.id,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Safety lock assurance footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = "סינון מאובטח",
                tint = RoyalBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "נגן מסונן פעיל: מנוע החיפוש הכללי, התגובות, והמלצות הוידאו חסומים לחלוטין.",
                fontSize = 11.sp,
                color = RoyalBlue,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KosherVideoPlayer(videoId: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: ""
                        // Strictly lock down WebView navigation to the embed iframe address only
                        return if (url.contains("youtube.com/embed/") || url.contains("youtube-nocookie.com/embed/")) {
                            false // Load embed
                        } else {
                            true // Block other navigations (like links inside the video or ads)
                        }
                    }
                }
                
                // Load embed utilizing youtube-nocookie for privacy and tighter kosher constraints
                val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&modestbranding=1&rel=0&showinfo=0&iv_load_policy=3&disablekb=0"
                loadUrl(embedUrl)
            }
        },
        modifier = modifier,
        update = { webView ->
            val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&modestbranding=1&rel=0&showinfo=0&iv_load_policy=3&disablekb=0"
            if (webView.url != embedUrl) {
                webView.loadUrl(embedUrl)
            }
        }
    )
}

// ==========================================
// 4. SUPERVISOR PANEL SCREEN
// ==========================================
@Composable
fun SupervisorPanelScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val approvedChannels by viewModel.approvedChannels.collectAsStateWithLifecycle()
    val channelIdInput by viewModel.inputChannelId.collectAsStateWithLifecycle()
    val customNameInput by viewModel.inputCustomName.collectAsStateWithLifecycle()
    val descriptionInput by viewModel.inputDescription.collectAsStateWithLifecycle()
    val newPinInput by viewModel.inputNewPin.collectAsStateWithLifecycle()
    val pinConfirmInput by viewModel.inputPinConfirm.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack, 
                        contentDescription = "חזרה",
                        tint = RoyalBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("חזרה לדף הבית", color = RoyalBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Add New Channel
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = RoyalBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("אישור והוספת ערוץ חדש", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBlue)
                    }
                    
                    Text(
                        text = "ניתן להוסיף ערוץ על ידי הזנת מזהה ערוץ יוטיוב תקני (מתחיל ב-UC באורך 24 תווים) או הדבקת קישור לעמוד הערוץ.",
                        fontSize = 12.sp,
                        color = MutedText,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = channelIdInput,
                        onValueChange = { viewModel.inputChannelId.value = it },
                        label = { Text("מזהה ערוץ (Channel ID) או קישור") },
                        placeholder = { Text("למשל UC3_xO72KOfxof...", color = MutedText) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = DarkText,
                            unfocusedTextColor = DarkText,
                            focusedLabelColor = RoyalBlue,
                            unfocusedLabelColor = MutedText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_channel_id_input")
                    )

                    OutlinedTextField(
                        value = customNameInput,
                        onValueChange = { viewModel.inputCustomName.value = it },
                        label = { Text("שם ערוץ מותאם אישית (אופציונלי)") },
                        placeholder = { Text("למשל: שיעורי הרב אלבז", color = MutedText) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = DarkText,
                            unfocusedTextColor = DarkText,
                            focusedLabelColor = RoyalBlue,
                            unfocusedLabelColor = MutedText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_channel_name_input")
                    )

                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { viewModel.inputDescription.value = it },
                        label = { Text("תיאור קצר לערוץ (אופציונלי)") },
                        placeholder = { Text("למשל: שיעורי הלכה ומוסר יומיים", color = MutedText) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = DarkText,
                            unfocusedTextColor = DarkText,
                            focusedLabelColor = RoyalBlue,
                            unfocusedLabelColor = MutedText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { viewModel.addNewApprovedChannel() },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_channel_button")
                    ) {
                        Text("אישור והוספה לרשימת הסינון", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Manage Approved Channels
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.PlaylistPlay, contentDescription = null, tint = RoyalBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ניהול והסרת ערוצים מאושרים (${approvedChannels.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBlue)
                    }

                    if (approvedChannels.isEmpty()) {
                        Text("אין ערוצים מאושרים ברשימה. הוסף ערוץ למעלה.", color = MutedText, fontSize = 13.sp)
                    } else {
                        approvedChannels.forEach { channel ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ScreenBg, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(channel.customName, fontWeight = FontWeight.Bold, color = DarkText, fontSize = 14.sp)
                                    Text("מזהה: ${channel.channelId}", color = MutedText, fontSize = 10.sp)
                                }
                                
                                IconButton(
                                    onClick = { viewModel.deleteChannel(channel) },
                                    modifier = Modifier.testTag("delete_channel_${channel.channelId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "מחיקה",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Change Supervisor PIN
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.AdminPanelSettings, contentDescription = null, tint = RoyalBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("שינוי קוד גישה מפקח (PIN)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBlue)
                    }

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { viewModel.inputNewPin.value = it },
                        label = { Text("קוד מפקח חדש (לפחות 4 ספרות)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = DarkText,
                            unfocusedTextColor = DarkText,
                            focusedLabelColor = RoyalBlue,
                            unfocusedLabelColor = MutedText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_pin_input")
                    )

                    OutlinedTextField(
                        value = pinConfirmInput,
                        onValueChange = { viewModel.inputPinConfirm.value = it },
                        label = { Text("אימות קוד מפקח חדש") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = DarkText,
                            unfocusedTextColor = DarkText,
                            focusedLabelColor = RoyalBlue,
                            unfocusedLabelColor = MutedText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_pin_input")
                    )

                    Button(
                        onClick = { viewModel.changeSupervisorPin() },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_new_pin_button")
                    ) {
                        Text("שמור קוד מפקח חדש", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. SECURITY PIN PROMPT DIALOG
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinPromptDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var pinText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.5.dp, RoyalBlue),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(40.dp)
                )

                Text(
                    text = "אימות מפקח נדרש",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DarkText,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "הזן קוד מפקח מורשה לצורך שינוי והוספת ערוצי יוטיוב (ברירת המחדל הינה 1234):",
                    fontSize = 13.sp,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = { pinText = it },
                    placeholder = { Text("הזן קוד", color = MutedText) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText,
                        focusedLabelColor = RoyalBlue,
                        unfocusedLabelColor = MutedText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_dialog_input"),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkText),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ביטול")
                    }

                    Button(
                        onClick = { onSubmit(pinText) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pin_dialog_submit"),
                        enabled = pinText.isNotBlank()
                    ) {
                        Text("אישור", color = Color.White)
                    }
                }
            }
        }
    }
}
