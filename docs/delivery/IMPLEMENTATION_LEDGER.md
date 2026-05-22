# Implementation Ledger

This ledger records delivery evidence for completed implementation beads.

## Summary

| Metric | Value |
| --- | ---: |
| Beads recorded | 56 |
| PRs merged | 28 |
| Merge commits recorded | 30 |
| Verification blockers recorded | 40 |
| Entries with elapsed time | 55 |

## Entries

### bo-k7u.10 - Review and reduce test log noise

| Field | Value |
| --- | --- |
| Status | Open GitHub PR |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-k7u.10@mpgqxpdc` |
| PR | https://github.com/AgenticFunProject/booking/pull/59 |
| Merge commit | Pending |
| Started UTC | 2026-05-22T09:58:06Z |
| Completed UTC | 2026-05-22T10:05:25Z |
| Elapsed wall time | 7m 19s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/test/resources/logback-test.xml`, `src/test/resources/application.properties`, `src/test/resources/application-test.yml`, `src/test/java/com/cargo/booking/repository/BookingRepositoryTest.java`, `src/test/java/com/cargo/booking/repository/BookingReferenceCounterRepositoryTest.java`, `src/test/java/com/cargo/booking/client/RestClientLoggingInterceptorTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `IMPLEMENTATION.md`, `AGENTS.md`, `docs/delivery/README.md` |

Delivered:

- Added test-only Logback configuration that keeps general WARN/ERROR output visible while silencing categories deliberately exercised by negative-path tests: global exception handling, invalid JWT validation/authentication, local client stub startup warnings, cancellation release-failure warnings, generated Spring Security passwords, expected context-startup failures, and embedded database startup chatter.
- Disabled the Spring Boot banner for tests through test resources.
- Disabled `@DataJpaTest` SQL echoing in repository tests.
- Kept `RestClientLoggingInterceptorTest` assertions intact while preventing captured DEBUG events from bubbling to the console.
- Remaining expected output is Maven/Surefire progress plus the host-injected `JAVA_TOOL_OPTIONS` line, which is outside the repository's logging configuration.

Verification:

- `./mvnw test -Dtest="GlobalExceptionHandlerTest,ErrorHandlingMockMvcTest,JwtTokenProviderTest,JwtAuthenticationFilterTest,ClientStubTest,BookingServiceCancelTest,SecurityConfigEnabledTest,SecurityConfigDisabledTest,BookingSecurityIntegrationTest"` passed with 50 tests, 0 failures, 0 errors.
- `./mvnw test -Dtest="SecurityConfigEnabledTest,SecurityConfigDisabledTest,BookingSecurityIntegrationTest"` passed with 13 tests, 0 failures, 0 errors.
- `./mvnw compile` passed.
- `./mvnw test -Dtest="BookingRepositoryTest,BookingReferenceCounterRepositoryTest,RestClientLoggingInterceptorTest"` passed with 18 tests, 0 failures, 0 errors.
- `./mvnw test` passed with 164 tests, 0 failures, 0 errors.
- Final Surefire report scan for the original noisy WARN/ERROR, generated-password, stub-warning, JWT-warning, handler-log, Hibernate SQL, and BeanPostProcessor patterns returned no matches.

### bo-k7u.8 - Configure Mockito Java agent for tests

| Field | Value |
| --- | --- |
| Status | Open GitHub PR |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-k7u.8@mpgplaz8` |
| PR | https://github.com/AgenticFunProject/booking/pull/56 |
| Merge commit | Pending |
| Started UTC | 2026-05-22T09:20:30Z |
| Completed UTC | 2026-05-22T09:29:34Z |
| Elapsed wall time | 9m 04s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `pom.xml`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `IMPLEMENTATION.md`, `specs/001_project_setup.md`, `specs/009_testing.md` |

Delivered:

- Added explicit Maven Surefire `argLine` configuration that starts the Spring Boot managed Mockito core jar as a Java agent for test JVMs.
- Added `-Xshare:off` alongside the Mockito agent to avoid the class-data-sharing warning that appears when Mockito appends to the bootstrap classpath.
- Kept the change scoped to Maven test execution; no production runtime behavior changed.

Verification:

- Baseline `./mvnw test` passed with 164 tests and reproduced the Mockito self-attach warning plus JDK dynamic-agent warnings.
- `./mvnw compile` passed after the Surefire configuration change.
- `./mvnw test -Dtest=BookingReferenceGeneratorTest` passed with 1 test, 0 failures, 0 errors, and the Mockito self-attach / dynamic-agent warning block did not appear.
- `./mvnw test` passed with 164 tests, 0 failures, 0 errors, and the Mockito self-attach / dynamic-agent warning block no longer appeared.

### bo-u5m - Run cumulative Phase 1-6 audit

| Field | Value |
| --- | --- |
| Status | Open GitHub PR |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-u5m@mpfoepyi` |
| PR | https://github.com/AgenticFunProject/booking/pull/53 |
| Merge commit | Pending |
| Started UTC | 2026-05-21T16:00:01Z |
| Completed UTC | 2026-05-21T16:07:55Z |
| Elapsed wall time | 7m 54s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/security/JwtProperties.java`, `src/main/java/com/cargo/booking/security/JwtPropertiesValidator.java`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `src/test/java/com/cargo/booking/security/BookingSecurityIntegrationTest.java`, `src/test/java/com/cargo/booking/security/JwtTokenProviderTest.java`, `src/test/java/com/cargo/booking/security/SecurityContextHelperTest.java`, `src/test/java/com/cargo/booking/security/SecurityPropertiesTest.java`, `src/test/java/com/cargo/booking/security/TestSecurityConfig.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `IMPLEMENTATION.md`, `specs/001_project_setup.md`, `specs/002_domain_model.md`, `specs/003_data_access.md`, `specs/004_business_rules.md`, `specs/005_api_endpoints.md`, `specs/006_security.md`, `specs/007_error_handling.md`, `specs/008_integrations.md` |

Delivered:

- Audited Phase 1 foundation, Phase 2 domain model, Phase 3 data access, Phase 4 service layer, Phase 5 API/error handling, and Phase 6 security/integration infrastructure against `IMPLEMENTATION.md` and specs 001-008.
- Confirmed the expected Maven/Spring Boot baseline, package tree, JPA entities, Flyway migration, repositories, yearly reference counter, service lifecycle flows, DTOs, mapper, controller endpoints, structured error handling, security ownership checks, local client stubs, RestClient infrastructure, Resilience4j defaults, logging interceptor, and actuator health configuration are present.
- Fixed the concrete Phase 6 security configuration gap by moving JWT binding from `app.jwt` to the spec-required `app.security.jwt.*` namespace and using `expiration-ms` while preserving the existing audience field for deployed token compatibility.
- Fixed the concrete Phase 6 test-support gap by adding `TestSecurityConfig` with mock JWT-style authentication helpers and the documented security-disable property for integration tests.
- No follow-up beads were filed because the concrete Phase 1-6 gaps found during this audit were fixed in this branch.

Verification:

- Manual spec audit passed after the two fixes above; no remaining concrete Phase 1-6 gaps were found.
- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper; installed `mvn` was used for compile and test gates.
- `mvn compile` passed.
- `mvn test -Dtest="SecurityPropertiesTest,JwtTokenProviderTest,SecurityContextHelperTest,BookingSecurityIntegrationTest"` passed with 25 tests, 0 failures, 0 errors.
- `mvn test` passed with 164 tests, 0 failures, 0 errors.

### bo-ww4.6 - Add integration infrastructure tests

| Field | Value |
| --- | --- |
| Status | Open GitHub PR |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-ww4.6@mpfnyi32` |
| PR | https://github.com/AgenticFunProject/booking/pull/52 |
| Merge commit | Pending |
| Started UTC | 2026-05-21T15:47:25Z |
| Completed UTC | 2026-05-21T15:54:26Z |
| Elapsed wall time | 7m 01s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/test/java/com/cargo/booking/config/IntegrationInfrastructureTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/008_integrations.md`, `specs/001_project_setup.md`, `specs/004_business_rules.md`, `specs/007_error_handling.md` |

Delivered:

- Added cumulative integration infrastructure regression coverage for the base `application.yml` integration placeholders, Resilience4j defaults, and health exposure assumptions.
- Verified all three qualified RestClient beans resolve their configured base URLs, use JSON default headers, apply service-specific connect/read timeouts, and include the actual `RestClientLoggingInterceptor` bean.
- Kept the scope limited to infrastructure tests; no real external client behavior or external API endpoint assumptions were added.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=IntegrationInfrastructureTest` passed with 2 tests, 0 failures, 0 errors.
- `mvn test -Dtest="IntegrationInfrastructureTest,IntegrationPropertiesTest,RestClientConfigTest,RestClientLoggingInterceptorTest,ActuatorHealthConfigurationTest"` passed with 11 tests, 0 failures, 0 errors.
- Post-rebase `git fetch origin master && git rebase origin/master && git diff --check origin/master...HEAD && mvn compile && mvn test` passed; branch was already up to date, compile passed, and the full suite passed with 163 tests, 0 failures, 0 errors.

### bo-ww4.3 - Add RestClient logging interceptor

| Field | Value |
| --- | --- |
| Status | Open GitHub PR |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-ww4.3@mpfnjv6z` |
| PR | https://github.com/AgenticFunProject/booking/pull/51 |
| Merge commit | Pending |
| Started UTC | 2026-05-21T15:36:01Z |
| Completed UTC | 2026-05-21T15:43:56Z |
| Elapsed wall time | 7m 55s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/client/RestClientLoggingInterceptor.java`, `src/test/java/com/cargo/booking/client/RestClientLoggingInterceptorTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/008_integrations.md`, `specs/001_project_setup.md`, `specs/004_business_rules.md`, `specs/007_error_handling.md` |

Delivered:

- Added a component-scanned `RestClientLoggingInterceptor` that implements `ClientHttpRequestInterceptor`.
- Logged outbound method/URL, redacted request headers, response status, and elapsed time at DEBUG.
- Redacted `Authorization` header values and avoided request/response body logging.
- Kept registration aligned with the existing `RestClientConfig`, which registers all interceptor beans on the schedule, equipment, and quote RestClients.
- Added focused tests for DEBUG log content, Authorization redaction, body omission, and adjacent RestClient interceptor registration.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- Initial `mvn test -Dtest=RestClientLoggingInterceptorTest` failed because the test `HttpRequest` double was missing Spring 6.2 interface methods; the test double was corrected before rerun.
- `mvn test -Dtest=RestClientLoggingInterceptorTest` passed with 1 test, 0 failures, 0 errors.
- `mvn compile` passed.
- `mvn test -Dtest="RestClientLoggingInterceptorTest,RestClientConfigTest"` passed with 3 tests, 0 failures, 0 errors.
- Post-rebase `git fetch origin master && git rebase origin/master` passed; branch was already up to date.
- Post-rebase `git diff --check origin/master...HEAD` passed.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 161 tests, 0 failures, 0 errors.

### bo-ww4.2 - Add RestClientConfig

| Field | Value |
| --- | --- |
| Status | Open GitHub PR |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-ww4.2@mpfn45g6` |
| PR | https://github.com/AgenticFunProject/booking/pull/50 |
| Merge commit | Pending |
| Started UTC | 2026-05-21T15:23:48Z |
| Completed UTC | 2026-05-21T15:32:47Z |
| Elapsed wall time | 8m 59s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/config/RestClientConfig.java`, `src/test/java/com/cargo/booking/config/RestClientConfigTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/008_integrations.md`, `specs/001_project_setup.md`, `specs/004_business_rules.md`, `specs/007_error_handling.md` |

Delivered:

