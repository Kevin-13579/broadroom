import React, { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { 
  API_BASE_URL,
  getRoom, 
  getRoomMembers, 
  getRoomMessages, 
  getTurnState, 
  startRoomSession, 
  endRoomSession,
  getRoomSwot
} from '../services/api';
import SwotReportModal from './SwotReportModal';
import { 
  Users, 
  Clock, 
  Mic, 
  Send, 
  Play, 
  Sparkles, 
  Copy, 
  Check, 
  Shield, 
  Crown, 
  AlertCircle, 
  ArrowLeft,
  Volume2,
  CheckCircle2
} from 'lucide-react';
import './BoardroomView.css';

export default function BoardroomView({ room: initialRoom, currentUser, onLeaveRoom }) {
  const [room, setRoom] = useState(initialRoom);
  const [members, setMembers] = useState([]);
  const [messages, setMessages] = useState([]);
  
  // Turn and Queue state
  const [turnState, setTurnState] = useState({
    activeUserId: null,
    username: null,
    expiresAt: null,
    queue: [],
    eventType: 'FLOOR_OPEN'
  });

  // Local active timer seconds
  const [timeLeft, setTimeLeft] = useState(0);

  // Message input state
  const [messageInput, setMessageInput] = useState('');
  const [copiedCode, setCopiedCode] = useState(false);
  const [loadingAction, setLoadingAction] = useState(false);
  const [actionError, setActionError] = useState('');

  // SWOT report state
  const [swotReport, setSwotReport] = useState(null);
  const [isGeneratingSwot, setIsGeneratingSwot] = useState(false);
  const [showEndConfirmModal, setShowEndConfirmModal] = useState(false);

  const stompClientRef = useRef(null);
  const chatBottomRef = useRef(null);

  const isHost = room?.host?.id === currentUser?.id;
  const isMyTurn = turnState.activeUserId === currentUser?.id;
  const isInQueue = turnState.queue?.some(q => q.userId === currentUser?.id);
  const canBid = room?.status === 'ACTIVE' && !isMyTurn && !isInQueue;

  // Fetch initial room data and members
  useEffect(() => {
    let isMounted = true;

    const fetchInitialData = async () => {
      try {
        const [roomData, membersData, messagesData, turnData] = await Promise.all([
          getRoom(initialRoom.id),
          getRoomMembers(initialRoom.id),
          getRoomMessages(initialRoom.id),
          getTurnState(initialRoom.id).catch(() => null)
        ]);

        if (isMounted) {
          setRoom(roomData);
          setMembers(membersData);
          setMessages(messagesData || []);
          if (turnData) {
            setTurnState(turnData);
          }
        }
      } catch (err) {
        console.error('Failed to load initial boardroom data:', err);
      }
    };

    fetchInitialData();

    // Poll member list every 5 seconds as backup
    const memberInterval = setInterval(async () => {
      try {
        const m = await getRoomMembers(initialRoom.id);
        if (isMounted) setMembers(m);
      } catch (e) {}
    }, 5000);

    return () => {
      isMounted = false;
      clearInterval(memberInterval);
    };
  }, [initialRoom.id]);

  // Establish STOMP WebSocket connection
  useEffect(() => {
    const socket = new SockJS(`${API_BASE_URL}/ws-boardroom`);
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('Connected to Boardroom STOMP WebSocket broker');

        // 1. Subscribe to Turn & Queue events
        client.subscribe(`/topic/room/${initialRoom.id}/turn`, (message) => {
          try {
            const turnPayload = JSON.parse(message.body);
            setTurnState(turnPayload);
          } catch (err) {
            console.error('Error parsing turn event:', err);
          }
        });

        // 2. Subscribe to Message broadcasts
        client.subscribe(`/topic/room/${initialRoom.id}/messages`, (message) => {
          try {
            const newMsg = JSON.parse(message.body);
            setMessages((prev) => [...prev, newMsg]);
          } catch (err) {
            console.error('Error parsing new message:', err);
          }
        });

        // 3. Subscribe to SWOT completion
        client.subscribe(`/topic/room/${initialRoom.id}/swot`, (message) => {
          try {
            const swot = JSON.parse(message.body);
            setSwotReport(swot);
            setIsGeneratingSwot(false);
            setRoom((prev) => ({ ...prev, status: 'ENDED' }));
          } catch (err) {
            console.error('Error parsing SWOT event:', err);
          }
        });

        // Request initial turn sync
        client.publish({
          destination: `/app/room/${initialRoom.id}/sync`,
          body: JSON.stringify({})
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message'], frame.body);
      }
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };
  }, [initialRoom.id]);

  // 45-second timer countdown ticker
  useEffect(() => {
    if (!turnState.expiresAt || turnState.expiresAt <= 0 || !turnState.activeUserId) {
      setTimeLeft(0);
      return;
    }

    const updateTimer = () => {
      const now = Date.now();
      const diff = Math.max(0, Math.ceil((turnState.expiresAt - now) / 1000));
      setTimeLeft(diff);
    };

    updateTimer();
    const interval = setInterval(updateTimer, 500);

    return () => clearInterval(interval);
  }, [turnState.expiresAt, turnState.activeUserId]);

  // Auto-scroll chat to bottom
  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Action: Bid to speak
  const handleBidToSpeak = () => {
    if (!canBid || !stompClientRef.current?.connected) return;
    stompClientRef.current.publish({
      destination: `/app/room/${room.id}/bid`,
      body: JSON.stringify({ userId: currentUser.id })
    });
  };

  // Action: Send message (Active Speaker only)
  const handleSendMessage = (e) => {
    e.preventDefault();
    if (!isMyTurn || !messageInput.trim() || !stompClientRef.current?.connected) return;

    stompClientRef.current.publish({
      destination: `/app/room/${room.id}/speak`,
      body: JSON.stringify({
        userId: currentUser.id,
        content: messageInput.trim()
      })
    });

    setMessageInput('');
  };

  // Host Action: Start Session
  const handleStartSession = async () => {
    setLoadingAction(true);
    setActionError('');
    try {
      const updated = await startRoomSession(room.id);
      setRoom(updated);
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || 'Failed to start session');
    } finally {
      setLoadingAction(false);
    }
  };

  // Host Action: Initiate End Session
  const handleEndSessionClick = () => {
    setShowEndConfirmModal(true);
  };

  // Host Action: Confirmed End Session & Generate SWOT
  const handleConfirmEndSession = async () => {
    setShowEndConfirmModal(false);
    setIsGeneratingSwot(true);
    setLoadingAction(true);
    setActionError('');
    try {
      const swot = await endRoomSession(room.id);
      setSwotReport(swot);
      setRoom((prev) => ({ ...prev, status: 'ENDED' }));
    } catch (err) {
      console.error('Failed to generate SWOT:', err);
      setActionError(err.response?.data?.message || err.message || 'Failed to generate SWOT analysis');
    } finally {
      setIsGeneratingSwot(false);
      setLoadingAction(false);
    }
  };

  // View existing SWOT
  const handleViewSwot = async () => {
    if (swotReport) return;
    setLoadingAction(true);
    try {
      const swot = await getRoomSwot(room.id);
      setSwotReport(swot);
    } catch (err) {
      setActionError(err.response?.data?.message || 'SWOT report not ready yet');
    } finally {
      setLoadingAction(false);
    }
  };

  const copyRoomCode = () => {
    navigator.clipboard.writeText(room.roomCode);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
  };

  // Timer color calculation
  const timerPercentage = Math.min(100, Math.max(0, (timeLeft / 45) * 100));
  const getTimerColor = () => {
    if (timeLeft > 20) return '#10b981'; // green
    if (timeLeft > 10) return '#f59e0b'; // amber
    return '#ef4444'; // red
  };

  return (
    <div className="boardroom-view-root">
      {/* Top Navigation Bar */}
      <header className="boardroom-topbar">
        <div className="topbar-left">
          <button className="back-dash-btn" onClick={onLeaveRoom} title="Return to Dashboard">
            <ArrowLeft size={18} />
            <span>Dashboard</span>
          </button>
          
          <div className="boardroom-topic-badge">
            <span className="topic-prefix">AGENDA:</span>
            <h2 className="topic-title">{room?.topic}</h2>
          </div>
        </div>

        <div className="topbar-center">
          <div className="room-code-pill" onClick={copyRoomCode} title="Click to copy Room Code">
            <span className="code-label">ROOM CODE:</span>
            <strong className="code-val">{room?.roomCode}</strong>
            {copiedCode ? <Check size={14} className="text-green" /> : <Copy size={14} />}
          </div>

          <div className={`status-pill status-${room?.status?.toLowerCase()}`}>
            <span className="status-indicator-dot"></span>
            <span>{room?.status}</span>
          </div>
        </div>

        <div className="topbar-right">
          {isHost && room?.status === 'LOBBY' && (
            <button 
              className="host-ctrl-btn start-session-btn" 
              onClick={handleStartSession}
              disabled={loadingAction}
            >
              <Play size={16} />
              <span>Start Session</span>
            </button>
          )}

          {isHost && room?.status === 'ACTIVE' && (
            <button 
              className="host-ctrl-btn end-session-btn" 
              onClick={handleEndSessionClick}
              disabled={loadingAction || isGeneratingSwot}
            >
              <Sparkles size={16} />
              <span>End & Generate SWOT</span>
            </button>
          )}

          {room?.status === 'ENDED' && (
            <button 
              className="view-swot-badge-btn" 
              onClick={handleViewSwot}
              disabled={loadingAction}
            >
              <Sparkles size={16} />
              <span>{swotReport ? 'View SWOT Report' : 'Fetch SWOT Report'}</span>
            </button>
          )}

          {isHost && <span className="host-tag"><Crown size={14} /> Room Host</span>}
        </div>
      </header>

      {actionError && (
        <div className="boardroom-alert-banner">
          <AlertCircle size={16} />
          <span>{actionError}</span>
        </div>
      )}

      {/* Main Boardroom Workspace */}
      <div className="boardroom-workspace">
        {/* Left: Participants Sidebar */}
        <aside className="boardroom-sidebar">
          <div className="sidebar-header">
            <div className="sidebar-title">
              <Users size={18} />
              <h3>Participants</h3>
            </div>
            <span className="member-count-badge">
              {members.length} / {room?.maxMembers}
            </span>
          </div>

          <div className="members-list">
            {members.map((m) => {
              const isSpeaker = turnState.activeUserId === m.userId;
              const isQueued = turnState.queue?.some((q) => q.userId === m.userId);
              const isSelf = m.userId === currentUser?.id;

              return (
                <div 
                  key={m.userId} 
                  className={`member-row ${isSpeaker ? 'is-speaking' : ''} ${isSelf ? 'is-self' : ''}`}
                >
                  <div className="member-avatar">
                    {m.username.charAt(0).toUpperCase()}
                    {m.isHost && <Crown size={12} className="avatar-crown" />}
                  </div>

                  <div className="member-info">
                    <div className="member-name-row">
                      <span className="member-username">{m.username}</span>
                      {isSelf && <span className="self-label">(You)</span>}
                    </div>
                    <span className="member-status-text">
                      {isSpeaker ? '🎙️ Speaking' : isQueued ? '⏳ In Queue' : 'Listening'}
                    </span>
                  </div>

                  {isSpeaker && (
                    <div className="speaking-wave">
                      <span></span><span></span><span></span>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {/* Up Next Bidding Queue Preview */}
          <div className="queue-preview-box">
            <div className="queue-preview-header">
              <Clock size={14} />
              <h4>Bidding Queue ({turnState.queue?.length || 0})</h4>
            </div>

            <div className="queue-preview-list">
              {turnState.queue && turnState.queue.length > 0 ? (
                turnState.queue.map((qUser, idx) => (
                  <div key={qUser.userId} className="queue-user-item">
                    <span className="queue-rank">#{idx + 1}</span>
                    <span className="queue-name">{qUser.username}</span>
                    {qUser.userId === currentUser?.id && <span className="queue-you">You</span>}
                  </div>
                ))
              ) : (
                <div className="queue-empty">Queue is empty. Click "Bid" to speak next!</div>
              )}
            </div>
          </div>
        </aside>

        {/* Center: Live Boardroom Stage & Turn Timer */}
        <section className="boardroom-stage">
          {/* Active Turn Header / Precision Countdown */}
          <div className="turn-stage-card">
            <div className="stage-speaker-details">
              <div className="stage-badge-row">
                <span className="stage-status-tag">
                  {room?.status === 'LOBBY' ? 'ROOM IN LOBBY' : turnState.activeUserId ? 'ACTIVE SPEAKER' : 'FLOOR OPEN FOR BIDS'}
                </span>
                {isMyTurn && <span className="your-turn-pill">🔥 YOUR TURN TO SPEAK</span>}
              </div>

              <h3 className="speaker-headline">
                {room?.status === 'LOBBY' 
                  ? 'Waiting for host to start the session...' 
                  : turnState.activeUserId 
                    ? `${turnState.username} is addressing the boardroom` 
                    : 'The floor is open. Click "Bid to Speak" below!'}
              </h3>
            </div>

            {/* 45s Precision Circular / Linear Countdown */}
            {room?.status === 'ACTIVE' && (
              <div className="timer-unit">
                <div className="timer-circular-wrap">
                  <svg className="timer-svg" viewBox="0 0 100 100">
                    <circle
                      className="timer-bg-circle"
                      cx="50"
                      cy="50"
                      r="42"
                    />
                    <circle
                      className="timer-progress-circle"
                      cx="50"
                      cy="50"
                      r="42"
                      style={{
                        strokeDasharray: 264,
                        strokeDashoffset: 264 - (264 * timerPercentage) / 100,
                        stroke: getTimerColor()
                      }}
                    />
                  </svg>
                  <div className="timer-number-overlay">
                    <span className="timer-seconds" style={{ color: getTimerColor() }}>
                      {turnState.activeUserId ? timeLeft : '--'}
                    </span>
                    <span className="timer-unit-text">sec</span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Transcript / Messages Flow */}
          <div className="messages-scroll-area">
            {messages.length === 0 ? (
              <div className="empty-messages-state">
                <Mic size={36} className="empty-mic" />
                <h4>No spoken contributions yet</h4>
                <p>When participants bid and speak during their 45s turn, contributions will stream here live.</p>
              </div>
            ) : (
              messages.map((msg) => {
                const isMsgMine = msg.userId === currentUser?.id;
                const formattedTime = msg.timestamp 
                  ? new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                  : '';

                return (
                  <div 
                    key={msg.id || Math.random()} 
                    className={`message-bubble-wrapper ${isMsgMine ? 'mine' : 'theirs'}`}
                  >
                    <div className="message-speaker-header">
                      <span className="msg-username">{msg.username}</span>
                      <span className="msg-time">{formattedTime}</span>
                    </div>
                    <div className="message-bubble">
                      <p>{msg.content}</p>
                    </div>
                  </div>
                );
              })
            )}
            <div ref={chatBottomRef} />
          </div>

          {/* Bottom Action Controls Bar */}
          <div className="boardroom-controls-bar">
            {/* Bid to speak button */}
            <div className="bid-control-wrap">
              <button
                type="button"
                className={`bid-button ${isMyTurn ? 'active-turn' : ''} ${isInQueue ? 'in-queue' : ''}`}
                onClick={handleBidToSpeak}
                disabled={!canBid}
                title={
                  room?.status !== 'ACTIVE'
                    ? 'Session is not active'
                    : isMyTurn
                    ? "You are currently speaking!"
                    : isInQueue
                    ? 'You are currently in the bidding queue'
                    : 'Add yourself to the turn queue'
                }
              >
                <Mic size={18} />
                <span>
                  {isMyTurn
                    ? '🎙️ You Are Speaking'
                    : isInQueue
                    ? '⏳ In Bidding Queue'
                    : 'Bid to Speak'}
                </span>
              </button>
            </div>

            {/* Speaking Input Form (Active Speaker only) */}
            <form onSubmit={handleSendMessage} className="speech-input-form">
              <div className="speech-input-container">
                <input
                  type="text"
                  value={messageInput}
                  onChange={(e) => setMessageInput(e.target.value)}
                  placeholder={
                    isMyTurn
                      ? 'You have the floor! Speak your point and press Send...'
                      : room?.status !== 'ACTIVE'
                      ? 'Waiting for session to start...'
                      : `Floor held by ${turnState.username || 'speaker'} (Bid to speak next)`
                  }
                  disabled={!isMyTurn}
                  className={isMyTurn ? 'input-active-speaker' : 'input-disabled'}
                />
                <button
                  type="submit"
                  disabled={!isMyTurn || !messageInput.trim()}
                  className="send-speech-btn"
                  title="Deliver spoken point to boardroom"
                >
                  <Send size={18} />
                </button>
              </div>
            </form>
          </div>
        </section>
      </div>

      {/* SWOT Generation Loading Overlay */}
      {isGeneratingSwot && (
        <div className="swot-generating-overlay">
          <div className="swot-generating-card">
            <div className="swot-loader-spin">
              <Sparkles size={32} className="spin-icon" />
            </div>
            <h3>Generating Strategic SWOT Analysis</h3>
            <p>Google Gemini 2.0 Flash is synthesizing boardroom contributions into structured quadrants...</p>
          </div>
        </div>
      )}

      {/* End Session Confirmation Modal */}
      {showEndConfirmModal && (
        <div className="modal-backdrop" onClick={() => setShowEndConfirmModal(false)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-icon-badge">
                <Sparkles size={22} />
              </div>
              <div>
                <h3>Conclude Boardroom Session</h3>
                <p>Synthesize Gemini 2.0 Flash Strategic SWOT Report</p>
              </div>
            </div>
            <p style={{ color: '#cbd5e1', lineHeight: '1.6', fontSize: '0.92rem', margin: '0 0 1.5rem 0' }}>
              Ending the session will close turn bidding and trigger Google Gemini 2.0 Flash to synthesize all contributions into a structured SWOT Report with an Executive Summary.
            </p>
            <div className="modal-actions">
              <button 
                type="button" 
                className="btn-cancel" 
                onClick={() => setShowEndConfirmModal(false)}
              >
                Keep Brainstorming
              </button>
              <button 
                type="button" 
                className="btn-confirm-host" 
                onClick={handleConfirmEndSession}
                disabled={loadingAction}
              >
                Confirm & Generate SWOT
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SWOT Report Modal */}
      {swotReport && (
        <SwotReportModal
          swotData={swotReport}
          onClose={() => setSwotReport(null)}
        />
      )}
    </div>
  );
}
