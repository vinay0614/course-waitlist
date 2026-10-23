# Course Registration and FIFO Waitlist - Design Specification

## 1. Overview and scope

This feature manages enrollment in the application's single configured course offering. It accepts enrollment requests, queues eligible learners when the offering is full, removes enrolled learners on drop, and promotes at most one eligible waitlisted learner for that vacancy.

The current sample offering is `JAVA-ADV-2026-SEM1`, with prerequisite course ID `JAVA-FUNDAMENTALS` and capacity 2. There is no frontend, payment processing, external identity integration, authentication, or persistent database infrastructure in this implementation.

## 2. Service boundary and architecture

- `EnrollmentController` maps HTTP requests to `EnrollmentService`, applies `@NotBlank` validation to path identifiers, and validates the enrollment request body.
- `EnrollmentService` enforces eligibility, duplicate handling, capacity checks, drops, FIFO promotion, and offering inspection. These business rules are applied on the controller-to-service path.
- `InMemoryEnrollmentRepository` stores the configured `EnrollmentState` in a `ConcurrentHashMap`; its default constructor creates the single sample offering. It has no API for adding offerings at runtime.
- `MockLearnerRepository` loads learner seed records from JSON with Jackson and keeps current learner records in a `ConcurrentHashMap`.
- `CourseOffering`, `Learner`, and `EnrollmentState` represent the offering, learner eligibility data, and enrollment/waitlist state respectively.
- `EnrollmentRequest`, `OperationResponse`, `OfferingResponse`, and `ApiErrorResponse` define the request, success, inspection, and error payloads.
- `ApiExceptionHandler` maps domain exceptions and request errors to structured HTTP errors. Unexpected failures return a generic 500 response and log the exception type.

The request path is controller -> service -> repositories/state. The service is the business-rule boundary for application requests. The repositories do not persist state outside process memory.

## 3. Data model

`CourseOffering` contains `offeringId`, `prerequisite`, and `capacity`. `offeringId` identifies a particular scheduled offering; `prerequisite` is a course ID string, not a separate prerequisite entity. The sample prerequisite is `JAVA-FUNDAMENTALS`. A learner's completed prerequisite course IDs are represented by `Learner.completedPrerequisites` and checked for an exact match.

`Learner` contains `learnerId`, a set of `completedPrerequisites`, and `activeStudent`. `EnrollmentState` associates one offering with an insertion-ordered set of enrolled learner IDs and an ordered list of waitlisted IDs. It calculates available seats as capacity minus enrolled count and returns snapshots of its collections.

`src/main/resources/mock-data/learners.json` is parsed at repository initialization. Missing or malformed data causes initialization to fail. The records are loaded into memory; `setActiveStatus` can update active status at runtime without changing the packaged JSON seed. Prerequisite completion is represented by the loaded learner record and has no public runtime update operation.

## 4. API design

| Method and endpoint | Purpose and request data | Successful response | Relevant errors |
|---|---|---|---|
| `POST /offerings/{offeringId}/enroll` | Enroll or waitlist; JSON body `{"learnerId":"L101"}` | HTTP 200 `OperationResponse`: `outcome`, `message` | HTTP 400 for invalid/malformed request; HTTP 404 for unknown offering or learner. Business outcomes such as `DUPLICATE`, `REJECTED_PREREQUISITE`, and `REJECTED_INACTIVE` are returned with HTTP 200. |
| `DELETE /offerings/{offeringId}/enrollments/{learnerId}` | Drop the specified enrolled learner; no body | HTTP 200 `OperationResponse`: `outcome`, `message` | HTTP 400 for blank path identifiers; HTTP 404 for unknown offering; HTTP 409 `NOT_ENROLLED` if the learner is not enrolled. |
| `GET /offerings/{offeringId}` | Inspect the offering; no body | HTTP 200 `OfferingResponse`: `offeringId`, `capacity`, `enrolledLearnerIds`, `availableSeats`, `waitlist` | HTTP 400 for blank offering ID; HTTP 404 for unknown offering. |

Example POST response:

```json
{"outcome":"ENROLLED","message":"Learner enrolled"}
```

DELETE also returns only `outcome` and `message`; it does not disclose the promoted learner. Promotion is visible through a later GET. GET returns copied lists for enrolled IDs and the FIFO waitlist. Error bodies contain `code`, `message`, and `timestamp`; validation and malformed-body messages are generic. The unknown-learner message includes the requested learner ID.

**Endpoint discrepancy:** The current controller maps enrollment to `/enroll` (not `/enrollments`). This specification documents the implemented route.

## 5. Business rules and invariants

