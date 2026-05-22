.PHONY: build test test-unit test-integration test-e2e run docker-build docker-up docker-down docker-logs clean swagger

APP_NAME := booking-service
SWAGGER_URL := http://localhost:8081/swagger-ui

build:
	./mvnw clean package -DskipTests

test:
	./mvnw test

test-unit:
	./mvnw test -Dgroups="!integration,!e2e"

test-integration:
	./mvnw test -Dgroups="integration"

test-e2e:
	./mvnw test -Dgroups="e2e"

run:
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local

docker-build:
	docker build -t $(APP_NAME) .

docker-up:
	docker-compose up -d

docker-down:
	docker-compose down

docker-logs:
	docker-compose logs -f $(APP_NAME)

clean:
	./mvnw clean
	docker-compose down -v

swagger:
	@command -v xdg-open >/dev/null 2>&1 && xdg-open $(SWAGGER_URL) || \
	command -v open >/dev/null 2>&1 && open $(SWAGGER_URL) || \
	printf '%s\n' $(SWAGGER_URL)
