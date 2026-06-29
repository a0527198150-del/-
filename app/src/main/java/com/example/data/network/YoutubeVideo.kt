package com.example.data.network

data class YoutubeVideo(
    val id: String,          // Video ID
    val title: String,       // Title of the video
    val channelId: String,   // Channel ID
    val channelName: String, // Channel Name
    val thumbnailUrl: String,// Image preview
    val description: String, // Description
    val publishedAt: String  // Publish date ISO/Text
)
