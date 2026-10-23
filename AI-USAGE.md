# AI Usage Record

## Purpose and scope

This record summarizes AI assistance visible in the current Codex conversation for the Course Registration and FIFO Waitlist assessment. Prompt descriptions marked **summary** condense the long assessment prompts while preserving their substance. No separate ChatGPT conversations or other AI-tool records were available in the project workspace.

## AI tool

Codex in the current project conversation. The project initially contained an empty AI-usage template, but no saved prompts, chat export, or decision log. A Git diff could not be inspected: `git` was unavailable in the shell and the project root contained no `.git` directory.

## Interaction summary

| ID | Task | My decision | Affected files / decisions | Verification status |
|---|---|---|---|---|
| A1 | Implement enrollment, eligibility, FIFO waitlist, REST API, tests, and docs | User-directed task; explicit acceptance of the AI's specific implementation choices is not recorded | Service, repositories, models, controller, DTOs, exceptions, seed JSON, tests, `pom.xml`, `README.md`, `DESIGN.md`, initial `AI-USAGE.md` | Partially verified; historical full test run passed at that checkpoint, not rerun against current workspace |
| A2 | Simplify POST/DELETE responses; add safe logging; review synchronization and tests | User-directed refactor; explicit accept/reject decision beyond later requested work is not recorded | `OperationResponse`, controller/service, exception handler, state snapshot, docs, tests | Historical full suite passed; subsequent workspace changes were not rerun in this AI-usage task |
| A3 | Create the detailed design specification and later add a limitations section | User-directed documentation; explicit acceptance decision is not recorded | `DESIGN.md` | Partially verified by source/test inspection; no tests run for documentation changes |
| A4 | Create setup and API usage instructions | User-directed documentation; explicit acceptance decision is not recorded | `README.md` | Checked against then-visible source; current README contains one stale API-test-path statement (see outstanding evidence) |

## Detailed interaction records

### A1 - Enrollment and FIFO waitlist feature

- **Actual prompt (faithful summary):** The user began, "Act as a senior Java engineer. Implement the Course Enrollment and Waitlist feature described below in the current project." The prompt then specified Java 17/Spring Boot, JSON-backed mock learners, in-memory offering state, enrollment/drop/FIFO promotion, REST endpoints, validation, tests, documentation, and actual test/reporting steps.
- **Relevant AI response:** Inspected the starter project, then added the service/controller/repository/model/DTO structure, sample JSON, API error handling, tests, and documentation. The build exposed that Jackson Databind was absent from the starter dependencies; it was added because the requested JSON loading needed it. The repository was adjusted to construct an `ObjectMapper` because the Spring context did not provide one in this starter setup.
- **My decision:** No explicit accept/edit/reject decision on the AI's implementation choices is recorded. The following user prompt requested a focused response refactor, preserving the business rules.
- **Rationale evidenced in discussion:** The initial user prompt specified the in-memory design, service responsibilities, mock JSON repository, endpoint behavior, and test requirements. The Jackson dependency was tied to the explicit JSON-loading requirement.
- **Affected files verified in the current workspace:** `pom.xml`; `EnrollmentController.java`, `EnrollmentService.java`, `InMemoryEnrollmentRepository.java`, `MockLearnerRepository.java`, `CourseOffering.java`, `Learner.java`, `EnrollmentState.java`, request/response DTOs, and `ApiExceptionHandler.java` under `src/main/java/com/vinay/aiassistance/courseregistration/`; `src/main/resources/mock-data/learners.json`; the three test classes; and `README.md`, `DESIGN.md`, and this record.
- **Verification evidence:** A prior observed PowerShell invocation set `JAVA_HOME` and `PATH` to the installed JDK 17 and ran `.\mvnw.cmd test`. The final output at that checkpoint reported **12 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. Earlier attempts in that implementation turn failed first because Jackson was missing and then because the application context had no `ObjectMapper` bean; those issues were corrected before the successful run.
- **Verification status:** Partially verified. The passing output is historical; this record task did not rerun tests, and the source workspace has changed since that run.

### A2 - Minimal operation responses, logging, and synchronization review

