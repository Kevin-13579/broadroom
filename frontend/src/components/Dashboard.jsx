import React, { useState } from 'react';
import { createRoom, joinRoom, logoutUser } from '../services/api';
import { Users, PlusCircle, ArrowRightCircle, LogOut, Key, Hash, HelpCircle, Sparkles, ShieldCheck } from 'lucide-react';
import './Dashboard.css';

export default function Dashboard({ currentUser, onEnterRoom, onLogout }) {
  const [showHostModal, setShowHostModal] = useState(false);
  const [showJoinModal, setShowJoinModal] = useState(false);

  // Host form state
  const [topic, setTopic] = useState('');
  const [hostPassword, setHostPassword] = useState('');
  const [maxMembers, setMaxMembers] = useState(6);

  // Join form state
  const [roomCode, setRoomCode] = useState('');
  const [joinPassword, setJoinPassword] = useState('');

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleCreateRoom = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const room = await createRoom(topic, hostPassword, maxMembers);
      setShowHostModal(false);
      onEnterRoom(room);
    } catch (err) {
      console.error('Create room error:', err);
      const msg = err.response?.data?.message || err.message || 'Failed to create room';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleJoinRoom = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const room = await joinRoom(roomCode, joinPassword);
      setShowJoinModal(false);
      onEnterRoom(room);
    } catch (err) {
      console.error('Join room error:', err);
      const msg = err.response?.data?.message || err.message || 'Failed to join room';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="dashboard-container">
      {/* Top Navigation */}
      <header className="dashboard-header">
        <div className="header-brand">
          <div className="brand-badge">
            <Users size={20} />
          </div>
          <div>
            <h1 className="brand-name">Executive Boardroom</h1>
            <p className="brand-tagline">Real-Time Collaborative Brainstorming & Turn Bidding</p>
          </div>
        </div>

        <div className="header-user-panel">
          <div className="user-avatar">
            {currentUser?.username ? currentUser.username.charAt(0).toUpperCase() : 'U'}
          </div>
          <div className="user-details">
            <span className="user-name">{currentUser?.username || 'Collaborator'}</span>
            <span className="user-email">{currentUser?.email}</span>
          </div>
          <button
            className="logout-btn"
            onClick={() => {
              logoutUser();
              onLogout();
            }}
            title="Log Out"
          >
            <LogOut size={16} />
            <span>Logout</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="dashboard-main">
        <div className="welcome-hero">
          <div className="hero-pill">
            <Sparkles size={14} />
            <span>Live Turn Execution Engine & AI SWOT Generation</span>
          </div>
          <h2>Collaborative Executive Brainstorming</h2>
          <p>
            Host a structured strategy session with live 45-second turn bidding, or join an active room with a 6-character code.
          </p>
        </div>

        {error && (
          <div className="dashboard-error-banner">
            <span>{error}</span>
          </div>
        )}

        <div className="action-cards-grid">
          {/* Host Card */}
          <div className="action-card host-card" onClick={() => { setShowHostModal(true); setError(''); }}>
            <div className="card-icon-wrapper host-icon-bg">
              <PlusCircle size={28} />
            </div>
            <h3>Host Session</h3>
            <p>Initiate a new boardroom brainstorming room with a topic, private password, and participant cap (3-10 members).</p>
            <button className="card-action-btn host-btn">
              <span>Create New Room</span>
              <ArrowRightCircle size={18} />
            </button>
          </div>

          {/* Join Card */}
          <div className="action-card join-card" onClick={() => { setShowJoinModal(true); setError(''); }}>
            <div className="card-icon-wrapper join-icon-bg">
              <Key size={28} />
            </div>
            <h3>Join Session</h3>
            <p>Enter an existing boardroom using the 6-character room code provided by your host along with the room password.</p>
            <button className="card-action-btn join-btn">
              <span>Join with Code</span>
              <ArrowRightCircle size={18} />
            </button>
          </div>
        </div>

        {/* Feature Highlights */}
        <div className="features-strip">
          <div className="feature-item">
            <ShieldCheck size={20} className="feature-icon" />
            <div>
              <h4>Structured Bidding</h4>
              <p>Thread-safe FIFO queue ensures fair, democratic speaking opportunities.</p>
            </div>
          </div>
          <div className="feature-item">
            <Users size={20} className="feature-icon" />
            <div>
              <h4>45s Precision Timer</h4>
              <p>Enforces concise, punchy contributions with server-timed turn transitions.</p>
            </div>
          </div>
          <div className="feature-item">
            <Sparkles size={20} className="feature-icon" />
            <div>
              <h4>Gemini SWOT Synthesis</h4>
              <p>Automated post-meeting quadrant report synthesized from full transcript.</p>
            </div>
          </div>
        </div>
      </main>

      {/* Host Session Modal */}
      {showHostModal && (
        <div className="modal-backdrop" onClick={() => setShowHostModal(false)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-icon-badge">
                <PlusCircle size={22} />
              </div>
              <div>
                <h3>Host a Boardroom Session</h3>
                <p>Configure meeting agenda and participant parameters</p>
              </div>
            </div>

            {error && <div className="modal-error">{error}</div>}

            <form onSubmit={handleCreateRoom} className="modal-form">
              <div className="form-group">
                <label>Meeting Topic / Strategic Agenda</label>
                <input
                  type="text"
                  value={topic}
                  onChange={(e) => setTopic(e.target.value)}
                  placeholder="e.g. Q3 Cloud Architecture Scaling & Cost Strategy"
                  required
                />
              </div>

              <div className="form-group">
                <label>Room Password (for participants to join)</label>
                <input
                  type="password"
                  value={hostPassword}
                  onChange={(e) => setHostPassword(e.target.value)}
                  placeholder="Enter secret room password"
                  required
                />
              </div>

              <div className="form-group">
                <div className="label-with-val">
                  <label>Max Participants (3–10)</label>
                  <span className="slider-val-badge">{maxMembers} members</span>
                </div>
                <input
                  type="range"
                  min="3"
                  max="10"
                  value={maxMembers}
                  onChange={(e) => setMaxMembers(Number(e.target.value))}
                  className="custom-range"
                />
                <div className="range-bounds">
                  <span>3</span>
                  <span>6</span>
                  <span>10</span>
                </div>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-cancel" onClick={() => setShowHostModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-confirm-host" disabled={loading}>
                  {loading ? 'Creating Room...' : 'Launch Boardroom'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Join Session Modal */}
      {showJoinModal && (
        <div className="modal-backdrop" onClick={() => setShowJoinModal(false)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-icon-badge join-badge">
                <Key size={22} />
              </div>
              <div>
                <h3>Join Boardroom Session</h3>
                <p>Enter the 6-character room code and room password</p>
              </div>
            </div>

            {error && <div className="modal-error">{error}</div>}

            <form onSubmit={handleJoinRoom} className="modal-form">
              <div className="form-group">
                <label>6-Character Room Code</label>
                <div className="input-with-icon">
                  <Hash size={18} className="inner-icon" />
                  <input
                    type="text"
                    maxLength={6}
                    value={roomCode}
                    onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
                    placeholder="e.g. A8X9K2"
                    className="room-code-input"
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Room Password</label>
                <input
                  type="password"
                  value={joinPassword}
                  onChange={(e) => setJoinPassword(e.target.value)}
                  placeholder="Enter room password"
                  required
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-cancel" onClick={() => setShowJoinModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-confirm-join" disabled={loading}>
                  {loading ? 'Connecting...' : 'Enter Boardroom'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
