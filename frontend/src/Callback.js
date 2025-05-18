import React, { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './Callback.css';

function Callback({ onError }) {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    // Parse URL parameters
    const searchParams = new URLSearchParams(location.search);
    const accessToken = searchParams.get('accessToken');
    const userId = searchParams.get('userId');
    const error = searchParams.get('error');
    const errorDescription = searchParams.get('error_description');

    if (error) {
      // Handle authentication errors
      console.error('Authentication error:', error, errorDescription);
      const errorMessage = errorDescription || 
                          (error === 'access_denied' ? 'Access was denied. Please try again.' : 
                           'Authentication failed. Please try again.');
      if (onError) {
        onError(errorMessage);
      }
      navigate('/login');
    } else if (accessToken && userId) {
      // Store authentication details in localStorage
      localStorage.setItem('spotistate_token', accessToken);
      localStorage.setItem('spotistate_user_id', userId);
      
      // Redirect to homepage
      navigate('/');
    } else {
      // If authentication failed without specific error, redirect to login
      console.error('Authentication failed: Missing access token or user ID');
      if (onError) {
        onError('Authentication failed. Please try again.');
      }
      navigate('/login');
    }
  }, [location, navigate, onError]);

  return (
    <div className="callback-container">
      <div className="loading-spinner"></div>
      <p>Connecting to Spotify...</p>
    </div>
  );
}

export default Callback;
