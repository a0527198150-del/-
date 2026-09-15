package com.example

import com.example.data.network.YoutubeTimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeTimeFormatTest {

  @Test
  fun `parses iso durations`() {
    assertEquals(3730, YoutubeTimeFormat.parseIsoDuration("PT1H2M10S"))
    assertEquals(750, YoutubeTimeFormat.parseIsoDuration("PT12M30S"))
    assertEquals(45, YoutubeTimeFormat.parseIsoDuration("PT45S"))
    assertEquals(0, YoutubeTimeFormat.parseIsoDuration("not-a-duration"))
  }

  @Test
  fun `formats a clock`() {
    assertEquals("1:02:10", YoutubeTimeFormat.formatClock(3730))
    assertEquals("12:30", YoutubeTimeFormat.formatClock(750))
    assertEquals("0:05", YoutubeTimeFormat.formatClock(5))
  }

  @Test
  fun `parses iso timestamps, honouring the offset`() {
    val utc = YoutubeTimeFormat.parseIsoToMillis("2024-03-11T08:15:00Z")
    val plusTwo = YoutubeTimeFormat.parseIsoToMillis("2024-03-11T10:15:00+02:00")
    assertEquals(utc, plusTwo)
    assertEquals(0L, YoutubeTimeFormat.parseIsoToMillis(""))
  }
}
