# 🏢 Boardroom - Multi-Agent Autonomous Executive Brainstorming Simulator

> **A virtual 2D executive boardroom powered by local LLMs and multi-persona AI agents.**  
> Pitch an innovative concept or launch an enterprise client project kickoff, watch specialized AI board members debate feasibility from their exact roles, bid into the discussion, and receive concrete tech stacks and an executive SWOT analysis.

---

## 📌 Problem & Motivation

### The Problem
- **Echo-Chamber Ideation**: Solo founders, software engineers, and product teams often build in a vacuum, overlooking critical blind spots in cloud architecture, security vulnerabilities, regulatory compliance, data privacy, or hardware feasibility.
- **High Coordination Overhead**: Convening 6 to 10 human domain experts (Lead Architect, Product Manager, DevOps Engineer, Legal Counsel, Compliance Auditor, Growth Hacker) for early-stage brainstorming or client RFP vetting is slow, expensive, and often impossible.
- **Flat AI Responses**: Standard single-prompt chat interfaces provide generic, homogenized answers without authentic debate, conflicting priorities, or rigorous role-based pushback.

### How Boardroom Solves It
1. **Specialized Multi-Agent Panel**: Assembles a simulated boardroom table of **6 to 10 AI executives**, each driven by a distinct domain skillset profile defining their core skills, responsibilities, behavioral DOs, and strict DON'Ts.
2. **Dynamic Turn-Taking with Bid Interjections**: The board deliberates naturally around a virtual conference table. After each panelist speaks, an **inter-turn countdown timer (45 seconds)** opens the floor for the human host to **"BID"** (interject an objection or guide the conversation) before the next member responds.
3. **Two Tailored Brainstorming Methodologies**:
   - **💡 Innovative Brainstorming**: Lightweight intake focusing on novelty, feasibility, and technical hurdles across **Software** or **Hardware** domains.
   - **🏢 Client Project Kickoff**: Structured corporate RFP intake capturing problem statements, target cloud hosting, platform delivery themes, and budget tiers.
4. **Automated Deliverables**: At meeting adjournment, the engine synthesizes the entire transcript into an **Innovation Assessment & Recommended Tech Stacks** (or an **Enterprise Project Charter**) along with a comprehensive **SWOT Analysis**.

---

