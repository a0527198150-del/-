package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApiService {

    @GET("channels")
    suspend fun getChannelByHandle(
        @Query("part") part: String = "id,snippet",
        @Query("forHandle") handle: String,
        @Query("key") apiKey: String
    ): YtChannelListResponse

    @GET("channels")
    suspend fun getChannelById(
        @Query("part") part: String = "id,snippet",
        @Query("id") channelId: String,
        @Query("key") apiKey: String
    ): YtChannelListResponse

    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("part") part: String = "snippet",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 25,
        @Query("key") apiKey: String
    ): YtPlaylistItemListResponse

    @GET("search")
    suspend fun searchChannel(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "channel",
        @Query("maxResults") maxResults: Int = 1,
        @Query("key") apiKey: String
    ): YtSearchListResponse

    companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
    }
}