- **Actual prompt (faithful summary):** The user began, "Make a focused, minimal refactor of the existing code." The prompt required POST/DELETE to return only outcome and concise message, preserve GET state and business behavior, add privacy-conscious logging, review synchronization, and update tests.
- **Relevant AI response:** Replaced the response DTO with `OperationResponse`, kept promotion in the service, added parameterized event/outcome logs without learner IDs, retained synchronized service methods, and logged unexpected failures by exception type while returning a generic 500 response. GET snapshots were retained/copied.
- **My decision:** No explicit acceptance or rejection is recorded. These changes were performed in response to direct user implementation instructions; that does not establish approval of every implementation choice.
- **Rationale:** The prompt explicitly required a minimal response contract, backend-only promotion, privacy-conscious logs, and a simple synchronization strategy. The retained synchronization and DTO shape match those constraints; the specific user approval of the final code is not recorded.
- **Affected files verified in the current workspace:** `OperationResponse.java`, `EnrollmentController.java`, `EnrollmentService.java`, `ApiExceptionHandler.java`, `EnrollmentState.java`, `EnrollmentApiTests.java`, `EnrollmentServiceTests.java`, `README.md`, and `DESIGN.md`.
- **Verification evidence:** The observed full Maven run at the response-refactor checkpoint reported **12 tests passing**. Current source inspection confirms POST is `/offerings/{offeringId}/enroll`, `OperationResponse` contains `outcome` and `message`, and enroll/drop/inspect remain synchronized. Current API tests assert operation outcomes/messages and GET state, but do not explicitly assert that extra operation-response properties are absent. No test run was performed for this record.
- **Verification status:** Partially verified; historical test result and current code inspection only, not a fresh run against the current workspace.

### A3 - Design specification and limitations

- **Actual prompts (faithful summaries):** First, create/update root `DESIGN.md` from inspected implementation, documenting scope, service boundary, data model, actual API, rules, concurrency, persistence, assumptions, tests, and future considerations. Later, add a concise section titled **Limitations and Follow-up Risks**, based on actual code and tests.
- **Relevant AI response:** Updated the design document with the current offering and API contract, then added limitations for restart loss, single-JVM synchronization, eligibility changes not triggering promotion, unexpected mid-operation failure handling, and test boundaries. It identified `EnrollmentState` public mutators as a way direct repository callers could bypass service-level invariants.
- **My decision:** No explicit accept/edit/reject decision is recorded. The user specified the content and scope; subsequent requests do not by themselves establish approval of the generated prose.
- **Rationale:** The document was based on the requested assessment boundaries and inspected implementation. Risks were described as limitations rather than proposed capabilities, and direct state mutation was qualified as outside the controller path.
- **Affected file verified:** `DESIGN.md`.
- **Verification evidence:** The document was inspected alongside the current controller, service, repositories, models, configuration, mock JSON, and tests. No application tests are applicable to the prose change and none were run for it.
- **Verification status:** Partially verified by source inspection; not independently validated by test execution.

### A4 - Setup and API README

- **Actual prompt (faithful summary):** Create/update root `README.md` after inspecting the project, documenting actual prerequisites, setup, run/test commands, endpoints, response shapes, in-memory limitations, and troubleshooting, without changing code or claiming unobserved test results.
- **Relevant AI response:** Updated setup/run/test/API instructions and stated that POST used `/enroll` while the then-inspected API tests appeared to use `/enrollments`.
- **My decision:** No explicit acceptance or rejection of the README wording is recorded.
- **Rationale:** The request required documentation to follow inspected source. At README-writing time the response identified the then-observed route discrepancy; the current controller and tests now agree, so that old README statement is inaccurate.
- **Affected file verified:** `README.md`.
- **Verification evidence:** Current inspection confirms the controller and API tests both use POST `/offerings/{offeringId}/enroll`. The current README still says the API test path differs; that statement is now stale. No commands were executed during the README-generation turn.
- **Verification status:** Partially verified; the documented endpoint is correct, but the stale note requires manual correction. No README test or runtime verification was performed.

## Engineering Judgment and Trade-offs

Alternatives below are labeled as actual prior AI output or as inferred during this retrospective review. An inferred alternative was not presented as an AI recommendation or recorded user proposal.

### 1. Keep operation responses minimal

- **Engineering decision:** POST and DELETE return only `outcome` and `message`; GET remains the way to inspect current offering state.
- **AI suggestion or alternative:** The earlier AI implementation used an `EnrollmentResponse` containing offering ID, learner ID, available seats, and promoted learner IDs in addition to the outcome and message. This was actual generated implementation, not just a hypothetical alternative.
- **Why it could be reasonable:** Returning promotion details and seat counts can save a client a follow-up GET and provide immediate operation context.
- **Decision and rationale:** The later user prompt explicitly requested the smaller operation contract. Keeping a single operation result avoids duplicating state in POST/DELETE responses; clients can use GET when they need a current snapshot.
- **Impact:** `OperationResponse`, `EnrollmentController`, `EnrollmentService`, and the separate `OfferingResponse` contract.

