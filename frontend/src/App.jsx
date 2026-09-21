import React, { useState, useEffect } from 'react';
import AuthModal from './components/AuthModal';
import Dashboard from './components/Dashboard';
import BoardroomView from './components/BoardroomView';
import { getStoredUser, getMe, logoutUser } from './services/api';
import './App.css';

export default function App() {
  const [currentUser, setCurrentUser] = useState(getStoredUser());
  const [activeRoom, setActiveRoom] = useState(null);
  const [isCheckingAuth, setIsCheckingAuth] = useState(true);

  // Validate stored JWT on mount
  useEffect(() => {
    const checkAuth = async () => {
      const token = localStorage.getItem('boardroom_jwt');
      if (token) {
        try {
          const user = await getMe();
          setCurrentUser(user);
        } catch (err) {
          console.warn('Session expired or invalid, logging out');
          logoutUser();
          setCurrentUser(null);
        }
      }
      setIsCheckingAuth(false);
    };

    checkAuth();
  }, []);

  const handleAuthSuccess = (user) => {
    setCurrentUser(user);
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setActiveRoom(null);
  };

  const handleEnterRoom = (room) => {
    setActiveRoom(room);
  };

  const handleLeaveRoom = () => {
    setActiveRoom(null);
  };

  if (isCheckingAuth) {
    return (
      <div className="app-loading-screen">
        <div className="loading-spinner"></div>
        <p>Connecting to Executive Boardroom...</p>
      </div>
    );
  }

  return (
    <div className="app-root">
      {!currentUser && (
        <AuthModal onAuthSuccess={handleAuthSuccess} />
      )}

      {currentUser && !activeRoom && (
        <Dashboard
          currentUser={currentUser}
          onEnterRoom={handleEnterRoom}
          onLogout={handleLogout}
        />
      )}

      {currentUser && activeRoom && (
        <BoardroomView
          room={activeRoom}
          currentUser={currentUser}
          onLeaveRoom={handleLeaveRoom}
        />
      )}
    </div>
  );
}