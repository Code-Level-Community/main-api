---
name: codelevel-scaffold-generator
description: Generate complete feature scaffolds following CodeLevel patterns. Use when starting a new endpoint, service layer, or domain feature. Creates entity, service, DTOs, controller, mappers, and database migrations following the exact patterns in CLAUDE.md. Generates production-ready boilerplate that follows naming conventions and layering structure. Always use AFTER test strategy is defined and BEFORE manual implementation.
---

# CodeLevel Scaffold Generator

Generate production-ready boilerplate for new features. This creates all layers (entity → service → controller) following CodeLevel patterns exactly.

## When to Use

- **Starting a new endpoint:** "Generate scaffold for lesson review endpoint"
- **Adding a service:** "Create full stack for enrollment management"
- **Before implementation:** Generate, then fill in logic
- **Multiple features in same module:** Generate each separately, avoid conflicts

## What Gets Generated

For a feature request like "Create lesson review endpoint in reviews_feedbacks module":

1. **Domain objects** - Value Objects with validation
2. **Entity** - JPA entity with query methods
3. **Service** - Business logic with constructor injection
4. **DTOs** - Request/response objects
5. **Mapper** - Entity ↔ DTO conversions
6. **Controller** - REST endpoints
7. **Exception Handlers** - Custom exception mappers
8. **Database Migration** - Flyway SQL
9. **Test Stubs** - Test class skeleton with empty methods

---

## How to Request a Scaffold

**Provide these details:**

```
Feature: [Name]
Module: [identity|course|gamification|student_progress|community|integration|course_requests|reviews_feedbacks|certificate]
Endpoint: POST /api/{module}/{resource}
Entity Name: [YourEntity]
Resource Name: [YourResource] (plural preferred: lessons, reviews, etc.)
Main Fields: [field1: type, field2: type, ...]
Value Objects: [Should email be EmailAddress? Should password be Password?]
Parent Entity: [If this is a child entity, parent module/entity]
Relationships: [Does it reference other modules?]
Cache Strategy: [None | Read-only | With invalidation]
```

### Example Request

```
Feature: Lesson Review Creation
Module: reviews_feedbacks
Endpoint: POST /api/reviews/lessons
Entity Name: LessonReviewEntity
Resource Name: LessonReviewResource
Main Fields:
  - lessonId: UUID (required)
  - authorId: UUID (required)
  - rating: int (1-5, required)
  - comment: String (1-500 chars, required)
  - createdAt: LocalDateTime (auto)

Value Objects: Yes
  - Rating (wraps int with 1-5 validation)
  - ReviewComment (wraps String with 1-500 validation)

Relationships: Depends on course module (lesson existence check)
Cache Strategy: Cache read results, invalidate on create/update
```

---

## Generated File Structure

Based on the example above:

```
src/main/java/com/codelevel/module/reviews_feedbacks/
├── domain/
│   ├── LessonReviewId.java (NEW)
│   ├── Rating.java (NEW)
│   ├── ReviewComment.java (NEW)
│   └── LessonReview.java (NEW)
├── http/rest/
│   ├── routes/
│   │   └── LessonReviewResource.java (NEW)
│   ├── dto/
│   │   ├── CreateLessonReviewRequest.java (NEW)
│   │   └── LessonReviewResponse.java (NEW)
│   ├── handler/
│   │   └── [ExceptionMappers] (uses shared ones)
│   └── mapper/
│       └── LessonReviewMapper.java (NEW)
├── persistence/
│   ├── resource/
│   │   ├── LessonReviewService.java (NEW)
│   │   └── dto/
│   │       └── [Service DTOs if needed]
│   └── entity/
│       └── LessonReviewEntity.java (NEW)

src/main/resources/db/migration/
└── V20250504120000__create_lesson_review_table.sql (NEW)

src/test/java/com/codelevel/module/reviews_feedbacks/
├── domain/
│   ├── LessonReviewIdTest.java (NEW - stub)
│   ├── RatingTest.java (NEW - stub)
│   ├── ReviewCommentTest.java (NEW - stub)
│   └── LessonReviewTest.java (NEW - stub)
├── persistence/resource/
│   ├── LessonReviewServiceTest.java (NEW - stub)
│   └── LessonReviewServiceIT.java (NEW - stub)
└── http/rest/routes/
    └── LessonReviewResourceIT.java (NEW - stub)
```

