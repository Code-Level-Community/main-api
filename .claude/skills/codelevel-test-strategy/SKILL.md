---
name: codelevel-test-strategy
description: Define comprehensive test strategies for CodeLevel features BEFORE implementation. Use this skill whenever a user starts working on a new feature, endpoint, or service. Maps happy paths, edge cases, error scenarios, and production risks. Generates test templates and identifies scenarios that commonly break in production. Always use this at the START of feature development in Test-Driven Development (TDD) workflow.
---

# CodeLevel Test Strategy

Define test scenarios and strategy **before writing implementation code**. This is the foundation of Test-Driven Development (TDD) in CodeLevel.

## When to Use

- **Starting a new feature:** "I'm creating a lesson review endpoint"
- **Adding business logic:** "I need to implement enrollment validation"
- **Modifying existing features:** "Changing password reset flow"
- **Before any coding:** Always ask "what tests should I write first?"

## Test Strategy Framework

### 1. Happy Path (Normal Flow)

What happens when everything works correctly?

Example for "Create Lesson Review" endpoint:
- Valid user authenticated
- Valid lesson exists
- Valid rating (1-5) provided
- Valid comment (1-500 chars)
- **Expected:** Review created, returns 201 with review data

**Test Template:**
```java
@Test
void shouldCreateReviewWithValidData() {
  // Arrange
  UUID lessonId = UUID.randomUUID();
  UUID userId = UUID.randomUUID();
  CreateLessonReviewRequest req = new CreateLessonReviewRequest(
    lessonId, 5, "Excellent lesson!"
  );
  
  // Act
  Response response = client.target("/api/reviews/lessons")
    .request()
    .post(Entity.json(req));
  
  // Assert
  assertThat(response.getStatus()).isEqualTo(201);
  LessonReviewResponse body = response.readEntity(LessonReviewResponse.class);
  assertThat(body.rating()).isEqualTo(5);
}
```

### 2. Edge Cases (Boundary Conditions)

What happens at the limits?

Example for "Create Lesson Review":
- **Rating = 1** (minimum valid)
- **Rating = 5** (maximum valid)
- **Comment = 1 char** (minimum non-empty)
- **Comment = 500 chars** (maximum)
- **Same user reviews same lesson twice** (duplicate)
- **Lesson with 1000 reviews already** (load test boundary)

**Test Template:**
```java
@Test
void shouldAcceptMinimumAndMaximumRatings() {
  assertThat(new Rating(1)).isNotNull();
  assertThat(new Rating(5)).isNotNull();
}

@Test
void shouldAcceptCommentAtBoundaries() {
  assertThatCode(() -> new ReviewComment("A"))
    .doesNotThrowAnyException();
  
  String max500 = "x".repeat(500);
  assertThatCode(() -> new ReviewComment(max500))
    .doesNotThrowAnyException();
}
```

### 3. Error Cases (What Should Fail)

What are the explicit rejection scenarios?

Example for "Create Lesson Review":
- **Rating = 0** → InvalidArgumentException (must be 1-5)
- **Rating = 6** → InvalidArgumentException
- **Comment = empty/blank** → InvalidArgumentException
- **Comment = 501 chars** → InvalidArgumentException (exceeds max)
- **Lesson doesn't exist** → ResourceNotFound (404)
- **User not authenticated** → Unauthorized (401)
- **User not enrolled in course** → BusinessRuleException (403 - forbidden by business rule)

**Test Template:**
```java
@Test
void shouldRejectInvalidRatings() {
  assertThatThrownBy(() -> new Rating(0))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessage("Rating must be between 1 and 5");
  
  assertThatThrownBy(() -> new Rating(6))
    .isInstanceOf(IllegalArgumentException.class);
}

@Test
void shouldRejectBlankComment() {
  assertThatThrownBy(() -> new ReviewComment(""))
    .isInstanceOf(IllegalArgumentException.class);
  
  assertThatThrownBy(() -> new ReviewComment("   "))
    .isInstanceOf(IllegalArgumentException.class);
}

@Test
void shouldReturn404WhenLessonNotFound() {
  UUID fakeLessonId = UUID.randomUUID();
  CreateLessonReviewRequest req = new CreateLessonReviewRequest(
    fakeLessonId, 5, "Review"
  );
  
  Response response = client.target("/api/reviews/lessons")
    .request()
    .post(Entity.json(req));
  
  assertThat(response.getStatus()).isEqualTo(404);
}
```

