# Fitlog backend

## Prerequisites

- **Java 17** (required, see `pom.xml`)
- **Maven** (for building and running the Spring Boot app)
- **Docker & Docker Compose** (for running the local Postgres database)

## Running locally

1. **Start the Postgres database with Docker Compose, see readme in db directory**
2. **Run the backend using Maven:**

   Make sure you are using Java 17. You can check your version with:
   ```sh
   java -version
   ```
   Then, start the backend in development mode:
   ```sh
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   - The backend will start on [http://localhost:8080](http://localhost:8080)

3. **API Documentation (Swagger UI):**

   - In development mode, Swagger UI is enabled at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - (Disabled in production for security)

## Running tests

You can run the backend tests using Maven. Make sure the Postgres database is running (see above) before running integration tests:

```sh
mvn test
```

- This will run all unit and integration tests in the backend.
- If you see database connection errors, ensure Docker Compose is running and the database is available at `localhost:5432` with the correct credentials.

## Notes

- The backend expects the Postgres database to be running before you start the app.
- If you change database credentials, update both `docker-compose.yml` and `src/main/resources/application.properties`.