- Added `RestClientConfig` with qualified `scheduleRestClient`, `equipmentRestClient`, and `quoteRestClient` beans backed by `IntegrationProperties`.
- Configured each client with its service base URL, JSON `Content-Type` and `Accept` defaults, and connect/read timeouts through `SimpleClientHttpRequestFactory`.
- Registered available `ClientHttpRequestInterceptor` beans on every RestClient so the follow-up logging interceptor bead can plug in without changing the client factory.
- Documented the future real-client implementation pattern in the config class without adding real external client behavior before contracts exist.
- Added focused tests for base URL resolution, JSON headers, interceptor registration, bean qualifiers, and configured read timeout behavior.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=RestClientConfigTest` passed with 2 tests, 0 failures, 0 errors.
- Post-rebase `git fetch origin master && git rebase origin/master` passed; branch was already up to date.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test -Dtest=RestClientConfigTest` passed with 2 tests, 0 failures, 0 errors.
- Post-rebase `mvn test` passed with 160 tests, 0 failures, 0 errors.

### bo-ww4.5 - Add health endpoint configuration

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-ww4.5@mpfmp5br` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-21T15:11:44Z |
| Completed UTC | 2026-05-21T15:17:12Z |
| Elapsed wall time | 5m 28s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/resources/application-local.yml`, `src/main/resources/application-dev.yml`, `src/main/resources/application-prod.yml`, `src/test/java/com/cargo/booking/config/ActuatorHealthConfigurationTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/008_integrations.md`, `specs/001_project_setup.md`, `specs/004_business_rules.md`, `specs/007_error_handling.md` |

Delivered:

- Added explicit local, dev, and prod profile resources for actuator health/info/metrics exposure.
- Configured local health components/details as always visible for unsecured stub development, dev as ADMIN-authorized, and prod as hidden with metrics not exposed.
- Kept the configuration limited to the booking service actuator endpoints; no assumptions were added about external service actuator endpoints or custom external health indicators.
- Added a focused YAML resource regression test for base, local, dev, and prod actuator health settings.

Verification:

- `./mvnw compile` and `./mvnw test -Dtest=ActuatorHealthConfigurationTest` were blocked because this checkout does not include a Maven wrapper.
- `mvn test -Dtest=ActuatorHealthConfigurationTest` passed with 4 tests, 0 failures, 0 errors.
- `mvn compile` passed.
- `git diff --check` passed.
- Post-rebase `git fetch origin master && git rebase origin/master && mvn compile && mvn test` passed; branch was already up to date, compile passed, and the full suite passed with 158 tests, 0 failures, 0 errors.

### bo-ww4.1 - Add IntegrationProperties

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-ww4.1@mpfls19s` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending |
| Started UTC | 2026-05-21T14:45:59Z |
| Completed UTC | 2026-05-21T14:52:00Z |
| Elapsed wall time | 6m 01s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/BookingServiceApplication.java`, `src/main/java/com/cargo/booking/config/IntegrationProperties.java`, `src/main/resources/application.yml`, `src/test/java/com/cargo/booking/config/IntegrationPropertiesTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/008_integrations.md`, `specs/001_project_setup.md`, `specs/004_business_rules.md`, `specs/007_error_handling.md` |

Delivered:

- Added typed `IntegrationProperties` for `app.integration` with nested schedule, equipment, and quote API configuration records.
- Added base URL and timeout defaults for schedule, equipment, and quote APIs, with validation and default values aligned to the integration spec.
- Enabled the integration properties for application injection from the main Spring Boot application class.
- Added base `application.yml` placeholders backed by `SCHEDULE_API_URL`, `SCHEDULE_API_TIMEOUT`, `EQUIPMENT_API_URL`, `EQUIPMENT_API_TIMEOUT`, `QUOTE_API_URL`, and `QUOTE_API_TIMEOUT`.
- Added focused property binding tests for default and externalized integration values.

Verification:

- `./mvnw compile` and `./mvnw test -Dtest="IntegrationPropertiesTest"` were blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest="IntegrationPropertiesTest"` passed with 2 tests, 0 failures, 0 errors.
- `git diff --check` passed.

### bo-7oj - Run cumulative Phase 1-5 audit

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-7oj@mpfku0rk` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-21T14:19:32Z |
| Completed UTC | 2026-05-21T14:26:20Z |
| Elapsed wall time | 6m 48s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/config/JacksonConfig.java`, `src/test/java/com/cargo/booking/config/JacksonConfigTest.java`, `src/main/resources/application.yml`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `IMPLEMENTATION.md`, `specs/001_project_setup.md`, `specs/002_domain_model.md`, `specs/003_data_access.md`, `specs/004_business_rules.md`, `specs/005_api_endpoints.md`, `specs/006_security.md`, `specs/007_error_handling.md` |

Delivered:

- Audited Phase 1 foundation, Phase 2 domain model, Phase 3 data access, Phase 4 service layer, and Phase 5 API/error handling against `IMPLEMENTATION.md` and specs 001 through 007.
- Confirmed the expected Maven/Spring Boot baseline, package tree, JPA entities, Flyway migration, repositories, reference counter, service flows, DTOs, mapper, controller endpoints, security ownership hooks, and structured error handlers are present.
- Fixed the concrete Phase 5/API configuration gap by adding `JacksonConfig` for ISO-8601 date serialization, unknown request field tolerance, Java time support, and global null-field omission.
- Fixed the concrete error-handling configuration gap by adding the required `server.error.*` leak-prevention and whitelabel-disable settings.
- No follow-up beads were filed because the concrete gaps found during this audit were fixed in this branch.

Verification:

- Manual spec audit passed after the two fixes above; no remaining concrete Phase 1-5 gaps were found.
- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper; installed `mvn` was used for compile and test gates.
- `mvn test -Dtest=JacksonConfigTest` passed with 2 tests, 0 failures, 0 errors.
- `mvn compile` passed.
- `git diff --check` passed.
- Post-rebase `git fetch origin master && git rebase origin/master && mvn compile && mvn test` passed; branch was already up to date, compile passed, and the full suite passed with 152 tests, 0 failures, 0 errors.

### bo-m7w.9 - Add security and ownership tests

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-m7w.9@mpfj4v3f` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-21T13:31:59Z |
| Completed UTC | 2026-05-21T13:41:55Z |
| Elapsed wall time | 9m 56s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/test/java/com/cargo/booking/security/BookingSecurityIntegrationTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/006_security.md` |

Delivered:

- Added MVC security integration coverage using real HS256 JWT parsing with issuer `platform-auth`, audience `equipments-service`, and the shared test `AUTH_JWT_SECRET`.
- Covered missing, malformed, expired, wrong-issuer, and wrong-audience tokens returning 401 before controller/service calls.
- Covered Users-compatible `role=admin` tokens with a Users-style subject and no Users introspection dependency.
- Covered endpoint role allow/deny behavior for customer, service, operator, and admin callers.
- Covered disabled security mode, customer ownership match/mismatch, missing customer claims, customer list `customerId` requirements, and privileged/service ownership bypass behavior.

Verification:

- `./mvnw compile` and `./mvnw test -Dtest="BookingSecurityIntegrationTest"` were blocked because this checkout does not include a Maven wrapper.
- Initial `mvn test -Dtest="BookingSecurityIntegrationTest"` failed because the test file's context classes did not include a class with that exact Surefire pattern; renaming the enabled context class fixed discovery.
- Initial `mvn test -Dtest="BookingSecurity*IntegrationTest"` failed because the MVC slice loaded duplicate old/new Spring Boot 3.5 Jackson auto-configurations; excluding the new Jackson auto-configuration fixed context startup.
- `mvn test -Dtest="BookingSecurity*IntegrationTest"` passed with 7 tests, 0 failures, 0 errors.
- `mvn compile` passed.
- `git diff --check` passed.
- Post-rebase `git fetch origin master && git rebase origin/master && mvn compile && mvn test` passed; branch was already up to date, compile passed, and the full suite passed with 150 tests, 0 failures, 0 errors.

### bo-m7w.7 - Add BookingAccessAuthorizer

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-m7w.7@mpfi61qu` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-21T13:04:58Z |
| Completed UTC | 2026-05-21T13:12:37Z |
| Elapsed wall time | 7m 39s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/security/BookingAccessAuthorizer.java`, `src/test/java/com/cargo/booking/security/BookingAccessAuthorizerTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/006_security.md` |

Delivered:

- Replaced the placeholder `BookingAccessAuthorizer` with create, list, booking ID, and booking reference ownership checks.
- Allowed access without customer ownership checks when security is disabled or the caller has `ROLE_SERVICE`, `ROLE_OPERATOR`, or normalized `ROLE_ADMIN`.
- Enforced missing customer identity claims as HTTP 403 paths via `AccessDeniedException` before comparing request or query customer IDs.
- Enforced the secured customer list requirement with `BookingValidationException` when a customer token has a customer identity claim but omits the `customerId` query parameter.
- Loaded existing booking owners through `BookingRepository` and returned without throwing on empty lookups so `BookingService` keeps ownership of final 404 responses.
- Added focused authorizer unit tests for disabled security, privileged callers, Users `admin` role shape, customer claim checks, owner mismatch rejection, and missing-record deferral.

Verification:

- `./mvnw compile` and `./mvnw test -Dtest="BookingAccessAuthorizerTest"` were blocked because this checkout does not include a Maven wrapper.
- `mvn test -Dtest="BookingAccessAuthorizerTest"` passed with 16 tests, 0 failures, 0 errors.
- `mvn compile` passed.
- Post-rebase `git fetch origin master && git rebase origin/master && git diff --check origin/master...HEAD && mvn compile && mvn test` passed; branch was already up to date, diff check and compile passed, and the full suite passed with 143 tests, 0 failures, 0 errors.

### bo-m7w.5 - Implement SecurityConfig

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-m7w.5@mpfg59n2` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending |
| Started UTC | 2026-05-21T12:08:18Z |
| Completed UTC | 2026-05-21T12:18:03Z |
| Elapsed wall time | 9m 45s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `pom.xml`, `src/main/java/com/cargo/booking/config/SecurityConfig.java`, `src/main/resources/application.yml`, `src/test/java/com/cargo/booking/config/SecurityConfigTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/006_security.md` |

Delivered:

- Added stateless `SecurityConfig` with CSRF disabled, STATELESS sessions, JWT entry point/access-denied handlers, and JWT filter registration before `UsernamePasswordAuthenticationFilter`.
- Added `app.security.enabled=false` permit-all mode that skips JWT filter registration.
- Configured public docs, API docs, health, and info endpoints; ADMIN-only metrics; and role rules for create, list, get, cancel, confirm, start, and complete booking endpoints.
- Added CORS configuration with externalized `app.cors.allowed-origins`, allowed methods/headers, exposed Authorization header, credentials, and max age.
- Added MVC security tests covering enabled and disabled security modes, public endpoints, protected routes, metrics, ADMIN JWT access, and CORS.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- Initial `mvn test -Dtest="SecurityConfigEnabledTest,SecurityConfigDisabledTest"` failed because Spring Boot 3.5 loaded duplicate old/new Jackson auto-configurations in the MVC slice; excluding the new Jackson auto-configuration fixed context startup.
- Second targeted test run failed because nested test controllers were not registered by the MVC slice and secured requests fell through to static-resource 500 responses; moving the test controller to a package-private top-level class fixed routing.
- `mvn test -Dtest="SecurityConfigEnabledTest,SecurityConfigDisabledTest"` passed with 7 tests, 0 failures, 0 errors.
- Post-rebase `git fetch origin master && git rebase origin/master && git diff --check origin/master...HEAD && mvn compile && mvn test` passed; branch was already up to date, compile passed, and the full suite passed with 122 tests, 0 failures, 0 errors.