### 2. Keep routine logs and unexpected-error responses low-detail

- **Engineering decision:** Service logs record outcomes and events without learner IDs or request bodies. Unexpected failures return a generic 500 body; the server log records the exception type without its message or stack trace.
- **AI suggestion or alternative:** The user explicitly required avoiding sensitive request data in logs. A more identifying log or a response containing detailed exception information is an inferred alternative, not an AI suggestion recorded in the conversation; it could make individual request tracing and diagnosis easier.
- **Why this choice fits:** The prompt prioritized privacy and avoiding implementation details in client errors. The current event logs provide operation-level signals without adding identifiers that are unnecessary for these events.
- **Impact:** Logging in `EnrollmentService` and generic unexpected-error handling in `ApiExceptionHandler`. The trade-off is less detail for diagnosing an individual unexpected failure.

### 3. Keep storage bounded to JSON seed data and in-memory offering state

- **Engineering decision:** Learners are loaded from the packaged JSON resource into a runtime repository; enrollment and waitlist state use ordinary in-memory collections for one configured offering.
- **AI suggestion or alternative:** A database, distributed cache, or message broker was not proposed in the available AI interaction history. These are inferred alternatives; persistence or shared state could support restart continuity or multiple instances.
- **Why this choice fits:** The user's implementation scope explicitly excluded databases and infrastructure and called for a single-offering prototype. Adding those components would increase setup and maintenance without serving the stated assessment workflow.
- **Impact:** `MockLearnerRepository`, `InMemoryEnrollmentRepository`, `EnrollmentState`, and the setup/design documentation. Runtime changes are lost on restart.

### 4. Use service-level synchronization for this single instance

- **Engineering decision:** The service synchronizes enrollment, drop with promotion, and inspection methods.
- **AI suggestion or alternative:** The user asked to review the existing synchronized approach and keep it simple unless a correctness benefit justified more complexity. Distributed locks or other coordination are inferred alternatives, not recorded AI recommendations; they would matter if multiple instances shared the same course state.
- **Why this choice fits:** Synchronized service calls serialize the application request paths within this JVM and keep the capacity check, mutation, promotion, and GET snapshot in one simple boundary. Distributed coordination would not help this single-process in-memory state.
- **Impact:** `EnrollmentService` and its concurrent-enrollment test. This choice does not coordinate multiple application instances.

### 5. Retain skipped waitlist learners in FIFO order

- **Engineering decision:** During promotion, the service rechecks prerequisite and active status, skips an ineligible candidate for that attempt, retains the candidate in place, and promotes the first eligible learner only.
- **AI suggestion or alternative:** The AI implementation followed this rule from the user's explicit business requirements. Removing an ineligible learner or moving them to the queue tail is an inferred alternative, not a proposal recorded in the interaction history; either could reduce repeated checks or prioritize currently eligible learners.
- **Why this choice fits:** The user explicitly required FIFO, preserving skipped learners' relative positions, and rechecking active status at promotion. Retaining entries also allows a learner whose status changes to be reconsidered on a later promotion attempt.
- **Impact:** The promotion loop in `EnrollmentService` and the service tests for inactive front-of-queue retention and status rechecking.

## Verification summary

- **Historical command observed:** In PowerShell, after setting `JAVA_HOME` to `C:\Program Files\Java\jdk-17` and prepending its `bin` directory to `PATH`, the Maven wrapper `test` goal completed successfully at an earlier implementation checkpoint: **12 run, 0 failures, 0 errors, 0 skipped**. The exact command is recorded in entry A1.
- **Current source/test inspection:** `EnrollmentServiceTests`, `EnrollmentApiTests`, and `CourseRegistrationApplicationTests` exist. Current controller and API test POST paths align. Current test reports exist under `target/surefire-reports`, but their timestamps/results do not establish a fresh run against every current workspace change.
- **Not performed for this record:** No Maven command or runtime verification was executed while writing this file.

## Outstanding evidence and limitations

- Separate ChatGPT conversations, other AI tools, and external prompts are unavailable here; add those records manually if they were used.
- User accept/edit/reject decisions for the summarized implementation and documentation are not explicitly captured in the available conversation. They remain unrecorded rather than inferred from files being present.
- Git diff evidence is unavailable in this workspace. File existence and current contents were inspected, but authorship of changes made outside the visible AI session cannot be established.
- Correct the stale API-test-path note in `README.md` separately if desired; this task intentionally changed only `AI-USAGE.md`.

