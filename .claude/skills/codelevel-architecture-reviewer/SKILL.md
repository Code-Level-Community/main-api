---
name: codelevel-architecture-reviewer
description: Review code against CodeLevel architecture patterns and forbidden patterns. Use this skill whenever reviewing pull requests, after writing implementation code, or when you need to validate code follows the CLAUDE.md standards. Detects violations of layering rules, injection patterns, cross-module dependencies, entity exposure, caching issues, null safety, and transaction handling. Provides severity-rated findings with actionable fixes.
---

# CodeLevel Architecture Reviewer

Validate code against CodeLevel patterns. Run this on **any code change** to catch architectural violations before they reach production.

## When to Use

- **After implementing a feature:** "Review this new endpoint code"
- **PR code review:** "Check if this follows our patterns"
- **Before merging:** Automated validation of architecture
- **When uncertain:** "Does this violate any forbidden patterns?"
- **Refactoring:** "Is this refactored code still compliant?"

## Quick Validation Checklist

Use this to spot obvious issues:

| Pattern | ✅ Correct | ❌ Forbidden |
|---|---|---|
| **Injection** | Constructor injection | `@Inject` on field |
| **Null Safety** | `Optional<T>` in queries | `return null` |
| **HTTP Layer** | DTOs only, map in controller | Return entities directly |
| **Cross-Module** | REST calls via clients | Direct service injection |
| **Exceptions** | Custom exceptions, mapped to HTTP | `WebApplicationException` in service |
| **Cache Invalidation** | `@CacheInvalidate` on mutations | Cache exists but never invalidates |
| **Transactions** | `@Transactional` on service | `@Transactional` on controller |
| **Comments** | None (code is self-documenting) | Excessive comments |

---

## Detailed Pattern Checks

### 1. Dependency Injection

**Pattern:** Constructor injection everywhere. NO field injection.

**Check this code:**
```java
@ApplicationScoped
public class LessonReviewService {
  
  @Inject
  private CourseClient courseClient; // ❌ PROBLEM
  
  public void createReview(...) { }
}
```

**Review Result:**
```
SEVERITY: ERROR
PATTERN: Field Injection Detected
ISSUE: LessonReviewService uses @Inject on field 'courseClient'
WHY: Field injection is null-prone, untestable, hard to reason about
FIX: Use constructor injection
EXAMPLE:
  private final CourseClient courseClient;
  
  public LessonReviewService(CourseClient courseClient) {
    this.courseClient = courseClient;
  }
```

**Check this code:**
```java
@ApplicationScoped
@Transactional
public class LessonReviewService {
  
  private final CourseClient courseClient;
  private final LessonReviewRepository repository;
  
  public LessonReviewService(
    CourseClient courseClient,
    LessonReviewRepository repository
  ) {
    this.courseClient = courseClient;
    this.repository = repository;
  }
  
  public LessonReview createReview(...) { }
}
```

**Review Result:**
```
✅ PASS: Injection pattern correct
- Constructor injection: CourseClient, LessonReviewRepository
- All dependencies final and non-null
```

---

### 2. Null Safety

**Pattern:** `Optional<T>` for query methods. No raw nulls.

**Check this code:**
```java
@Entity
public class LessonReviewEntity extends PanacheEntityBase {
  
  public static LessonReviewEntity findById(UUID id) {
    return find("id", id).firstResult(); // ❌ PROBLEM
  }
}
```

**Review Result:**
```
SEVERITY: ERROR
PATTERN: Unsafe Null Return
ISSUE: findById() returns raw null if not found
WHY: Callers must null-check; easy to miss → NPE in production
FIX: Use Optional
EXAMPLE:
  public static Optional<LessonReviewEntity> findById(UUID id) {
    return find("id", id).firstResultOptional();
  }
```

**Check this code:**
```java
public Optional<LessonReview> getReview(UUID reviewId) {
  return LessonReviewEntity.findByIdOptional(reviewId)
    .map(LessonReview::new);
}

public List<LessonReview> getReviewsForLesson(UUID lessonId) {
  return LessonReviewEntity.findByLesson(lessonId)
    .stream()
    .map(LessonReview::new)
    .toList();
}
```

**Review Result:**
```
✅ PASS: Null safety correct
- Query methods return Optional (getReview)
- List-returning methods return empty list, never null (getReviewsForLesson)
```