### bo-m7w.4 - Implement security error handlers

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-m7w.4@mpffqj4n` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-21T11:56:53Z |
| Completed UTC | 2026-05-21T12:01:14Z |
| Elapsed wall time | 4m 21s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `pom.xml`, `src/main/java/com/cargo/booking/security/JwtAuthenticationEntryPoint.java`, `src/main/java/com/cargo/booking/security/JwtAccessDeniedHandler.java`, `src/test/java/com/cargo/booking/security/JwtAuthenticationEntryPointTest.java`, `src/test/java/com/cargo/booking/security/JwtAccessDeniedHandlerTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/006_security.md` |

Delivered:

- Added `JwtAuthenticationEntryPoint` to return structured 401 JSON responses for unauthenticated secured endpoint access.
- Added `JwtAccessDeniedHandler` to return structured 403 JSON responses for authenticated callers without required permissions.
- Reused `ErrorResponseBuilder` so security responses include timestamp, status, error, path, and optional `X-Request-ID` consistently with API errors.
- Replaced the narrow `spring-security-core` dependency with `spring-boot-starter-security` so the security web handler contracts are available and the dependency matches `006_security`.
- Added focused unit tests for status, content type, safe fixed messages, request path, request ID propagation, and omission of absent request IDs.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- Initial `mvn test -Dtest="JwtAuthenticationEntryPointTest,JwtAccessDeniedHandlerTest"` failed because `spring-security-web` was missing from the dependency graph; replacing `spring-security-core` with `spring-boot-starter-security` fixed the gap.
- `mvn test -Dtest="JwtAuthenticationEntryPointTest,JwtAccessDeniedHandlerTest"` passed with 2 tests, 0 failures, 0 errors.
- `mvn compile` passed.
- `git diff --check` passed.
- Post-rebase `git fetch origin master && git rebase origin/master && git diff --check origin/master...HEAD && mvn compile && mvn test` passed with 115 tests, 0 failures, 0 errors.

### bo-m7w.1 - Add security and JWT properties

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-m7w.1@mpfede5r` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-21T11:18:38Z |
| Completed UTC | 2026-05-21T11:26:34Z |
| Elapsed wall time | 7m 56s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/BookingServiceApplication.java`, `src/main/java/com/cargo/booking/security/JwtProperties.java`, `src/main/java/com/cargo/booking/security/JwtPropertiesValidator.java`, `src/main/java/com/cargo/booking/security/SecurityProperties.java`, `src/main/resources/application.yml`, `src/test/java/com/cargo/booking/security/SecurityPropertiesTest.java`, `src/test/resources/application-test.yml`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/006_security.md` |

Delivered:

- Added typed `app.security` and `app.jwt` configuration property records with configuration-property scanning.
- Added JWT issuer, audience, secret, and expiration defaults compatible with current platform tokens: issuer `platform-auth`, audience `equipments-service`, and secret from `AUTH_JWT_SECRET`.
- Added startup validation that requires a non-blank 32-character JWT secret only when `app.security.enabled=true`, while allowing unsecured/local mode to run without a secret.
- Externalized security and JWT values through base and test YAML configuration.
- Added focused property-binding tests for defaults, overrides, disabled-security behavior, and enabled-security secret validation.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- Initial `mvn test -Dtest=SecurityPropertiesTest` failed because startup failure assertions expected a root cause, while `ApplicationContextRunner` exposed the validator exception directly; assertions were corrected.
- `mvn test -Dtest=SecurityPropertiesTest` passed with 5 tests, 0 failures, 0 errors.
- `git diff --check` passed.
- `mvn test` passed with 99 tests, 0 failures, 0 errors.
- Post-rebase `git fetch origin master && git rebase origin/master` passed; branch was already up to date.
- Post-rebase `git diff --check origin/master...HEAD && mvn compile && mvn test` passed with 99 tests, 0 failures, 0 errors.

### bo-2tm.9 - Add controller tests for happy paths

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | jasper |
| Branch | `polecat/jasper/bo-2tm.9@mpdthb72` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-20T08:46:07Z |
| Completed UTC | 2026-05-20T08:53:02Z |
| Elapsed wall time | 6m 55s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/test/java/com/cargo/booking/controller/BookingControllerTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/005_api_endpoints.md` |

Delivered:

- Added standalone MockMvc happy-path coverage for `PATCH /api/v1/bookings/{id}/confirm`.
- Added standalone MockMvc happy-path coverage for `PATCH /api/v1/bookings/{id}/start`.
- Added standalone MockMvc happy-path coverage for `PATCH /api/v1/bookings/{id}/complete`.
- Verified each lifecycle endpoint returns HTTP 200, mapped booking JSON fields, and calls the matching `BookingService` method before mapping the result.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=BookingControllerTest` passed with 10 tests, 0 failures, 0 errors.
- Post-rebase `git diff --check origin/master...HEAD` passed.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 94 tests, 0 failures, 0 errors.

### bo-b0p.5 - Add error handling tests

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | quartz |
| Branch | `polecat/quartz/bo-b0p.5@mpdslwg5` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-20T08:21:38Z |
| Completed UTC | 2026-05-20T08:32:06Z |
| Elapsed wall time | 10m 28s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/test/java/com/cargo/booking/exception/ErrorHandlingMockMvcTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/007_error_handling.md` |

Delivered:

- Added standalone MockMvc error-handling coverage that exercises `GlobalExceptionHandler` through Spring MVC exception resolution.
- Verified structured JSON fields, status codes, request path, and optional `X-Request-ID` propagation.
- Verified sorted validation violation responses for nested create-booking DTO fields.
- Verified safe response messages for malformed JSON, equipment reservation failures, and unhandled exceptions.
- Verified representative business, framework, security, method-not-allowed, and unknown API path mappings.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest="GlobalExceptionHandlerTest,ErrorHandlingMockMvcTest,ErrorResponseTest,ErrorResponseBuilderTest"` passed with 19 tests, 0 failures, 0 errors.
- `mvn test -Dtest=ErrorHandlingMockMvcTest` passed with 5 tests, 0 failures, 0 errors after adding HTTP-level 409 coverage.
- `mvn test` passed with 90 tests, 0 failures, 0 errors.
- Post-rebase `git diff --check origin/master...HEAD` passed.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 90 tests, 0 failures, 0 errors.

### bo-2tm.7 - Implement cancel booking endpoint

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-2tm.7@mpdsooa7` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-20T08:23:44Z |
| Completed UTC | 2026-05-20T08:31:12Z |
| Elapsed wall time | 7m 28s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/controller/BookingController.java`, `src/main/java/com/cargo/booking/security/BookingAccessAuthorizer.java`, `src/test/java/com/cargo/booking/controller/BookingControllerTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/005_api_endpoints.md` |

Delivered:

- Added `PATCH /api/v1/bookings/{id}/cancel` to `BookingController`.
- Authorized cancel access through `BookingAccessAuthorizer.authorizeBookingAccess(id)` before calling `BookingService.cancelBooking(id)`.
- Returned the updated booking through `BookingMapper.toResponse`.
- Added a minimal `BookingAccessAuthorizer` component so endpoint ownership checks have a concrete controller dependency until the full security phase fills in policy behavior.
- Added standalone MockMvc coverage for the cancel endpoint response and authorizer-service-mapper call order.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=BookingControllerTest` passed with 7 tests, 0 failures, 0 errors.
- Post-rebase `git diff --check origin/master...HEAD` passed.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 86 tests, 0 failures, 0 errors.

### bo-b0p.3 - Map business exceptions

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | quartz |
| Branch | `polecat/quartz/bo-b0p.3@mpcllvnp` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-19T12:17:50Z |
| Completed UTC | 2026-05-19T12:24:11Z |
| Elapsed wall time | 6m 21s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/exception/GlobalExceptionHandler.java`, `src/test/java/com/cargo/booking/exception/GlobalExceptionHandlerTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/007_error_handling.md` |

Delivered:

- Added `GlobalExceptionHandler` mappings for booking not found, booking validation, illegal state transitions, schedule availability, quote validity, and equipment reservation failures.
- Mapped custom business exceptions to the specified HTTP statuses: 404, 400, 409, 422, and 503.
- Kept client-side failures at WARN logging and equipment reservation failures at ERROR logging with stack traces.
- Returned a safe generic 503 message for equipment reservation failures instead of exposing integration details.
- Added focused unit coverage for each business exception mapping and the equipment safe-message behavior.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=GlobalExceptionHandlerTest` passed with 7 tests, 0 failures, 0 errors.
- Post-rebase `git diff --check origin/master...HEAD` passed.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 78 tests, 0 failures, 0 errors.

### bo-2tm.4 - Implement create booking endpoint

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-2tm.4@mpcl2cea` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-19T12:02:47Z |
| Completed UTC | 2026-05-19T12:11:47Z |
| Elapsed wall time | 9m 0s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/controller/BookingController.java`, `src/test/java/com/cargo/booking/controller/BookingControllerTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/005_api_endpoints.md` |

Delivered:

- Added `BookingController` with `POST /api/v1/bookings`, JSON consume/produce metadata, `@Valid @RequestBody` validation, HTTP 201 response status, and OpenAPI response annotations.
- Mapped API create DTOs into the service-layer create request record before calling `BookingService.createBooking`.
- Returned the slim `BookingCreatedResponse` through `BookingMapper.toCreatedResponse`.
- Added focused standalone MockMvc coverage for the happy-path create endpoint, response JSON, and service request conversion.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- Initial `mvn test -Dtest=BookingControllerTest` with `@WebMvcTest` was blocked by duplicate Spring Boot 3.5 Jackson auto-configuration bean `jsonComponentModule`; the test was converted to standalone MockMvc.
- Second `mvn test -Dtest=BookingControllerTest` failed because standalone MockMvc used timestamp serialization for `Instant`; the test message converter was configured with JavaTimeModule and timestamp serialization disabled.
- `mvn test -Dtest=BookingControllerTest` passed with 1 test, 0 failures, 0 errors.
- Final `mvn compile` passed.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 72 tests, 0 failures, 0 errors.
- Post-rebase `git diff --check origin/master...HEAD` passed.

Notes:

- `BookingAccessAuthorizer` is not wired here because `bo-m7w.7` adds it and `bo-m7w.8` wires ownership checks into controllers after the endpoint set exists.

### bo-2tm.3 - Add BookingMapper

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | quartz |
| Branch | `polecat/quartz/bo-2tm.3@mpckmnvl` |
| PR | Pending merge queue submission via `gt done --pre-verified` |
| Merge commit | Pending |
| Started UTC | 2026-05-19T11:50:26Z |
| Completed UTC | 2026-05-19T11:56:06Z |
| Elapsed wall time | 5m 40s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/mapper/BookingMapper.java`, `src/test/java/com/cargo/booking/mapper/BookingMapperTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/005_api_endpoints.md` |

Delivered:

- Added explicit `BookingMapper` component for entity-to-response conversion.
- Mapped nested customer and cargo response records from the flat booking aggregate fields.
- Mapped equipment lines with external equipment API codes such as `20FT`, `40HC`, and `REEFER`.
- Added focused mapper unit coverage for full booking responses, created responses, equipment code conversion, and null equipment-line handling.

Verification:

