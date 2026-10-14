# 🏥 Smart Hospital Emergency Resource Manager

**A real-time emergency resource management system designed to help hospitals coordinate patient care, monitor resource availability, and allocate critical resources efficiently.**

**OpenHack'26 | Problem Statement: HT-03 | Team 007**

## 🌐 Live Application

**[Launch Smart Hospital Emergency Resource Manager](https://smart-hospital-emergency-manager.vercel.app)**

| Component | Platform |
|---|---|
| Frontend | Vercel |
| Backend API | Render |
| Database | Aiven MySQL |
| Source Code | GitHub |

**Backend:** https://smart-hospital-backend-87wx.onrender.com

> **Note:** The backend uses Render's free hosting tier, so the first request after inactivity may take some time.

---

## 📌 Problem Statement

During emergencies, hospitals must manage limited resources while responding to patients with different levels of urgency.

Manual coordination can lead to delayed treatment, inaccurate resource availability, and inefficient allocation of critical medical resources.

The **Smart Hospital Emergency Resource Manager** addresses these challenges through a centralized digital platform that supports patient management, resource tracking, and emergency resource allocation.

## 🎯 Our Solution

Our system connects hospital management, emergency patient handling, and resource allocation in a single web application.

It provides an interface for administrators and authorized staff to manage hospital information, monitor available resources, and coordinate emergency operations.

## ✨ Key Features

### 🏥 Hospital Management
- Register and manage hospitals.
- Maintain hospital information.
- Track hospital resources through a centralized system.

### 🚑 Emergency Patient Management
- Register emergency patients.
- Record patient information and emergency details.
- Support patient prioritization based on urgency.

### 🛏️ Resource Management
- Monitor emergency resource availability.
- Add and update hospital resources.
- Track resource allocation and availability.

### ⚡ Emergency Resource Allocation
- Allocate available resources to patients.
- Manage resource availability during allocation.
- Use backend concurrency controls to reduce conflicting allocations.

### 📊 Management Dashboard
- Centralized hospital and emergency management interface.
- View relevant resource and patient information.
- Perform administrative operations through an interactive dashboard.

### 🔐 Authentication and Security
- Administrator and staff authentication.
- Session-based authentication using Spring Security.
- CSRF protection for authenticated operations.
- Database credentials stored in environment variables rather than source code.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| Frontend | React, Vite, JavaScript, CSS |
| Backend | Java 25, Spring Boot 4, Spring Security |
| Database | MySQL 8.4 |
| Database Hosting | Aiven |
| Backend Deployment | Render, Docker |
| Frontend Deployment | Vercel |
| Version Control | Git, GitHub |
| Development Tools | VS Code, MySQL Workbench, Maven |

## 🏗️ System Architecture

```text
                USER
                 |
                 v
         React + Vite Frontend
                Vercel
                 |
                 | HTTPS API Requests
                 v
         Spring Boot Backend
            Render + Docker
                 |
                 | JDBC / JPA
                 v
          Aiven Cloud MySQL
```

The frontend communicates with the Spring Boot backend through API requests. The backend handles application logic, authentication, resource management, and database operations.

---

## 📂 Project Structure

```text
smart-hospital-emergency-manager/
|
|-- frontend/
|   |-- src/
|   |-- public/
|   |-- package.json
|   |-- vite.config.js
|   |-- vercel.json
|
|-- backend/
|   |-- src/
|   |-- pom.xml
|   |-- mvnw
|   |-- mvnw.cmd
|   |-- Dockerfile
|
|-- .gitignore
|-- README.md
```

## 💻 Running the Project Locally

### Prerequisites

Install the following:

- Java JDK 25
- Node.js and npm
- MySQL
- Git

### 1. Clone the Repository

```bash
git clone https://github.com/Hasaan-04/smart-hospital-emergency-manager.git
cd smart-hospital-emergency-manager
```

### 2. Configure the Database

Create a MySQL database:

```sql
CREATE DATABASE emergency_manager;
```

Configure these environment variables for the backend:

| Variable | Description |
|---|---|
| `DB_HOST` | MySQL hostname |
| `DB_PORT` | MySQL port (default: 3306) |
| `DB_NAME` | Database name |
| `DB_USER` | Database username |
| `DB_PASSWORD` | Database password |
| `DB_SSL_MODE` | MySQL SSL mode |
| `BOOTSTRAP_ADMIN_EMAIL` | Initial administrator email |
| `BOOTSTRAP_ADMIN_PASSWORD` | Initial administrator password |

**Never commit real database passwords or administrator credentials to GitHub.**

### 3. Run the Backend

```bash
cd backend
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

On Linux/macOS:

```bash
./mvnw spring-boot:run
```

By default, the backend runs at:

```text
http://localhost:8081
```

### 4. Run the Frontend

Open a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the local URL displayed by Vite.

For local development, configure API proxying to the local backend.

---

## 🚀 Deployment

The application is deployed using three cloud services:

**1. Vercel — Frontend**

Hosts the React application and serves the public website.

**2. Render — Backend**

Runs the Spring Boot application inside a Docker container using Java 25.

**3. Aiven — Database**

Provides a hosted MySQL database for persistent application data.

Production database credentials and administrator bootstrap settings are configured using environment variables.

---

## 🔄 Example Workflow

1. An administrator logs into the system.
2. A hospital is registered.
3. Available emergency resources are configured.
4. An emergency patient is registered.
5. The system supports prioritization and resource allocation.
6. Resource availability is updated as operations are performed.
7. Hospital staff can review information through the dashboard.

## 🌱 Future Improvements

Potential enhancements include:

- Integration with hospital information systems.
- Real-time notifications and alerts.
- Advanced resource allocation recommendations.
- Geographic hospital search and emergency routing.
- Analytics and resource demand forecasting.
- Expanded role-based access controls.

## 👥 Team

**Team 007 — OpenHack'26**

Developed as a hackathon project to explore how modern web technologies can improve emergency resource coordination in healthcare.

## ⚠️ Disclaimer

This application is a hackathon prototype intended for demonstration and educational purposes. It is not certified for clinical decision-making or production use with real patient data.

---

**Built with React, Java, Spring Boot, MySQL, Docker, Vercel, and Render.**