---

### 3. HTTP Layer Separation

**Pattern:** Controllers return DTOs. Services throw domain exceptions.

**Check this code:**
```java
@Path("/api/reviews/lessons")
public class LessonReviewResource {
  
  private final LessonReviewService service;
  
  @POST
  public LessonReviewEntity createReview(CreateReviewRequest req) { // ❌ PROBLEM
    return service.createReview(...);
  }
}
```

**Review Result:**
```
SEVERITY: ERROR
PATTERN: Entity Leaked to HTTP Layer
ISSUE: LessonReviewResource returns LessonReviewEntity directly
WHY: Exposes database schema, breaks encapsulation, schema changes break API
FIX: Return DTO, map in controller
EXAMPLE:
  @POST
  public Response createReview(CreateReviewRequest req) {
    LessonReview review = service.createReview(...);
    return Response
      .status(201)
      .entity(mapper.toDto(review))
      .build();
  }
```

**Check this code:**
```java
@Path("/api/reviews/lessons")
@Produces(MediaType.APPLICATION_JSON)
public class LessonReviewResource {
  
  private final LessonReviewService service;
  private final LessonReviewMapper mapper;
  
  @POST
  public Response createReview(CreateLessonReviewRequest req) {
    LessonReview review = service.createReview(...);
    return Response
      .status(201)
      .entity(mapper.toDto(review))
      .build();
  }
  
  @GET
  @Path("/{id}")
  public LessonReviewResponse getReview(@PathParam("id") UUID reviewId) {
    return service.getReview(reviewId)
      .map(mapper::toDto)
      .orElseThrow(() -> new ResourceNotFound("Review not found"));
  }
}
```

**Review Result:**
```
✅ PASS: HTTP layer separation correct
- Returns DTOs (LessonReviewResponse), not entities
- Maps exceptions to HTTP status codes
- Uses proper Response builder
```

---

### 4. Cross-Module Dependencies

**Pattern:** Modules communicate via REST only. No direct service injection.

**Check this code:**
```java
// In student_progress module
@ApplicationScoped
public class EnrollmentService {
  
  @Inject
  private com.codelevel.module.course.CourseService courseService; // ❌ PROBLEM
  
  public void enrollStudent(UUID courseId, UUID studentId) {
    courseService.getCourse(courseId); // Direct call to another module's service
  }
}
```

**Review Result:**
```
SEVERITY: ERROR
PATTERN: Cross-Module Direct Dependency
ISSUE: EnrollmentService injects CourseService from course module
WHY: Breaks module isolation, impossible to test independently, circular dependencies
FIX: Call course module's REST API instead
EXAMPLE:
  private final CourseClient courseClient;
  
  public EnrollmentService(CourseClient courseClient) {
    this.courseClient = courseClient;
  }
  
  public void enrollStudent(UUID courseId, UUID studentId) {
    courseClient.getCourse(courseId); // HTTP call
  }
```

**Check this code:**
```java
// In student_progress module
@ApplicationScoped
@Transactional
public class EnrollmentService {
  
  private final CourseClient courseClient;
  private final EnrollmentRepository repository;
  
  public EnrollmentService(
    CourseClient courseClient,
    EnrollmentRepository repository
  ) {
    this.courseClient = courseClient;
    this.repository = repository;
  }
  
  public void enrollStudent(UUID courseId, UUID studentId) {
    CoursePublicDto course = courseClient.getCourse(courseId);
    // Use course DTO, not entity
  }
}
```

**Review Result:**
```
✅ PASS: Cross-module communication correct
- Uses CourseClient (REST client), not CourseService
- Calls module's public REST API
- Maps response DTO to internal domain objects
```

---

### 5. Exception Handling

**Pattern:** Services throw domain exceptions. Controllers map to HTTP.

**Check this code:**
```java
@Path("/api/reviews")
public class ReviewResource {
  
  @POST
  public Response create(CreateReviewRequest req) {
    try {
      LessonReview review = service.createReview(...);
      return Response.status(201).entity(review).build();
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400); // ❌ PROBLEM
    }
  }
}
```

**Review Result:**
```
SEVERITY: ERROR
PATTERN: HTTP Exception in Controller
ISSUE: Catching and re-throwing as WebApplicationException
WHY: Services become aware of HTTP layer, untestable, violates separation
FIX: Let service throw domain exception, map in exception handler
```