- `./mvnw compile` was blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=BookingMapperTest` passed with 4 tests, 0 failures, 0 errors.
- `mvn test` passed with 67 tests, 0 failures, 0 errors.
- Post-rebase `git diff --check origin/master...HEAD` passed.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 67 tests, 0 failures, 0 errors.

### bo-b0p.2 - Implement GlobalExceptionHandler skeleton

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-b0p.2@mpckmi6s` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending |
| Started UTC | 2026-05-19T11:50:24Z |
| Completed UTC | 2026-05-19T11:56:50Z |
| Elapsed wall time | 6m 26s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/exception/ErrorResponseBuilder.java`, `src/main/java/com/cargo/booking/exception/GlobalExceptionHandler.java`, `src/test/java/com/cargo/booking/exception/ErrorResponseBuilderTest.java`, `src/test/java/com/cargo/booking/exception/GlobalExceptionHandlerTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/007_error_handling.md` |

Delivered:

- Added a reusable `ErrorResponseBuilder` that constructs standard error and validation-error responses from `HttpStatus` plus `HttpServletRequest`.
- Added `GlobalExceptionHandler` as `@RestControllerAdvice` with no injected services and a lowest-precedence safe catch-all mapping for unhandled exceptions.
- Preserved request URI and optional `X-Request-ID` propagation while returning a generic 500 response body that does not expose exception details.
- Added focused tests for response builder behavior and the generic handler response.

Verification:

- `./mvnw compile` was attempted and blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest="ErrorResponseTest,ErrorResponseBuilderTest,GlobalExceptionHandlerTest"` passed with 6 tests, 0 failures, 0 errors.
- `git diff --check` passed.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 67 tests, 0 failures, 0 errors.

### bo-2tm.1 - Add request DTO records

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission |
| Agent | obsidian |
| Branch | `polecat/obsidian/bo-2tm.1@mpcfd56t` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending |
| Started UTC | 2026-05-19T09:23:04Z |
| Completed UTC | 2026-05-19T09:27:12Z |
| Elapsed wall time | 4m 8s |
| Timing source | Hook attachment time and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/dto/request/CreateBookingRequest.java`, `src/main/java/com/cargo/booking/dto/request/CustomerRequest.java`, `src/main/java/com/cargo/booking/dto/request/CargoRequest.java`, `src/main/java/com/cargo/booking/dto/request/EquipmentRequest.java`, `src/test/java/com/cargo/booking/dto/request/CreateBookingRequestValidationTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/005_api_endpoints.md` |

Delivered:

- Added API request DTO records for create booking input with nested customer, cargo, and equipment payloads.
- Added Jakarta Bean Validation annotations matching the API spec, including nested object and equipment-list validation.
- Added focused validation tests for valid input, scalar violations, nested violations, and equipment-line violations.

Verification:

- `mvn compile` passed.
- `mvn test -Dtest=CreateBookingRequestValidationTest` passed with 3 tests, 0 failures, 0 errors.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 61 tests, 0 failures, 0 errors.
- `git diff --check` passed.

### bo-u2r.1 - Scaffold Maven Spring Boot project

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-u2r-1-scaffold-maven` |
| PR | https://github.com/AgenticFunProject/booking/pull/6 |
| Merge commit | `b23c9df` |
| Started UTC | 2026-05-18T08:41:50Z |
| Completed UTC | 2026-05-18T09:02:47Z |
| Elapsed wall time | 20m 57s |
| Timing source | Copied from bead `started_at` and `closed_at` fields for GitHub-readable reporting |
| Files changed | `pom.xml`, `MAVEN.md` |
| Spec | `specs/001_project_setup.md` |

Delivered:

- Added Maven `pom.xml` for `booking-service`.
- Configured Java 21 and Spring Boot `3.5.14`.
- Added core Spring Boot starters, PostgreSQL/Flyway, Lombok, SpringDoc, Spring Boot test, and embedded PostgreSQL test dependencies.
- Added `MAVEN.md` documenting Maven commands and wrapper generation.

Verification:

- `git diff --cached --check` passed.
- `pom.xml` parsed successfully with Python `xml.etree.ElementTree`.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Maven wrapper was not generated in this environment. `MAVEN.md` documents `mvn -N wrapper:wrapper` for a Java 21/Maven machine.

### bo-u2r.2 - Create base package structure

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-u2r-2-package-structure` |
| PR | https://github.com/AgenticFunProject/booking/pull/11 |
| Merge commit | `f66c10b` |
| Started UTC | 2026-05-18T09:46:28Z |
| Completed UTC | 2026-05-18T09:46:54Z |
| Elapsed wall time | 26s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/**/package-info.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/001_project_setup.md` |

Delivered:

- Added the base `com.cargo.booking` package tree.
- Added Java package markers for controller, service, repository, model/entity, model/enums, DTO request/response, config, exception, client, client DTO, mapper, and security packages.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Empty packages are represented with `package-info.java` files so the package structure is tracked by Git.

### bo-u2r.6 - Add project ignore and developer docs shell

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-u2r-6-ignore-docs-shell` |
| PR | https://github.com/AgenticFunProject/booking/pull/12 |
| Merge commit | `471e92c` |
| Started UTC | 2026-05-18T09:49:38Z |
| Completed UTC | 2026-05-18T09:50:36Z |
| Elapsed wall time | 58s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `.gitignore`, `README.md`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/001_project_setup.md` |

Delivered:

- Added project ignore rules for build output, IDE/editor files, environment files, logs, OS files, and test/coverage output.
- Updated the README to describe the generated service status, implementation plan, delivery evidence files, and current Maven commands.

Verification:

- `git diff --check` passed.
- Portable absolute-path scan passed.

Notes:

- Maven wrapper artifacts are not ignored so a future wrapper-generation bead can commit them if needed.

### bo-u2r.3 - Add BookingServiceApplication entry point

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-u2r-3-application-entrypoint` |
| PR | https://github.com/AgenticFunProject/booking/pull/13 |
| Merge commit | `05d4331` |
| Started UTC | 2026-05-18T09:58:14Z |
| Completed UTC | 2026-05-18T09:58:34Z |
| Elapsed wall time | 20s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/BookingServiceApplication.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/001_project_setup.md` |

Delivered:

- Added the `BookingServiceApplication` Spring Boot main class in the base package.
- Added a standard `main` method calling `SpringApplication.run`.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Compile verification remains environment-blocked until Java 21 is available in this workspace.

### bo-u2r.4 - Add base application configuration

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-u2r-4-base-application-config` |
| PR | https://github.com/AgenticFunProject/booking/pull/14 |
| Merge commit | `cb898b5` |
| Started UTC | 2026-05-18T10:00:33Z |
| Completed UTC | 2026-05-18T10:00:52Z |
| Elapsed wall time | 19s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/resources/application.yml`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/001_project_setup.md` |

Delivered:

- Added base Spring application configuration for port, app name, datasource defaults, JPA, Flyway, OpenAPI, actuator exposure, pagination, and API base path.
- Kept messaging/event streaming configuration out of scope.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Profile-specific overrides remain in later foundation/deployment tasks.

### bo-u2r.5 - Add test profile configuration

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-u2r-5-test-profile-config` |
| PR | https://github.com/AgenticFunProject/booking/pull/15 |
| Merge commit | `346c36c` |
| Started UTC | 2026-05-18T10:02:47Z |
| Completed UTC | 2026-05-18T10:03:05Z |
| Elapsed wall time | 18s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/test/resources/application-test.yml`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/001_project_setup.md` |

Delivered:

- Added test profile configuration for PostgreSQL-oriented tests.
- Configured PostgreSQL driver expectations, JPA validation, Flyway validation, and test JWT/security defaults for later specs.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Embedded PostgreSQL test bootstrap code is planned in later test/integration beads; this profile provides the expected configuration surface.

### bo-7or.1 - Add BookingStatus enum

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-7or-1-booking-status` |
| PR | https://github.com/AgenticFunProject/booking/pull/16 |
| Merge commit | `1881bd8` |
| Started UTC | 2026-05-18T10:09:09Z |
| Completed UTC | 2026-05-18T10:09:26Z |
| Elapsed wall time | 17s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/model/enums/BookingStatus.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/002_domain_model.md` |

Delivered:

- Added the `BookingStatus` enum in the domain enum package.
- Added lifecycle values in the exact spec order: `PENDING`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Transition validation remains in the later service-layer state machine task.

### bo-7or.2 - Add EquipmentType enum with API codes

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-7or-2-equipment-type` |
| PR | https://github.com/AgenticFunProject/booking/pull/17 |
| Merge commit | `012ad52` |
| Started UTC | 2026-05-18T10:11:23Z |
| Completed UTC | 2026-05-18T10:11:48Z |
| Elapsed wall time | 25s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/model/enums/EquipmentType.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/002_domain_model.md` |

Delivered:

- Added Java-safe `EquipmentType` constants for `20FT`, `40FT`, `40HC`, and `REEFER`.
- Added `@JsonValue` code output and `fromCode(String)` parsing for request input.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Invalid or blank codes currently throw `IllegalArgumentException`; API-level error mapping is handled in later error/API tasks.

### bo-7or.3 - Add Booking entity

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-7or-entities` |
| PR | https://github.com/AgenticFunProject/booking/pull/18 |
| Merge commit | `94fefad` |
| Started UTC | 2026-05-18T10:14:37Z |
| Completed UTC | 2026-05-18T10:15:21Z |
| Elapsed wall time | 44s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/model/entity/Booking.java`, `src/main/java/com/cargo/booking/model/entity/BookingEquipmentLine.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/002_domain_model.md` |

Delivered:

- Added the `Booking` aggregate root with Long identity, reference, status, schedule, quote, customer, cargo, and timestamp fields.
- Added validation annotations, table/index metadata, UTC timestamp annotations, and the equipment-line relationship.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- This PR also includes `BookingEquipmentLine` because the two entities reference each other.

### bo-7or.4 - Add BookingEquipmentLine entity

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-7or-entities` |
| PR | https://github.com/AgenticFunProject/booking/pull/18 |
| Merge commit | `94fefad` |
| Started UTC | 2026-05-18T10:14:37Z |
| Completed UTC | 2026-05-18T10:15:21Z |
| Elapsed wall time | 44s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/model/entity/BookingEquipmentLine.java`, `src/main/java/com/cargo/booking/model/entity/Booking.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/002_domain_model.md` |

Delivered:

- Added `BookingEquipmentLine` with Long identity, equipment type, quantity validation, and lazy `Booking` back-reference.
- Added JSON/toString recursion safeguards and ID-based equality configuration needed by the relationship.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Entity safeguards are present here; the dedicated safeguards bead will verify and close any remaining gaps.

### bo-7or.6 - Add entity equality and serialization safeguards

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-7or-entities` |
| PR | https://github.com/AgenticFunProject/booking/pull/18 |
| Merge commit | `94fefad` |
| Started UTC | 2026-05-18T10:17:25Z |
| Completed UTC | 2026-05-18T10:17:25Z |
| Elapsed wall time | 0s |
| Timing source | Closed as verification of safeguards delivered by PR #18 |
| Files changed | `src/main/java/com/cargo/booking/model/entity/Booking.java`, `src/main/java/com/cargo/booking/model/entity/BookingEquipmentLine.java` |
| Spec | `specs/002_domain_model.md` |

Delivered:

- Verified both entities use ID-only Lombok equality configuration.
- Verified relationship fields avoid recursive `toString`, and the equipment-line back-reference is ignored for JSON serialization.

Verification:

- `rg` annotation scan confirmed `@EqualsAndHashCode`, `@EqualsAndHashCode.Include`, `@ToString.Exclude`, and `@JsonIgnore` placement.

Notes:

- No additional code PR was required after PR #18.

### bo-7or.5 - Add booking reference counter migration

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-7or-5-booking-migration` |
| PR | https://github.com/AgenticFunProject/booking/pull/19 |
| Merge commit | `7ed0265` |
| Started UTC | 2026-05-18T10:18:18Z |
| Completed UTC | 2026-05-18T10:18:50Z |
| Elapsed wall time | 32s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/resources/db/migration/V1__create_booking_tables.sql`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/002_domain_model.md` |

Delivered:

