import React, { useState, useEffect } from 'react';
import { getPresetRoles } from '../services/api';
import './SetupForm.css';

const DEFAULT_NAMES = ["Alex", "Sarah", "David", "Elena", "Marcus", "Chloe", "Viktor", "Maya", "Julian", "Zoe"];

const CLIENT_TEMPLATES = [
  {
    label: "⚡ FinTech Payment Hub",
    topic: "Cross-Border B2B Escrow & Settlement Engine",
    problemStatement: "Global SMBs face 3-5 day wire delays, 4% FX loss, and zero visibility when settling international supplier invoices.",
    domain: "FinTech",
    theme: "Full-Stack Web Application",
    hostingPreference: "AWS (ECS Fargate + RDS PostgreSQL)",
    budgetTier: "Growth-Stage ($50k - $150k)"
  },
  {
    label: "🏥 HealthTech Patient Portal",
    topic: "HIPAA-Compliant Remote Patient Monitoring",
    problemStatement: "Cardiology clinics struggle to monitor vital signs post-discharge, resulting in 22% 30-day readmissions and heavy nurse overhead.",
    domain: "HealthTech",
    theme: "Web & Mobile App (iOS/Android)",
    hostingPreference: "Google Cloud (Cloud Run + Cloud SQL)",
    budgetTier: "Enterprise ($250k+)"
  },
  {
    label: "📦 AI Logistics & Fleet",
    topic: "Autonomous Freight Dispatch & Route Optimizer",
    problemStatement: "Regional trucking fleets lose 25% revenue to deadhead empty miles and manual phone call dispatching.",
    domain: "Logistics & Supply Chain",
    theme: "AI/LLM Agent & RAG System",
    hostingPreference: "AWS (Lambda + EKS + BigQuery)",
    budgetTier: "Lean MVP (<$35k / 3-Month Launch)"
  },
  {
    label: "🛍️ E-Commerce Smart Checkout",
    topic: "Omnichannel Headless Checkout & Cart Recovery",
    problemStatement: "Retailers experience 70% cart abandonment due to rigid legacy checkouts and out-of-sync multi-store inventory.",
    domain: "E-Commerce / Retail",
    theme: "Full-Stack Web Application",
    hostingPreference: "Serverless (Vercel + Supabase + Redis)",
    budgetTier: "Growth-Stage ($50k - $150k)"
  }
];

const INNOVATIVE_TEMPLATES = [
  {
    label: "🤖 Edge AI Wearable for Deaf Assistance",
    ideaLine: "Sub-50ms wearable tactile sound-to-haptic belt for deaf individuals navigating traffic",
    problemStatement: "Deaf and hard-of-hearing pedestrians face high danger from silent electric vehicles and emergency sirens.",
    proposedSolution: "ESP32 or ARM Cortex micro-controller with microphone array running tinyML acoustic classification to trigger vibrating haptic motors.",
    domain: "Hardware"
  },
  {
    label: "⚡ Zero-Knowledge Identity Verification",
    ideaLine: "Decentralized age and residency verification without exposing PII or ID scans",
    problemStatement: "Websites force users to upload government IDs to third-party databases, causing catastrophic data breaches.",
    proposedSolution: "Client-side zk-SNARK cryptographic proofs computed in WebAssembly, validated by an ultra-fast Go verification engine.",
    domain: "Software"
  }
];