**Check this code:**
```java
// Service throws domain exception
@ApplicationScoped
@Transactional
public class LessonReviewService {
  
  public LessonReview createReview(...) {
    if (rating < 1 || rating > 5) {
      throw new BusinessRuleException("Rating must be between 1 and 5");
    }
    // ...
  }
}

// Controller lets exception propagate
@Path("/api/reviews")
public class ReviewResource {
  
  private final LessonReviewService service;
  
  @POST
  public Response create(CreateReviewRequest req) {
    LessonReview review = service.createReview(...);
    return Response.status(201).entity(mapper.toDto(review)).build();
  }
}

// Exception mapper handles HTTP translation
@Provider
public class BusinessRuleExceptionMapper implements ExceptionMapper<BusinessRuleException> {
  
  @Override
  public Response toResponse(BusinessRuleException e) {
    return Response
      .status(400)
      .entity(new ErrorResponse(e.getMessage()))
      .build();
  }
}
```

**Review Result:**
```
✅ PASS: Exception handling correct
- Service throws BusinessRuleException (domain)
- Controller doesn't catch/transform (clean)
- ExceptionMapper translates to HTTP 400 (separation of concerns)
```

---

### 6. Caching

**Pattern:** Cache only reads. Invalidate on mutations. Check for stale data.

**Check this code:**
```java
@ApplicationScoped
public class UserService {
  
  @CacheResult(cacheName = "user-cache")
  public Optional<User> getUserById(UUID id) {
    return repository.findById(id);
  }
  
  @Transactional
  public User updateUser(UUID id, UpdateUserRequest req) {
    User user = getUserById(id).orElseThrow(...);
    user.setName(req.name());
    user.setEmail(req.email());
    repository.save(user);
    return user; // ❌ PROBLEM: Cache not invalidated
  }
}
```

**Review Result:**
```
SEVERITY: ERROR
PATTERN: Missing Cache Invalidation
ISSUE: updateUser() modifies data but doesn't invalidate cache
WHY: Other services see stale user data for up to 30 minutes
WHERE: UserService.updateUser()
FIX: Add @CacheInvalidate
EXAMPLE:
  @Transactional
  @CacheInvalidate(cacheName = "user-cache")
  public User updateUser(UUID id, UpdateUserRequest req) {
    // ...
  }
```

**Check this code:**
```java
@ApplicationScoped
@Transactional
public class UserService {
  
  @CacheResult(cacheName = "user-cache")
  public Optional<User> getUserById(UUID id) {
    return repository.findById(id);
  }
  
  @CacheInvalidate(cacheName = "user-cache")
  public User updateUser(UUID id, UpdateUserRequest req) {
    User user = getUserById(id).orElseThrow(...);
    user.setName(req.name());
    user.setEmail(req.email());
    repository.save(user);
    return user;
  }
  
  @CacheInvalidateAll(cacheName = "user-cache")
  public void deleteUser(UUID id) {
    repository.deleteById(id);
  }
}
```

**Review Result:**
```
✅ PASS: Caching pattern correct
- @CacheResult on read method (getUserById)
- @CacheInvalidate on single-record mutations (updateUser)
- @CacheInvalidateAll on bulk/delete operations (deleteUser)
```

---

### 7. Transactions

**Pattern:** `@Transactional` on services, not controllers.

**Check this code:**
```java
@Path("/api/reviews")
@Transactional // ❌ PROBLEM
public class ReviewResource {
  
  @POST
  public Response create(CreateReviewRequest req) {
    return Response.status(201).entity(service.create(req)).build();
  }
}
```

**Review Result:**
```
SEVERITY: ERROR
PATTERN: @Transactional on Controller
ISSUE: ReviewResource has @Transactional at class level
WHY: Controllers are HTTP layer; transaction should be at business logic layer
FIX: Move @Transactional to Service layer
EXAMPLE:
  @Path("/api/reviews")
  public class ReviewResource {
    
    @POST
    public Response create(CreateReviewRequest req) {
      return Response.status(201).entity(service.create(req)).build();
    }
  }
  
  @ApplicationScoped
  @Transactional // Correct
  public class ReviewService {
    public Review create(CreateReviewRequest req) { ... }
  }
```

