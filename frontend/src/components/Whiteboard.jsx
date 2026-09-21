import React, { useEffect, useRef } from 'react';
import './Whiteboard.css';

export default function Whiteboard({ activeSpeaker, currentMessage, history, topic }) {
  const chatEndRef = useRef(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [history, currentMessage]);

  return (
    <div className="whiteboard-container">
      <div className="whiteboard-header">
        <span className="board-title">TOPIC: {topic}</span>
        {activeSpeaker && (
          <span className="active-badge">
            ● {activeSpeaker.name} ({activeSpeaker.role}) is typing...
          </span>
        )}
      </div>

      <div className="whiteboard-screen">
        {history.map((item, index) => (
          <div key={index} className={`message-line ${item.speakerName.includes('Host') ? 'user-line' : ''}`}>
            <span className="speaker-tag">[{item.speakerName} - {item.speakerRole}]:</span>
            <span className="speaker-text">{item.messageText}</span>
          </div>
        ))}
        {currentMessage && (
          <div className="message-line streaming">
            <span className="speaker-tag">[{activeSpeaker?.name} - {activeSpeaker?.role}]:</span>
            <span className="speaker-text">{currentMessage}</span>
          </div>
        )}
        <div ref={chatEndRef} />
      </div>
    </div>
  );
}