## 🏛️ System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            FRONTEND (React + Vite)                          │
│                                                                             │
│   ┌──────────────────────┐   ┌────────────────────┐   ┌─────────────────┐   │
│   │ SetupForm (Dual Mode)│   │  Boardroom Table   │   │ Interactive Bid │   │
│   │ • Innovative Intake  │──▶│  • 10-Seat Layout  │◀─▶│ • 45s Countdown │   │
│   │ • Client RFP Intake  │   │  • Live Whiteboard │   │ • Human In Loop │   │
│   └──────────────────────┘   └────────────────────┘   └─────────────────┘   │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ REST / JSON (HTTP)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Spring Boot 3.2.4)                         │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ SessionController (/api/sessions)                                   │   │
│   │ • /start  • /npc-turn  • /user-bid  • /transcript  • /analysis      │   │
│   └──────────────────┬───────────────────────────────┬──────────────────┘   │
│                      │                               │                      │
│                      ▼                               ▼                      │
│   ┌──────────────────────────────────┐   ┌──────────────────────────────┐   │
│   │ BrainstormService                │   │ SkillsetLoader (@Service)    │   │
│   │ • Session state orchestration    │   │ • Scans /skillset/*.json     │   │
│   │ • Transcript logging & retrieval │   │ • 20+ Cached RoleSkillsets   │   │
│   └──────────────────┬───────────────┘   └──────────────┬───────────────┘   │
│                      │                                  │                   │
│                      ▼                                  ▼                   │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ OllamaService (Local LLM Inference)                                 │   │
│   │ • Dynamic persona prompt synthesis (DOs, DON'Ts, Context)           │   │
│   │ • Novelty & feasibility evaluation                                  │   │
│   │ • Automated SWOT & Tech Stack synthesis generation                  │   │
│   └──────────────────┬──────────────────────────────────────────────────┘   │
└──────────────────────┼──────────────────────────────────────────────────────┘
                       │
       ┌───────────────┴───────────────┐
       ▼                               ▼
┌──────────────┐              ┌────────────────┐
│ MySQL DB     │              │ Local Ollama   │
│ (Persistence)│              │ (llama3 / API) │
└──────────────┘              └────────────────┘
```

---

## 💻 Technology Stack

### Backend
| Technology | Purpose |
| :--- | :--- |
| **Java 21 (LTS)** | Core backend language |
| **Spring Boot 3.2.4** | Web framework, REST controllers, and dependency injection |
| **Spring Data JPA & Hibernate** | Object-relational persistence and schema generation |
| **MySQL 8.x** | Persistent storage for boardroom sessions, briefs, and transcripts |
| **Ollama HTTP Client** | Local, private, zero-latency inference engine (e.g. `llama3`) |
| **Jackson JSON** | JSON serialization and parsing of custom skillset models |
| **Maven Wrapper** | Build orchestration and dependency management |

### Frontend
| Technology | Purpose |
| :--- | :--- |
| **React 18** | Modular UI components and real-time state management |
| **Vite** | Blazing-fast development server and production bundler |
| **Vanilla CSS3** | Custom design system with glassmorphism, animations, and CSS variables |
| **Axios** | HTTP client for backend REST communication |

---

## 🌟 Key Features

### 1. Dual Brainstorming Modes (Toggle Switcher)
Easily switch between two specialized ideation workflows right from the setup dashboard:

- **💡 Innovative Brainstorming Mode**:
  - **3 Flexible Fields**:
    1. *Idea / Concept Line* (tagline or single-sentence pitch)
    2. *Problem Statement* (the core unmet customer pain point)
    3. *Proposed Solution / Mechanism* (the technical angle or secret sauce)
  - **Relaxed Smart Validation**: Not mandatory to fill all 3 fields—**providing any 1 of the 3 fields** is sufficient to launch the boardroom.
  - **Domain Filter**: Strictly **Software** (`apps, web, algorithms, cryptography`) or **Hardware** (`sensors, microcontrollers, IoT, firmware`).
  - **1-Click Inspiration Starters**: Pre-configured templates (e.g., Edge AI Wearable for Deaf Assistance, Zero-Knowledge Identity Verification).

- **🏢 Client Project Kickoff Mode**:
  - Structured client intake form for enterprise software development.
  - Specify **Industry Domain** (FinTech, HealthTech, E-Commerce, Logistics, SaaS, etc.).
  - Select **Delivery Platform Theme** (Full-Stack Web, iOS/Android Mobile, AI/LLM Agent, Data Lakehouse).
  - Target **Cloud Infrastructure** (AWS Fargate, Google Cloud Run, Azure App Service, Serverless Vercel, Bare-Metal).
  - Define **Budget & Scope Tier** (Lean MVP <$35k, Growth-Stage $50k-$150k, Enterprise $250k+).
  - 1-Click Client RFP presets for instant demonstration.

---

### 2. Multi-Persona Skillset System
Each panelist isn't just given a generic role prompt; their personality and perspective are injected from structured JSON skillset files located in `backend/skillset/`:
- **Role Title & Domain Focus**
- **Core Skills & Technical Competencies**
- **Explicit Responsibilities**
- **Behavioral Guidelines (DOs)**
- **Strict Guardrails (DON'Ts)**

Available preset personas include:
`Lead Developer`, `Product Manager`, `QA Lead`, `Security Specialist`, `DevOps/Infrastructure Architect`, `UI/UX Designer`, `Data Privacy Officer (GDPR/HIPAA)`, `Legal Counsel`, `Compliance Auditor`, `Growth Hacker`, `Operations Director`, `Scrum Master`, `Customer Support Lead`, and more.

---

### 3. Interactive Boardroom with Floor Bidding
- **2D Virtual Table**: Visual layout displaying the active speaker with animated glow effects, avatar initials, and domain titles.
- **Center Whiteboard**: Live updating transcription feed showing each panelist's argument and technical challenges.
- **45-Second Inter-Turn Bid Timer**:
  - When an AI panelist finishes speaking, an animated progress bar and countdown indicator announce: `FLOOR OPEN FOR BID (45s)`.
  - The human host can click **🎙️ BID (MY POINT)** at any time during this countdown.
  - The countdown pauses while you compose your thought. Once submitted, the AI panelists immediately address your interjection.
  - If the timer reaches 0 without a bid, the floor automatically passes to the next panelist in sequence.

---

### 4. Automated Tech Stack & SWOT Synthesis
At the conclusion of the meeting (or by clicking **End Session & Get Spec**), the backend triggers a comprehensive synthesis:
- **For Innovative Sessions**:
  1. Executive Concept Summary & Novelty Evaluation
  2. Concrete **Recommended Technology & Engineering Stack** (microcontrollers, sensors, communication protocols for hardware; frameworks, databases, and APIs for software)
  3. **SWOT Analysis** (Strengths, Weaknesses, Opportunities, Threats)
  4. **MVP Prototyping Roadmap** with 4-phase execution milestones and success metrics
- **For Client Sessions**:
  1. Executive Summary & Problem Validation
  2. Technical Architecture & Cloud Infrastructure Strategy
  3. Core MVP Prioritized Feature Breakdown (P0 vs P1)
  4. Compliance, Security & Data Privacy Matrix
  5. SWOT & Risk Register with estimated monthly hosting OPEX

---

## 🚀 Getting Started

### Prerequisites
1. **Java Development Kit (JDK) 21**: Make sure Java 21 is installed and available on your PATH.
   ```bash
   java -version
   ```
2. **Node.js (v18+) & npm**:
   ```bash
   node -v
   npm -v
   ```
3. **MySQL Server**: Running on `localhost:3306`.
4. **Ollama**: Running locally on `http://localhost:11434` with your preferred model downloaded:
   ```bash
   ollama run llama3
   ```

---

### Step 1: Configure MySQL Database
Open your MySQL client or CLI and create the database:
```sql
CREATE DATABASE IF NOT EXISTS boardroom_db;
```
Configure your MySQL credentials in `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/boardroom_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.jpa.hibernate.ddl-auto=update
```

---

### Step 2: Configure Ollama Local LLM
In `backend/src/main/resources/application.properties`, verify the Ollama endpoint:
```properties
ollama.api.url=http://localhost:11434/api/generate
ollama.model.name=llama3
```

---

### Step 3: Run the Backend
From the root repository directory:
```bash
cd backend

# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
chmod +x mvnw
./mvnw spring-boot:run
```
The backend will launch on `http://localhost:8080`. You will see the `SkillsetLoader` cache all role skillsets upon startup:
```
INFO: SkillsetLoader initialized with 20 unique role skillsets cached.
INFO: Tomcat started on port 8080 (http)
```

---

### Step 4: Run the Frontend
In a new terminal window:
```bash
cd frontend
npm install
npm run dev
```
The Vite development server will start on `http://localhost:5173`. Open this URL in any modern web browser.

---

## 📡 REST API Reference

| Method | Endpoint | Description | Payload Example |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/sessions/roles` | Lists all cached skillset roles | *None* |
| `POST` | `/api/sessions/start` | Creates a new session (Innovative or Client) | `{"sessionType": "INNOVATIVE", "topic": "Edge AI", "domain": "Hardware", ...}` |
| `POST` | `/api/sessions/{id}/npc-turn` | Triggers a turn for a specific AI panelist | `{"speakerName": "Alex", "speakerRole": "Lead Developer"}` |
| `POST` | `/api/sessions/{id}/user-bid` | Records a point/objection from the human host | `{"message": "We should use WebSockets instead of polling."}` |
| `GET` | `/api/sessions/{id}/transcript` | Retrieves all chronological session messages | *None* |
| `GET` | `/api/sessions/{id}/analysis` | Synthesizes transcript into Tech Stacks & SWOT | *None* |

---

## 📂 Project Structure

```
boardroom/
├── backend/
│   ├── skillset/                       # Role definitions (JSON) with DOs/DON'Ts
│   │   ├── lead_developer.json
│   │   ├── product_manager.json
│   │   ├── security_specialist.json
│   │   ├── devops-infrastructure_architect.json
│   │   ├── legal_counsel.json
│   │   └── ...
│   ├── src/main/java/com/boardroom/backend/
│   │   ├── controller/
│   │   │   └── SessionController.java  # REST API endpoints
│   │   ├── dto/
│   │   │   └── RoleSkillset.java       # DTO for deserializing skillset JSONs
│   │   ├── model/
│   │   │   ├── Session.java            # JPA entity (Session specs, domain, type)
│   │   │   └── Transcript.java         # JPA entity (Speaker, role, timestamp)
│   │   ├── repository/
│   │   │   ├── SessionRepository.java
│   │   │   └── TranscriptRepository.java
│   │   └── service/
│   │       ├── BrainstormService.java  # Business logic & turn coordination
│   │       ├── SkillsetLoader.java     # Startup scanner for /skillset/*.json
│   │       └── OllamaService.java      # Local LLM persona & SWOT generator
│   ├── src/main/resources/
│   │   └── application.properties     # Database, server & Ollama configuration
│   ├── pom.xml
│   └── mvnw.cmd
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── SetupForm.jsx          # Dual-mode setup form (Innovative/Client)
│   │   │   ├── SetupForm.css          # Mode switcher & intake styling
│   │   │   ├── Boardroom.jsx          # 2D table stage, countdown, bid modal
│   │   │   ├── Boardroom.css          # Badges, avatars, animations
│   │   │   ├── Whiteboard.jsx         # Live center transcript feed
│   │   │   └── Whiteboard.css         # Glassmorphism whiteboard styling
│   │   ├── services/
│   │   │   └── api.js                 # Axios API connector
│   │   ├── App.jsx                    # Root view controller & spec modal
│   │   └── App.css
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
│
└── README.md
```

---

## 🛡️ License

Distributed under the **MIT License**. Feel free to customize personas, prompts, and boardroom dynamics for your own products and organizations.