---

## Generated Code Examples

### Domain: Value Objects

**LessonReviewId.java**
```java
package com.codelevel.module.reviews_feedbacks.domain;

public record LessonReviewId(UUID value) {
  
  public LessonReviewId {
    if (value == null) {
      throw new IllegalArgumentException("Review ID cannot be null");
    }
  }
  
  public static LessonReviewId generate() {
    return new LessonReviewId(UUID.randomUUID());
  }
}
```

**Rating.java**
```java
package com.codelevel.module.reviews_feedbacks.domain;

public record Rating(int score) {
  
  public Rating {
    if (score < 1 || score > 5) {
      throw new IllegalArgumentException("Rating must be between 1 and 5");
    }
  }
}
```

**ReviewComment.java**
```java
package com.codelevel.module.reviews_feedbacks.domain;

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

**LessonReview.java**
```java
package com.codelevel.module.reviews_feedbacks.domain;

public final class LessonReview {
  
  private final LessonReviewEntity entity;

  public LessonReview(LessonReviewEntity entity) {
    if (entity == null) {
      throw new IllegalArgumentException("Entity cannot be null");
    }
    this.entity = entity;
  }

  public LessonReviewId id() {
    return new LessonReviewId(entity.id);
  }

  public UUID lessonId() {
    return entity.lessonId;
  }

  public UUID authorId() {
    return entity.authorId;
  }

  public Rating rating() {
    return new Rating(entity.rating);
  }

  public ReviewComment comment() {
    return new ReviewComment(entity.comment);
  }

  public LocalDateTime createdAt() {
    return entity.createdAt;
  }

  public boolean canBeEditedBy(UUID userId) {
    return authorId().equals(userId);
  }
}
```

### Persistence: Entity

**LessonReviewEntity.java**
```java
package com.codelevel.module.reviews_feedbacks.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "CL_LESSON_REVIEW")
public class LessonReviewEntity extends PanacheEntityBase {
  
  @Id
  public UUID id = UUID.randomUUID();

  @Column(name = "rf_lesson_id", nullable = false)
  public UUID lessonId;

  @Column(name = "rf_author_id", nullable = false)
  public UUID authorId;

  @Column(name = "rf_rating", nullable = false)
  public int rating;

  @Column(name = "rf_comment", nullable = false, length = 500)
  public String comment;

  @Column(name = "rf_created_at", nullable = false)
  public LocalDateTime createdAt = LocalDateTime.now();

  // Query methods (Active Record)
  public static Optional<LessonReviewEntity> findByIdOptional(UUID id) {
    return find("id", id).firstResultOptional();
  }

  public static List<LessonReviewEntity> findByLessonOrderByCreatedDesc(UUID lessonId) {
    return find("lessonId", lessonId)
      .order("createdAt DESC")
      .list();
  }

  public static long countByLesson(UUID lessonId) {
    return find("lessonId", lessonId).count();
  }

  public static void deleteByLesson(UUID lessonId) {
    delete("lessonId", lessonId);
  }
}
```

### Persistence: Service

**LessonReviewService.java**
```java
package com.codelevel.module.reviews_feedbacks.persistence.resource;

