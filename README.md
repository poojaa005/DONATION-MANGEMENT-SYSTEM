# NGO Donation Management System

## Project Overview

The NGO Donation Management System is a full-stack web application designed to streamline and manage donation activities between donors and non-governmental organizations (NGOs). The platform enables users to contribute monetary and material donations, manage campaigns, and track donation activities efficiently while ensuring transparency and accountability.

---

## Features

* User registration and authentication
* Role-based access control
* Campaign creation and management
* Support for monetary and goods donations
* Donation pickup request handling
* Secure payment processing
* Donation tracking and status updates
* Automated donation receipt generation
* Urgent needs posting by NGOs
* Volunteer task assignment and tracking

---

## Users and Roles

### Donor

* Register and log in
* Donate money or goods
* Track donation history
* Request pickup for donated items

### Admin

* Manage users and system operations
* Approve or reject donation requests
* Monitor platform activities

### NGO Admin

* Create and manage campaigns
* Post urgent requirements
* Track received donations

### Volunteer

* View assigned tasks
* Handle pickup and delivery of goods
* Update task status

---

## Technology Stack

**Frontend**

* React.js
* HTML5, CSS3, JavaScript

**Backend**

* Java
* Spring Boot
* REST APIs
* Spring Security

**Database**

* MySQL

**Authentication**

* JWT (JSON Web Token)

**Tools and Deployment**

* Docker
* Git
* GitHub

---

## Project Structure

```
DONATION-MANAGEMENT-SYSTEM/
│
├── ngo-frontend/             # React frontend
├── ngo-backend/              # Spring Boot backend
├── ngo_donation_system.sql   # Database schema
├── docker-compose.yml        # Docker configuration
├── .gitignore
├── README.md
```

---

## Installation and Setup

### 1. Clone the Repository

```
git clone https://github.com/poojaa005/donation-management-system.git
cd donation-management-system
```

---

### 2. Configure Environment Variables

Copy `.env.example` to `.env` and fill in your real values.

Required variables:

```
MYSQL_ROOT_PASSWORD=change-me
JWT_SECRET=replace-with-a-long-random-secret
SPRING_MAIL_USERNAME=your-email@example.com
SPRING_MAIL_PASSWORD=your-app-password
APP_MAIL_FROM=your-email@example.com
GOOGLE_MAPS_API_KEY=replace-with-your-google-maps-api-key
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Do not commit `.env` to Git. It is already ignored.

---

### 3. Backend Setup (Spring Boot)

```
cd ngo-backend
mvn clean package
mvn spring-boot:run
```

---

### 4. Frontend Setup (React)

```
cd ngo-frontend
npm install
npm start
```

---

### 5. Database Setup

1. Open MySQL
2. Run the SQL file:

```
source ngo_donation_system.sql;
```

---

## Docker Deployment

### Build and Run Containers

```
docker compose down -v
docker compose up --build
```

Notes:

* MySQL is exposed on `localhost:3307`
* The SQL file `ngo_donation_system.sql` is imported automatically on first startup
* If you already have an old MySQL volume, `docker compose down -v` recreates it cleanly

### Access Application

* Frontend: http://localhost:3000
* Backend: http://localhost:8080
* MySQL: `localhost:3307`

---

## Environment Configuration

The backend and Docker setup now use environment variables instead of hardcoded secrets.

Main variables used by Docker and Spring Boot:

```
MYSQL_ROOT_PASSWORD=change-me
JWT_SECRET=replace-with-a-long-random-secret
SPRING_MAIL_USERNAME=your-email@example.com
SPRING_MAIL_PASSWORD=your-app-password
APP_MAIL_FROM=your-email@example.com
GOOGLE_MAPS_API_KEY=replace-with-your-google-maps-api-key
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

For cloud deployment, add the same values in your provider's secret or environment variable settings instead of uploading `.env`.

---

## CI/CD Pipeline

The project uses GitHub Actions for continuous integration and deployment:

* Automatically builds frontend and backend on each push
* Creates Docker images
* Ensures consistent and reliable deployment

---

## Future Enhancements

* Real-time notifications
* Mobile application support
* AI-based donation recommendations
* Multi-language support

---

## Key Highlights

* Full-stack application using React and Spring Boot
* Secure authentication using JWT
* Role-based access control implementation
* Docker-based containerized deployment
* CI/CD integration using GitHub Actions

---

## Author

Poojaa Kumar
B.Tech Information Technology
Easwari Engineering College


