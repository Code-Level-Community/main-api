---
name: codelevel-test-implementer
description: Implement production-ready tests for CodeLevel features following project patterns. Use when writing unit tests, integration tests, or API tests. Handles @QuarkusTest setup, mocking with constructor injection, testing domain validation, edge cases, and error scenarios. Generates tests that follow CodeLevel naming conventions and best practices. Always use this AFTER test strategy is defined but BEFORE implementation code.
---

# CodeLevel Test Implementer

Write **clean, complete, production-ready tests** that follow CodeLevel patterns. Tests should be self-documenting and catch real bugs.

## When to Use

- Writing unit tests for domain objects or services
- Creating integration tests (entity + service)
- Testing REST endpoints (API tests)
- After `codelevel-test-strategy` defines what to test
- Before implementation code is written (TDD workflow)

## Core Testing Patterns

### Pattern 1: Domain Value Object Tests (Unit)

Test domain validation happens **at construction**, not in service layer.

```java
// File: src/test/java/com/codelevel/module/{module}/domain/RatingTest.java

@DisplayName("Rating Value Object")
class RatingTest {

  @Test
  @DisplayName("should create rating with valid score 1-5")
  void shouldCreateValidRating() {
    assertThat(new Rating(1)).isNotNull();
    assertThat(new Rating(3)).isNotNull();
    assertThat(new Rating(5)).isNotNull();
  }

  @Test
  @DisplayName("should reject rating below minimum")
  void shouldRejectRatingBelowMinimum() {
    assertThatThrownBy(() -> new Rating(0))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Rating must be between 1 and 5");
    
    assertThatThrownBy(() -> new Rating(-1))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should reject rating above maximum")
  void shouldRejectRatingAboveMaximum() {
    assertThatThrownBy(() -> new Rating(6))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Rating must be between 1 and 5");
    
    assertThatThrownBy(() -> new Rating(10))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should be immutable")
  void shouldBeImmutable() {
    Rating rating = new Rating(5);
    // Record fields are private final - cannot modify
    assertThat(rating.score()).isEqualTo(5);
  }

  @Test
  @DisplayName("should implement equals and hashCode correctly")
  void shouldImplementEqualsAndHashCode() {
    Rating r1 = new Rating(5);
    Rating r2 = new Rating(5);
    Rating r3 = new Rating(4);

    assertThat(r1).isEqualTo(r2);
    assertThat(r1).isNotEqualTo(r3);
    assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
  }
}
```

### Pattern 2: Service Tests (Unit + Mock Dependencies)

Test service logic with **constructor injection** and mocks.

```java
// File: src/test/java/com/codelevel/module/{module}/persistence/resource/LessonReviewServiceTest.java

@DisplayName("LessonReviewService")
class LessonReviewServiceTest {

  private LessonReviewService service;
  
  @Mock
  private CourseClient courseClient;
  
  @Mock
  private Logger logger;

  @BeforeEach
  void setup() {
    // Constructor injection for testability
    service = new LessonReviewService(courseClient, logger);
  }

  @Test
  @DisplayName("should create review with valid data")
  void shouldCreateReviewWithValidData() {
    // Arrange
    UUID lessonId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Rating rating = new Rating(5);
    ReviewComment comment = new ReviewComment("Excellent lesson!");

    // Act
    LessonReview review = service.createReview(lessonId, userId, rating, comment);

    // Assert
    assertThat(review).isNotNull();
    assertThat(review.rating()).isEqualTo(rating);
    assertThat(review.comment()).isEqualTo(comment);
  }

  @Test
  @DisplayName("should throw if lesson doesn't exist")
  void shouldThrowIfLessonDoesNotExist() {
    // Arrange
    UUID fakeLessonId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    
    when(courseClient.getLessonExists(fakeLessonId))
      .thenThrow(new ResourceNotFound("Lesson not found"));

    // Act & Assert
    assertThatThrownBy(() -> 
      service.createReview(fakeLessonId, userId, new Rating(5), new ReviewComment("Test"))
    ).isInstanceOf(ResourceNotFound.class);
  }

  @Test
  @DisplayName("should retrieve reviews for lesson")
  void shouldRetrieveReviewsForLesson() {
    // Arrange
    UUID lessonId = UUID.randomUUID();
    LessonReview review1 = createTestReview(lessonId, "Good");
    LessonReview review2 = createTestReview(lessonId, "Excellent");

    // Act
    List<LessonReview> reviews = service.getReviewsForLesson(lessonId);

    // Assert
    assertThat(reviews).hasSize(2).contains(review1, review2);
  }

  @Test
  @DisplayName("should return empty list if no reviews exist")
  void shouldReturnEmptyListIfNoReviews() {
    // Act
    List<LessonReview> reviews = service.getReviewsForLesson(UUID.randomUUID());

    // Assert
    assertThat(reviews).isEmpty();
  }

  // Private helper for test data
  private LessonReview createTestReview(UUID lessonId, String comment) {
    return service.createReview(
      lessonId,
      UUID.randomUUID(),
      new Rating(4),
      new ReviewComment(comment)
    );
  }
}
```

