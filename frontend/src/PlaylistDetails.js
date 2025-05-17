import React, { useState, useEffect } from 'react';
import './PlaylistDetails.css';

function PlaylistDetails({ playlist, onBack }) {
  const [tracks, setTracks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sortBy, setSortBy] = useState('default');
  const [sortDirection, setSortDirection] = useState('asc');
  const [searchTerm, setSearchTerm] = useState('');
  const [isClosing, setIsClosing] = useState(false);

  useEffect(() => {
    if (!playlist) return;
    
    const token = localStorage.getItem('spotistate_token');
    
    setLoading(true);
    fetch(`http://localhost:8080/api/spotify/playlist/${playlist.id}/tracks`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
      .then(response => response.json())
      .then(data => {
        setTracks(data || []);
        setLoading(false);
      })
      .catch(error => {
        console.error('Error fetching tracks:', error);
        setLoading(false);
      });
      
    // Add escape key listener
    const handleEscapeKey = (e) => {
      if (e.key === 'Escape') {
        handleClose();
      }
    };
    
    document.addEventListener('keydown', handleEscapeKey);
    
    // Clean up listener
    return () => {
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, [playlist]);

  const formatDuration = (ms) => {
    const minutes = Math.floor(ms / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const handleSort = (column) => {
    if (sortBy === column) {
      // If already sorting by this column, toggle direction
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      // New column, set it as sort column with ascending direction
      setSortBy(column);
      setSortDirection('asc');
    }
  };

  const handleSearch = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleClose = () => {
    setIsClosing(true);
    setTimeout(() => {
      onBack();
    }, 300); // Match the CSS animation duration
  };

  const getSortIcon = (column) => {
    if (sortBy !== column) return null;
    return sortDirection === 'asc' ? '↑' : '↓';
  };

  const sortedAndFilteredTracks = tracks
    .filter(track => {
      if (!searchTerm) return true;
      const term = searchTerm.toLowerCase();
      return (
        track.name.toLowerCase().includes(term) ||
        track.artist.toLowerCase().includes(term)
      );
    })
    .sort((a, b) => {
      let comparison = 0;
      const direction = sortDirection === 'asc' ? 1 : -1;
      
      switch (sortBy) {
        case 'name':
          comparison = a.name.localeCompare(b.name);
          break;
        case 'artist':
          comparison = a.artist.localeCompare(b.artist);
          break;
        case 'album':
          comparison = a.album.localeCompare(b.album);
          break;
        case 'duration':
          comparison = a.durationMs - b.durationMs;
          break;
        case 'added':
          if (!a.addedAt || !b.addedAt) return 0;
          comparison = new Date(a.addedAt) - new Date(b.addedAt);
          break;
        default:
          return 0;
      }
      
      return comparison * direction;
    });

  return (
    <div 
      className={`playlist-details-overlay ${isClosing ? 'closing' : ''}`} 
      onClick={handleClose}
    >
      <div 
        className={`playlist-details ${isClosing ? 'closing' : ''}`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="details-header">
          <div className="back-button-container">
            <button className="back-button" onClick={handleClose} aria-label="Back to playlists">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z" />
              </svg>
            </button>
          </div>
          
          <div className="playlist-header-content">
            <div className="playlist-info-header">
              <img 
                src={playlist.imageUrl || 'https://via.placeholder.com/100'} 
                alt={playlist.name} 
                className="playlist-details-image"
              />
              <div className="playlist-text-info">
                <h2>{playlist.name}</h2>
                <p>{playlist.trackCount} tracks</p>
                <a href={playlist.spotifyUrl} target="_blank" rel="noopener noreferrer" className="spotify-link">
                  Open in Spotify
                </a>
              </div>
            </div>
            <div className="search-box">
              <input 
                type="text" 
                placeholder="Search by track or artist..." 
                value={searchTerm}
                onChange={handleSearch}
              />
            </div>
          </div>
        </div>
        
        {loading ? (
          <div className="loading">Loading tracks...</div>
        ) : (
          <div className="tracks-list">
            <div className="track-header">
              <div 
                className={`track-name sortable ${sortBy === 'name' ? 'active' : ''}`}
                onClick={() => handleSort('name')}
              >
                Name {getSortIcon('name')}
              </div>
              <div 
                className={`track-artist sortable ${sortBy === 'artist' ? 'active' : ''}`}
                onClick={() => handleSort('artist')}
              >
                Artist {getSortIcon('artist')}
              </div>
              <div 
                className={`track-album sortable ${sortBy === 'album' ? 'active' : ''}`}
                onClick={() => handleSort('album')}
              >
                Album {getSortIcon('album')}
              </div>
              <div 
                className={`track-duration sortable ${sortBy === 'duration' ? 'active' : ''}`}
                onClick={() => handleSort('duration')}
              >
                Duration {getSortIcon('duration')}
              </div>
            </div>
            
            {sortedAndFilteredTracks.length > 0 ? (
              sortedAndFilteredTracks.map((track) => (
                <div key={track.id} className="track-item">
                  <div className="track-name">
                    <a href={track.spotifyUrl} target="_blank" rel="noopener noreferrer">
                      {track.name}
                    </a>
                  </div>
                  <div className="track-artist">{track.artist}</div>
                  <div className="track-album">{track.album}</div>
                  <div className="track-duration">{formatDuration(track.durationMs)}</div>
                </div>
              ))
            ) : (
              <div className="no-tracks">
                {searchTerm ? 'No tracks match your search.' : 'No tracks found in this playlist.'}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default PlaylistDetails;