import com.codelevel.module.reviews_feedbacks.domain.*;
import com.codelevel.module.reviews_feedbacks.persistence.entity.LessonReviewEntity;
import com.codelevel.shared.contract.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class LessonReviewService {
  
  private static final Logger log = Logger.getLogger(LessonReviewService.class);

  public LessonReview createReview(
    UUID lessonId,
    UUID authorId,
    Rating rating,
    ReviewComment comment
  ) {
    // TODO: Validate lesson exists via course module REST call
    
    LessonReviewEntity entity = new LessonReviewEntity();
    entity.lessonId = lessonId;
    entity.authorId = authorId;
    entity.rating = rating.score();
    entity.comment = comment.text();
    entity.persistAndFlush();

    log.info("Review created: lessonId=%s, authorId=%s", lessonId, authorId);
    return new LessonReview(entity);
  }

  public Optional<LessonReview> getReview(UUID reviewId) {
    return LessonReviewEntity.findByIdOptional(reviewId)
      .map(LessonReview::new);
  }

  public List<LessonReview> getReviewsForLesson(UUID lessonId) {
    return LessonReviewEntity.findByLessonOrderByCreatedDesc(lessonId)
      .stream()
      .map(LessonReview::new)
      .toList();
  }

  public long countReviewsForLesson(UUID lessonId) {
    return LessonReviewEntity.countByLesson(lessonId);
  }

  public void deleteReview(UUID reviewId) {
    LessonReviewEntity.deleteById(reviewId);
    log.info("Review deleted: reviewId=%s", reviewId);
  }

  // TODO: Implement other methods (update, etc.)
}
```

### HTTP: DTOs

**CreateLessonReviewRequest.java**
```java
package com.codelevel.module.reviews_feedbacks.http.rest.dto;

import java.util.UUID;

public record CreateLessonReviewRequest(
  UUID lessonId,
  int rating,
  String comment
) {}
```

**LessonReviewResponse.java**
```java
package com.codelevel.module.reviews_feedbacks.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LessonReviewResponse(
  UUID id,
  UUID lessonId,
  UUID authorId,
  int rating,
  String comment,
  LocalDateTime createdAt
) {}
```

### HTTP: Mapper

**LessonReviewMapper.java**
```java
package com.codelevel.module.reviews_feedbacks.http.rest.mapper;

import com.codelevel.module.reviews_feedbacks.domain.*;
import com.codelevel.module.reviews_feedbacks.http.rest.dto.*;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LessonReviewMapper {
  
  public LessonReviewResponse toDto(LessonReview review) {
    return new LessonReviewResponse(
      review.id().value(),
      review.lessonId(),
      review.authorId(),
      review.rating().score(),
      review.comment().text(),
      review.createdAt()
    );
  }

  public Rating ratingFromRequest(int score) {
    return new Rating(score);
  }

  public ReviewComment commentFromRequest(String text) {
    return new ReviewComment(text);
  }
}
```

### HTTP: Controller

**LessonReviewResource.java**
```java
package com.codelevel.module.reviews_feedbacks.http.rest.routes;

import com.codelevel.module.reviews_feedbacks.domain.LessonReview;
import com.codelevel.module.reviews_feedbacks.http.rest.dto.*;
import com.codelevel.module.reviews_feedbacks.http.rest.mapper.LessonReviewMapper;
import com.codelevel.module.reviews_feedbacks.persistence.resource.LessonReviewService;
import com.codelevel.shared.contract.exception.ResourceNotFound;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.List;
import java.util.UUID;

