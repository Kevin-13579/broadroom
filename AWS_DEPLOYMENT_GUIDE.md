# ☁️ Boardroom - AWS Cloud Deployment & Architecture Guide

> **Cloud Computing Mini-Project Guide**  
> Comprehensive instructions on selected AWS services, architecture, safe Git workflow, and step-by-step production deployment.

---

## 📋 Table of Contents
1. [Which AWS Services to Use & Why](#1-which-aws-services-to-use--why)
2. [Cloud Architecture Diagram](#2-cloud-architecture-diagram)
3. [Pre-Flight Security Checklist](#3-pre-flight-security-checklist)
4. [Step-by-Step: Pushing Safely to GitHub](#4-step-by-step-pushing-safely-to-github)
5. [Step-by-Step: AWS Setup & Deployment](#5-step-by-step-aws-setup--deployment)
   - [Phase A: Security Group & VPC Configuration](#phase-a-security-group--vpc-configuration)
   - [Phase B: Create Amazon RDS MySQL Database](#phase-b-create-amazon-rds-mysql-database)
   - [Phase C: Launch Amazon EC2 Instance](#phase-c-launch-amazon-ec2-instance)
   - [Phase D: Deploy Application Stack via Docker Compose](#phase-d-deploy-application-stack-via-docker-compose)
6. [Testing & Verifying the Deployment](#6-testing--verifying-the-deployment)
7. [Cloud Viva / Project Presentation Talking Points](#7-cloud-viva--project-presentation-talking-points)
8. [Clean-up & Cost Optimization (Free Tier Guardrails)](#8-clean-up--cost-optimization-free-tier-guardrails)

---

## 1. Which AWS Services to Use & Why

For an academic or portfolio cloud computing project, **running everything inside a single virtual machine (EC2) is considered an anti-pattern**. Evaluators look for **decoupled tiers, managed services, network security, and observability**.

This project implements a **Cloud-Native Decoupled 3-Tier Architecture**:

| Service | Category | Role in This Project | Free Tier Eligibility |
| :--- | :--- | :--- | :--- |
| **Amazon EC2** (`t3.small` / `t3.micro`) | Compute | Runs the containerized Spring Boot backend (Java 21) and the React frontend served by Nginx. | 750 hours/month free (`t2.micro` or `t3.micro`) |
| **Amazon RDS (MySQL 8.0)** | Managed Database | Stores users, rooms, board sessions, and SWOT analyses. Automated backups, health monitoring, and data durability. | 750 hours/month free (`db.t3.micro` / `db.t4g.micro`) |
| **AWS VPC & Security Groups** | Networking & Security | Enforces network isolation. The database is shielded in a private security group accessible **only** by the EC2 instance. | Always Free |
| **Amazon CloudWatch** | Monitoring | Monitors CPU utilization, memory, network I/O, and tracks backend application health. | Basic metrics always free |
| **AWS IAM** | Identity & Access | Manages least-privilege policies for EC2 and developer administration without using the Root account. | Always Free |

*(Optional Extension: For high-scale static hosting, the React frontend build files in `frontend/dist` can alternatively be served via **Amazon S3 + CloudFront CDN**).*

---

## 2. Cloud Architecture Diagram

```
                 Internet Users / Web Browsers
                               │
                               │ HTTPS / HTTP (Port 80)
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ AWS VPC (Virtual Private Cloud)                                             │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ Public Subnet (Security Group: boardroom-ec2-sg)                      │  │
│  │                                                                       │  │
│  │   ┌─────────────────────────────────────────────────────────────────┐ │  │
│  │   │ Amazon EC2 Instance                                             │ │  │
│  │   │                                                                 │ │  │
│  │   │  ┌─────────────────────────┐       ┌──────────────────────────┐ │ │  │
│  │   │  │ Frontend Container      │       │ Backend Container        │ │ │  │
│  │   │  │ (Nginx Web Server)      │◀─────▶│ (Spring Boot 3.2.4)      │ │ │  │
│  │   │  │ • Serves React SPA      │ Proxy │ • REST API (/api/*)      │ │ │  │
│  │   │  │ • Port 80               │ :8080 │ • STOMP WebSocket (/ws)  │ │ │  │
│  │   │  │                         │       │ • Health Check (/health) │ │ │  │
│  │   │  └─────────────────────────┘       └────────────┬─────────────┘ │ │  │
│  │   └─────────────────────────────────────────────────┼───────────────┘ │  │
│  └─────────────────────────────────────────────────────┼─────────────────┘  │
│                                                        │                    │
│                                                        │ MySQL TCP:3306     │
│                                                        │ (Private Link)     │
│                                                        ▼                    │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ Private Subnet / DB Security Group: boardroom-rds-sg                  │  │
│  │                                                                       │  │
│  │   ┌─────────────────────────────────────────────────────────────────┐ │  │
│  │   │ Amazon RDS (Managed MySQL 8.0)                                  │ │  │
│  │   │ • Database: boardroom_db                                        │ │  │
│  │   │ • Automated snapshots & storage management                      │ │  │
│  │   │ • Strictly closed to direct public internet traffic             │ │  │
│  │   └─────────────────────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
                    External Cloud AI Service
                 Google Gemini 2.0 Flash REST API
```

---

## 3. Pre-Flight Security Checklist

Before touching Git or AWS, verify:
- [x] **No hardcoded credentials**: The repository uses environment variables (`SPRING_DATASOURCE_URL`, `GEMINI_API_KEY`, `JWT_SECRET`).
- [x] **Git Ignore verified**: `.gitignore` is configured to ignore `.env`, `.env.*`, `*.pem`, `*.key`, and build artifacts.
- [x] **Dynamic CORS configured**: `WebConfig.java` and `SecurityConfig.java` dynamically accept the AWS domain/IP.
- [x] **Dedicated Health Check endpoint added**: `GET /api/health` returns HTTP 200 for cloud monitoring.

---

## 4. Step-by-Step: Pushing Safely to GitHub

### 4.1 Verify Status & Untracked Files
Open your terminal in `c:\Users\kevin\Desktop\boardroom` and check:
```bash
git status
```
Ensure no `.env` or secret keys are listed.

### 4.2 Stage, Commit, and Push
```bash
# 1. Stage all project modifications
git add .

# 2. Commit the changes
git commit -m "feat(aws): configure cloud deployment, health checks, nginx proxy, and docker-compose"

# 3. Push to main branch
git push origin main
```

---

## 5. Step-by-Step: AWS Setup & Deployment

### Phase A: Security Group & VPC Configuration
In the **AWS Management Console**:

1. Navigate to **EC2** > **Security Groups** > **Create Security Group**.
2. **Security Group 1: `boardroom-ec2-sg`** (For the Web & App Server):
   - **Inbound Rules**:
     - `HTTP` | TCP | Port `80` | Source: `Anywhere-IPv4` (`0.0.0.0/0`)
     - `SSH` | TCP | Port `22` | Source: `My IP` (For secure administration)
     - *(Optional)* `Custom TCP` | Port `8080` | Source: `Anywhere-IPv4` (Only if accessing backend directly)

3. **Security Group 2: `boardroom-rds-sg`** (For the Database):
   - **Inbound Rules**:
     - `MYSQL/Aurora` | TCP | Port `3306` | Source: Choose **Security Group** > select `boardroom-ec2-sg`.
     *(This guarantees that only your EC2 server can communicate with MySQL. The database is invisible to the public internet).*

---

### Phase B: Create Amazon RDS MySQL Database

1. Open the **Amazon RDS Console** > click **Create database**.
2. **Engine options**: Choose **MySQL** (version 8.0.x).
3. **Templates**: Choose **Free tier**.
4. **Settings**:
   - DB instance identifier: `boardroom-db`
   - Master username: `admin`
   - Master password: `CreateAStrongPassword123!` (Save this securely)
5. **Instance configuration**:
   - `db.t3.micro` or `db.t4g.micro` (Free Tier eligible).
6. **Connectivity**:
   - Virtual Private Cloud (VPC): Default VPC
   - Public access: **No** (Best practice: keep DB private)
   - Existing VPC security groups: Select `boardroom-rds-sg` (remove 'default').
7. **Additional configuration**:
   - Initial database name: `boardroom_db`
8. Click **Create database** (Takes ~5 minutes to provision).
9. Once **Status** is `Available`, click on `boardroom-db` and copy the **Endpoint**:
   *(Example: `boardroom-db.c3xxxxxx.us-east-1.rds.amazonaws.com`)*

---

### Phase C: Launch Amazon EC2 Instance

1. Navigate to **EC2 Console** > click **Launch Instance**.
2. **Name**: `boardroom-server`
3. **Application and OS Images**: **Ubuntu Server 24.04 LTS** or **Ubuntu 22.04 LTS** (64-bit x86).
4. **Instance type**: `t3.small` (Recommended for building Java 21) or `t3.micro` (with swap space).
5. **Key pair (login)**: Click **Create new key pair**:
   - Name: `boardroom-key`
   - Type: `RSA`, format: `.pem`
   - Download and store `boardroom-key.pem` in a safe location.
6. **Network settings**:
   - Select **Select existing security group** > choose `boardroom-ec2-sg`.
7. **Storage**: 20 GiB gp3 (within 30 GB Free Tier limit).
8. Click **Launch Instance**.

---

### Phase D: Deploy Application Stack via Docker Compose

#### 1. Connect to your EC2 instance via SSH:
On Windows PowerShell:
```powershell
ssh -i "path\to\boardroom-key.pem" ubuntu@<YOUR-EC2-PUBLIC-IP>
```
*(On Linux/Mac: run `chmod 400 boardroom-key.pem` first).*

#### 2. Install Docker & Docker Compose on EC2:
Run these commands inside the EC2 terminal:
```bash
# Update package index
sudo apt update && sudo apt upgrade -y

# Install Docker
sudo apt install -y docker.io docker-compose-v2 git

# Allow ubuntu user to run docker without sudo
sudo usermod -aG docker $USER
newgrp docker

# Verify installations
docker --version
docker compose version
```

#### 3. (Optional but recommended for t3.micro/t3.small) Enable 2GB Swap Memory:
```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

#### 4. Clone your GitHub repository:
```bash
git clone https://github.com/Kevin-13579/broadroom.git
cd broadroom
```

#### 5. Create the `.env` production file:
```bash
cp .env.example .env
nano .env
```
Fill in your actual production values:
```env
SPRING_DATASOURCE_URL=jdbc:mysql://<YOUR-RDS-ENDPOINT>:3306/boardroom_db?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=CreateAStrongPassword123!
GEMINI_API_KEY=AIzaSyYourActualGeminiKey
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
APP_CORS_ALLOWED_ORIGINS=*
VITE_API_URL=
```
Press `Ctrl + O`, then `Enter` to save, and `Ctrl + X` to exit `nano`.

#### 6. Build and launch the stack:
```bash
docker compose -f docker-compose.aws.yml up -d --build
```
Docker will automatically build the backend JAR, build the optimized React static bundle, configure Nginx, and launch both containers!

#### 7. Check container status & logs:
```bash
docker compose -f docker-compose.aws.yml ps
docker logs -f boardroom-backend
```

---

## 6. Testing & Verifying the Deployment

1. **Verify Backend Health**:
   Run in your local browser or terminal:
   ```bash
   curl http://<YOUR-EC2-PUBLIC-IP>/api/health
   ```
   Expected response:
   ```json
   {
     "status": "UP",
     "service": "boardroom-backend",
     "cloud": "AWS",
     "timestamp": "2026-09-24T14:10:00Z"
   }
   ```

2. **Access the Live Web App**:
   Navigate to:
   ```
   http://<YOUR-EC2-PUBLIC-IP>
   ```
   - Register a new account / Log in.
   - Create an executive boardroom room.
   - Start an innovative brainstorming session or client kickoff.
   - Observe live real-time WebSocket communication and automated SWOT analysis generation!

3. **Check CloudWatch Metrics**:
   - Go to the **EC2 Console** > Select your instance > Click the **Monitoring** tab.
   - Take screenshots of **CPU Utilization**, **Network In**, and **Disk Read/Write Operations** to include in your project documentation!

---

## 7. Cloud Viva / Project Presentation Talking Points

Use these points when explaining your architecture to professors or interviewers:

1. **Why didn't you put MySQL inside the EC2 container?**
   > *"In production cloud environments, stateful components (databases) must be decoupled from stateless application containers. Amazon RDS provides automated OS patching, multi-AZ failover capability, point-in-time recovery, and independent vertical scaling without taking down our application."*

2. **How did you secure the database?**
   > *"We used AWS Security Group chaining. The RDS instance has no public IP and resides in a private security group that permits traffic on port 3306 ONLY from the EC2 instance's security group ID. Even if an attacker knows the database endpoint, they cannot establish a connection from the outside world."*

3. **How does real-time communication work through the cloud?**
   > *"We configured Nginx as a reverse proxy on Port 80. When a client connects to the STOMP/SockJS endpoint (`/ws-boardroom`), Nginx upgrades the HTTP connection using `Upgrade: websocket` headers, transparently maintaining persistent full-duplex TCP connections with the Spring Boot backend."*

4. **How are secrets managed?**
   > *"Sensitive credentials (Gemini API keys, database credentials, JWT secrets) are injected at runtime via environment variables and Docker secrets. They are strictly excluded from version control using `.gitignore`."*

---

## 8. Clean-up & Cost Optimization (Free Tier Guardrails)

When you have completed your project demonstration and submission:

1. **To avoid unintended AWS charges**:
   - **EC2**: If not in use, select the instance and click **Instance state** > **Stop instance** (or **Terminate** if project is over).
   - **RDS**: Select your database > **Actions** > **Stop temporarily** (or **Delete** if you do not need the data, deselect 'Create final snapshot' for immediate deletion).
   - **Elastic IPs**: If you allocated an Elastic IP, release it after terminating the instance (AWS charges for unattached Elastic IPs).
2. **AWS Budget Alert**:
   - In AWS Console, search for **AWS Budgets**.
   - Create a budget with a threshold of **$1.00** to receive an immediate email notification if any service exceeds the Free Tier limits.
