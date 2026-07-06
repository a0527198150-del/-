package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.res.painterResource
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

// Colors for our elegant "Professional Polish - ערוצים כשרים" Theme
val ScreenBg = Color(0xFF0D0E15)        // Ultra-sleek dark space background
val CardBg = Color(0xFF161824)          // Elegant high-contrast slate-blue card surface
val RoyalBlue = Color(0xFF4F46E5)       // Modern Indigo Primary
val LightBlue = Color(0xFF1E1E30)       // Dark slate highlight
val LightBlueBorder = Color(0xFF2E2E48) // Custom outline for highlights
val DarkText = Color(0xFFF1F5F9)        // Bright white-slate text for high readability
val MutedText = Color(0xFF94A3B8)       // Cool slate secondary text
val BorderColor = Color(0xFF232536)     // Sleek subtle border

// Gold Accent colors for professional kosher badges and headers
val GoldColor = Color(0xFFFBBF24)       // Vibrant Gold for Kosher/Security labels
val GoldBg = Color(0xFF232015)          // Warm dark background for golden labels
val GoldBorder = Color(0xFF42371E)      // Gold border

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

    // Material 3 Custom Theme Wrapper (Dark Theme for a cool, premium aesthetic)
    MaterialTheme(
        colorScheme = darkColorScheme(
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
                                    tint = GoldColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = when (screenState) {
                                        ScreenState.CHANNELS -> "ערוצים כשרים"
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
                                tint = if (isSupervisor) GoldColor else MutedText
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
        // Beautiful Hero Banner Image
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(bottom = 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.img_hero_banner),
                    contentDescription = "ערוצים כשרים - סביבת לימוד מוגנת",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Dark elegant overlay gradient for modern polish
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
                // Banner text
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "ערוצים כשרים לשיעורי תורה",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "סביבת צפייה מוגנת, מבוקרת ומסוננת לחלוטין",
                        fontSize = 11.sp,
                        color = GoldColor,
                        fontWeight = FontWeight.Medium
                    )
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("channel_card_${channel.channelId}")
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sleek mini play circle icon instead of large initials circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RoyalBlue.copy(alpha = 0.15f))
                    .border(1.dp, RoyalBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = GoldColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = channel.customName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.description.ifBlank { "ערוץ שנבחר ומאושר לצפייה מסוננת" },
                    fontSize = 11.sp,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "פתח ערוץ",
                tint = RoyalBlue,
                modifier = Modifier.size(20.dp)
            )
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
    var searchQuery by remember { mutableStateOf("") }

    val filteredVideos = remember(videosState, searchQuery) {
        if (videosState is VideosUiState.Success) {
            if (searchQuery.isBlank()) {
                videosState.videos
            } else {
                videosState.videos.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
                }
            }
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Space-saving compact Side-by-Side Back Button + Search Bar Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(CardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .testTag("back_to_channels_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack, 
                    contentDescription = "חזרה",
                    tint = RoyalBlue
                )
            }
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("חפש שיעור בערוץ...", color = MutedText, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "חיפוש",
                        tint = MutedText,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "נקה",
                                tint = MutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = LightBlue,
                    unfocusedContainerColor = LightBlue,
                    focusedTextColor = DarkText,
                    unfocusedTextColor = DarkText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("channel_video_search_input")
            )
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
                if (filteredVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.SearchOff,
                                contentDescription = "אין תוצאות",
                                tint = MutedText,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("לא נמצאו שיעורים המתאימים לחיפוש שלך", color = MutedText, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredVideos) { video ->
                            VideoListItem(
                                video = video,
                                onClick = { onVideoClick(video) }
                            )
                        }
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
            .background(ScreenBg)
    ) {
        // Immersive full-bleed 16:9 Video Player at the top
        // Rendered flat directly without a clipping rounded Card to prevent the black screen composition bug!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            KosherVideoPlayer(
                videoId = video.id,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Details & controls column underneath
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Elegant title and channel row
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = video.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DarkText,
                        lineHeight = 21.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(LightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = video.channelName.take(1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldColor
                            )
                        }
                        Text(
                            text = "ערוץ: ${video.channelName} • צפייה כשרה ומסוננת",
                            fontSize = 11.sp,
                            color = MutedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Description card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "על השיעור",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = video.description.ifBlank { "שיעור תורה מאושר ומסונן היטב לצפייה מוגנת ובטוחה." },
                        fontSize = 12.sp,
                        color = MutedText,
                        lineHeight = 17.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Control Button: Back to list of videos
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("back_to_videos_button")
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

            Spacer(modifier = Modifier.weight(1f))

            // Safety badge footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldBg, RoundedCornerShape(8.dp))
                    .border(1.dp, GoldBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = "סינון מאובטח",
                    tint = GoldColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "נגן מסונן פעיל: מנוע החיפוש הכללי, התגובות, והמלצות הוידאו חסומים לחלוטין.",
                    fontSize = 10.sp,
                    color = GoldColor,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KosherVideoPlayer(videoId: String, modifier: Modifier = Modifier) {
    var isFullScreen by remember { mutableStateOf(false) }
    var customViewRef by remember { mutableStateOf<View?>(null) }
    var customCallbackRef by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val context = LocalContext.current

    if (isFullScreen) {
        BackHandler {
            val activity = context.findActivity()
            val customViewLocal = customViewRef
            if (activity != null && customViewLocal != null) {
                (activity.window.decorView as ViewGroup).removeView(customViewLocal)
                customViewRef = null
                customCallbackRef?.onCustomViewHidden()
                customCallbackRef = null
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                isFullScreen = false
            }
        }
    }

    key(videoId) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    settings.javaScriptEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.databaseEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: ""
                            val isMainFrame = request?.isForMainFrame ?: false
                            if (isMainFrame) {
                                return if (url.contains("youtube.com/embed/") || url.contains("youtube-nocookie.com/embed/")) {
                                    false
                                } else {
                                    true
                                }
                            }
                            return false
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            super.onShowCustomView(view, callback)
                            val activity = ctx.findActivity() ?: return
                            if (customViewRef != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            customViewRef = view
                            customCallbackRef = callback
                            isFullScreen = true
                            
                            activity.window.decorView.systemUiVisibility = (
                                View.SYSTEM_UI_FLAG_FULLSCREEN or
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            )
                            
                            (activity.window.decorView as ViewGroup).addView(
                                view,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }

                        override fun onHideCustomView() {
                            super.onHideCustomView()
                            val activity = ctx.findActivity() ?: return
                            val customViewLocal = customViewRef ?: return
                            
                            (activity.window.decorView as ViewGroup).removeView(customViewLocal)
                            customViewRef = null
                            customCallbackRef?.onCustomViewHidden()
                            customCallbackRef = null
                            isFullScreen = false
                            
                            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                        }
                    }
                    
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <style>
                                body, html {
                                    margin: 0;
                                    padding: 0;
                                    width: 100%;
                                    height: 100%;
                                    background-color: #0A0B10;
                                    overflow: hidden;
                                }
                                iframe {
                                    width: 100%;
                                    height: 100%;
                                    border: none;
                                }
                            </style>
                        </head>
                        <body>
                            <iframe 
                                id="player"
                                src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&modestbranding=1&rel=0&fs=1&showinfo=0&iv_load_policy=3&disablekb=0"
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                                allowfullscreen>
                            </iframe>
                        </body>
                        </html>
                    """.trimIndent()
                    
                    loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
                }
            },
            modifier = modifier
        )
    }
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
                        text = "ניתן להוסיף ערוץ בקלות! פשוט הדבק קישור לערוץ, רשום את שם המשתמש/הכינוי (למשל @Hidabroot_LTD) או הזן מזהה ערוץ ישיר (המתחיל ב-UC). האפליקציה תפענח ותמצא את הערוץ בצורה אוטומטית.",
                        fontSize = 12.sp,
                        color = MutedText,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = channelIdInput,
                        onValueChange = { viewModel.inputChannelId.value = it },
                        label = { Text("קישור לערוץ, שם משתמש (@) או מזהה ערוץ") },
                        placeholder = { Text("למשל @Hidabroot_LTD או קישור לערוץ...", color = MutedText) },
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
