import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import './App.css';
import Home from './Home';
import Callback from './Callback';

function App() {
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  const loginPage = () => {
    const handleLogin = () => {
      setIsLoggingIn(true);
      setTimeout(() => {
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
      </div>
    );
  };

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={loginPage()} />
        <Route path="/callback" element={<Callback />} />
        <Route path="/" element={<Home />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