- Added the initial Flyway migration for `bookings`, `booking_equipment_lines`, and `booking_reference_counters`.
- Added indexes, constraints, PostgreSQL column types, foreign key behavior, and reference counter shape.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- The migration stores enum values using Java enum names, matching `@Enumerated(EnumType.STRING)`.

### bo-43o - Audit and fix Phase 1-2 implementation

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-43o-phase-1-2-audit` |
| PR | https://github.com/AgenticFunProject/booking/pull/21 |
| Merge commit | `6a7fcd6` |
| Started UTC | 2026-05-18T10:30:42Z |
| Completed UTC | 2026-05-18T10:31:03Z |
| Elapsed wall time | 21s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/test/resources/application-test.yml`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/001_project_setup.md`, `specs/002_domain_model.md` |

Delivered:

- Audited Phase 1 and Phase 2 implementation against the project setup and domain model specs.
- Removed fallback test datasource URL and credentials from `application-test.yml` so the datasource can be provided by the embedded PostgreSQL test bootstrap as specified.

Verification:

- Manual spec audit found the remaining Phase 1 and Phase 2 implementation aligned after the test-profile fix.
- `git diff --check` passed.
- Portable absolute-path scan passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Compile and runtime validation still require Java 21 on the machine.

### bo-eyx.1 - Add BookingRepository

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-eyx-1-booking-repository` |
| PR | https://github.com/AgenticFunProject/booking/pull/22 |
| Merge commit | `9983dc8` |
| Started UTC | 2026-05-18T10:40:59Z |
| Completed UTC | 2026-05-18T10:41:28Z |
| Elapsed wall time | 29s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/repository/BookingRepository.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/003_data_access.md` |

Delivered:

- Added `BookingRepository` in the repository package.
- Extended `JpaRepository<Booking, Long>` and `JpaSpecificationExecutor<Booking>`.
- Added derived query methods for booking reference, customer, status, customer/status, schedule, reference existence, and customer/status counts.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Eager equipment-line fetch methods are intentionally left for the separate `bo-eyx.3` bead.

### bo-eyx.2 - Add BookingEquipmentLineRepository

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-eyx-2-equipment-line-repository` |
| PR | https://github.com/AgenticFunProject/booking/pull/23 |
| Merge commit | `c94a53e` |
| Started UTC | 2026-05-18T10:43:21Z |
| Completed UTC | 2026-05-18T10:43:36Z |
| Elapsed wall time | 15s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/repository/BookingEquipmentLineRepository.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/003_data_access.md` |

Delivered:

- Added `BookingEquipmentLineRepository` in the repository package.
- Extended `JpaRepository<BookingEquipmentLine, Long>`.
- Added `findByBookingId(Long bookingId)` and `deleteByBookingId(Long bookingId)`.
- Annotated the delete method with `@Modifying` and `@Transactional` as required by the data-access transaction rules.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- The delete transaction is scoped to the modifying repository method, matching the explicit exception in `specs/003_data_access.md`.

### bo-eyx.5 - Add BookingReferenceCounterRepository

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-eyx-5-reference-counter-repository` |
| PR | https://github.com/AgenticFunProject/booking/pull/24 |
| Merge commit | `9a666ae` |
| Started UTC | 2026-05-18T10:45:03Z |
| Completed UTC | 2026-05-18T10:45:20Z |
| Elapsed wall time | 17s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/repository/BookingReferenceCounterRepository.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/003_data_access.md` |

Delivered:

- Added `BookingReferenceCounterRepository` as a custom repository component.
- Implemented `getNextReferenceSeqForYear(int year)` with a native PostgreSQL upsert.
- Used `INSERT ... ON CONFLICT ... DO UPDATE ... RETURNING` so a new year stores `next_value = 2` and returns `1`, while existing years increment and return the previous value.
- Wrapped the counter method in `@Transactional`.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Runtime validation of the upsert behavior is deferred to the data-access slice test bead.

### bo-eyx.3 - Add eager booking fetch queries

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-eyx-3-eager-fetch-queries` |
| PR | https://github.com/AgenticFunProject/booking/pull/25 |
| Merge commit | `e17de8d` |
| Started UTC | 2026-05-18T10:46:57Z |
| Completed UTC | 2026-05-18T10:47:10Z |
| Elapsed wall time | 13s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/repository/BookingRepository.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/003_data_access.md` |

Delivered:

- Added `findWithEquipmentLinesById(Long id)` to `BookingRepository`.
- Added `findWithEquipmentLinesByBookingReference(String reference)` to `BookingRepository`.
- Used JPQL `LEFT JOIN FETCH` queries so equipment lines can be loaded explicitly while the entity relationship stays lazy by default.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Fetch-query behavior is covered by the later data-access slice test bead.

### bo-eyx.4 - Add BookingSpecification

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-eyx-4-booking-specification` |
| PR | https://github.com/AgenticFunProject/booking/pull/26 |
| Merge commit | `7335b8d` |
| Started UTC | 2026-05-18T10:48:37Z |
| Completed UTC | 2026-05-18T10:48:57Z |
| Elapsed wall time | 20s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/repository/BookingSpecification.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/003_data_access.md` |

Delivered:

- Added `BookingSpecification` in the repository package.
- Added null-safe `hasCustomerId`, `hasStatus`, `hasScheduleId`, `createdAfter`, and `createdBefore` helpers.
- Used entity field names for criteria paths so the specifications compose with Spring Data JPA filtering.

Verification:

- `git diff --check` passed.
- `mvn compile` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Specification query behavior is covered by the later data-access slice test bead.

### bo-eyx.6 - Add data access slice tests

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-eyx-6-data-access-tests` |
| PR | https://github.com/AgenticFunProject/booking/pull/27 |
| Merge commit | `8299fd0` |
| Started UTC | 2026-05-18T10:51:12Z |
| Completed UTC | 2026-05-18T10:53:02Z |
| Elapsed wall time | 1m 50s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/test/java/com/cargo/booking/repository/BookingRepositoryTest.java`, `src/test/java/com/cargo/booking/repository/BookingReferenceCounterRepositoryTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/003_data_access.md`, `specs/009_testing.md` |

Delivered:

- Added embedded PostgreSQL `@DataJpaTest` coverage for booking repository persistence and derived queries.
- Covered eager equipment-line fetch queries, cascade save/delete behavior, and composable specifications.
- Added counter repository tests for new year allocation, existing year increments, independent yearly counters, migration shape, and concurrent allocation.

Verification:

- `git diff --check` passed.
- `mvn test` was attempted but blocked because no Java runtime was available and Maven reported `JAVA_HOME` was not defined correctly.

Notes:

- Runtime execution of the new tests requires a Java 21 environment with Maven dependency resolution available.

### bo-7yn - Use native embedded PostgreSQL provider for repository tests

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/native-embedded-postgres-tests` |
| PR | https://github.com/AgenticFunProject/booking/pull/29 |
| Merge commit | `903c393` |
| Started UTC | 2026-05-18T11:03:00Z |
| Completed UTC | 2026-05-18T11:05:00Z |
| Elapsed wall time | 2m |
| Timing source | Agent-recorded approximate UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `pom.xml`, `src/test/java/com/cargo/booking/repository/BookingRepositoryTest.java`, `src/test/java/com/cargo/booking/repository/BookingReferenceCounterRepositoryTest.java` |
| Spec | `specs/003_data_access.md`, `specs/009_testing.md` |

Delivered:

- Added the native `io.zonky.test:embedded-postgres` test dependency.
- Configured repository slice tests to use `AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY` instead of the default Docker provider.
- Enabled repository tests to run in WSL without a Docker socket.

Verification:

- `git diff --check` passed.
- `mvn compile` passed.
- `mvn test` passed with 17 tests, 0 failures, 0 errors.

Notes:

- Java and Maven were installed user-locally in WSL before this PR; those machine-local tool installs are not repository files.

### bo-1co - Use constructor injection in reference counter repository

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/reference-counter-constructor-injection` |
| PR | https://github.com/AgenticFunProject/booking/pull/30 |
| Merge commit | `b489335` |
| Started UTC | 2026-05-18T11:09:00Z |
| Completed UTC | 2026-05-18T11:09:58Z |
| Elapsed wall time | 58s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/repository/BookingReferenceCounterRepository.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/001_project_setup.md`, `specs/003_data_access.md` |

Delivered:

- Audited Phase 1, Phase 2, and Phase 3 implementation against the setup, domain model, and data access specs.
- Replaced `EntityManager` field injection in `BookingReferenceCounterRepository` with constructor injection to satisfy the Phase 1 Spring bean convention.
- Preserved the native PostgreSQL upsert sequence behavior.

Verification:

- Manual Phase 1-3 spec audit found this as the only concrete implementation mismatch.
- `git diff --check` passed.
- `mvn compile` passed.
- `mvn test` passed with 17 tests, 0 failures, 0 errors.

Notes:

- Existing historical ledger entries still record their original Java-environment blockers; current verification now passes after the WSL Java/Maven setup was fixed.

