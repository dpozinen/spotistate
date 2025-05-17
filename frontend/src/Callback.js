import React, { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './Callback.css';

function Callback() {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    // Parse URL parameters
    const searchParams = new URLSearchParams(location.search);
    const accessToken = searchParams.get('accessToken');
    const userId = searchParams.get('userId');

    if (accessToken && userId) {
      // Store authentication details in localStorage
      localStorage.setItem('spotistate_token', accessToken);
      localStorage.setItem('spotistate_user_id', userId);
      
      // Redirect to homepage
      navigate('/');
    } else {
      // If authentication failed, redirect to login
      navigate('/login');
    }
  }, [location, navigate]);

  return (
    <div className="callback-container">
      <div className="loading-spinner"></div>
      <p>Connecting to Spotify...</p>
    </div>
  );
}

export default Callback;
