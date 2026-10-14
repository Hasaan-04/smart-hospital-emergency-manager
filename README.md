# 🏥 Smart Hospital Emergency Resource Manager

### A Smart, Centralized Platform for Emergency Healthcare Resource Management

**OpenHack'26 | Problem Statement: HT-03 | Team 007**

The **Smart Hospital Emergency Resource Manager** is a full-stack web application designed to help hospitals efficiently manage emergency patients, monitor available medical resources, and coordinate resource allocation during critical situations.

Our goal is to reduce delays in emergency response, improve resource visibility, and support more efficient hospital operations.

---

## 🌐 Live Application

🚀 **[Launch Smart Hospital Emergency Resource Manager](https://smart-hospital-emergency-manager.vercel.app)**

The application is deployed and accessible online.

| Component | Hosting Platform |
|---|---|
| Frontend | Vercel |
| Backend | Render |
| Database | Aiven MySQL |

**Backend API:** https://smart-hospital-backend-87wx.onrender.com

> **Note:** The backend runs on Render's free hosting tier. The first request after inactivity may take approximately one minute while the server starts.

---

## 🔑 Guest Access — For Hackathon Judges

We have provided a dedicated guest account so that hackathon judges, evaluators, and reviewers can explore the application without creating their own accounts.

### 🔐 Guest Login Credentials

| Field | Credentials |
|---|---|
| **Email / User ID** | `guest@gmail.com` |
| **Password** | `quest1234567` |

### 🧪 How to Test the Application

1. Open the **[Live Application](https://smart-hospital-emergency-manager.vercel.app)**.
2. Log in using the guest credentials above.
3. Explore the dashboard and registered hospitals.
4. View and manage available emergency resources, subject to the guest account's permissions.
5. Register a sample emergency patient.
6. Test resource allocation and observe the updated resource availability.

**Please use fictional patient information for testing.**

This guest account is intended exclusively for demonstration and hackathon evaluation.

---

## 📌 Problem Statement

During medical emergencies, hospitals often face challenges such as:

- Limited visibility into available emergency resources.
- Delays in assigning critical resources to patients.
- Difficulty coordinating emergency operations.
- Inefficient tracking of occupied and available resources.
- Risk of conflicting resource allocations.
- Lack of a centralized platform for managing emergency operations.

These challenges can reduce operational efficiency when time is critical.

## 💡 Our Solution

The **Smart Hospital Emergency Resource Manager** provides a centralized system that combines hospital administration, emergency patient management, resource monitoring, and allocation workflows.

The platform helps authorized users:

- Register and manage hospitals.
- Track available medical resources.
- Register emergency patients.
- Support emergency patient prioritization.
- Allocate resources through coordinated backend operations.
- Monitor hospital operations through an interactive dashboard.

---

## ✨ Key Features

### 🏥 1. Hospital Management

- Register hospitals in the system.
- Manage hospital information.
- Organize emergency resources by hospital.
- View registered hospitals through a centralized interface.

### 🚑 2. Emergency Patient Management

- Register patients requiring emergency care.
- Maintain patient and emergency information.
- Support prioritization according to patient urgency.
- Track patients during emergency management workflows.

### 🛏️ 3. Emergency Resource Management

- Add and manage hospital emergency resources.
- Track available and allocated resources.
- Update resource information.
- Monitor resource availability to support emergency operations.

### ⚡ 4. Resource Allocation

- Allocate available resources to emergency patients.
- Update availability when allocations occur.
- Use backend concurrency controls to reduce conflicting allocations.
- Support coordinated management of limited resources.

### 📊 5. Interactive Dashboard

- Centralized management interface.
- Hospital and resource information.
- Emergency patient management workflows.
- Clear navigation between application features.
- Responsive React-based user interface.

### 🔐 6. Authentication and Security

- Administrator and staff authentication.
- Session-based authentication using Spring Security.
- CSRF protection for authenticated operations.
- Environment-based configuration for database credentials.
- Dedicated guest credentials for hackathon evaluation.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| Frontend | React, Vite, JavaScript, CSS |
| Backend | Java 25, Spring Boot 4, Spring Security |
| Database | MySQL 8.4 |
| ORM | Spring Data JPA / Hibernate |
| Frontend Hosting | Vercel |
| Backend Hosting | Render |
| Database Hosting | Aiven |
| Containerization | Docker |
| Build Tools | Maven, npm |
| Version Control | Git and GitHub |

---

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
          Spring Boot REST API
             Render / Docker
                  |
                  | Spring Data JPA
                  v
             Aiven MySQL
```

### How It Works

1. Users access the application through the React frontend deployed on Vercel.
2. The frontend sends API requests to the Spring Boot backend.
3. Spring Security handles authentication and protected operations.
4. The backend processes hospital, patient, resource, and allocation requests.
5. Spring Data JPA and Hibernate interact with the Aiven MySQL database.
6. Updated information is returned to the frontend.

---

## 📂 Project Structure

```text
smart-hospital-emergency-manager/
|
|-- frontend/
|   |-- public/
|   |-- src/
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

---

## 💻 Running the Project Locally

### Prerequisites

Make sure the following tools are installed:

- Java JDK 25
- Node.js and npm
- MySQL Server
- Git

### Step 1 — Clone the Repository

```bash
git clone https://github.com/Hasaan-04/smart-hospital-emergency-manager.git
cd smart-hospital-emergency-manager
```

### Step 2 — Create the Database

Create a MySQL database:

```sql
CREATE DATABASE emergency_manager;
```

### Step 3 — Configure Backend Environment Variables

The backend reads database credentials and initial administrator settings from environment variables.

| Variable | Description |
|---|---|
| `DB_HOST` | MySQL server hostname |
| `DB_PORT` | MySQL server port |
| `DB_NAME` | MySQL database name |
| `DB_USER` | MySQL username |
| `DB_PASSWORD` | MySQL password |
| `DB_SSL_MODE` | Database SSL configuration |
| `BOOTSTRAP_ADMIN_EMAIL` | Initial administrator email |
| `BOOTSTRAP_ADMIN_PASSWORD` | Initial administrator password |

For a local MySQL instance, typical values are:

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=emergency_manager
DB_USER=root
DB_PASSWORD=your_local_mysql_password
DB_SSL_MODE=PREFERRED
```

Set these variables in your local environment before running the backend.

**Security:** Never commit actual database credentials or private administrator passwords to the repository.

### Step 4 — Start the Backend

Navigate to the backend directory:

```bash
cd backend
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

The backend runs locally at:

`http://localhost:8081`

### Step 5 — Start the Frontend

Open a new terminal and navigate to the frontend directory:

```bash
cd frontend
npm install
npm run dev
```

Open the local development URL displayed by Vite.

For local development, ensure that API requests are routed to the local Spring Boot backend.

---

## 🚀 Cloud Deployment

The application uses three cloud platforms.

### 1. Vercel — Frontend

The React application is built using Vite and deployed on Vercel.

**Live URL:** https://smart-hospital-emergency-manager.vercel.app

### 2. Render — Backend

The Java 25 Spring Boot application is packaged using Docker and deployed as a Render web service.

**Backend URL:** https://smart-hospital-backend-87wx.onrender.com

### 3. Aiven — MySQL Database

The application uses a cloud-hosted MySQL database provided by Aiven.

Database credentials are configured securely through backend environment variables.

---

## 🔄 Example Emergency Management Workflow

1. **Administrator Login:** An authorized user signs in.
2. **Hospital Registration:** A hospital is added to the system.
3. **Resource Configuration:** Emergency resources are registered and their availability is recorded.
4. **Patient Registration:** An emergency patient is entered into the system.
5. **Patient Prioritization:** The patient's urgency is considered during emergency handling.
6. **Resource Allocation:** An available resource is assigned to the patient.
7. **Availability Update:** Resource availability is updated following allocation.
8. **Dashboard Review:** Users monitor hospital information and resource status.

---

## 🌱 Future Enhancements

Potential future improvements include:

- Real-time alerts and emergency notifications.
- Integration with existing hospital information systems.
- Advanced resource allocation recommendations.
- Geographic hospital search and routing.
- Emergency demand forecasting and analytics.
- Expanded role-based access controls.
- Mobile application support.

---

## 👥 Team

**Team 007**

**Hackathon:** OpenHack'26

**Problem Statement:** HT-03

This project was developed to demonstrate how full-stack web technologies and cloud deployment can support better emergency healthcare resource coordination.

---

## ⚠️ Disclaimer

This application is a hackathon prototype developed for educational and demonstration purposes.

It is not certified for clinical use and should not be relied upon for real-world medical decisions or emergency response operations.

All testing should use fictional patient information.

---

### ⭐ Built With

**React • Vite • Java 25 • Spring Boot • Spring Security • MySQL • Docker • Vercel • Render • Aiven**

**Smart Hospital Emergency Resource Manager — Making Emergency Resource Coordination Smarter.**
