import React, { useState } from 'react';
import { loginUser, registerUser } from '../services/api';
import { LogIn, UserPlus, Shield, Sparkles, AlertCircle } from 'lucide-react';
import './AuthModal.css';

export default function AuthModal({ onAuthSuccess }) {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [slowNotice, setSlowNotice] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSlowNotice(false);
    setLoading(true);

    // If request takes longer than 4 seconds, inform the user about Render cold start
    const slowTimer = setTimeout(() => {
      setSlowNotice(true);
    }, 4000);

    try {
      let data;
      if (isLogin) {
        data = await loginUser(username || email, password);
      } else {
        if (!username || !email || !password) {
          setError('Please fill in all fields.');
          setLoading(false);
          clearTimeout(slowTimer);
          return;
        }
        data = await registerUser(username, email, password);
      }
      if (data?.user) {
        onAuthSuccess(data.user);
      }
    } catch (err) {
      console.error('Auth error:', err);
      let msg = err.response?.data?.message || err.message || 'Authentication failed. Please try again.';
      if (err.message === 'Network Error' || !err.response) {
        msg = 'Cannot connect to server. If deployed on Vercel, ensure VITE_API_URL is set in Vercel settings and Render is awake.';
      }
      setError(msg);
    } finally {
      clearTimeout(slowTimer);
      setLoading(false);
      setSlowNotice(false);
    }
  };

  return (
    <div className="auth-overlay">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-brand-badge">
            <Shield className="brand-icon" size={24} />
            <Sparkles className="sparkle-icon" size={16} />
          </div>
          <h2 className="auth-title">Executive Boardroom</h2>
          <p className="auth-subtitle">Real-Time Collaborative Brainstorming Platform</p>
        </div>

        <div className="auth-tabs">
          <button
            type="button"
            className={`auth-tab ${isLogin ? 'active' : ''}`}
            onClick={() => { setIsLogin(true); setError(''); }}
          >
            <LogIn size={16} /> Sign In
          </button>
          <button
            type="button"
            className={`auth-tab ${!isLogin ? 'active' : ''}`}
            onClick={() => { setIsLogin(false); setError(''); }}
          >
            <UserPlus size={16} /> Create Account
          </button>
        </div>

        {error && (
          <div className="auth-error-banner">
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        {slowNotice && (
          <div className="auth-notice-banner">
            <span className="notice-dot"></span>
            <span>Connecting to backend... If the cloud server is waking up from sleep, this may take ~45–60s on the first request.</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          {!isLogin && (
            <div className="input-group">
              <label htmlFor="auth-username">Username</label>
              <input
                id="auth-username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. alex_chen"
                required
              />
            </div>
          )}

          <div className="input-group">
            <label htmlFor="auth-email">{isLogin ? 'Username or Email' : 'Work Email'}</label>
            <input
              id="auth-email"
              type={isLogin ? 'text' : 'email'}
              value={isLogin ? (username || email) : email}
              onChange={(e) => {
                if (isLogin) {
                  setUsername(e.target.value);
                  setEmail(e.target.value);
                } else {
                  setEmail(e.target.value);
                }
              }}
              placeholder={isLogin ? 'Enter username or email' : 'name@company.com'}
              required
            />
          </div>

          <div className="input-group">
            <label htmlFor="auth-password">Password</label>
            <input
              id="auth-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
              minLength={6}
            />
          </div>

          <button type="submit" className="auth-submit-btn" disabled={loading}>
            {loading ? (
              <span className="btn-spinner"></span>
            ) : isLogin ? (
              <>
                <LogIn size={18} /> Enter Boardroom
              </>
            ) : (
              <>
                <UserPlus size={18} /> Register & Join
              </>
            )}
          </button>
        </form>

        <div className="auth-footer-hint">
          <span>Protected with JWT Authentication & BCrypt Security</span>
        </div>
      </div>
    </div>
  );
}