export default function SetupForm({ onStartSession }) {
  // Mode toggle: 'INNOVATIVE' vs 'CLIENT'
  const [sessionType, setSessionType] = useState('INNOVATIVE');

  // Innovative Form State (3 flexible fields where at least 1 is required)
  const [ideaLine, setIdeaLine] = useState('');
  const [innovativeProblem, setInnovativeProblem] = useState('');
  const [innovativeSolution, setInnovativeSolution] = useState('');
  const [innovativeDomain, setInnovativeDomain] = useState('Software'); // 'Software' or 'Hardware' only

  // Client Form State
  const [clientTopic, setClientTopic] = useState(CLIENT_TEMPLATES[0].topic);
  const [clientProblem, setClientProblem] = useState(CLIENT_TEMPLATES[0].problemStatement);
  const [clientDomain, setClientDomain] = useState(CLIENT_TEMPLATES[0].domain);
  const [clientTheme, setClientTheme] = useState(CLIENT_TEMPLATES[0].theme);
  const [clientHosting, setClientHosting] = useState(CLIENT_TEMPLATES[0].hostingPreference);
  const [clientBudget, setClientBudget] = useState(CLIENT_TEMPLATES[0].budgetTier);

  // Common Boardroom & Panel Configuration
  const [charCount, setCharCount] = useState(6);
  const [duration, setDuration] = useState(15);
  const [availableRoles, setAvailableRoles] = useState([]);
  const [assignedRoles, setAssignedRoles] = useState({});

  useEffect(() => {
    getPresetRoles().then(roles => {
      setAvailableRoles(roles);
      const initial = {};
      DEFAULT_NAMES.slice(0, 10).forEach((name, idx) => {
        initial[name] = roles[idx % roles.length] || 'Lead Developer';
      });
      setAssignedRoles(initial);
    });
  }, []);

  const handleApplyClientTemplate = (tmpl) => {
    setClientTopic(tmpl.topic);
    setClientProblem(tmpl.problemStatement);
    setClientDomain(tmpl.domain);
    setClientTheme(tmpl.theme);
    setClientHosting(tmpl.hostingPreference);
    setClientBudget(tmpl.budgetTier);
  };

  const handleApplyInnovativeTemplate = (tmpl) => {
    setIdeaLine(tmpl.ideaLine);
    setInnovativeProblem(tmpl.problemStatement);
    setInnovativeSolution(tmpl.proposedSolution);
    setInnovativeDomain(tmpl.domain);
  };

  const handleRoleChange = (name, role) => {
    setAssignedRoles(prev => ({ ...prev, [name]: role }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    const characters = DEFAULT_NAMES.slice(0, charCount).map(name => ({
      name,
      role: assignedRoles[name]
    }));

    if (sessionType === 'INNOVATIVE') {
      const hasIdea = Boolean(ideaLine.trim());
      const hasProblem = Boolean(innovativeProblem.trim());
      const hasSolution = Boolean(innovativeSolution.trim());

      // At least 1 field must be provided within the 3
      if (!hasIdea && !hasProblem && !hasSolution) {
        return alert("Please provide at least one field: an Idea Line, a Problem Statement, or your Proposed Solution.");
      }

      // Generate a representative topic title from whichever field is populated
      const topicTitle = ideaLine.trim() || innovativeProblem.trim() || innovativeSolution.trim();

      onStartSession({
        sessionType: 'INNOVATIVE',
        topic: topicTitle,
        problemStatement: innovativeProblem.trim(),
        proposedSolution: innovativeSolution.trim(),
        domain: innovativeDomain, // Strictly "Software" or "Hardware"
        theme: `${innovativeDomain} Innovation Concept`,
        hostingPreference: innovativeDomain === 'Hardware' ? 'Embedded Microcontroller / Firmware' : 'Cloud Architecture',
        budgetTier: 'R&D / Prototyping Phase',
        charCount,
        duration,
        characters
      });
    } else {
      // Client Project Mode
      if (!clientTopic.trim()) return alert("Please enter a project topic / name.");
      if (!clientProblem.trim()) return alert("Please describe the client problem statement.");

      onStartSession({
        sessionType: 'CLIENT',
        topic: clientTopic.trim(),
        problemStatement: clientProblem.trim(),
        proposedSolution: '',
        domain: clientDomain,
        theme: clientTheme,
        hostingPreference: clientHosting,
        budgetTier: clientBudget,
        charCount,
        duration,
        characters
      });
    }
  };

  return (
    <div className="setup-container">
      <div className="setup-card">
        {/* Header with Mode Switcher */}
        <div className="setup-header">
          <span className="badge-enterprise">ENTERPRISE BOARDROOM SUITE</span>
          <h2>Virtual Executive Session</h2>
          <p className="setup-subtitle">
            Choose your brainstorming approach. Convene a specialized multi-agent boardroom panel to debate, critique, and synthesize tech stacks &amp; strategic roadmaps.
          </p>

          {/* Toggle Switcher */}
          <div className="session-type-toggle-container">
            <div className="session-type-toggle">
              <button
                type="button"
                className={`toggle-tab ${sessionType === 'INNOVATIVE' ? 'active' : ''}`}
                onClick={() => setSessionType('INNOVATIVE')}
              >
                <span className="toggle-tab-icon">💡</span>
                <div className="toggle-tab-text">
                  <span className="toggle-tab-title">Innovative Brainstorming</span>
                  <span className="toggle-tab-desc">Idea, Problem or Solution · Software/Hardware</span>
                </div>
              </button>

              <button
                type="button"
                className={`toggle-tab ${sessionType === 'CLIENT' ? 'active' : ''}`}
                onClick={() => setSessionType('CLIENT')}
              >
                <span className="toggle-tab-icon">🏢</span>
                <div className="toggle-tab-text">
                  <span className="toggle-tab-title">Client Project Kickoff</span>
                  <span className="toggle-tab-desc">RFP Specs, Domain, Cloud Hosting &amp; Budgets</span>
                </div>
              </button>
            </div>
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          {/* =========================================================================
              MODE 1: INNOVATIVE BRAINSTORMING
             ========================================================================= */}
          {sessionType === 'INNOVATIVE' && (
            <div className="form-section mode-innovative-section">
              <div className="section-header-row">
                <h3 className="section-title">
                  💡 Innovative Concept Intake
                </h3>
                <span className="validation-pill">At least 1 of 3 fields required</span>
              </div>
              <p className="section-helper-text">
                Pitch a nascent idea, an unsolved problem, or a novel mechanism. The panel will challenge feasibility, evaluate market novelty, and formulate recommended tech stacks and a SWOT analysis.
              </p>

              {/* Quick Innovative Starters */}
              <div className="templates-section micro-templates">
                <label className="section-label">⚡ Inspiration Starters</label>
                <div className="template-pills">
                  {INNOVATIVE_TEMPLATES.map(t => (
                    <button
                      type="button"
                      key={t.label}
                      className={`template-pill ${ideaLine === t.ideaLine ? 'active' : ''}`}
                      onClick={() => handleApplyInnovativeTemplate(t)}
                    >
                      {t.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Field 1: Idea / Concept Line */}
              <div className="form-group">
                <div className="field-header">
                  <label htmlFor="idea-line">1. Idea / Concept Line</label>
                  <span className="optional-tag">Optional if Problem or Solution provided</span>
                </div>
                <input
                  id="idea-line"
                  type="text"
                  placeholder="e.g., Decentralized P2P mesh network for disaster relief communications without cellular towers"
                  value={ideaLine}
                  onChange={e => setIdeaLine(e.target.value)}
                />
              </div>

              {/* Field 2: Problem Statement */}
              <div className="form-group">
                <div className="field-header">
                  <label htmlFor="innovative-problem">2. Problem Statement</label>
                  <span className="optional-tag">Optional if Idea or Solution provided</span>
                </div>
                <textarea
                  id="innovative-problem"
                  rows="2"
                  placeholder="e.g., During hurricanes, power grids and cellular towers collapse, stranding citizens without SOS communication."
                  value={innovativeProblem}
                  onChange={e => setInnovativeProblem(e.target.value)}
                />
              </div>

              {/* Field 3: Proposed Solution / Mechanism */}
              <div className="form-group">
                <div className="field-header">
                  <label htmlFor="innovative-solution">3. My Solution / Proposed Mechanism</label>
                  <span className="optional-tag">Optional if Idea or Problem provided</span>
                </div>
                <textarea
                  id="innovative-solution"
                  rows="2"
                  placeholder="e.g., Utilize solar-powered LoRa relays operating on 915MHz frequency with store-and-forward packet encryption."
                  value={innovativeSolution}
                  onChange={e => setInnovativeSolution(e.target.value)}
                />
              </div>

              {/* Domain: Strictly Software vs Hardware */}
              <div className="form-group">
                <label className="domain-label">Target Domain (Software / Hardware Only)</label>
                <div className="domain-selector-toggle">
                  <button
                    type="button"
                    className={`domain-btn ${innovativeDomain === 'Software' ? 'active' : ''}`}
                    onClick={() => setInnovativeDomain('Software')}
                  >
                    <span className="domain-icon">💻</span>
                    <div className="domain-btn-info">
                      <strong>Software</strong>
                      <span>Apps, Web, AI Models, Cryptography, Cloud Engines</span>
                    </div>
                  </button>

                  <button
                    type="button"
                    className={`domain-btn ${innovativeDomain === 'Hardware' ? 'active' : ''}`}
                    onClick={() => setInnovativeDomain('Hardware')}
                  >
                    <span className="domain-icon">⚙️</span>
                    <div className="domain-btn-info">
                      <strong>Hardware</strong>
                      <span>Sensors, Embedded IoT, Microcontrollers, Robotics, Circuitry</span>
                    </div>
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* =========================================================================
              MODE 2: CLIENT PROJECT KICKOFF
             ========================================================================= */}
          {sessionType === 'CLIENT' && (
            <>
              {/* Quick Templates */}
              <div className="templates-section">
                <label className="section-label">⚡ 1-Click Client Project Templates</label>
                <div className="template-pills">
                  {CLIENT_TEMPLATES.map(t => (
                    <button
                      type="button"
                      key={t.label}
                      className={`template-pill ${clientTopic === t.topic ? 'active' : ''}`}
                      onClick={() => handleApplyClientTemplate(t)}
                    >
                      {t.label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="form-section">
                <h3 className="section-title">1. Client Project Intake &amp; Brief</h3>

                <div className="form-group">
                  <label>Project Name / Topic Title</label>
                  <input 
                    type="text" 
                    placeholder="e.g., Cross-Border B2B Escrow & Settlement Engine" 
                    value={clientTopic} 
                    onChange={e => setClientTopic(e.target.value)} 
                    required 
                  />
                </div>

                <div className="form-group">
                  <label>Client Problem Statement &amp; Business Objective</label>
                  <textarea 
                    rows="3"
                    placeholder="Describe the core customer pain point, current failure metrics, and why this project is being commissioned..."
                    value={clientProblem} 
                    onChange={e => setClientProblem(e.target.value)} 
                    required 
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Industry Domain</label>
                    <select value={clientDomain} onChange={e => setClientDomain(e.target.value)}>
                      <option value="FinTech">FinTech (Banking, Payments, Escrow)</option>
                      <option value="HealthTech">HealthTech (Clinical, EHR, HIPAA)</option>
                      <option value="E-Commerce / Retail">E-Commerce / Retail &amp; Direct-to-Consumer</option>
                      <option value="Logistics & Supply Chain">Logistics &amp; Supply Chain Operations</option>
                      <option value="Enterprise SaaS">Enterprise SaaS &amp; B2B Productivity</option>
                      <option value="EdTech">EdTech &amp; Digital Learning</option>
                      <option value="Cybersecurity">Cybersecurity &amp; Threat Detection</option>
                      <option value="GovTech / LegalTech">GovTech &amp; LegalTech Compliance</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label>Delivery Platform / Theme</label>
                    <select value={clientTheme} onChange={e => setClientTheme(e.target.value)}>
                      <option value="Full-Stack Web Application">Full-Stack Web Application</option>
                      <option value="Web & Mobile App (iOS/Android)">Web &amp; Mobile App (iOS/Android)</option>
                      <option value="AI/LLM Agent & RAG System">AI/LLM Agent &amp; RAG System</option>
                      <option value="High-Throughput Microservices API">High-Throughput Microservices API</option>
                      <option value="Data Platform / Lakehouse">Data Platform &amp; Analytics Lakehouse</option>
                      <option value="IoT & Hardware Embedded">IoT &amp; Hardware Embedded System</option>
                    </select>
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Hosting &amp; Cloud Infrastructure Target</label>
                    <select value={clientHosting} onChange={e => setClientHosting(e.target.value)}>
                      <option value="AWS (ECS Fargate + RDS PostgreSQL)">AWS (ECS Fargate + RDS PostgreSQL)</option>
                      <option value="Google Cloud (Cloud Run + Cloud SQL)">Google Cloud Platform (Cloud Run + Cloud SQL)</option>
                      <option value="Microsoft Azure (App Service + CosmosDB)">Microsoft Azure (App Service + Azure SQL)</option>
                      <option value="Serverless (Vercel + Supabase + Redis)">Serverless (Vercel + Supabase + Redis)</option>
                      <option value="Hybrid / On-Premise Bare-Metal">Hybrid / On-Premise Enterprise</option>
                      <option value="Docker / Self-Hosted Cluster">Docker / Self-Hosted Kubernetes</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label>Budget &amp; Scope Tier</label>
                    <select value={clientBudget} onChange={e => setClientBudget(e.target.value)}>
                      <option value="Lean MVP (<$35k / 3-Month Launch)">Lean MVP (&lt;$35k / 3-Month Launch)</option>
                      <option value="Growth-Stage ($50k - $150k)">Growth-Stage ($50k - $150k / 6-Month Build)</option>
                      <option value="Enterprise ($250k+)">Enterprise ($250k+ / Mission-Critical)</option>
                    </select>
                  </div>
                </div>
              </div>
            </>
          )}

          {/* =========================================================================
              COMMON SECTION: BOARDROOM SESSION & EXECUTIVE PANEL
             ========================================================================= */}
          <div className="form-section">
            <h3 className="section-title">
              {sessionType === 'INNOVATIVE' ? '⚙️ Boardroom Session & Executive Panel' : '2. Boardroom Session & Executive Panel'}
            </h3>

            <div className="form-row">
              <div className="form-group">
                <label>Number of Panelists (6 - 10)</label>
                <input 
                  type="number" 
                  min="6" 
                  max="10" 
                  value={charCount} 
                  onChange={e => setCharCount(parseInt(e.target.value))} 
                />
              </div>

              <div className="form-group">
                <label>Session Duration (10 - 30 Mins)</label>
                <input 
                  type="number" 
                  min="10" 
                  max="30" 
                  value={duration} 
                  onChange={e => setDuration(parseInt(e.target.value))} 
                />
              </div>
            </div>

            <label className="section-label">Panelist Roles &amp; Domain Assignments</label>
            <div className="roles-grid">
              {DEFAULT_NAMES.slice(0, charCount).map((name) => (
                <div key={name} className="role-item">
                  <div className="char-badge">
                    <span className="char-avatar-mini">{name[0]}</span>
                    <span className="char-name">{name}</span>
                  </div>
                  <select 
                    value={assignedRoles[name] || ''} 
                    onChange={e => handleRoleChange(name, e.target.value)}
                  >
                    {availableRoles.map(role => (
                      <option key={role} value={role}>{role}</option>
                    ))}
                  </select>
                </div>
              ))}
            </div>
          </div>

          <button type="submit" className="start-btn">
            {sessionType === 'INNOVATIVE' ? '🚀 Launch Innovative Brainstorming Session' : '🚀 Launch Client Project Kickoff'}
          </button>
        </form>
      </div>
    </div>
  );
}