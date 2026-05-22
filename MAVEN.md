# Maven Usage

This project is a Maven-based Spring Boot service.

Use the committed Maven wrapper:

```bash
./mvnw compile
./mvnw test
./mvnw package
```

The Maven wrapper is committed so verification does not depend on a
machine-installed Maven. If the wrapper ever needs to be regenerated, use a
machine with Java 21 and Maven available:

```bash
mvn -N wrapper:wrapper
```