### 4. Production Risks (Scenarios That Break in Production)

These are scenarios that **work in dev with H2** but fail in production:

#### Data Consistency & Concurrency
- **Two users submit review for same lesson simultaneously** → Duplicate reviews? Race condition?
- **User submits review, then deletes account** → Orphaned review? FK constraint fails?
- **Lesson gets deleted after review created** → Cascade delete? Data corruption?

**Test Template:**
```java
@Test
void shouldHandleConcurrentReviewCreation() throws InterruptedException {
  UUID lessonId = UUID.randomUUID();
  
  ExecutorService executor = Executors.newFixedThreadPool(2);
  
  Callable<LessonReview> task1 = () -> reviewService.createReview(
    lessonId, userId1, new Rating(5), new ReviewComment("Great!")
  );
  
  Callable<LessonReview> task2 = () -> reviewService.createReview(
    lessonId, userId2, new Rating(4), new ReviewComment("Good!")
  );
  
  Future<LessonReview> f1 = executor.submit(task1);
  Future<LessonReview> f2 = executor.submit(task2);
  
  LessonReview r1 = f1.get();
  LessonReview r2 = f2.get();
  
  // Both should succeed, different IDs
  assertThat(r1.id()).isNotEqualTo(r2.id());
  
  executor.shutdown();
}

@Test
void shouldMaintainForeignKeyIntegrity() {
  LessonReview review = reviewService.createReview(
    lessonId, userId, new Rating(5), new ReviewComment("Good")
  );
  
  // Delete lesson - what happens?
  courseService.deleteLesson(lessonId);
  
  // Review should be deleted (cascade) or FK constraint should prevent deletion
  List<LessonReview> remaining = reviewService.getReviewsForLesson(lessonId);
  assertThat(remaining).isEmpty();
}
```

#### Performance & Resource Exhaustion
- **User retrieves reviews for lesson with 10,000 reviews** → Memory issue? Timeout? Pagination needed?
- **Batch operation: create 1000 reviews in loop** → Stack overflow? Connection pool exhausted? N+1 queries?

**Test Template:**
```java
@Test
void shouldPaginateWhenManyReviews() {
  UUID lessonId = UUID.randomUUID();
  
  // Create 1000 reviews
  IntStream.range(0, 1000).forEach(i -> {
    reviewService.createReview(
      lessonId,
      UUID.randomUUID(),
      new Rating((i % 5) + 1),
      new ReviewComment("Review " + i)
    );
  });
  
  // Should not load all at once
  List<LessonReviewResponse> page1 = client.getReviewsForLesson(
    lessonId, 
    page(1, 20) // Offset/limit
  );
  
  assertThat(page1).hasSize(20);
}

@Test
void shouldNotExhaustConnectionPool() {
  ExecutorService executor = Executors.newFixedThreadPool(50);
  
  List<Future<?>> futures = new ArrayList<>();
  for (int i = 0; i < 500; i++) {
    final int index = i;
    futures.add(executor.submit(() -> {
      reviewService.createReview(
        UUID.randomUUID(),
        UUID.randomUUID(),
        new Rating((index % 5) + 1),
        new ReviewComment("Concurrent review")
      );
    }));
  }
  
  // All should complete without timeout/exception
  futures.forEach(f -> {
    assertThatCode(() -> f.get(5, TimeUnit.SECONDS))
      .doesNotThrowAnyException();
  });
  
  executor.shutdown();
}
```

#### Cache Invalidation
- **Review created → cached review list still shows old data** → Cache invalidation issue?
- **Review updated → other users still see old cached version** → Stale data in production?

**Test Template:**
```java
@Test
void shouldInvalidateCacheWhenReviewCreated() {
  UUID lessonId = UUID.randomUUID();
  
  // First call - populates cache
  List<LessonReview> reviews1 = reviewService.getReviewsForLesson(lessonId);
  assertThat(reviews1).isEmpty();
  
  // Create a review
  reviewService.createReview(
    lessonId, userId, new Rating(5), new ReviewComment("New!")
  );
  
  // Cache should be invalidated - second call should see new review
  List<LessonReview> reviews2 = reviewService.getReviewsForLesson(lessonId);
  assertThat(reviews2).hasSize(1);
}
```