For an enrollment request, the service first resolves the offering and learner. It returns `DUPLICATE` without changing state if the learner is already enrolled or waitlisted. Otherwise, the learner must have completed the offering prerequisite and have active student status. Failures return `REJECTED_PREREQUISITE` or `REJECTED_INACTIVE` without enrollment or waitlisting. An eligible learner is enrolled if a seat is available; otherwise they are appended to the waitlist.

A drop succeeds only when the learner is currently enrolled. The service removes that enrollment and scans a snapshot of the waitlist from the front. It rechecks prerequisite completion and active status for each candidate. An ineligible candidate is skipped and left in the queue in its existing relative position. The first eligible candidate is removed from the waitlist and enrolled. At most one learner is promoted per drop. If none qualify, the seat remains available. A repeated or otherwise invalid drop returns `NOT_ENROLLED` and does not trigger promotion.

For requests through `EnrollmentService`, the principal invariants are:

- Enrolled count does not exceed offering capacity.
- A learner already in either collection cannot be added again through enrollment requests.
- A learner is not added to both collections by the service; promotion removes the waitlist entry before enrollment.
- Waitlist order is FIFO, except for the learner actually removed by promotion.
- A successful drop causes at most one promotion.

These protections are implemented in service flow and collection choices; `EnrollmentState` itself does not enforce capacity, and its mutation methods are public. A caller that obtains state directly from the repository could bypass service invariants. The REST controller does not expose such a path. The concurrency test checks enrollment and waitlist counts for simultaneous service calls, but does not exhaustively verify every invariant under every interleaving.

## 6. Concurrency and persistence

`EnrollmentService.enroll`, `drop`, and `inspect` are synchronized instance methods. This serializes service calls in the singleton application's JVM, including capacity checks and state changes, drop-plus-promotion, and inspection snapshots. It does not provide distributed locking or coordination between multiple application instances. Direct use of the repository/state mutation API is outside this service synchronization boundary.

Enrollment state is held in ordinary Java collections by `InMemoryEnrollmentRepository`. Learner seed JSON is loaded into an in-memory map, where active status can be changed at runtime. Neither learner changes nor enrollment/waitlist state are written back to disk. All runtime state resets when the application restarts.

## 7. Assumptions and tradeoffs

**Implemented decisions:** The repository initializes one fixed sample offering. A prerequisite is a course ID string compared against a learner's completed-course set. Both initial enrollment and waitlist admission require prerequisite completion and active status. Promotion repeats those checks, skips ineligible entries without reordering them, and promotes no more than one learner per vacancy.

**Limitations:** Collections and synchronized service methods are intentionally small and suitable only for a single process. The in-memory repository has no durable storage or multi-instance coordination. `EnrollmentState` exposes public mutation methods, so invariants rely on application mutations going through `EnrollmentService` rather than arbitrary repository callers. The sample learner records and fixed offering are assessment fixtures, not an external student or course system.

## 8. Limitations and Follow-up Risks

- **Runtime state is not durable.** Enrollment and waitlist changes, along with runtime learner-status changes, are lost on restart. If continuity across restarts becomes necessary, add persistence appropriate to the assessment's scope.
- **Concurrency is single-JVM only.** Synchronized service methods coordinate requests handled by one application instance; multiple instances would maintain separate state and could make conflicting capacity decisions. Keep deployment single-instance or add shared coordination if scaling becomes a requirement.
- **Eligibility changes do not trigger promotion.** Eligibility is checked when a learner first enrolls and rechecked when a drop scans the queue. An ineligible learner stays in place; a later change to active status does not itself trigger promotion, so an available seat may remain unused until another drop. If immediate promotion becomes a requirement, connect eligibility changes to a promotion attempt.
- **No rollback is provided for unexpected mid-operation failures.** Expected rejections are checked before mutation, and a non-enrolled drop leaves state unchanged. However, a runtime failure after a successful drop has removed the learner could leave a vacancy or partially completed promotion.

Audit history and learner notifications are also outside the current implementation.

## 9. Verification

The existing tests are `EnrollmentServiceTests`, `EnrollmentApiTests`, and `CourseRegistrationApplicationTests`. The service tests use fresh repository/service fixtures and assert enrollment, waitlisting, duplicate outcomes, prerequisite and inactive rejection, FIFO promotion, skipped inactive learners, retained waitlist state, vacant-seat behavior, status changes before promotion, unknown IDs, drop errors, and concurrent enrollment counts. API tests assert blank and malformed request errors, operation outcomes/messages, and GET offering state before and after promotion. The context test verifies that the Spring application context loads.

## 10. Future considerations (out of scope)

Persistent enrollment storage, multi-instance concurrency control, audit history, and learner notifications are possible future additions. None is implemented in this project.