@Path("/api/reviews/lessons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class LessonReviewResource {
  
  private final LessonReviewService service;
  private final LessonReviewMapper mapper;
  private final SecurityContext securityContext;

  public LessonReviewResource(
    LessonReviewService service,
    LessonReviewMapper mapper,
    SecurityContext securityContext
  ) {
    this.service = service;
    this.mapper = mapper;
    this.securityContext = securityContext;
  }

  @POST
  @RolesAllowed("USER")
  public Response createReview(CreateLessonReviewRequest req) {
    UUID currentUserId = UUID.fromString(securityContext.getUserPrincipal().getName());

    LessonReview review = service.createReview(
      req.lessonId(),
      currentUserId,
      mapper.ratingFromRequest(req.rating()),
      mapper.commentFromRequest(req.comment())
    );

    return Response
      .status(Response.Status.CREATED)
      .entity(mapper.toDto(review))
      .build();
  }

  @GET
  @Path("/{reviewId}")
  public LessonReviewResponse getReview(@PathParam("reviewId") UUID reviewId) {
    return service.getReview(reviewId)
      .map(mapper::toDto)
      .orElseThrow(() -> new ResourceNotFound("Review not found"));
  }

  @GET
  @Path("/by-lesson/{lessonId}")
  public List<LessonReviewResponse> getReviewsForLesson(@PathParam("lessonId") UUID lessonId) {
    return service.getReviewsForLesson(lessonId)
      .stream()
      .map(mapper::toDto)
      .toList();
  }

  // TODO: Implement other endpoints (update, delete, etc.)
}
```

### Database Migration

**V20250504120000__create_lesson_review_table.sql**
```sql
CREATE TABLE CL_LESSON_REVIEW (
  id UUID PRIMARY KEY,
  rf_lesson_id UUID NOT NULL,
  rf_author_id UUID NOT NULL,
  rf_rating INTEGER NOT NULL CHECK (rf_rating BETWEEN 1 AND 5),
  rf_comment VARCHAR(500) NOT NULL,
  rf_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cl_lesson_review_lesson_id ON CL_LESSON_REVIEW(rf_lesson_id);
CREATE INDEX idx_cl_lesson_review_author_id ON CL_LESSON_REVIEW(rf_author_id);
CREATE SEQUENCE cl_lesson_review_seq START WITH 1 INCREMENT BY 50;
```

### Test Stub

**LessonReviewServiceTest.java**
```java
package com.codelevel.module.reviews_feedbacks.persistence.resource;

import com.codelevel.module.reviews_feedbacks.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

@DisplayName("LessonReviewService")
class LessonReviewServiceTest {

  private LessonReviewService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    // TODO: Initialize service with mocks
  }

  @Test
  @DisplayName("should create review with valid data")
  void shouldCreateReviewWithValidData() {
    fail("Not implemented");
  }

  @Test
  @DisplayName("should throw if lesson doesn't exist")
  void shouldThrowIfLessonDoesNotExist() {
    fail("Not implemented");
  }

  @Test
  @DisplayName("should retrieve reviews for lesson")
  void shouldRetrieveReviewsForLesson() {
    fail("Not implemented");
  }

  // TODO: Add more test stubs based on test strategy
}
```

---

## Configuration Template

Include this in your scaffold request if you need special setup:

```yaml
feature_config:
  pagination: true
  pagination_size: 20
  
  caching:
    enabled: true
    read_cache: user-cache
    write_invalidates: true
  
  external_dependencies:
    - module: course
      method: getLessonExists
  
  special_fields:
    createdAt: auto-timestamp-utc
    updatedAt: auto-timestamp-utc
    
  constraints:
    rating: between(1, 5)
    comment: length(1, 500)
```

---

## After Generation

1. **Review generated code** - Make sure structure matches CLAUDE.md
2. **Fill in TODOs** - Logic placeholders marked with `// TODO:`
3. **Write tests** - Use generated test stubs with `codelevel-test-implementer`
4. **Implement logic** - Follow TDD workflow
5. **Run validator** - Use `codelevel-architecture-reviewer` before commit

---

## Naming Conventions Applied

The generator automatically applies:
- **Entities:** `{Feature}Entity` (e.g., `LessonReviewEntity`)
- **Controllers:** `{Feature}Resource` (e.g., `LessonReviewResource`)
- **Services:** `{Feature}Service` (e.g., `LessonReviewService`)
- **Mappers:** `{Feature}Mapper` (e.g., `LessonReviewMapper`)
- **Value Objects:** Domain-specific (e.g., `Rating`, `ReviewComment`)
- **DTOs:** `{Action}{Feature}Request/Response` (e.g., `CreateLessonReviewRequest`)
- **Tables:** `CL_{MODULE_PREFIX}_{FEATURE}` (e.g., `CL_LESSON_REVIEW`)
- **Columns:** Abbreviated (e.g., `rf_lesson_id`, `rf_rating`)
- **Test Classes:** `{Subject}Test.java` (unit) or `{Subject}IT.java` (integration)

---

## Limitations & When NOT to Use

❌ **Don't use for:**
- Complex business logic (scaffold is basic; you add logic)
- Cross-module integration (generate each module separately first)
- Existing features (creates new files, won't merge with existing)

✅ **Use for:**
- New features in new module
- Simple CRUD endpoints
- Boilerplate heavy lifting
- Establishing patterns for other developers

---

## Quick Commands

```bash
# After generation, check structure
find src -name "*LessonReview*" | head -20

# Run tests (should all fail with "Not implemented")
./mvnw test -Dtest=LessonReviewServiceTest

# Check database migration syntax
cat src/main/resources/db/migration/V*.sql | head -20

# Format generated code
./mvnw spotless:apply
```
