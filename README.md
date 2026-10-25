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
| Database | TiDB Cloud Starter |

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

This guest account is intended exclusively for demonstration and hackathon evaluation. Limit its permissions and rotate or disable these publicly documented credentials when judge access is no longer needed.

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

### ⚡ 4. Smart-Assisted Manual Allocation (SA-MA)

- Present staff with available beds, doctors, and equipment, including eligibility explanations.
- Require staff confirmation before assigning resources to an arrived patient.
- Validate bed type, oxygen and ventilator requirements, doctor specialization and shift, and equipment location.
- Update patient and resource statuses together when an allocation is confirmed.
- Complete treatment and release assigned resources through a dedicated workflow.
- Use database transactions and row-level locks to prevent conflicting concurrent allocations.

### 🚑 5. Pre-Arrival and Emergency Coordination

- Record arrived and pre-arrival emergency cases.
- Track urgency and arrival status for the emergency queue.
- Manage ambulance-related emergency workflows without live GPS tracking.

### 📊 6. Interactive Dashboard

- Centralized management interface.
- Hospital and resource information.
- Emergency patient management workflows.
- Clear navigation between application features.
- Responsive React-based user interface.

### 🔐 7. Authentication and Security

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
| Database | TiDB Cloud Starter (MySQL-compatible SQL) |
| ORM | Spring Data JPA / Hibernate |
| Frontend Hosting | Vercel |
| Backend Hosting | Render |
| Database Hosting | TiDB Cloud |
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
       TiDB Cloud Starter
         (TLS / 4000)
```

### How It Works

1. Users access the application through the React frontend deployed on Vercel.
2. The frontend sends API requests to the Spring Boot backend.
3. Spring Security handles authentication and protected operations.
4. The backend processes hospital, patient, resource, and allocation requests.
5. Spring Data JPA and Hibernate interact with TiDB Cloud over an encrypted MySQL-compatible connection.
6. For allocation and release operations, the backend uses TiDB-compatible `SELECT ... FOR UPDATE` row locking within transactions.
7. Updated information is returned to the frontend.

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
- A MySQL-compatible database: local MySQL for development **or** a TiDB Cloud Starter instance
- Git

### Step 1 — Clone the Repository

```bash
git clone https://github.com/Hasaan-04/smart-hospital-emergency-manager.git
cd smart-hospital-emergency-manager
```

### Step 2 — Create the Database

For local development, create a database in your local MySQL server. For a cloud-backed setup, run the same statement in the TiDB Cloud SQL editor:

```sql
CREATE DATABASE emergency_manager;
```

### Step 3 — Configure Backend Environment Variables

The backend reads database credentials and initial administrator settings from environment variables.

| Variable | Description |
|---|---|
| `DB_HOST` | MySQL or TiDB Cloud server hostname |
| `DB_PORT` | `3306` for typical local MySQL; `4000` for TiDB Cloud Starter |
| `DB_NAME` | MySQL database name |
| `DB_USER` | MySQL username |
| `DB_PASSWORD` | MySQL password |
| `DB_SSL_MODE` | JDBC SSL mode; use `REQUIRED` for TiDB Cloud |
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

For **TiDB Cloud Starter**, use the connection details from your own TiDB Cloud dashboard:

```env
DB_HOST=<your-tidb-endpoint>
DB_PORT=4000
DB_NAME=emergency_manager
DB_USER=<your-full-tidb-username>
DB_PASSWORD=<your-tidb-password>
DB_SSL_MODE=REQUIRED
```

Use the **full username** provided by TiDB Cloud; it may include a cluster-specific prefix. Keep the password in environment variables or a secrets manager, not in source control. The backend constructs its JDBC connection using these variables. The TiDB Cloud connection requires TLS.

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

Open a new terminal in the **repository root** and navigate to the frontend directory:

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

### 3. TiDB Cloud Starter — Database

The deployed application uses **TiDB Cloud Starter**, a MySQL-compatible managed database. The earlier Aiven MySQL deployment has been replaced.

- **Database name:** `emergency_manager`
- **Connection port:** `4000`
- **TLS:** required (`DB_SSL_MODE=REQUIRED`)
- **Credentials:** supplied through Render environment variables; never committed to GitHub
- **Schema:** managed by the application's Spring Data JPA/Hibernate configuration

The allocation service uses TiDB-compatible `SELECT id FROM ... WHERE id = ? FOR UPDATE` queries inside Spring-managed transactions. This preserves row-level locking for case and resource allocation without relying on Hibernate's incompatible `FOR UPDATE OF <alias>` syntax.

> **Deployment note:** Configure `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, and `DB_SSL_MODE` in **Render → Environment**. Use your own TiDB Cloud connection values rather than copying someone else's credentials. The backend remains hosted on Render; the frontend remains hosted on Vercel.

---

## 🔄 Example Emergency Management Workflow

1. **Administrator Login:** An authorized user signs in.
2. **Hospital Registration:** A hospital is added to the system.
3. **Resource Configuration:** Emergency resources are registered and their availability is recorded.
4. **Patient Registration:** An emergency patient is entered into the system.
5. **Patient Prioritization:** The patient's urgency is considered during emergency handling.
6. **SA-MA Resource Review:** Staff review eligibility explanations and choose the required bed, doctor, and (if needed) ventilator.
7. **Confirm Allocation:** The backend locks and rechecks the selected records in a transaction, starts treatment, and updates resource availability.
8. **Complete & Release:** Staff complete treatment and release the assigned resources.
9. **Dashboard Review:** Users monitor hospital information and resource status.

---

## 🔧 Deployment and Database Notes

- Render free-tier instances may take extra time to respond after inactivity. This is a hosting cold start, not necessarily a database outage.
- TiDB Cloud Starter replaces the old Aiven database for the deployed application; the README does not claim that every historical Aiven record was copied.
- Use fictional patient data only. Test data and account permissions should be reviewed before sharing the public demo.
- The database and API should be monitored separately when troubleshooting connectivity issues.

---

## 🌱 Future Enhancements

Potential future improvements include:

- Additional alert delivery channels and notification integrations.
- Integration with existing hospital information systems.
- More advanced allocation recommendations and decision-support explanations.
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

**React • Vite • Java 25 • Spring Boot • Spring Security • TiDB Cloud • Docker • Vercel • Render**

**Smart Hospital Emergency Resource Manager — Making Emergency Resource Coordination Smarter.**
