# SIT Campus App

A full-stack Java-based application designed to streamline the reporting and management of issues across the university campus. It provides a robust backend to handle complaints and a simple, accessible frontend interface for students and staff to log in and report problems.

## Features

- **Issue Reporting:** Enables users to report maintenance, infrastructural, or other campus-related issues directly.
- **Authentication System:** Secure login for authenticated students and staff to access the platform.
- **RESTful Backend:** Powered by Spring Boot, the backend ensures secure and scalable handling of data.
- **Lightweight Frontend:** Pure HTML/JS frontend served locally to interact seamlessly with the API.

## Technology Stack

- **Backend:** Java, Spring Boot, Maven
- **Frontend:** HTML, CSS, JavaScript
- **Server:** Embedded Tomcat (Spring Boot), Python `http.server` (Frontend dev server)

## Repository Structure

```text
SIT-Campus-App/
├── campusbackend/        # Spring Boot Java Application
├── src/                  # Frontend User Interface source files
│   └── main/resources/   # HTML templates and static assets
├── start.bat             # Windows batch script for automated startup
└── hierarchy.txt         # Project file hierarchy and documentation
```

## Setup & Installation

### Prerequisites

- **Java JDK 17+**
- **Maven** (optional, uses wrapper `mvnw` included in the project)
- **Python 3.x** (used to serve the frontend via `http.server`)

### Automated Startup (Windows)

For Windows users, you can launch the entire stack (both backend and frontend) with a single click:

1. Double click the `start.bat` script located in the root directory.
2. The script will automatically:
   - Start the Spring Boot backend on `http://localhost:8080`
   - Start the Python HTTP server for the frontend on `http://localhost:5500`
   - Open your default browser to the login page at `http://localhost:5500/templates/auth/login.html`

### Manual Startup

If you are not on Windows or prefer to start the servers manually:

#### 1. Start the Backend

```bash
cd campusbackend

# For Windows
mvnw.cmd spring-boot:run

# For macOS/Linux
./mvnw spring-boot:run
```
The backend API will run on `http://localhost:8080`.

#### 2. Start the Frontend

In a new terminal window:

```bash
cd src/main/resources
python -m http.server 5500
```
The frontend UI will be available at `http://localhost:5500/templates/auth/login.html`.

## Usage

1. Navigate to the login page in your browser.
2. Authenticate using your campus credentials (or test credentials).
3. Access the dashboard to view existing issues or report a new one.
