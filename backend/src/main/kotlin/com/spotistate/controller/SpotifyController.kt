package com.spotistate.controller

import com.spotistate.dto.ImportResponseDTO
import com.spotistate.dto.PlaylistDTO
import com.spotistate.dto.TrackDTO
import com.spotistate.service.SpotifyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/spotify")
class SpotifyController(private val spotifyService: SpotifyService) {

    @PostMapping("/import/{userId}")
    fun importPlaylists(@PathVariable userId: String): ResponseEntity<ImportResponseDTO> {
        val playlists = spotifyService.importUserLibrary(userId)
        return ResponseEntity.ok(ImportResponseDTO(true, playlists))
    }

    @GetMapping("/playlists/{userId}")
    fun getUserPlaylists(@PathVariable userId: String): ResponseEntity<List<PlaylistDTO>> {
        val playlists = spotifyService.getPlaylists(userId)
        return ResponseEntity.ok(playlists)
    }

    @GetMapping("/playlist/{playlistId}/tracks")
    fun getPlaylistTracks(@PathVariable playlistId: String): ResponseEntity<List<TrackDTO>> {
        val tracks = spotifyService.getPlaylistTracks(playlistId)
        return ResponseEntity.ok(tracks)
    }
}
