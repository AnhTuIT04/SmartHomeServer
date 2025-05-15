# Smart Home Server

This is the server-side application for the Smart Home project.

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

## Introduction

The Smart Home project is designed to provide a comprehensive solution for managing and automating various aspects of a smart home. This server-side application is built using Spring Boot with integrations for Firebase, Cloudinary, and other services. The project requires **Java 23** to run.

## Features

- User authentication and authorization with JWT
- Integration with Firebase for real-time database
- Cloudinary integration for image storage
- RESTful API documented with Swagger
- Exception handling and input validation

## Installation

To install and run the project locally:

1. Clone the repository:
   ```sh
   git clone https://github.com/AnhTuIT04/SmartHomeServer.git
   cd SmartHomeServer
   ```

## Configuration

### Environment Variables

- `JWT_SECRET`: A secure string with a minimum length of 256 bits. It is recommended to generate this using a secure random generator.
- `ACCESS_TOKEN_EXPIRATION` & `REFRESH_TOKEN_EXPIRATION`: The expiration times for access and refresh tokens, respectively, in milliseconds.
- `CLOUDINARY_URL`: Obtainable from your Cloudinary dashboard under **Account Details**.
- `FIREBASE_URL`: The database URL from your Firebase project settings under the **Realtime Database** section.
- `FIREBASE_CREDENTIALS`: This is a Base64 encoded version of your Firebase service account JSON file.
- `FACE_EMBEDDING_THRESHOLD`: The confidence threshold for face recognition. Two faces are considered similar if the cosine similarity calculated from their embeddings is greater than this threshold. A lower value allows more matches (less strict), while a higher value requires a closer match (more strict). Adjust according to your application's security and usability needs.

To download the `service-account.json`:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project and navigate to **Project Settings**.
3. In the **Service Accounts** tab, click **Generate New Private Key**.
4. Save the downloaded `service-account.json` file.

### Environment Setup

1. Create a `.env` file in the root directory (for example, you can use the following template):
   ```properties
   JWT_SECRET=your-jwt-secret
   ACCESS_TOKEN_EXPIRATION=8640000
   REFRESH_TOKEN_EXPIRATION=604800000

   CLOUDINARY_URL=cloudinary://your-cloudinary-url

   FIREBASE_URL=https://your-firebase-url
   FIREBASE_CREDENTIALS=your-base64-encoded-service-account

   FACE_EMBEDDING_THRESHOLD=0.7
   ```

2. To get `FIREBASE_CREDENTIALS`, encode your Firebase service account file (`service-account.json`) as Base64:
   - **Linux / Git Bash**:
      ```sh
      cat service-account.json | base64 -w 0
      ```
   - **Windows PowerShell**:
      ```powershell
      [Convert]::ToBase64String([System.IO.File]::ReadAllBytes("service-account.json"))
      ```
   - **Python**:
      ```sh
      python -c "import base64; print(base64.b64encode(open('service-account.json', 'rb').read()).decode())"
      ```

## Running the Application

### Using Maven

1. Ensure the `.env` file is correctly configured.
2. Load environment variables:
   - **Linux / Git Bash**:
      ```sh
      export $(grep -v '^#' .env | xargs)
      ```
   - **Windows PowerShell**:
      ```powershell
      Get-Content .env | Where-Object { $_ -match "^\s*([^#].*?)\s*=\s*(.*?)\s*$" } | ForEach-Object { [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') }
      ```
3. Build and run the project:
   ```sh
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

### Using Docker

1. Build the Docker image:
   ```sh
   docker build -t smart-home .
   ```
2. Run the container, exposing port 8080 and using the `.env` file:
   ```sh
   docker run -p 8080:8080 --env-file .env smart-home
   ```

## API Documentation

API documentation is available at:
```
http://localhost:8080/swagger-ui
```

## Project Structure

```
SmartHomeServer/
├── .env
├── .gitignore
├── .gitattributes
├── Dockerfile
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── hcmut/
│   │   │       └── smart_home/
│   │   │           ├── config/
│   │   │           ├── controller/
│   │   │           ├── dto/
│   │   │           ├── exception/
│   │   │           ├── handler/
│   │   │           ├── interceptor/
│   │   │           ├── service/
│   │   │           ├── util/
│   │   │           └── Application.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── META-INF/
│   └── test/
│       └── java/
│           └── hcmut/
│               └── smart_home/
└── target/
    └── smart-home-0.0.1-SNAPSHOT.jar

```

## Contributing

Contributions are welcome! Please fork the repository and create a pull request with your changes.

## License

This project is licensed under the MIT License. See the LICENSE file for details.

