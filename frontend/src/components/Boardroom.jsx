import React, { useState, useEffect, useRef } from 'react';
import Whiteboard from './Whiteboard';
import { triggerNpcTurn, postUserBid, getTranscript } from '../services/api';
import './Boardroom.css';

export default function Boardroom({ sessionData, onEndSession }) {
  const [timeLeft, setTimeLeft] = useState((sessionData.durationMinutes || sessionData.duration) * 60);

  const [history, setHistory] = useState([]);
  const [activeSpeakerIdx, setActiveSpeakerIdx] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [showBidModal, setShowBidModal] = useState(false);
  const [userPoint, setUserPoint] = useState('');
  const [bidCountdown, setBidCountdown] = useState(null);
  const [showBriefModal, setShowBriefModal] = useState(false);

  const characters = sessionData.characters;
  const isPausedRef = useRef(false);
  const currentSpeakerIdxRef = useRef(-1);
  const bidTimerRef = useRef(null);

  // Overall session countdown timer
  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          handleFinish();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Inter-turn countdown timer (45 seconds to allow comfortable reading)
  const INTER_TURN_SECONDS = 45;

  const startBidTimer = () => {
    if (bidTimerRef.current) {
      clearInterval(bidTimerRef.current);
    }
    setBidCountdown(INTER_TURN_SECONDS);

    bidTimerRef.current = setInterval(() => {
      if (isPausedRef.current) {
        return; // Don't decrement while modal is open
      }

      setBidCountdown(prev => {
        if (prev === null) return null;
        if (prev <= 1) {
          clearInterval(bidTimerRef.current);
          bidTimerRef.current = null;
          // 45-second timer reached zero -> automatically advance to the next panel member
          runNextNpcTurn();
          return null;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const runNextNpcTurn = async (forcedIndex = null) => {
    if (bidTimerRef.current) {
      clearInterval(bidTimerRef.current);
      bidTimerRef.current = null;
    }
    setBidCountdown(null);
    setIsProcessing(true);

    const nextIdx = forcedIndex !== null
      ? forcedIndex
      : (currentSpeakerIdxRef.current + 1) % characters.length;

    currentSpeakerIdxRef.current = nextIdx;
    setActiveSpeakerIdx(nextIdx);

    const speaker = characters[nextIdx];
    try {
      const turnResult = await triggerNpcTurn(sessionData.id, speaker.name, speaker.role);
      setHistory(prev => [...prev, turnResult]);
    } catch (err) {
      console.error("Turn execution failed", err);
    } finally {
      setIsProcessing(false);
      // Once the person finishes and their point is displayed, start the 15-second bid timer
      startBidTimer();
    }
  };

  // Automatically start the first panelist turn after entering the boardroom
  useEffect(() => {
    const initialDelay = setTimeout(() => {
      runNextNpcTurn(0);
    }, 1200);

    return () => {
      clearTimeout(initialDelay);
      if (bidTimerRef.current) {
        clearInterval(bidTimerRef.current);
      }
    };
  }, []);

  const handleSkip = () => {
    if (isProcessing) return;
    if (bidTimerRef.current) {
      clearInterval(bidTimerRef.current);
      bidTimerRef.current = null;
    }
    setBidCountdown(null);
    runNextNpcTurn();
  };

  const handleOpenBid = () => {
    if (isProcessing) return;
    isPausedRef.current = true; // Pause timer while composing point
    setShowBidModal(true);
  };

  const handleCancelBid = () => {
    setShowBidModal(false);
    isPausedRef.current = false; // Resume timer
  };

  const handleSendBid = async () => {
    if (!userPoint.trim()) return;
    if (bidTimerRef.current) {
      clearInterval(bidTimerRef.current);
      bidTimerRef.current = null;
    }
    setBidCountdown(null);
    setIsProcessing(true);
    setShowBidModal(false);

    try {
      const userResult = await postUserBid(sessionData.id, userPoint);
      setHistory(prev => [...prev, userResult]);
      setUserPoint('');
    } catch (err) {
      console.error("Bid submission failed", err);
    } finally {
      setIsProcessing(false);
      isPausedRef.current = false;
      // Immediately run the next NPC turn to respond to the host's input
      runNextNpcTurn();
    }
  };

  const handleFinish = async () => {
    isPausedRef.current = true;
    if (bidTimerRef.current) {
      clearInterval(bidTimerRef.current);
      bidTimerRef.current = null;
    }
    setBidCountdown(null);
    const finalTranscript = await getTranscript(sessionData.id);
    onEndSession(finalTranscript);
  };

  const formatTime = (secs) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const leftCharacters = characters.slice(0, 5);
  const rightCharacters = characters.slice(5, 10);

  return (
    <div className="boardroom-stage">
      <div className="boardroom-top-bar">
        <div className="top-left-info">
          <div className="timer">TIME REMAINING: {formatTime(timeLeft)}</div>
          {sessionData.sessionType === 'INNOVATIVE' ? (
            <div className="project-brief-badges">
              <span className="meta-badge innovative">💡 Innovative Brainstorming</span>
              <span className="meta-badge domain">⚙️ {sessionData.domain || 'Software'}</span>
            </div>
          ) : sessionData.domain && (
            <div className="project-brief-badges">
              <span className="meta-badge domain">🏛️ {sessionData.domain}</span>
              <span className="meta-badge theme">💻 {sessionData.theme}</span>
              <span className="meta-badge hosting">☁️ {sessionData.hostingPreference}</span>
              <span className="meta-badge budget">💰 {sessionData.budgetTier}</span>
            </div>
          )}
        </div>
        <div className="top-right-actions">
          <button 
            type="button" 
            className="brief-toggle-btn" 
            onClick={() => setShowBriefModal(true)}
          >
            {sessionData.sessionType === 'INNOVATIVE' ? '💡 Concept Brief' : '📋 Project Charter'}
          </button>
          <button className="end-btn" onClick={handleFinish}>
            {sessionData.sessionType === 'INNOVATIVE' ? 'End Session & Get Spec' : 'End Session & Get Charter'}
          </button>
        </div>
      </div>

      <div className="boardroom-table-area">
        {/* Left Side Members (5 Seats) */}
        <div className="side-column left">
          {leftCharacters.map((char, i) => {
            const globalIdx = i;
            const isActive = activeSpeakerIdx === globalIdx;
            return (
              <div key={char.name} className={`seat-card ${isActive ? 'speaking' : ''}`}>
                <div className="avatar">{char.name[0]}</div>
                <div className="seat-info">
                  <div className="seat-name">{char.name}</div>
                  <div className="seat-role">{char.role}</div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Center Camera Angle & Whiteboard */}
        <div className="center-camera">
          <Whiteboard 
            topic={sessionData.topic} 
            activeSpeaker={isProcessing && activeSpeakerIdx !== null ? characters[activeSpeakerIdx] : null} 
            history={history} 
          />
        </div>

        {/* Right Side Members (5 Seats) */}
        <div className="side-column right">
          {rightCharacters.map((char, i) => {
            const globalIdx = i + 5;
            const isActive = activeSpeakerIdx === globalIdx;
            return (
              <div key={char.name} className={`seat-card ${isActive ? 'speaking' : ''}`}>
                <div className="avatar">{char.name[0]}</div>
                <div className="seat-info">
                  <div className="seat-name">{char.name}</div>
                  <div className="seat-role">{char.role}</div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Bottom Center User Seat & Controls */}
      <div className="boardroom-bottom-bar">
        <div className="user-seat">
          <div className="avatar user">YOU</div>
          <div>
            <div className="seat-name">Host</div>
            <div className="seat-role">Session Leader</div>
          </div>
        </div>

        {/* Inter-turn 45-second Bid Countdown Indicator */}
        {bidCountdown !== null && !isProcessing && (
          <div className="bid-countdown-pill">
            <div className="bid-countdown-header">
              <span className="pulse-dot"></span>
              <span className="bid-timer-title">FLOOR OPEN FOR BID</span>
              <span className="bid-timer-sec">{bidCountdown}s</span>
            </div>
            <div className="bid-progress-track">
              <div 
                className="bid-progress-fill" 
                style={{ width: `${(bidCountdown / INTER_TURN_SECONDS) * 100}%` }}
              />
            </div>
          </div>
        )}

        {isProcessing && (
          <div className="processing-pill">
            <span className="processing-dot"></span>
            <span>Panelist speaking...</span>
          </div>
        )}

        <div className="action-buttons">
          <button className="btn skip" onClick={handleSkip} disabled={isProcessing}>
            SKIP TURN ⏭
          </button>
          <button 
            className={`btn bid ${bidCountdown !== null ? 'active-bid pulse-glow' : ''}`} 
            onClick={handleOpenBid} 
            disabled={isProcessing}
          >
            🎙️ BID (MY POINT) {bidCountdown !== null ? `(${bidCountdown}s)` : ''}
          </button>
        </div>
      </div>

      {/* Bid Modal Overlay */}
      {showBidModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Speak to Boardroom</h3>
            <p className="modal-subtext">The panel is listening. Enter your point, objection, or direction:</p>
            <textarea 
              rows="4" 
              placeholder="Type your point or objection here..." 
              value={userPoint}
              onChange={e => setUserPoint(e.target.value)}
              autoFocus
            />
            <div className="modal-actions">
              <button onClick={handleCancelBid}>Cancel</button>
              <button className="submit-bid" onClick={handleSendBid}>Submit Point</button>
            </div>
          </div>
        </div>
      )}

      {/* Project Charter / Innovation Brief Modal Overlay */}
      {showBriefModal && (
        <div className="modal-overlay" onClick={() => setShowBriefModal(false)}>
          <div className="modal-content charter-modal" onClick={e => e.stopPropagation()}>
            <div className="charter-modal-header">
              <h3>
                {sessionData.sessionType === 'INNOVATIVE' 
                  ? '💡 Innovative Brainstorming Brief' 
                  : '📋 Client Project Intake Charter'}
              </h3>
              <button className="close-btn" onClick={() => setShowBriefModal(false)}>✕</button>
            </div>

            {sessionData.sessionType === 'INNOVATIVE' ? (
              <div className="charter-details-container">
                <div className="charter-grid">
                  <div className="charter-item">
                    <span className="charter-label">Brainstorming Mode:</span>
                    <strong className="charter-value">💡 Innovative Idea Brainstorming</strong>
                  </div>
                  <div className="charter-item">
                    <span className="charter-label">Target Domain:</span>
                    <strong className="charter-value">{sessionData.domain === 'Hardware' ? '⚙️ Hardware' : '💻 Software'}</strong>
                  </div>
                </div>

                <div className="charter-problem">
                  <span className="charter-label">1. Idea / Concept Line:</span>
                  <p className="charter-problem-text">{sessionData.topic || 'Not specified'}</p>
                </div>

                <div className="charter-problem">
                  <span className="charter-label">2. Problem Statement:</span>
                  <p className="charter-problem-text">{sessionData.problemStatement || 'Not specified'}</p>
                </div>

                <div className="charter-problem">
                  <span className="charter-label">3. Proposed Solution / Mechanism:</span>
                  <p className="charter-problem-text">{sessionData.proposedSolution || 'Not specified'}</p>
                </div>
              </div>
            ) : (
              <div className="charter-details-container">
                <div className="charter-grid">
                  <div className="charter-item">
                    <span className="charter-label">Project Name:</span>
                    <strong className="charter-value">{sessionData.topic}</strong>
                  </div>
                  <div className="charter-item">
                    <span className="charter-label">Industry Domain:</span>
                    <span className="charter-value">{sessionData.domain || 'General Software'}</span>
                  </div>
                  <div className="charter-item">
                    <span className="charter-label">Delivery Platform:</span>
                    <span className="charter-value">{sessionData.theme || 'Web Application'}</span>
                  </div>
                  <div className="charter-item">
                    <span className="charter-label">Target Cloud / Hosting:</span>
                    <span className="charter-value">{sessionData.hostingPreference || 'Cloud'}</span>
                  </div>
                  <div className="charter-item">
                    <span className="charter-label">Budget Scope:</span>
                    <span className="charter-value">{sessionData.budgetTier || 'MVP'}</span>
                  </div>
                </div>
                {sessionData.problemStatement && (
                  <div className="charter-problem">
                    <span className="charter-label">Client Problem Statement:</span>
                    <p className="charter-problem-text">{sessionData.problemStatement}</p>
                  </div>
                )}
              </div>
            )}

            <div className="modal-actions">
              <button onClick={() => setShowBriefModal(false)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}