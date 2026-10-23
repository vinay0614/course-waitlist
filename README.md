# Course Registration and FIFO Waitlist

## Project overview

A Java 17 Spring Boot REST service for one configured course offering. It checks prerequisite and active-student eligibility, enrolls learners while seats remain, queues eligible learners when full, and performs FIFO promotion after a successful drop.

## Prerequisites

- JDK 17.
- The included Maven Wrapper (`mvnw` / `mvnw.cmd`). A separate Maven installation is not required.
- Network access may be needed on the first build so the wrapper distribution and Maven dependencies can be downloaded.

## Setup

Open the project root (the directory containing `pom.xml`) in IntelliJ IDEA and import it as a Maven project. Allow Maven to resolve dependencies. From a terminal, commands below can also be run directly in the project root.

`src/main/resources/mock-data/learners.json` is required seed data. `MockLearnerRepository` loads it with Jackson during application initialization and fails startup if it is missing or malformed. The sample offering is initialized in `InMemoryEnrollmentRepository`; its offering ID is `JAVA-ADV-2026-SEM1`, prerequisite course ID is `JAVA-FUNDAMENTALS`, and capacity is 2. Enrollment state and runtime learner status are held in memory.

The only application property currently set is `spring.application.name=course-registration`; no custom server port is configured.

## Run the application

From the project root:

**Windows PowerShell**

```powershell
.\mvnw.cmd spring-boot:run
```

**Unix-like shell**

```sh
./mvnw spring-boot:run
```

In IntelliJ IDEA, run `com.vinay.aiassistance.courseregistration.CourseRegistrationApplication` from `src/main/java`. No additional application configuration is required.

There is no `server.port` setting in the project. Spring Boot's default is port `8080` (base URL `http://localhost:8080`) unless changed by the runtime environment. This README does not claim to have verified a live startup.

## Execute tests

Run the full Maven test suite from the root:

**Windows PowerShell**

```powershell
.\mvnw.cmd test
```

**Unix-like shell**

```sh
./mvnw test
```

In IntelliJ IDEA, run the tests under `src/test/java` or use the Maven tool window's `test` lifecycle goal. Maven Surefire reports are written to `target/surefire-reports` after a test run.

The service tests assert enrollment, waitlisting, duplicates, eligibility, drops, FIFO promotion, status changes, unknown IDs, and concurrent service-call counts.

## API usage

All examples use synthetic learner IDs from the mock seed data. The examples assume the default local port and a fresh application state.

| Method and endpoint | Request and successful response | Important errors |
|---|---|---|
| `POST /offerings/{offeringId}/enroll` | JSON `{"learnerId":"L101"}`; HTTP 200 with `{"outcome":"ENROLLED","message":"Learner enrolled"}`. Other HTTP 200 outcomes are `WAITLISTED`, `DUPLICATE`, `REJECTED_PREREQUISITE`, and `REJECTED_INACTIVE`. | HTTP 400 for invalid or malformed requests; HTTP 404 for an unknown offering or learner. |
| `DELETE /offerings/{offeringId}/enrollments/{learnerId}` | No body; HTTP 200 with `{"outcome":"DROPPED","message":"Enrollment dropped"}`. Promotion happens internally. | HTTP 400 for blank path identifiers; HTTP 404 for an unknown offering; HTTP 409 `NOT_ENROLLED` if the learner is not enrolled. |
| `GET /offerings/{offeringId}` | HTTP 200 with `offeringId`, `capacity`, `enrolledLearnerIds`, `availableSeats`, and FIFO `waitlist`. | HTTP 400 for a blank offering ID; HTTP 404 for an unknown offering. |

Error responses contain `code`, `message`, and `timestamp`. Business rejection outcomes from POST use HTTP 200. For malformed or blank request data, the API returns HTTP 400.

For sample request refer to the attached postman collection. `Course Registration.postman_collection.json`

The DELETE example succeeds only while `L101` is enrolled. After state changes, use GET to inspect current enrollment and waitlist order.

## Important implementation notes

Enrollment and waitlist state use in-memory Java collections and reset when the application restarts. Learner records are seeded from JSON into an in-memory repository; runtime active-status changes are not persisted. The service synchronizes enrollment, drop/promotion, and inspection calls within this JVM. This does not coordinate multiple application instances or provide distributed locking. There is no database.

## Troubleshooting

- Check `java -version`; the project targets Java 17.
- If Maven dependencies do not resolve in IntelliJ, reload the Maven project. The first wrapper invocation may need network access.
- If the application cannot start, confirm `src/main/resources/mock-data/learners.json` is present and valid JSON.
- After a Maven test run, inspect `target/surefire-reports` for failures. The API test POST path currently differs from the controller mapping, as noted above.