#### Transaction & Rollback Scenarios
- **Review created, but notification service fails** → Partial success? Transaction rolled back?
- **Bulk operation partially fails** → What state does data end up in?

**Test Template:**
```java
@Test
void shouldRollbackIfNotificationFails() {
  // Mock notification service to throw
  when(notificationService.sendReviewNotification(any()))
    .thenThrow(new ServiceUnavailableException());
  
  assertThatThrownBy(() -> reviewService.createReviewWithNotification(
    lessonId, userId, rating, comment
  )).isInstanceOf(ServiceUnavailableException.class);
  
  // Review should NOT be persisted
  assertThat(reviewService.getReview(anyId())).isEmpty();
}
```

#### Timezone & Timestamp Issues
- **Review created in different timezone** → Timestamps correct?
- **Daylight saving time transition** → Timestamps off by an hour?

**Test Template:**
```java
@Test
void shouldStoreSqlTimestampsInUtc() {
  ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
  
  LessonReview review = reviewService.createReview(
    lessonId, userId, rating, comment
  );
  
  // Stored timestamp should be in UTC, not local timezone
  LessonReviewEntity entity = LessonReviewEntity.findById(review.id().value());
  assertThat(entity.createdAt.atZone(ZoneOffset.UTC))
    .isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(2, ChronoUnit.SECONDS));
}
```

#### Database Migration Issues
- **Column renamed but old queries still reference it** → Application crashes after deploy?
- **NOT NULL column added without default** → Deployment fails on large tables?

**Test Template:** (More QA than unit test)
```java
@Test
void shouldSupportMigrationRollback() {
  // After migration V20250504__add_helpful_field.sql:
  // Column should exist and be queryable
  LessonReviewEntity review = reviewService.createReview(...);
  assertThat(review.helpful).isNotNull(); // New field
  
  // Rollback: column should gracefully handle absence
  // (This is more of a manual QA test, but document it)
}
```

---

## Test Strategy Template (Copy & Adapt)

```
## Feature: [Name]

### Module & Layer
- **Module:** [identity|course|gamification|etc.]
- **Layers Affected:** [entity|service|controller]
- **External Dependencies:** [auth|database|email|etc.]

### Happy Path
- [ ] Normal scenario 1
- [ ] Normal scenario 2

### Edge Cases (Boundaries)
- [ ] Min/max values
- [ ] Empty collections
- [ ] Duplicate data

### Error Scenarios
- [ ] Invalid input (each validation rule)
- [ ] Resource not found
- [ ] Unauthorized/forbidden
- [ ] Business rule violations

### Production Risks
- [ ] Concurrency (race conditions)
- [ ] Foreign key integrity
- [ ] Cache invalidation
- [ ] Performance/pagination
- [ ] Transaction rollback
- [ ] Timezone handling
- [ ] Migration rollback

### Test Count Summary
- **Unit Tests (Domain/Service):** [N] tests
- **Integration Tests (Service + Entity):** [N] tests
- **API Tests (Controller + Service):** [N] tests
- **Total Coverage Target:** [X%]
```

---

## Workflow: Test-First (TDD Pragmatic)

1. **Define Strategy** (this skill) → Creates test outline
2. **Write Test Skeletons** → All tests present but empty (`fail("not implemented")`)
3. **Run Tests** → All fail (RED)
4. **Implement Feature** → Code with freedom
5. **Tests Pass** → All green (GREEN)
6. **Refactor** → Clean up, tests still passing (REFACTOR)

This is **faster than pure TDD** (Red → Green → Refactor one test at a time) because you don't wait for test failures to start coding. You just ensure **all contracts are defined upfront**.

---

## Key Principles

✅ **Test behavior, not implementation** - If you refactor code tomorrow, tests still pass  
✅ **One assertion per scenario** - Test name explains what fails  
✅ **Use realistic data** - Not `"abc"`, use `"user@example.com"`  
✅ **Mock external dependencies** - Test the service, not the database  
✅ **Reject partial success** - If test can pass multiple ways, split it  
✅ **Name tests by outcome** - `shouldRejectIfCommentExceeds500Chars`, not `testComment`

---

## Next Steps

Once strategy is defined:
1. Use `codelevel-test-implementer` to write actual tests
2. Use `codelevel-scaffold-generator` to create entity/service stubs
3. Implement code following TDD workflow
4. Use `codelevel-architecture-reviewer` to validate patterns
