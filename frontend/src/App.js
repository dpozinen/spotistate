import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import './App.css';
import Home from './Home';
import Callback from './Callback';

function App() {
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [error, setError] = useState(null);
  const [errorFading, setErrorFading] = useState(false);

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

  const handleDismissError = () => {
    setErrorFading(true);
    setTimeout(() => {
      setError(null);
      setErrorFading(false);
    }, 500); // Wait for animation to complete
  };

  const loginPage = () => {
    const handleLogin = () => {
      console.log('Login button clicked');
      setIsLoggingIn(true);
      setError(null);
      
      // Directly redirect without pre-flight check to avoid CORS issues
      setTimeout(() => {
        console.log('Redirecting to:', "http://localhost:8080/api/auth/login");
        window.location.href = "http://localhost:8080/api/auth/login";
      }, 600);
    };

    return (
      <div className="App">
        <header className={`App-header ${isLoggingIn ? 'fade-out-up' : ''}`}>
          <h1>Spotistate</h1>
          <p>
            Import and store your Spotify library for reference.
          </p>
          <button 
            className="login-button" 
            onClick={handleLogin} 
            disabled={isLoggingIn}
          >
            Connect with Spotify
          </button>
        </header>
        
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
  };

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={loginPage()} />
        <Route path="/callback" element={<Callback onError={setError} />} />
        <Route path="/" element={<Home />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
