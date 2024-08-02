/*
 * Copyright (c) 2023 Auxio Project
 * TextTagsTest.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.oxycblt.auxio.music.metadata

import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.flac.PictureFrame
import androidx.media3.extractor.metadata.id3.ApicFrame
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextTagsTest {
    @Test
    fun textTags_vorbis() {
        val textTags = TextTags(VORBIS_METADATA)
        assertTrue(textTags.id3v2.isEmpty())
        assertEquals(listOf("Wheel"), textTags.vorbis["title"])
        assertEquals(listOf("Paraglow"), textTags.vorbis["album"])
        assertEquals(listOf("Parannoul", "Asian Glow"), textTags.vorbis["artist"])
        assertEquals(listOf("2022"), textTags.vorbis["date"])
        assertEquals(listOf("ep"), textTags.vorbis["releasetype"])
        assertEquals(listOf("+2 dB"), textTags.vorbis["replaygain_track_gain"])
        assertEquals(null, textTags.id3v2["APIC"])
    }

    @Test
    fun textTags_id3v2() {
        val textTags = TextTags(ID3V2_METADATA)
        assertTrue(textTags.vorbis.isEmpty())
        assertEquals(listOf("Wheel"), textTags.id3v2["TIT2"])
        assertEquals(listOf("Paraglow"), textTags.id3v2["TALB"])
        assertEquals(listOf("Parannoul", "Asian Glow"), textTags.id3v2["TPE1"])
        assertEquals(listOf("2022"), textTags.id3v2["TDRC"])
        assertEquals(listOf("ep"), textTags.id3v2["TXXX:musicbrainz album type"])
        assertEquals(listOf("+2 dB"), textTags.id3v2["TXXX:replaygain_track_gain"])
        assertEquals(null, textTags.id3v2["metadata_block_picture"])
    }

    @Test
    fun textTags_mp4() {
        val textTags = TextTags(MP4_METADATA)
        assertTrue(textTags.vorbis.isEmpty())
        assertEquals(listOf("Wheel"), textTags.id3v2["TIT2"])
        assertEquals(listOf("Paraglow"), textTags.id3v2["TALB"])
        assertEquals(listOf("Parannoul", "Asian Glow"), textTags.id3v2["TPE1"])
        assertEquals(listOf("2022"), textTags.id3v2["TDRC"])
        assertEquals(listOf("ep"), textTags.id3v2["TXXX:musicbrainz album type"])
        assertEquals(listOf("+2 dB"), textTags.id3v2["TXXX:replaygain_track_gain"])
        assertEquals(null, textTags.id3v2["metadata_block_picture"])
    }

    @Test
    fun textTags_id3v2_vorbis_combined() {
        val textTags = TextTags(VORBIS_METADATA.copyWithAppendedEntriesFrom(ID3V2_METADATA))
        assertEquals(listOf("Wheel"), textTags.vorbis["title"])
        assertEquals(listOf("Paraglow"), textTags.vorbis["album"])
        assertEquals(listOf("Parannoul", "Asian Glow"), textTags.vorbis["artist"])
        assertEquals(listOf("2022"), textTags.vorbis["date"])
        assertEquals(listOf("ep"), textTags.vorbis["releasetype"])
        assertEquals(listOf("+2 dB"), textTags.vorbis["replaygain_track_gain"])
        assertEquals(null, textTags.id3v2["metadata_block_picture"])

        assertEquals(listOf("Wheel"), textTags.id3v2["TIT2"])
        assertEquals(listOf("Paraglow"), textTags.id3v2["TALB"])
        assertEquals(listOf("Parannoul", "Asian Glow"), textTags.id3v2["TPE1"])
        assertEquals(listOf("2022"), textTags.id3v2["TDRC"])
        assertEquals(null, textTags.id3v2["APIC"])
        assertEquals(listOf("ep"), textTags.id3v2["TXXX:musicbrainz album type"])
        assertEquals(listOf("+2 dB"), textTags.id3v2["TXXX:replaygain_track_gain"])
    }

    companion object {
        private val VORBIS_METADATA =
            Metadata(
                VorbisComment("TITLE", "Wheel"),
                VorbisComment("ALBUM", "Paraglow"),
                VorbisComment("ARTIST", "Parannoul"),
                VorbisComment("ARTIST", "Asian Glow"),
                VorbisComment("DATE", "2022"),
                VorbisComment("RELEASETYPE", "ep"),
                VorbisComment("METADATA_BLOCK_PICTURE", ""),
                VorbisComment("REPLAYGAIN_TRACK_GAIN", "+2 dB"),
                PictureFrame(0, "", "", 0, 0, 0, 0, byteArrayOf()))

        private val ID3V2_METADATA =
            Metadata(
                TextInformationFrame("TIT2", null, listOf("Wheel")),
                TextInformationFrame("TALB", null, listOf("Paraglow")),
                TextInformationFrame("TPE1", null, listOf("Parannoul", "Asian Glow")),
                TextInformationFrame("TDRC", null, listOf("2022")),
                TextInformationFrame("TXXX", "MusicBrainz Album Type", listOf("ep")),
                TextInformationFrame("TXXX", "replaygain_track_gain", listOf("+2 dB")),
                ApicFrame("", "", 0, byteArrayOf()))

        // MP4 atoms are mapped to ID3v2 text information frames by ExoPlayer, but can
        // duplicate frames and have ---- mapped to InternalFrame.
        private val MP4_METADATA =
            Metadata(
                TextInformationFrame("TIT2", null, listOf("Wheel")),
                TextInformationFrame("TALB", null, listOf("Paraglow")),
                TextInformationFrame("TPE1", null, listOf("Parannoul")),
                TextInformationFrame("TPE1", null, listOf("Asian Glow")),
                TextInformationFrame("TDRC", null, listOf("2022")),
                TextInformationFrame("TXXX", "MusicBrainz Album Type", listOf("ep")),
                InternalFrame("com.apple.iTunes", "replaygain_track_gain", "+2 dB"),
                ApicFrame("", "", 0, byteArrayOf()))
    }
}
