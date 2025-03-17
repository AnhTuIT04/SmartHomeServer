# Smart Home

This is the server-side application for the Smart Home project.

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

## Introduction

The Smart Home project is designed to provide a comprehensive solution for managing and automating various aspects of a smart home. This server-side application is built using Spring Boot and integrates with Firebase, Cloudinary, and other services.

## Features

- User authentication and authorization
- JWT-based security
- Integration with Firebase for real-time database
- Cloudinary integration for image storage
- RESTful API with Swagger documentation
- Exception handling and validation

## Installation

To install and run the project locally, follow these steps:

1. Clone the repository:
    ```sh
    git clone https://github.com/your-username/smart-home.git
    cd smart-home
    ```

2. Build the project using Maven:
    ```sh
    ./mvnw clean install
    ```

3. Run the application:
    ```sh
    ./mvnw spring-boot:run
    ```

## Configuration

The application requires configuration for Firebase, Cloudinary, and JWT properties. These configurations are specified in the `src/main/resources/application.properties` file.

```properties
# Application properties
spring.application.name=smart-home
server.port=8080

# JWT properties
jwt.secret=your-jwt-secret
jwt.access-token-expiration=900000
jwt.refresh-token-expiration=604800000

# Firebase properties
firebase.url=https://your-firebase-url

# Cloudinary properties
cloudinary.url=cloudinary://your-cloudinary-url
```

## Usage
The application provides a RESTful API for managing users and other smart home functionalities. You can use tools like Postman to interact with the API.

## API Documentation
The API documentation is available at ```/swagger-ui``` once the application is running. You can access it by navigating to ```http://localhost:8080/swagger-ui```.

## Project Structure
The project structure is as follows:
```
smart_home/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── smarthome/
│   │   │               ├── controller/
│   │   │               ├── model/
│   │   │               ├── repository/
│   │   │               ├── service/
│   │   │               └── SmartHomeApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── smarthome/
├── .gitignore
├── mvnw
├── [mvnw.cmd](http://_vscodecontentref_/1)
├── [pom.xml](http://_vscodecontentref_/2)
└── [README.md](http://_vscodecontentref_/3)
```

# Contributing
Contributions are welcome! Please fork the repository and create a pull request with your changes.

# License
This project is licensed under the MIT License. See the LICENSE file for details.

```License
Feel free to customize the content as per your project's requirements.
```