### Pattern 3: Integration Tests (Entity + Service + DB)

Test with **@QuarkusTest** and real H2 database.

```java
// File: src/test/java/com/codelevel/module/{module}/persistence/resource/LessonReviewServiceIT.java
// Note: IT = Integration Test

@QuarkusTest
@DisplayName("LessonReviewService Integration")
class LessonReviewServiceIT {

  @Inject
  LessonReviewService service;

  @Test
  @DisplayName("should persist review to database")
  @Transactional
  void shouldPersistReviewToDatabase() {
    // Arrange
    UUID lessonId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // Act
    LessonReview review = service.createReview(
      lessonId,
      userId,
      new Rating(5),
      new ReviewComment("Great lesson")
    );

    // Clear persistence context to force DB read
    entityManager.flush();
    entityManager.clear();

    // Assert: data actually in DB
    Optional<LessonReview> retrieved = service.getReview(review.id().value());
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().comment()).isEqualTo("Great lesson");
  }

  @Test
  @DisplayName("should handle concurrent review creation")
  @Transactional
  void shouldHandleConcurrentReviewCreation() throws Exception {
    // Arrange
    UUID lessonId = UUID.randomUUID();
    ExecutorService executor = Executors.newFixedThreadPool(3);

    // Act: Create 3 reviews concurrently
    List<Future<LessonReview>> futures = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      final int index = i;
      futures.add(executor.submit(() -> 
        service.createReview(
          lessonId,
          UUID.randomUUID(),
          new Rating((index % 5) + 1),
          new ReviewComment("Review " + index)
        )
      ));
    }

    // Assert: all succeed
    List<LessonReview> reviews = futures.stream()
      .map(f -> {
        try {
          return f.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      })
      .toList();

    assertThat(reviews).hasSize(3).allSatisfy(r -> 
      assertThat(r.lessonId()).isEqualTo(lessonId)
    );

    executor.shutdown();
  }

  @Test
  @DisplayName("should maintain foreign key integrity")
  @Transactional
  void shouldMaintainForeignKeyIntegrity() {
    // Arrange
    UUID lessonId = UUID.randomUUID();
    LessonReview review = service.createReview(
      lessonId,
      UUID.randomUUID(),
      new Rating(5),
      new ReviewComment("Test")
    );

    // Act: Delete the lesson (cascade should delete review)
    LessonReviewEntity.deleteById(review.id().value());

    // Assert: Review is gone
    Optional<LessonReview> result = service.getReview(review.id().value());
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("should paginate results efficiently")
  @Transactional
  void shouldPaginateResultsEfficiently() {
    // Arrange: Create 50 reviews
    UUID lessonId = UUID.randomUUID();
    for (int i = 0; i < 50; i++) {
      service.createReview(
        lessonId,
        UUID.randomUUID(),
        new Rating((i % 5) + 1),
        new ReviewComment("Review " + i)
      );
    }

    // Act: Fetch paginated
    List<LessonReview> page1 = service.getReviewsForLesson(lessonId, 0, 20);
    List<LessonReview> page2 = service.getReviewsForLesson(lessonId, 20, 20);
    List<LessonReview> page3 = service.getReviewsForLesson(lessonId, 40, 20);

    // Assert: Correct pagination
    assertThat(page1).hasSize(20);
    assertThat(page2).hasSize(20);
    assertThat(page3).hasSize(10);
    
    // No overlaps
    Set<UUID> ids = new HashSet<>();
    Stream.of(page1, page2, page3).flatMap(List::stream)
      .forEach(r -> ids.add(r.id().value()));
    assertThat(ids).hasSize(50);
  }
}
```

### Pattern 4: REST API Tests (Full Stack)

Test endpoints with **@QuarkusTest** and HTTP client.

