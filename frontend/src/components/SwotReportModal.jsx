import React, { useState } from 'react';
import { Shield, TrendingUp, AlertTriangle, AlertOctagon, Download, Copy, Check, X, Sparkles, FileText } from 'lucide-react';
import './SwotReportModal.css';

export default function SwotReportModal({ swotData, onClose }) {
  const [copied, setCopied] = useState(false);

  if (!swotData) return null;

  const { topic, strengths = [], weaknesses = [], opportunities = [], threats = [], summary = '' } = swotData;

  const generateMarkdownReport = () => {
    return `# EXECUTIVE SWOT ANALYSIS: ${topic.toUpperCase()}
Generated on: ${new Date().toLocaleString()}

## EXECUTIVE SUMMARY
${summary}

---

## 1. STRENGTHS (Internal Advantages)
${strengths.map(s => `- ${s}`).join('\n')}

## 2. WEAKNESSES (Internal Blockers)
${weaknesses.map(w => `- ${w}`).join('\n')}

## 3. OPPORTUNITIES (External Potential)
${opportunities.map(o => `- ${o}`).join('\n')}

## 4. THREATS (External Risks)
${threats.map(t => `- ${t}`).join('\n')}

---
*Synthesized by Google Gemini 2.0 Flash Boardroom Facilitator*
`;
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(generateMarkdownReport());
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  const handleDownload = () => {
    const content = generateMarkdownReport();
    const blob = new Blob([content], { type: 'text/markdown;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `SWOT_Analysis_${topic.replace(/[^a-z0-9]/gi, '_').toLowerCase()}.md`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="swot-overlay">
      <div className="swot-modal-container">
        {/* Modal Top Bar */}
        <div className="swot-modal-header">
          <div className="swot-title-wrap">
            <div className="swot-badge">
              <Sparkles size={18} />
              <span>Gemini 2.0 Flash Synthesis</span>
            </div>
            <h2>Executive Strategic SWOT Report</h2>
            <p className="swot-topic-label">
              <span>Topic:</span> <strong>{topic}</strong>
            </p>
          </div>

          <div className="swot-header-actions">
            <button className="swot-action-btn" onClick={handleCopy} title="Copy Markdown Report">
              {copied ? <Check size={16} className="text-green" /> : <Copy size={16} />}
              <span>{copied ? 'Copied' : 'Copy'}</span>
            </button>
            <button className="swot-action-btn download-btn" onClick={handleDownload} title="Download Report">
              <Download size={16} />
              <span>Export MD</span>
            </button>
            <button className="swot-close-btn" onClick={onClose} title="Close Report">
              <X size={20} />
            </button>
          </div>
        </div>

        {/* Executive Summary */}
        <div className="swot-summary-box">
          <div className="summary-title">
            <FileText size={18} />
            <h4>Executive Synthesis</h4>
          </div>
          <p>{summary || 'Comprehensive strategic synthesis compiled from multi-user boardroom transcript.'}</p>
        </div>

        {/* 4 Quadrants Grid */}
        <div className="swot-quadrants-grid">
          {/* Strengths - Green */}
          <div className="swot-quadrant quadrant-strengths">
            <div className="quadrant-header">
              <div className="quadrant-icon strengths-icon">
                <Shield size={20} />
              </div>
              <div className="quadrant-title">
                <h3>Strengths</h3>
                <span>Internal Positives</span>
              </div>
              <span className="quadrant-count">{strengths.length}</span>
            </div>
            <ul className="quadrant-list">
              {strengths.map((item, idx) => (
                <li key={idx} className="quadrant-item">
                  <span className="bullet-dot strengths-bullet"></span>
                  <span>{item}</span>
                </li>
              ))}
              {strengths.length === 0 && <li className="empty-hint">No explicit strengths captured.</li>}
            </ul>
          </div>

          {/* Weaknesses - Red */}
          <div className="swot-quadrant quadrant-weaknesses">
            <div className="quadrant-header">
              <div className="quadrant-icon weaknesses-icon">
                <AlertTriangle size={20} />
              </div>
              <div className="quadrant-title">
                <h3>Weaknesses</h3>
                <span>Internal Limitations</span>
              </div>
              <span className="quadrant-count">{weaknesses.length}</span>
            </div>
            <ul className="quadrant-list">
              {weaknesses.map((item, idx) => (
                <li key={idx} className="quadrant-item">
                  <span className="bullet-dot weaknesses-bullet"></span>
                  <span>{item}</span>
                </li>
              ))}
              {weaknesses.length === 0 && <li className="empty-hint">No explicit weaknesses captured.</li>}
            </ul>
          </div>

          {/* Opportunities - Blue */}
          <div className="swot-quadrant quadrant-opportunities">
            <div className="quadrant-header">
              <div className="quadrant-icon opportunities-icon">
                <TrendingUp size={20} />
              </div>
              <div className="quadrant-title">
                <h3>Opportunities</h3>
                <span>External Potential</span>
              </div>
              <span className="quadrant-count">{opportunities.length}</span>
            </div>
            <ul className="quadrant-list">
              {opportunities.map((item, idx) => (
                <li key={idx} className="quadrant-item">
                  <span className="bullet-dot opportunities-bullet"></span>
                  <span>{item}</span>
                </li>
              ))}
              {opportunities.length === 0 && <li className="empty-hint">No explicit opportunities captured.</li>}
            </ul>
          </div>

          {/* Threats - Yellow */}
          <div className="swot-quadrant quadrant-threats">
            <div className="quadrant-header">
              <div className="quadrant-icon threats-icon">
                <AlertOctagon size={20} />
              </div>
              <div className="quadrant-title">
                <h3>Threats</h3>
                <span>External Risks</span>
              </div>
              <span className="quadrant-count">{threats.length}</span>
            </div>
            <ul className="quadrant-list">
              {threats.map((item, idx) => (
                <li key={idx} className="quadrant-item">
                  <span className="bullet-dot threats-bullet"></span>
                  <span>{item}</span>
                </li>
              ))}
              {threats.length === 0 && <li className="empty-hint">No explicit threats captured.</li>}
            </ul>
          </div>
        </div>

        {/* Modal Footer */}
        <div className="swot-modal-footer">
          <span className="footer-note">Session has successfully concluded. Transcript & SWOT persisted to database.</span>
          <button className="swot-primary-close" onClick={onClose}>
            Return to Dashboard
          </button>
        </div>
      </div>
    </div>
  );
}