**Check this code:**
```java
@Path("/api/reviews")
public class ReviewResource {
  
  private final ReviewService service;
  
  @POST
  public Response create(CreateReviewRequest req) {
    LessonReview review = service.createReview(...);
    return Response.status(201).entity(mapper.toDto(review)).build();
  }
}

@ApplicationScoped
@Transactional
public class ReviewService {
  
  public LessonReview createReview(CreateReviewRequest req) {
    // Transaction starts here
    // Database operations happen
    // Transaction commits automatically
  }
}
```

**Review Result:**
```
✅ PASS: Transaction boundaries correct
- @Transactional on service (ReviewService), not controller
- HTTP layer clean, database layer isolated
```

---

### 8. Code Comments

**Pattern:** Minimal comments. Code should be self-documenting.

**Check this code:**
```java
public class ReviewComment {
  
  // Constructor
  public ReviewComment(String text) { }
  
  // Get text
  public String text() { }
  
  // Validate text is not empty and under 500 chars
  private void validate(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Comment cannot be blank");
    }
    if (text.length() > 500) {
      throw new IllegalArgumentException("Comment cannot exceed 500 chars");
    }
  }
}
```

**Review Result:**
```
SEVERITY: WARNING
PATTERN: Unnecessary Comments
ISSUE: Comments like "// Constructor", "// Get text" add noise
WHY: Code names are self-explanatory; comments go stale quickly
FIX: Remove redundant comments, keep business logic comments
EXAMPLE:
  public class ReviewComment {
    
    public ReviewComment(String text) {
      validate(text);
      this.text = text;
    }
    
    public String text() { return text; }
    
    private void validate(String text) {
      if (text == null || text.isBlank()) {
        throw new IllegalArgumentException("Comment cannot be blank");
      }
      if (text.length() > 500) {
        throw new IllegalArgumentException("Comment cannot exceed 500 chars");
      }
    }
  }
```

**Check this code:**
```java
public record ReviewComment(String text) {
  public ReviewComment {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Comment cannot be blank");
    }
    if (text.length() > 500) {
      throw new IllegalArgumentException("Comment cannot exceed 500 chars");
    }
  }
}
```

**Review Result:**
```
✅ PASS: Code is self-documenting
- Record syntax is clear (text is the content)
- Method names explain intent (text())
- Constructor validation is explicit
- No unnecessary comments
```

---

## Review Report Format

When reviewing code, provide structured feedback:

```
REVIEW: LessonReviewService.java

VIOLATIONS FOUND: 3

[ERROR] Injection Pattern
  Location: LessonReviewService.java:15
  Issue: Field injection used for courseClient
  Severity: ERROR (breaks testability)
  Fix: Convert to constructor injection
  
[ERROR] Missing Cache Invalidation
  Location: LessonReviewService.java:35 (updateReview method)
  Issue: Cache decorated read exists but no invalidation on write
  Severity: ERROR (stale data in production)
  Fix: Add @CacheInvalidate on updateReview()

[WARNING] Unnecessary Comment
  Location: ReviewComment.java:5
  Issue: Comment "// Validate input" explains what code already says
  Severity: WARNING (code quality)
  Fix: Remove comment

SUMMARY
-------
Errors: 2 (must fix before merge)
Warnings: 1 (fix before merge if time allows)
Overall: ❌ FAIL

RECOMMENDED ACTION: Request changes. Resubmit after fixing errors.
```

---

## Checklist Before Merging

Use this before you merge any feature:

- [ ] All services use constructor injection (no `@Inject` fields)
- [ ] All query methods return `Optional<T>` or empty collections
- [ ] Controllers return DTOs, not entities
- [ ] No direct service calls across modules (REST clients only)
- [ ] Exceptions are custom domain types, mapped in handlers
- [ ] Mutations invalidate caches (`@CacheInvalidate` present)
- [ ] Transactions are on services, not controllers
- [ ] Database migrations exist in `src/main/resources/db/migration/`
- [ ] Tests exist (unit + integration)
- [ ] Code has no unnecessary comments
- [ ] No `WebApplicationException` in services
- [ ] No `@Inject` on fields
- [ ] No raw nulls returned from queries

---

## Integration with Code Review Workflow

1. **After implementing:** Run this reviewer
2. **Before PR:** Self-review with this checklist
3. **In code review:** Use detailed pattern checks above
4. **Before merge:** Verify checklist passes

This catches violations early, saves review cycles.
