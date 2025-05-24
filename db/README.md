# Fitlog database

## Running locally 
1. **Start the Postgres database with Docker Compose:**
   This will start a Postgres container with the correct database, user, and password as expected by the backend.
   ```sh
   docker-compose up -d
   ```
   - The database will be available at `localhost:5432`.
   - Credentials (see `docker-compose.yml` and `application.properties`):
     - Database: `fitlog`
     - User: `fitlog_user`
     - Password: `fitlog_password`

## Remaking locally
1. **Wiping and Remaking the Database locally:**
   1. Stop the database container (if running):
      ```sh
      docker-compose down
      ```
   2. Remove the database volume:
      ```sh
      docker volume rm db_postgres_data
      ```
   3. Start the database again (this will recreate the volume and a fresh database):
      ```sh
      docker-compose up -d
      ```
