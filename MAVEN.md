# Maven Usage

This project is a Maven-based Spring Boot service.

Use Maven from the local toolchain:

```bash
mvn compile
mvn test
mvn package
```

The Maven wrapper has not been generated yet because this checkout may be used
in environments without a Java/Maven bootstrap toolchain. If a wrapper is needed,
generate it from a machine with Java 21 and Maven available:

```bash
mvn -N wrapper:wrapper
```