### bo-jyh - Prepare Phase 4 service execution plan

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-jyh-phase-4-plan` |
| PR | https://github.com/AgenticFunProject/booking/pull/33 |
| Merge commit | `74cca40` |
| Started UTC | 2026-05-18T12:02:32Z |
| Completed UTC | 2026-05-18T12:03:12Z |
| Elapsed wall time | 40s |
| Timing source | Agent-recorded UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `IMPLEMENTATION.md`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Added the Phase 4 service-layer execution waves to `IMPLEMENTATION.md`.
- Documented controlled parallelism guidance for service work and the hot-file risk around `BookingService`.
- Fixed local bead dependency metadata so `bo-0wh.7` read/list service work depends on `bo-0wh.1` exception classes before it starts.

Verification:

- `git diff --check` passed.

Notes:

- Bead dependency metadata is local Gas Town state; the GitHub-readable execution order is recorded in `IMPLEMENTATION.md`.

### bo-0wh.1 - Add service exception classes

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-0wh-1-service-exceptions` |
| PR | https://github.com/AgenticFunProject/booking/pull/35 |
| Merge commit | `c9639b2` |
| Started UTC | 2026-05-18T12:40:00Z |
| Completed UTC | 2026-05-18T12:41:52Z |
| Elapsed wall time | 1m 52s |
| Timing source | Agent-recorded approximate UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/exception/*.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Added all six service/business exception classes required by the business-rules spec.
- Provided String-message and String-message-plus-cause constructors on each exception.

Verification:

- `git diff --check` passed.
- `mvn compile` passed.

Notes:

- No broad catch-all exception behavior was added.

### bo-0wh.5 - Add BookingReferenceGenerator

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-0wh-5-reference-generator` |
| PR | https://github.com/AgenticFunProject/booking/pull/36 |
| Merge commit | `b946bb0` |
| Started UTC | 2026-05-18T12:42:00Z |
| Completed UTC | 2026-05-18T12:44:37Z |
| Elapsed wall time | 2m 37s |
| Timing source | Agent-recorded approximate UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/service/BookingReferenceGenerator.java`, `src/test/java/com/cargo/booking/service/BookingReferenceGeneratorTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Added a Spring `BookingReferenceGenerator` service using constructor injection.
- Generated references with the UTC year and zero-padded five-digit sequence values from `BookingReferenceCounterRepository`.
- Added a focused unit test covering UTC year selection and sequence formatting.

Verification:

- `git diff --check` passed.
- `mvn compile` passed.
- `mvn test -Dtest=BookingReferenceGeneratorTest` passed with 1 test, 0 failures, 0 errors.

Notes:

- The database counter repository remains responsible for concurrent sequence allocation.

### bo-0wh.2 - Add external client interfaces and DTOs

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-0wh-2-client-contracts` |
| PR | https://github.com/AgenticFunProject/booking/pull/37 |
| Merge commit | `b28b0dd` |
| Started UTC | 2026-05-18T12:47:00Z |
| Completed UTC | 2026-05-18T12:48:00Z |
| Elapsed wall time | 1m |
| Timing source | Agent-recorded approximate UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/client/*.java`, `src/main/java/com/cargo/booking/client/dto/*.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Added `ScheduleClient`, `EquipmentClient`, and `QuoteClient` interfaces.
- Added immutable `ScheduleDTO` and `EquipmentLineDTO` records for external client contracts.

Verification:

- `git diff --check` passed.
- `mvn compile` passed.

Notes:

- Real external integrations remain deferred to `specs/008_integrations.md`.

### bo-0wh.4 - Add BookingStateMachine

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-0wh-4-state-machine` |
| PR | https://github.com/AgenticFunProject/booking/pull/38 |
| Merge commit | `431f137` |
| Started UTC | 2026-05-18T12:49:00Z |
| Completed UTC | 2026-05-18T12:50:39Z |
| Elapsed wall time | 1m 39s |
| Timing source | Agent-recorded approximate UTC timestamps copied into this file for GitHub-readable reporting |
| Files changed | `src/main/java/com/cargo/booking/service/BookingStateMachine.java`, `src/test/java/com/cargo/booking/service/BookingStateMachineTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Added `BookingStateMachine` as a Spring component.
- Implemented the allowed lifecycle transitions from the domain model spec.
- Added focused tests for allowed transitions, rejected transitions, and null status handling.

Verification:

- `git diff --check` passed.
- `mvn compile` passed.
- `mvn test -Dtest=BookingStateMachineTest` passed with 3 tests, 0 failures, 0 errors.

Notes:

- Rejected transition messages include the current and target statuses for downstream error handling.

### bo-0wh.7 - Implement booking read service flows

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | mayor |
| Branch | `work/bo-0wh-7-booking-read-flows` |
| PR | https://github.com/AgenticFunProject/booking/pull/39 |
| Merge commit | `c77e745` |
| Started UTC | 2026-05-18T12:52:42Z |
| Completed UTC | 2026-05-18T14:49:04Z |
| Elapsed wall time | 1h 56m 22s |
| Timing source | Started time copied from bead `started_at`; completed time captured by agent when verification passed |
| Files changed | `src/main/java/com/cargo/booking/service/BookingService.java`, `src/test/java/com/cargo/booking/service/BookingServiceReadTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Added `BookingService` read orchestration with constructor injection and a service logger.
- Implemented `getBookingById` and `getBookingByReference` using eager equipment-line repository queries.
- Implemented pageable `getBookings` with optional customer and status filters.
- Added focused unit tests for found/missing reads and all list filter combinations.

Verification:

- `mvn test -Dtest=BookingServiceReadTest` passed with 7 tests, 0 failures, 0 errors.
- `git diff --check` passed.

Notes:

- Authorization and caller visibility remain at the later API/security boundary per `specs/004_business_rules.md`.

### bo-0wh.3 - Add local stub client implementations

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-0wh.3@mpbbizq5` |
| PR | https://github.com/AgenticFunProject/booking/pull/41 |
| Merge commit | `0a5611f` |
| Started UTC | 2026-05-18T14:47:59Z |
| Completed UTC | 2026-05-18T14:51:26Z |
| Elapsed wall time | 3m 27s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/client/ScheduleClientStub.java`, `src/main/java/com/cargo/booking/client/EquipmentClientStub.java`, `src/main/java/com/cargo/booking/client/QuoteClientStub.java`, `src/test/java/com/cargo/booking/client/ClientStubTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Added `@Service` and `@Profile("local")` stub implementations for `ScheduleClient`, `EquipmentClient`, and `QuoteClient`.
- Returned deterministic local schedule data and unconditional success for schedule and quote validation.
- Logged local equipment reservation and release actions without external calls.
- Added focused tests covering stub behavior and local-profile service annotations.

Verification:

- `./mvnw compile` blocked because the Maven wrapper is not present in this checkout.
- `mvn compile` passed.
- `mvn test -Dtest="ClientStubTest"` passed with 4 tests, 0 failures, 0 errors.

Notes:

- No `BookingService` or service-flow files were changed; this bead stayed within the assigned parallel write set.

### bo-0wh.6 - Implement create booking service flow

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | polecat/quartz |
| Branch | `polecat/quartz/bo-0wh.6@mpbbr762` |
| PR | https://github.com/AgenticFunProject/booking/pull/43 |
| Merge commit | `9f82c28` |
| Started UTC | 2026-05-18T14:54:20Z |
| Completed UTC | 2026-05-18T14:59:39Z |
| Elapsed wall time | 5m 19s |
| Timing source | Started time copied from hook attachment; completed time captured by agent after focused verification passed |
| Files changed | `src/main/java/com/cargo/booking/service/BookingService.java`, `src/main/java/com/cargo/booking/service/CreateBookingRequest.java`, `src/test/java/com/cargo/booking/service/BookingServiceCreateTest.java`, `src/test/java/com/cargo/booking/service/BookingServiceReadTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Added a service-layer `CreateBookingRequest` command record for create orchestration.
- Implemented transactional `BookingService.createBooking` validation, schedule validation, quote validation, reference generation, pending booking assembly, equipment-line association, and cascade persistence.
- Added focused create-flow unit tests for the happy path, validation failures, unavailable schedules, and invalid quotes.
- Updated existing read-service tests for the expanded constructor dependencies required by the service contract.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=BookingServiceCreateTest` passed with 6 tests, 0 failures, 0 errors.
- `mvn test -Dtest=BookingServiceReadTest` passed with 7 tests, 0 failures, 0 errors.
- `git diff --check` passed.

Notes:

- Authorization remains outside the service method for later API/security beads.
- Local stub client files were not edited.

### bo-0wh.8 - Implement confirm booking flow

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-0wh.8@mpbc87v2` |
| PR | https://github.com/AgenticFunProject/booking/pull/45 |
| Merge commit | `0b7eee5` |
| Started UTC | 2026-05-18T15:07:32Z |
| Completed UTC | 2026-05-18T15:13:09Z |
| Elapsed wall time | 5m 37s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/service/BookingService.java`, `src/test/java/com/cargo/booking/service/BookingServiceConfirmTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Implemented transactional `BookingService.confirmBooking(Long)` for PENDING to CONFIRMED only.
- Loaded bookings with equipment lines, validated the lifecycle transition before reservation, reserved equipment through `EquipmentClient`, then saved the CONFIRMED status.
- Mapped booking equipment lines to external `EquipmentLineDTO` values using the public equipment type code.
- Added focused confirm-flow unit tests for success, missing booking, invalid transition, and equipment reservation failure.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- Initial `mvn test -Dtest="BookingServiceConfirmTest"` failed because the new test mixed raw values and Mockito matchers; the test was corrected.
- `mvn test -Dtest="BookingServiceConfirmTest"` passed with 4 tests, 0 failures, 0 errors.
- `git diff --check` passed.

Notes:

- The status is changed only after `reserveEquipment` returns successfully, so reservation failures leave the booking status unchanged.
- This bead intentionally avoids `startBooking`, `completeBooking`, and `cancelBooking`; those lifecycle flows are owned by separate beads.

### bo-0wh.9 - Implement start and complete booking flows

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/quartz |
| Branch | `polecat/quartz/bo-0wh.9@mpbca64c` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-18T15:09:05Z |
| Completed UTC | 2026-05-18T15:17:42Z |
| Elapsed wall time | 8m 37s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/service/BookingService.java`, `src/test/java/com/cargo/booking/service/BookingServiceLifecycleTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Added transactional `BookingService.startBooking` and `BookingService.completeBooking` flows.
- Loaded bookings by ID, validated `CONFIRMED -> IN_PROGRESS` and `IN_PROGRESS -> COMPLETED` transitions through `BookingStateMachine`, updated status only after validation, saved the booking, and returned the persisted result.
- Added focused lifecycle unit tests covering success, missing bookings, and invalid transition rejection without persistence.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=BookingServiceLifecycleTest` passed with 6 tests, 0 failures, 0 errors.
- `mvn test` passed with 48 tests, 0 failures, 0 errors.
- `git diff --check` passed.

Notes:

- Confirm and cancel flows were intentionally not changed; those remain separate lifecycle beads.

### bo-0wh.10 - Implement cancel booking flow

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-0wh.10@mpbcw7x9` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-18T15:26:13Z |
| Completed UTC | 2026-05-18T15:31:10Z |
| Elapsed wall time | 4m 57s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/service/BookingService.java`, `src/test/java/com/cargo/booking/service/BookingServiceCancelTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Added transactional `BookingService.cancelBooking(Long)`.
- Loaded bookings with equipment lines, validated transitions to `CANCELLED` through `BookingStateMachine`, released equipment only for `CONFIRMED` bookings, and saved the cancelled booking.
- Made equipment release failures warning-only by catching the external client contract exception while still persisting cancellation.
- Added focused cancel-flow unit tests for pending cancellation, confirmed cancellation with release, warning-only release failure, missing booking, and invalid transition rejection.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=BookingServiceCancelTest` passed with 5 tests, 0 failures, 0 errors.
- After `git fetch origin master && git rebase origin/master`, `mvn test` passed with 53 tests, 0 failures, 0 errors.
- `git diff --check` passed.

Notes:

- Authorization and ownership checks remain outside the service method for later API/security beads.
- Release failure handling catches `EquipmentReservationException` only; generic service exceptions still bubble to the global handler.

### bo-0wh.11 - Add service unit tests

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-0wh.11@mpbd8oxb` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-18T15:35:57Z |
| Completed UTC | 2026-05-18T15:40:31Z |
| Elapsed wall time | 4m 34s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/test/java/com/cargo/booking/service/BookingServiceCreateTest.java`, `src/test/java/com/cargo/booking/service/BookingServiceReadTest.java`, `src/test/java/com/cargo/booking/service/BookingStateMachineTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/004_business_rules.md` |

Delivered:

- Expanded create-flow unit coverage for validation-before-client behavior, null requests, null equipment lines, invalid equipment quantities, and no reference generation after schedule or quote rejection.
- Added read-flow coverage for missing booking references.
- Expanded state-machine coverage to reject every transition not allowed by the lifecycle rules.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest="BookingServiceCreateTest,BookingServiceReadTest,BookingServiceConfirmTest,BookingServiceLifecycleTest,BookingServiceCancelTest,BookingReferenceGeneratorTest,BookingStateMachineTest"` passed with 37 tests, 0 failures, 0 errors.
- `mvn test` passed with 58 tests, 0 failures, 0 errors.
- After `git fetch origin master && git rebase origin/master`, `mvn compile` and `mvn test` passed with 58 tests, 0 failures, 0 errors.
- `git diff --check` passed.

Notes:

- The main service implementation was already present on `origin/master`; this bead adds focused edge-case tests without changing production service behavior.

### bo-2cu - Run cumulative Phase 1-4 audit

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-2cu@mpbdpigb` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-18T15:49:02Z |
| Completed UTC | 2026-05-18T15:54:00Z |
| Elapsed wall time | 4m 58s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `pom.xml`, `src/main/java/com/cargo/booking/service/BookingService.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `IMPLEMENTATION.md`, `specs/001_project_setup.md`, `specs/002_domain_model.md`, `specs/003_data_access.md`, `specs/004_business_rules.md` |

Delivered:

- Audited Phase 1 foundation, Phase 2 domain, Phase 3 data access, and Phase 4 service layer against the implementation guide and specs 001 through 004.
- Confirmed the expected Maven/Spring Boot foundation, Java package tree, application and test configuration, domain enums/entities/migration, repositories/specifications/reference counter, service exceptions, client interfaces/stubs, reference generator, state machine, service flows, and focused tests are present.
- Fixed the concrete Phase 1 dependency-scope gap by changing Lombok to Maven `provided` scope as specified.
- Fixed the concrete Phase 4 logging gap by making start, complete, and cancel lifecycle success logs include booking reference plus `from` and `to` statuses.

Verification:

- Manual spec audit passed after the two fixes above; no remaining concrete Phase 1-4 spec gaps were found.
- `mvn compile` passed.
- `mvn test -Dtest="BookingServiceLifecycleTest,BookingServiceCancelTest"` passed with 11 tests, 0 failures, 0 errors.
- `mvn test` passed with 58 tests, 0 failures, 0 errors.
- `git diff --check` passed.
- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper; installed `mvn` was used for the compile and test gates.

Notes:

- Later API, security, global error handling, real integrations, E2E testing, and deployment requirements remain out of scope for this Phase 1-4 audit and are covered by later specs.

### bo-2tm.2 - Add response DTO records

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/quartz |
| Branch | `polecat/quartz/bo-2tm.2@mpcfiff5` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-19T09:27:10Z |
| Completed UTC | 2026-05-19T09:35:58Z |
| Elapsed wall time | 8m 48s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/dto/response/BookingResponse.java`, `src/main/java/com/cargo/booking/dto/response/BookingCreatedResponse.java`, `src/main/java/com/cargo/booking/dto/response/CustomerResponse.java`, `src/main/java/com/cargo/booking/dto/response/CargoResponse.java`, `src/main/java/com/cargo/booking/dto/response/EquipmentResponse.java`, `src/main/java/com/cargo/booking/dto/response/PagedResponse.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/005_api_endpoints.md` |

Delivered:

- Added `BookingResponse` with nested customer, cargo, equipment response fields and UTC timestamp fields.
- Added slim `BookingCreatedResponse` for create-booking responses.
- Added `CustomerResponse`, `CargoResponse`, and `EquipmentResponse` records for nested response data.
- Added generic `PagedResponse<T>` with page metadata and a static `from(Page<T>)` factory.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test` passed with 58 tests, 0 failures, 0 errors.
- `git diff --check` passed.
- After rebasing onto the latest `origin/master`, `mvn compile` passed and `mvn test` passed with 61 tests, 0 failures, 0 errors.

Notes:

- Mapper and controller behavior remain intentionally out of scope for follow-on API beads.

### bo-b0p.1 - Add error response DTOs

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/jasper |
| Branch | `polecat/jasper/bo-b0p.1@mpcfc8dd` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-19T09:22:22Z |
| Completed UTC | 2026-05-19T09:36:26Z |
| Elapsed wall time | 14m 04s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/exception/ErrorResponse.java`, `src/main/java/com/cargo/booking/exception/ValidationErrorResponse.java`, `src/main/java/com/cargo/booking/exception/FieldViolation.java`, `src/test/java/com/cargo/booking/exception/ErrorResponseTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/007_error_handling.md` |

Delivered:

- Added `ErrorResponse`, `ValidationErrorResponse`, and `FieldViolation` records in `com.cargo.booking.exception`.
- Modeled the standard error fields: timestamp, status, error, message, path, and optional request ID.
- Added field-level validation details with immutable violation list storage.
- Annotated optional request ID components so absent request IDs are omitted from JSON responses.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest="ErrorResponseTest"` passed with 2 tests, 0 failures, 0 errors.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 63 tests, 0 failures, 0 errors.

Notes:

- `GlobalExceptionHandler` and `ErrorResponseBuilder` remain out of scope for this DTO bead and are covered by later error-handling beads.

### bo-b0p.4 - Map framework validation and HTTP exceptions

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-b0p.4@mpcliuk8` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-19T12:15:34Z |
| Completed UTC | 2026-05-19T12:25:34Z |
| Elapsed wall time | 10m 00s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `pom.xml`, `src/main/java/com/cargo/booking/exception/GlobalExceptionHandler.java`, `src/main/resources/application.yml`, `src/test/java/com/cargo/booking/exception/GlobalExceptionHandlerTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/007_error_handling.md` |

Delivered:

- Mapped business, validation, malformed JSON, missing parameter, type mismatch, unsupported method/media, no-handler, access-denied fallback, equipment availability, and catch-all errors through `GlobalExceptionHandler`.
- Added validation response generation with sorted field violations for `MethodArgumentNotValidException` and Jakarta `ConstraintViolationException`.
- Configured Spring MVC to throw `NoHandlerFoundException` for missing handlers while preserving static resource mappings.
- Added `spring-security-core` so the fallback Spring Security `AccessDeniedException` handler compiles without enabling web security auto-configuration.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=GlobalExceptionHandlerTest` initially failed during test-source compilation because of a missing test import and a private Spring constructor call; the test source was corrected.
- `mvn test -Dtest=GlobalExceptionHandlerTest` passed with 9 tests, 0 failures, 0 errors.
- After rebasing onto the latest `origin/master`, `git diff --check origin/master...HEAD`, `mvn compile`, and `mvn test` passed with 80 tests, 0 failures, 0 errors.

Notes:

- Full gate output included expected application/test logging from exception handler and repository tests.

### bo-2tm.5 - Implement get booking endpoint

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-2tm.5@mpcm0igo` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-19T12:29:17Z |
| Completed UTC | 2026-05-19T12:39:53Z |
| Elapsed wall time | 10m 36s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/controller/BookingController.java`, `src/test/java/com/cargo/booking/controller/BookingControllerTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/005_api_endpoints.md` |

Delivered:

- Added `GET /api/v1/bookings/{id}` with numeric ID and `BKG-YYYY-NNNNN` reference dispatch.
- Invalid identifiers now throw `BookingValidationException` with an explicit expected-format message.
- Mapped successful reads to `BookingResponse` and documented OpenAPI response statuses.
- Added focused controller tests for numeric ID lookup, reference lookup, and invalid identifier handling.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest="BookingControllerTest"` passed with 4 tests, 0 failures, 0 errors.
- Post-rebase `git diff --check origin/master...HEAD`, `mvn compile`, and `mvn test` passed with 83 tests, 0 failures, 0 errors.

Notes:

- `BookingAccessAuthorizer` is not present in this branch; ownership wiring is tracked separately by `bo-m7w.7` and `bo-m7w.8`.

### bo-2tm.6 - Implement list bookings endpoint

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/quartz |
| Branch | `polecat/quartz/bo-2tm.6@mpcm6do7` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-19T12:33:50Z |
| Completed UTC | 2026-05-19T12:48:59Z |
| Elapsed wall time | 15m 09s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/controller/BookingController.java`, `src/test/java/com/cargo/booking/controller/BookingControllerTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/005_api_endpoints.md` |

Delivered:

- Added `GET /api/v1/bookings` with optional `customerId` and `status` filters.
- Applied pageable defaults using `@PageableDefault(size = 20, sort = "createdAt", direction = DESC)`.
- Mapped the service `Page<Booking>` to `Page<BookingResponse>` through `BookingMapper` and wrapped it in `PagedResponse`.
- Added controller tests for filtered list responses, page metadata, default sorting, and max page size behavior.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn test -Dtest=BookingControllerTest` initially failed during test-source compilation because the standalone pageable resolver test used an unavailable customizer type; the test setup was corrected.
- `mvn test -Dtest=BookingControllerTest` then failed because the page fixture expected `last=false` for the final page; the fixture total was corrected.
- `mvn test -Dtest=BookingControllerTest` passed with 3 tests, 0 failures, 0 errors.
- `mvn compile` passed.
- `git diff --check` passed.
- Rebase onto the latest `origin/master` required resolving overlap with `bo-2tm.5` in controller, controller test, and delivery evidence files.
- After conflict resolution, `mvn test -Dtest=BookingControllerTest` passed with 6 tests, 0 failures, 0 errors.
- Post-rebase `git diff --check origin/master...HEAD` and `mvn compile` passed.
- Post-rebase `mvn test` passed with 85 tests, 0 failures, 0 errors.

Notes:

- `BookingAccessAuthorizer` is intentionally not wired in this bead because `bo-m7w.7` adds the authorizer and `bo-m7w.8` wires ownership checks after controller endpoints exist.

### bo-2tm.8 - Implement lifecycle endpoints

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/jasper |
| Branch | `polecat/jasper/bo-2tm.8@mpdsqfn9` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-20T08:25:19Z |
| Completed UTC | 2026-05-20T08:32:16Z |
| Elapsed wall time | 6m 57s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/controller/BookingController.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/005_api_endpoints.md` |

Delivered:

- Added `PATCH /api/v1/bookings/{id}/confirm`, `PATCH /api/v1/bookings/{id}/start`, and `PATCH /api/v1/bookings/{id}/complete`.
- Delegated each endpoint to the matching `BookingService` lifecycle method.
- Mapped lifecycle service results to `BookingResponse` through `BookingMapper`.
- Added OpenAPI operation descriptions and response metadata for the lifecycle endpoints.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=BookingControllerTest` passed with 6 tests, 0 failures, 0 errors.
- Post-rebase `git diff --check origin/master...HEAD` passed.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 85 tests, 0 failures, 0 errors.

Notes:

- `BookingAccessAuthorizer` is intentionally not wired in this bead because `bo-m7w.7` adds the authorizer and `bo-m7w.8` wires ownership checks after controller endpoints exist.

### bo-m7w.2 - Implement JwtTokenProvider

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-m7w.2@mpfexigu` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-21T11:34:17Z |
| Completed UTC | 2026-05-21T11:41:00Z |
| Elapsed wall time | 6m 43s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `pom.xml`, `src/main/java/com/cargo/booking/security/JwtTokenProvider.java`, `src/test/java/com/cargo/booking/security/JwtTokenProviderTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/006_security.md` |

Delivered:

- Added JJWT dependencies needed for local HS256 JWT validation.
- Added `JwtTokenProvider` with issuer, audience, signature, expiration, and non-empty subject validation.
- Extracted subject, username/name, `customerId`/`customer_id`, normalized `ROLE_*` roles, Users `role=admin`, and scope authorities without logging token contents or calling Users.
- Added focused unit coverage for valid tokens, wrong issuer/audience, expired tokens, invalid signatures, missing subject, blank tokens, customer ID variants, roles, and scopes.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest="JwtTokenProviderTest,SecurityPropertiesTest"` passed with 13 tests, 0 failures, 0 errors.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 107 tests, 0 failures, 0 errors.

Notes:

- The provider creates its signing key lazily so local/security-disabled contexts can instantiate it when no JWT secret is configured; enabled deployments remain guarded by `JwtPropertiesValidator`.

### bo-m7w.3 - Implement JWT authentication filter

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-m7w.3@mpffbz4k` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-21T11:45:30Z |
| Completed UTC | 2026-05-21T11:49:52Z |
| Elapsed wall time | 4m 22s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/security/JwtAuthenticationFilter.java`, `src/test/java/com/cargo/booking/security/JwtAuthenticationFilterTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/006_security.md` |

Delivered:

- Added `JwtAuthenticationFilter` as a `OncePerRequestFilter` that extracts `Authorization: Bearer ...`, validates tokens through `JwtTokenProvider`, and sets the Spring `SecurityContext` only for valid tokens.
- Missing, non-Bearer, invalid, or provider-rejected tokens continue the chain unauthenticated.
- JWT parsing/authentication extraction failures are caught without logging token contents or throwing raw JWT exceptions from the filter.
- Added focused unit coverage for missing headers, non-Bearer headers, valid authentication population, invalid tokens, provider parsing failures, and preserving an existing authentication.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest="JwtAuthenticationFilterTest,JwtTokenProviderTest,SecurityPropertiesTest"` passed with 19 tests, 0 failures, 0 errors.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 113 tests, 0 failures, 0 errors.

Notes:

- Security filter chain registration and enabled/disabled security behavior are intentionally left for `bo-m7w.5`; this bead only adds the filter class and its behavior.

### bo-m7w.6 - Add SecurityContextHelper

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-m7w.6@mpfgob4q` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-21T12:23:09Z |
| Completed UTC | 2026-05-21T12:32:05Z |
| Elapsed wall time | 8m 56s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/security/AuthenticatedRequester.java`, `src/main/java/com/cargo/booking/security/JwtTokenProvider.java`, `src/main/java/com/cargo/booking/security/SecurityContextHelper.java`, `src/test/java/com/cargo/booking/security/JwtTokenProviderTest.java`, `src/test/java/com/cargo/booking/security/SecurityContextHelperTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/006_security.md` |

Delivered:

- Added `SecurityContextHelper` static accessors for the current subject, optional customer ID, username, normalized roles, `hasRole`, and `isOwnerOrPrivileged`.
- Added `AuthenticatedRequester` as the JWT authentication principal so security/ownership code can read JWT subject, username/name, optional `customerId` or `customer_id`, and normalized roles from `SecurityContext`.
- Updated `JwtTokenProvider` authentication creation to preserve requester details while keeping Spring authorities for existing endpoint role checks.
- Added focused coverage for unauthenticated contexts, customer ownership matches/mismatches, privileged service/operator/admin callers, Users `role=admin` normalization, map-backed future token details, and JWT principal population.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest="SecurityContextHelperTest,JwtTokenProviderTest"` passed with 13 tests, 0 failures, 0 errors.
- `mvn test -Dtest="SecurityContextHelperTest,JwtTokenProviderTest,JwtAuthenticationFilterTest,SecurityConfig*Test"` passed with 26 tests, 0 failures, 0 errors.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 127 tests, 0 failures, 0 errors.

Notes:

- Existing `BookingAccessAuthorizer` implementation remains intentionally out of scope for dependent bead `bo-m7w.7`.

### bo-m7w.8 - Wire ownership checks into controllers

| Field | Value |
| --- | --- |
| Status | Submitted |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-m7w.8@mpfiqhro` |
| PR | Pending merge queue submission via `gt done` |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-21T13:20:46Z |
| Completed UTC | 2026-05-21T13:26:07Z |
| Elapsed wall time | 5m 21s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/controller/BookingController.java`, `src/test/java/com/cargo/booking/controller/BookingControllerTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/006_security.md` |

Delivered:

- Wired `BookingAccessAuthorizer` into create, list, and get controller paths before the corresponding `BookingService` calls.
- Preserved existing cancel authorization and kept lifecycle operator endpoints unchanged.
- Updated controller tests to verify authorizer calls occur before service calls for create, list, numeric get, reference get, and cancel.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test -Dtest=BookingControllerTest` passed with 10 tests, 0 failures, 0 errors.
- Post-rebase `git diff --check origin/master...HEAD` passed.
- Post-rebase `mvn compile` passed.
- Post-rebase `mvn test` passed with 143 tests, 0 failures, 0 errors.

Notes:

- No broad `@PreAuthorize` replacement was introduced; ownership checks remain explicit controller calls as specified.

### bo-ww4.4 - Add Resilience4j defaults

| Field | Value |
| --- | --- |
| Status | GitHub PR opened; pending merge queue submission |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-ww4.4@mpfm7s2d` |
| PR | https://github.com/AgenticFunProject/booking/pull/48 |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-21T14:58:13Z |
| Completed UTC | 2026-05-21T15:03:43Z |
| Elapsed wall time | 5m 30s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `pom.xml`, `src/main/resources/application.yml`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/008_integrations.md` |

Delivered:

- Added Resilience4j Spring Boot 3, circuit breaker, retry, and Spring AOP dependencies with a shared Resilience4j version property.
- Added default circuit breaker and retry configuration in `application.yml`, including comments for future per-client overrides.
- Added authorized health component/detail settings required by the integration health spec.

Verification:

- `./mvnw compile` was attempted but blocked because this checkout does not include a Maven wrapper.
- `mvn compile` passed.
- `mvn test` passed with 154 tests, 0 failures, 0 errors.
- `git diff --check` passed.

Notes:

- Per-client Resilience4j instances remain intentionally empty until external API SLAs and client error contracts are known.
- GitHub PR #48 was opened before Gas Town completion to satisfy the repository branch/PR rule.

### bo-k7u.5 - Add Maven wrapper

| Field | Value |
| --- | --- |
| Status | GitHub PR opened; pending merge |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-k7u.5@mpgn8usw` |
| PR | https://github.com/AgenticFunProject/booking/pull/54 |
| Merge commit | Pending refinery merge |
| Started UTC | 2026-05-22T08:15:15Z |
| Completed UTC | 2026-05-22T08:23:27Z |
| Elapsed wall time | 8m 12s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, `mvnw.cmd`, `MAVEN.md`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/001_project_setup.md` |

Delivered:

- Generated the Maven wrapper with Maven Wrapper Plugin 3.3.4.
- Configured the wrapper to download Apache Maven 3.9.16 from Maven Central.
- Updated Maven usage docs to prefer `./mvnw`.
- Resolved the prior delivery blocker where `./mvnw compile` could not run because wrapper files were missing.

Verification:

- `mvn -N wrapper:wrapper` passed and generated `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties`.
- `./mvnw -version` passed using Apache Maven 3.9.16 from the wrapper cache.
- Post-rebase `./mvnw compile` passed.
- Post-rebase `./mvnw test` passed with 164 tests, 0 failures, 0 errors.

Notes:

- Wrapper generation used the default `only-script` distribution type from Maven Wrapper Plugin 3.3.4; no wrapper JAR is required.

### bo-k7u.6 - Configure explicit annotation processing

| Field | Value |
| --- | --- |
| Status | GitHub PR opened; pending merge |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-k7u.6@mpgoytfw` |
| PR | https://github.com/AgenticFunProject/booking/pull/55 |
| Merge commit | Pending GitHub merge |
| Started UTC | 2026-05-22T09:03:01Z |
| Completed UTC | 2026-05-22T09:08:47Z |
| Elapsed wall time | 5m 46s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `pom.xml`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/001_project_setup.md` |

Delivered:

- Added an explicit `maven-compiler-plugin` configuration with Lombok on the annotation processor path.
- Preserved Lombok as a provided dependency while removing javac's implicit annotation-processing future-warning on clean compile.

Verification:

- `./mvnw -DskipTests clean compile` passed and no longer emitted the implicit annotation-processing warning.
- `./mvnw test` passed with 164 tests, 0 failures, 0 errors.
- `git diff --check` passed.

Notes:

- The worktree initially sat on a stale `bo-k7u.7` branch with no commits; it was reconciled to the existing `bo-k7u.6` branch from current `origin/master` before editing.
- Existing Mockito dynamic-agent warnings remain out of scope for this bead and are tracked by follow-up `bo-k7u.8`.

### bo-k7u.7 - Fix deprecated test API usage

| Field | Value |
| --- | --- |
| Status | Pending merge queue submission via `gt done` |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-k7u.7@mpgq0wqh` |
| PR | Pending merge queue |
| Merge commit | Pending merge |
| Started UTC | 2026-05-22T09:32:37Z |
| Completed UTC | 2026-05-22T09:42:26Z |
| Elapsed wall time | 9m 49s |
| Timing source | Hook attachment timestamp and agent-recorded UTC completion timestamp |
| Files changed | `src/main/java/com/cargo/booking/exception/GlobalExceptionHandler.java`, `src/test/java/com/cargo/booking/exception/ErrorHandlingMockMvcTest.java`, `src/test/java/com/cargo/booking/exception/GlobalExceptionHandlerTest.java`, `src/test/java/com/cargo/booking/repository/BookingRepositoryTest.java`, `src/test/java/com/cargo/booking/repository/BookingReferenceCounterRepositoryTest.java`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/009_testing.md`, `specs/007_error_handling.md`, `specs/003_data_access.md` |

Delivered:

- Replaced deprecated no-handler testing through `DispatcherServlet#setThrowExceptionIfNoHandlerFound(boolean)` and `NoHandlerFoundException` with current `NoResourceFoundException` handling.
- Removed deprecated `Specification.where(...)` usage from repository tests.
- Replaced deprecated `HttpMessageNotReadableException` test construction with the current `HttpInputMessage` constructor.
- Replaced deprecated embedded database provider enum usage from `ZONKY` to `EMBEDDED` in repository slice tests.

Verification:

- Baseline `./mvnw test` passed with 164 tests and showed the deprecated API warnings targeted by this bead.
- `./mvnw compile` passed.
- Focused `./mvnw test -Dtest="GlobalExceptionHandlerTest,ErrorHandlingMockMvcTest,BookingRepositoryTest,BookingReferenceCounterRepositoryTest" -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.showWarnings=true` passed with 31 tests and no compiler deprecation warnings.
- Final `./mvnw test` passed with 164 tests, 0 failures, 0 errors; prior Java compiler deprecated API warnings were absent.

Notes:

- Full test output still includes existing runtime log noise such as Hibernate dialect deprecation logging; this bead removed Java compiler deprecated API warnings from the affected test paths.

### bo-k7u.9 - Remove explicit Hibernate dialect warning

| Field | Value |
| --- | --- |
| Status | GitHub PR opened; pending merge |
| Agent | booking/polecats/obsidian |
| Branch | `polecat/obsidian/bo-k7u.9@mpgqnpxh` |
| PR | https://github.com/AgenticFunProject/booking/pull/58 |
| Merge commit | Pending GitHub merge |
| Started UTC | 2026-05-22T09:50:23Z |
| Completed UTC | 2026-05-22T09:55:49Z |
| Elapsed wall time | 5m 26s |
| Timing source | Hook attachment timestamp and agent-recorded UTC PR evidence timestamp |
| Files changed | `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `specs/001_project_setup.md`, `docs/delivery/IMPLEMENTATION_LEDGER.md`, `docs/delivery/QUALITY_LOG.md` |
| Spec | `specs/001_project_setup.md`, `specs/003_data_access.md`, `specs/009_testing.md` |

Delivered:

- Removed explicit `hibernate.dialect` configuration from base and test YAML so Hibernate infers PostgreSQL from JDBC metadata.
- Kept the existing Hibernate JDBC UTC time-zone configuration.
- Updated the setup spec to require the UTC Hibernate JDBC setting instead of the explicit PostgreSQL dialect.

Verification:

- Baseline `./mvnw test -Dtest="BookingRepositoryTest,BookingReferenceCounterRepositoryTest"` passed with 17 tests and captured 2 `HHH90000025` warnings before the cleanup.
- `./mvnw compile` passed.
- Focused repository tests passed with 17 tests and captured 0 `HHH90000025` warnings after the cleanup.
- Full `./mvnw test` passed with 164 tests, 0 failures, 0 errors, and captured 0 `HHH90000025` warnings.

Notes:

- Remaining repository-test logging, such as embedded PostgreSQL and SQL statement output, is outside this bead and remains available for follow-up log-noise cleanup.

## Entry Template

```md
### <bead-id> - <title>

| Field | Value |
| --- | --- |
| Status | Closed |
| Agent | <agent> |
| Branch | `<branch>` |
| PR | <url> |
| Merge commit | `<sha>` |
| Started UTC | <ISO-8601 UTC timestamp copied into this file> |
| Completed UTC | <ISO-8601 UTC timestamp copied into this file> |
| Elapsed wall time | <duration copied into this file, for example "20m 57s"> |
| Timing source | <bead fields, agent timer, CI timestamps, or other source> |
| Files changed | `<file>`, `<file>` |
| Spec | `<spec path>` |

Delivered:

- ...

Verification:

- ...

Notes:

- ...
```