```java
// File: src/test/java/com/codelevel/module/{module}/http/rest/routes/LessonReviewResourceIT.java

@QuarkusTest
@DisplayName("LessonReviewResource API")
class LessonReviewResourceIT {

  @Inject
  TestClient client;

  @Test
  @DisplayName("should return 201 when creating review")
  void shouldReturn201OnCreate() {
    // Arrange
    CreateLessonReviewRequest req = new CreateLessonReviewRequest(
      UUID.randomUUID(),
      5,
      "Excellent lesson!"
    );

    // Act
    Response response = client
      .target("/api/reviews/lessons")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.json(req));

    // Assert
    assertThat(response.getStatus()).isEqualTo(201);
    
    LessonReviewResponse body = response.readEntity(LessonReviewResponse.class);
    assertThat(body.rating()).isEqualTo(5);
    assertThat(body.comment()).isEqualTo("Excellent lesson!");
  }

  @Test
  @DisplayName("should return 400 for invalid rating")
  void shouldReturn400ForInvalidRating() {
    // Arrange
    CreateLessonReviewRequest req = new CreateLessonReviewRequest(
      UUID.randomUUID(),
      6, // Invalid: max is 5
      "Review"
    );

    // Act
    Response response = client
      .target("/api/reviews/lessons")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.json(req));

    // Assert
    assertThat(response.getStatus()).isEqualTo(400);
    
    ErrorResponse error = response.readEntity(ErrorResponse.class);
    assertThat(error.message()).contains("Rating must be between 1 and 5");
  }

  @Test
  @DisplayName("should return 401 if not authenticated")
  void shouldReturn401IfNotAuthenticated() {
    // Act: No auth token
    Response response = client
      .target("/api/reviews/lessons")
      .request()
      .post(Entity.json(new CreateLessonReviewRequest(UUID.randomUUID(), 5, "Test")));

    // Assert
    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  @DisplayName("should return 404 if lesson doesn't exist")
  void shouldReturn404IfLessonNotFound() {
    // Arrange
    CreateLessonReviewRequest req = new CreateLessonReviewRequest(
      UUID.randomUUID(), // Fake lesson ID
      5,
      "Review"
    );

    // Act
    Response response = client
      .target("/api/reviews/lessons")
      .request()
      .post(Entity.json(req));

    // Assert
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  @DisplayName("should retrieve review by ID")
  void shouldRetrieveReviewById() {
    // Arrange: Create a review first
    CreateLessonReviewRequest createReq = new CreateLessonReviewRequest(
      UUID.randomUUID(),
      5,
      "Great lesson"
    );
    
    Response createResponse = client
      .target("/api/reviews/lessons")
      .request()
      .post(Entity.json(createReq));
    
    LessonReviewResponse created = createResponse.readEntity(LessonReviewResponse.class);
    UUID reviewId = created.id();

    // Act
    Response getResponse = client
      .target("/api/reviews/lessons/" + reviewId)
      .request()
      .get();

    // Assert
    assertThat(getResponse.getStatus()).isEqualTo(200);
    
    LessonReviewResponse retrieved = getResponse.readEntity(LessonReviewResponse.class);
    assertThat(retrieved.id()).isEqualTo(reviewId);
    assertThat(retrieved.comment()).isEqualTo("Great lesson");
  }

  @Test
  @DisplayName("should list reviews for lesson")
  void shouldListReviewsForLesson() {
    // Arrange
    UUID lessonId = UUID.randomUUID();
    
    for (int i = 0; i < 3; i++) {
      client
        .target("/api/reviews/lessons")
        .request()
        .post(Entity.json(new CreateLessonReviewRequest(
          lessonId,
          (i % 5) + 1,
          "Review " + i
        )));
    }

    // Act
    Response response = client
      .target("/api/reviews/lessons/by-lesson/" + lessonId)
      .request()
      .get();

    // Assert
    assertThat(response.getStatus()).isEqualTo(200);
    
    List<LessonReviewResponse> reviews = response.readEntity(
      new GenericType<List<LessonReviewResponse>>() {}
    );
    assertThat(reviews).hasSize(3);
  }
}
```

---

## Naming Conventions

### Test Classes
- **Unit tests:** `{Subject}Test.java` (no persistence)
  - Example: `RatingTest.java`, `ReviewCommentTest.java`
- **Integration tests:** `{Subject}IT.java` (with DB)
  - Example: `LessonReviewServiceIT.java`, `LessonReviewResourceIT.java`
- **API tests:** `{Resource}IT.java`
  - Example: `LessonReviewResourceIT.java`

### Test Methods
- **Format:** `should{ExpectedBehavior}{WhenCondition}()`
- **Examples:**
  - ✅ `shouldCreateReviewWithValidData()`
  - ✅ `shouldThrowIfRatingExceeds5()`
  - ✅ `shouldReturn404IfLessonNotFound()`
  - ❌ `testCreateReview()` (vague)
  - ❌ `shouldWork()` (meaningless)

