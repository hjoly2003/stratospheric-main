# Stratospheric - Chapter 11: Connecting to a Database with

## Amazon RDS

]:rds - In the sample Todo application, we’ll be using a database for storing todos as well as for managing users and sharing todos, and for that we’ve opted for a PostgreSQL RDBMS managed by Amazon Relational Database Service (RDS).

]:kms - We'll use AWS Key Management Service (KMS) for encrypting sensitive data at rest.

]:local]:postgres  - When running the application locally, we’ll be using a PostgreSQL database, for emulating AWS RDS.

]:spring]:jpa - The application uses Spring Data JPA to store data in a PostgreSQL database.

]:flyway - We use Flyway for setting up and subsequently migrating the application database to the state required by the current application source code. The `resources` folder contains a sub-directory named `db/migration/postgresql` with the Flyway migration SQL scripts for setting up the application database.

## Running the app

### For Local Development

```bash
cd stratospheric-main/application
./gradlew build
docker-compose up
./gradlew bootRun
```

Open your browser to `http://localhost:8080/`.

### Deploying to AWS
