# Spring Boot REST API Demo

A Spring Boot REST API application built with Java 21 and Lombok.

## Features

- **Java 21**: Built using the latest LTS version of Java
- **Lombok**: Reduces boilerplate code with annotations
- **Spring Boot 3.2.0**: Modern Spring Boot framework
- **REST API**: Complete CRUD operations for User management
- **Maven**: Project build and dependency management

## Technologies

- Java 21
- Spring Boot 3.2.0
- Lombok 1.18.30
- Maven

## Prerequisites

- Java 21 or higher
- Maven 3.6+

## Building the Project

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Endpoints

### User Management

- **GET /api/users** - Get all users
- **GET /api/users/{id}** - Get user by ID
- **POST /api/users** - Create a new user
- **PUT /api/users/{id}** - Update an existing user
- **DELETE /api/users/{id}** - Delete a user

### Example Requests

#### Get all users
```bash
curl http://localhost:8080/api/users
```

#### Get user by ID
```bash
curl http://localhost:8080/api/users/1
```

#### Create a new user
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice Johnson","email":"alice@example.com"}'
```

#### Update a user
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"John Updated","email":"john.updated@example.com"}'
```

#### Delete a user
```bash
curl -X DELETE http://localhost:8080/api/users/1
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/demo/
│   │       ├── DemoApplication.java          # Main application class
│   │       ├── controller/
│   │       │   └── UserController.java       # REST controller
│   │       └── model/
│   │           └── User.java                 # User entity with Lombok
│   └── resources/
│       └── application.properties            # Application configuration
└── test/
    └── java/
        └── com/example/demo/
            └── DemoApplicationTests.java     # Basic tests
```

## Lombok Annotations Used

The `User` model class uses the following Lombok annotations:

- `@Data` - Generates getters, setters, toString, equals, and hashCode
- `@NoArgsConstructor` - Generates a no-argument constructor
- `@AllArgsConstructor` - Generates a constructor with all fields

## Running Tests

```bash
mvn test
```