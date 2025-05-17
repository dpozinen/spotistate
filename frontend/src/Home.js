import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Home.css';
import PlaylistDetails from './PlaylistDetails';

function Home() {
  const [userName, setUserName] = useState('');
  const [playlists, setPlaylists] = useState([]);
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [imported, setImported] = useState(false);
  const [selectedPlaylist, setSelectedPlaylist] = useState(null);
  const [showPlaylistDetails, setShowPlaylistDetails] = useState(false);
  const [fadeOut, setFadeOut] = useState(false);
  const [error, setError] = useState(null);
  const [errorFading, setErrorFading] = useState(false);
  const navigate = useNavigate();

  // Auto-dismiss error after 5 seconds
  useEffect(() => {
    let errorTimer;
    if (error) {
      errorTimer = setTimeout(() => {
        setErrorFading(true);
        setTimeout(() => {
          setError(null);
          setErrorFading(false);
        }, 500); // Wait for animation to complete
      }, 5000);
    }
    return () => {
      clearTimeout(errorTimer);
    };
  }, [error]);

  useEffect(() => {
    // Check authentication
    const token = localStorage.getItem('spotistate_token');
    const userId = localStorage.getItem('spotistate_user_id');

    if (!token || !userId) {
      navigate('/login');
      return;
    }

    // Get user info
    fetch(`http://localhost:8080/api/auth/user-info?token=${token}&userId=${userId}`)
      .then(response => response.json())
      .then(data => {
        setUserName(data.user.displayName || 'Spotify User');
      })
      .catch(error => {
        console.error('Error fetching user info:', error);
        setError('Failed to fetch user information. Please try again.');
      });

    // Get playlists
    setLoading(true);
    fetch(`http://localhost:8080/api/spotify/playlists/${userId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
      .then(response => response.json())
      .then(data => {
        setPlaylists(data || []);
        setLoading(false);
        // Set imported to true if we have playlists
        if (data && data.length > 0) {
          setImported(true);
        }
      })
      .catch(error => {
        console.error('Error fetching playlists:', error);
        setLoading(false);
        setError('Failed to fetch playlists. Please try again.');
      });
  }, [navigate]);
  
  // Toggle body scrolling when playlist modal is shown
  useEffect(() => {
    if (showPlaylistDetails) {
      // Disable scrolling on body when modal is open
      document.body.style.overflow = 'hidden';
    } else {
      // Re-enable scrolling when modal is closed
      document.body.style.overflow = 'auto';
    }
    
    // Cleanup function to ensure scrolling is re-enabled when component unmounts
    return () => {
      document.body.style.overflow = 'auto';
    };
  }, [showPlaylistDetails]);

  const handleRefreshLibrary = () => {
    const token = localStorage.getItem('spotistate_token');
    const userId = localStorage.getItem('spotistate_user_id');

    // Start animation after delay
    setError(null);
    
    // Add 200ms delay before showing loading animation
    setTimeout(() => {
      setImporting(true);
      
      // Begin refresh operation
      fetch(`http://localhost:8080/api/spotify/import/${userId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
        .then(async response => {
          const data = await response.json();
          if (!response.ok) {
            throw new Error(data.message || `Server returned ${response.status}`);
          }
          return data;
        })
        .then(data => {
          setImporting(false);
          // Add a slight delay to make the animation visible
          setTimeout(() => {
            setPlaylists(data.playlists || []);
            // Force re-animation by updating a key
            const playlistsGrid = document.querySelector('.playlists-grid');
            if (playlistsGrid) {
              playlistsGrid.classList.remove('animate-playlists');
              void playlistsGrid.offsetWidth; // Trigger reflow
              playlistsGrid.classList.add('animate-playlists');
            }
            setImported(true);
          }, 300);
        })
        .catch(error => {
          console.error('Error refreshing library:', error);
          setImporting(false);
          setError(error.message || 'Failed to refresh library. Please try again.');
        });
    }, 200);
  };

  const handleImport = () => {
    const token = localStorage.getItem('spotistate_token');
    const userId = localStorage.getItem('spotistate_user_id');

    setFadeOut(true);
    setError(null);
    
    setTimeout(() => {
      setFadeOut(false);
      
      // Add 200ms delay before showing loading animation
      setTimeout(() => {
        setImporting(true);
        
        fetch(`http://localhost:8080/api/spotify/import/${userId}`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
          .then(async response => {
            const data = await response.json();
            if (!response.ok) {
              throw new Error(data.message || `Server returned ${response.status}`);
            }
            return data;
          })
          .then(data => {
            setPlaylists(data.playlists || []);
            setImporting(false);
            setImported(true);
          })
          .catch(error => {
            console.error('Error importing playlists:', error);
            setImporting(false);
            setError(error.message || 'Failed to import playlists. Please try again.');
          });
      }, 200); // 200ms delay before showing loading animation
    }, 300); // Wait for fade out to complete
  };

  const handlePlaylistClick = (playlist) => {
    setSelectedPlaylist(playlist);
    setShowPlaylistDetails(true);
  };

  const handleBackToPlaylists = () => {
    setShowPlaylistDetails(false);
  };

  const handleLogout = () => {
    // Add fadeout animation
    document.querySelector('.home-header').classList.add('fade-out-down');
    document.querySelector('main').classList.add('fade-out-down');
    
    // Wait for animation to complete before redirecting
    setTimeout(() => {
      localStorage.removeItem('spotistate_token');
      localStorage.removeItem('spotistate_user_id');
      navigate('/login');
    }, 600);
  };

  const handleDismissError = () => {
    setErrorFading(true);
    setTimeout(() => {
      setError(null);
      setErrorFading(false);
    }, 500); // Wait for animation to complete
  };

  const renderMainContent = () => {
    if (importing) {
      return (
        <div className="center-content">
          <div className="import-button loading">
            <div className="spinner">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
          <div className="empty-state">
            <p>Importing... this may take some time</p>
          </div>
        </div>
      );
    }
    
    if (loading) {
      return <div className="loading-playlists">Loading your playlists...</div>;
    }
    
    if (playlists.length > 0) {
      return (
        <div className="playlists-grid">
          {playlists.map(playlist => (
            <div 
              key={playlist.id} 
              className="playlist-card"
              onClick={() => handlePlaylistClick(playlist)}
            >
              <img 
                src={playlist.imageUrl || 'https://via.placeholder.com/300'} 
                alt={playlist.name} 
                className="playlist-image"
              />
              <div className="playlist-info">
                <h3>{playlist.name}</h3>
                <p>{playlist.trackCount} tracks</p>
              </div>
            </div>
          ))}
        </div>
      );
    }
    
    return (
      <div className="center-content">
        {!imported && (
          <button 
            className={`import-button ${importing ? 'loading' : ''} ${fadeOut ? 'fade-out' : ''}`} 
            onClick={handleImport}
            disabled={importing || fadeOut}
          >
            {importing ? (
              <div className="spinner">
                <span></span>
                <span></span>
                <span></span>
              </div>
            ) : 'Import from Spotify'}
          </button>
        )}
        <div className={`empty-state ${fadeOut ? 'fade-out' : ''}`}>
          <p>{importing ? 'Importing... this may take some time' : 'No playlists found. Import your playlists from Spotify to get started.'}</p>
        </div>
      </div>
    );
  };

  return (
    <div className="home-container">
      <header className="home-header">
        <h1>Spotistate</h1>
        <div className="user-info">
          <span>Welcome, {userName}</span>
          <button className="refresh-button" onClick={handleRefreshLibrary} disabled={importing || loading}>Refresh</button>
          <button className="logout-button" onClick={handleLogout}>Logout</button>
        </div>
      </header>

      <main>
        {renderMainContent()}
      </main>

      {showPlaylistDetails && selectedPlaylist && (
        <PlaylistDetails 
          playlist={selectedPlaylist} 
          onBack={handleBackToPlaylists} 
        />
      )}

      {error && (
        <div className={`error-inline ${errorFading ? 'fade-out' : ''}`}>
          <div className="error-content">
            <div className="error-icon">⚠</div>
            <p>{error}</p>
            <button onClick={handleDismissError}>Dismiss</button>
          </div>
        </div>
      )}
    </div>
  );
}

export default Home;