### Display Names
Use `@DisplayName("Clear description")` for Javadoc-like readability:
```java
@Test
@DisplayName("should reject rating above maximum")
void shouldRejectRatingAboveMaximum() { ... }
```

---

## Test Data Builders

Create reusable test data:

```java
// File: src/test/java/com/codelevel/module/{module}/TestDataBuilder.java

class TestDataBuilder {

  public static CreateLessonReviewRequest aReviewRequest() {
    return new CreateLessonReviewRequest(
      UUID.randomUUID(),
      5,
      "Excellent!"
    );
  }

  public static CreateLessonReviewRequest aReviewRequestWithRating(int rating) {
    return new CreateLessonReviewRequest(
      UUID.randomUUID(),
      rating,
      "Review"
    );
  }

  public static CreateLessonReviewRequest aReviewRequestWithComment(String comment) {
    return new CreateLessonReviewRequest(
      UUID.randomUUID(),
      5,
      comment
    );
  }

  // Usage in tests:
  // CreateLessonReviewRequest req = aReviewRequest();
  // CreateLessonReviewRequest req = aReviewRequestWithRating(3);
}
```

---

## Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=RatingTest

# Run specific test method
./mvnw test -Dtest=RatingTest#shouldCreateValidRating

# Run with coverage report
./mvnw jacoco:report
# Report: target/site/jacoco/index.html

# Run only integration tests
./mvnw test -Dtest=*IT
```

---

## Coverage Standards

| Layer | Min Coverage |
|---|---|
| Domain Value Objects | 100% |
| Service Layer | 80-90% |
| Entity/Persistence | 70% (some JPA magic is hard to test) |
| Controller | 80% (focus on business logic, not HTTP details) |
| Exception Handlers | 100% |

Use `./mvnw jacoco:report` to check coverage.

---

## Best Practices

✅ **One assertion per test outcome**
```java
// ✅ GOOD: Test one thing, name is specific
@Test
void shouldRejectCommentOver500Chars() {
  assertThatThrownBy(() -> new ReviewComment("x".repeat(501)))
    .isInstanceOf(IllegalArgumentException.class);
}

// ❌ BAD: Multiple unrelated assertions
@Test
void shouldValidateComment() {
  assertThat(new ReviewComment("short")).isNotNull();
  assertThat(new ReviewComment("x".repeat(500))).isNotNull();
  assertThatThrownBy(() -> new ReviewComment("")).isInstanceOf(...);
  // What does this test prove if one assertion fails?
}
```

✅ **Use @DisplayName for readability**
```java
@DisplayName("Rating Value Object")
class RatingTest {
  @Test
  @DisplayName("should reject rating below minimum")
  void shouldRejectRatingBelowMinimum() { ... }
}

// Output: Rating Value Object > should reject rating below minimum
```

✅ **Never test the framework**
```java
// ❌ DON'T: Testing that Quarkus wires @Transactional
@Test
void shouldBeTransactional() {
  assertThat(service.getClass().getAnnotation(Transactional.class))
    .isNotNull();
}

// ✅ DO: Test behavior affected by transactions
@Test
void shouldRollbackIfValidationFails() {
  assertThatThrownBy(() -> service.createInvalid())
    .isInstanceOf(ValidationException.class);
  
  assertThat(database.findAllReviews()).isEmpty();
}
```

✅ **Mock external services, test business logic**
```java
// ✅ GOOD: Mock HTTP client, test service logic
@Test
void shouldCreateReviewIfLessonExists() {
  when(courseClient.getLessonExists(lessonId))
    .thenReturn(true);
  
  LessonReview review = service.createReview(...);
  assertThat(review).isNotNull();
}

// ❌ BAD: Actually calling external service in test
@Test
void shouldCreateReview() {
  // Real HTTP call to production course service!
  LessonReview review = service.createReview(...);
}
```

✅ **Use assertThat() for fluent assertions**
```java
// ✅ Fluent and clear
assertThat(review)
  .isNotNull()
  .extracting(LessonReview::rating)
  .extracting(Rating::score)
  .isEqualTo(5);

// ❌ Old style
assertTrue(review != null);
assertEquals(5, review.rating().score());
```

---

## Next Steps

1. Define test strategy with `codelevel-test-strategy`
2. Write test skeletons with method stubs
3. Implement feature code
4. This skill generates actual test implementations
5. Use `codelevel-architecture-reviewer` to validate patterns
