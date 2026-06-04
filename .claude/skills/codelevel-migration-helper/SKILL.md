---
name: codelevel-migration-helper
description: Create and validate Flyway database migrations for CodeLevel. Use when adding fields to entities, creating new tables, or making schema changes. Generates migration SQL with correct naming conventions, timestamps, and constraints. Validates migrations are safe (no data loss, backward compatible), tests on both H2 and PostgreSQL patterns. Always use AFTER entities are designed but BEFORE deployment.
---

# CodeLevel Migration Helper

Create **safe, testable, production-ready** Flyway migrations. This ensures schema changes are reversible, don't break existing data, and work on both H2 (dev) and PostgreSQL (prod).

## When to Use

- **Adding a new entity:** "Create migration for LessonReviewEntity"
- **Modifying existing schema:** "Add authorId column to reviews table"
- **Creating indexes:** "Add performance index for lesson_id lookups"
- **Adding constraints:** "Add CHECK constraint for rating 1-5"
- **Data cleanup:** "Remove duplicate reviews"
- **Validating migrations:** "Is this migration safe for production?"

---

## Migration Naming Convention

**Format:** `V{TIMESTAMP}__{description}.sql`

**TIMESTAMP:** YYYYMMDDHHmmss (UTC)

**Example:**
```
V20250504120000__create_lesson_review_table.sql
V20250504121500__add_author_id_to_reviews.sql
V20250504130000__create_index_on_lesson_id.sql
```

**Why timestamp?**
- Prevents naming conflicts
- Natural ordering (migrations run in order)
- Single responsibility (one change per file)

---

## Generated Migration Examples

### 1. Create New Table

**Request:** "Create migration for LessonReviewEntity"

**Generated:** `V20250504120000__create_lesson_review_table.sql`

```sql
-- Create table for lesson reviews (reviews_feedbacks module)
CREATE TABLE CL_LESSON_REVIEW (
  id UUID PRIMARY KEY,
  rf_lesson_id UUID NOT NULL,
  rf_author_id UUID NOT NULL,
  rf_rating INTEGER NOT NULL CHECK (rf_rating BETWEEN 1 AND 5),
  rf_comment VARCHAR(500) NOT NULL,
  rf_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for query performance
CREATE INDEX idx_cl_lesson_review_lesson_id ON CL_LESSON_REVIEW(rf_lesson_id);
CREATE INDEX idx_cl_lesson_review_author_id ON CL_LESSON_REVIEW(rf_author_id);

-- Sequence for ID generation (if using sequences)
CREATE SEQUENCE cl_lesson_review_seq START WITH 1 INCREMENT BY 50;

-- Comments for documentation
COMMENT ON TABLE CL_LESSON_REVIEW IS 'Lesson review records: user ratings and feedback on lessons';
COMMENT ON COLUMN CL_LESSON_REVIEW.rf_rating IS 'Rating score: 1 (poor) to 5 (excellent)';
COMMENT ON COLUMN CL_LESSON_REVIEW.rf_comment IS 'User feedback: max 500 characters';
```

**Validation:**
```
✅ PASS: Migration validated

Checks:
- ✅ Table name follows CL_* convention
- ✅ Column names abbreviated (rf_lesson_id, rf_rating)
- ✅ CHECK constraint enforces valid ratings (1-5)
- ✅ Indexes created on foreign keys (lesson_id, author_id)
- ✅ Default timestamp uses CURRENT_TIMESTAMP (works in H2 + PostgreSQL)
- ✅ Sequence created with Quarkus defaults (1, 50 increment)
- ✅ Comments explain purpose

Recommendations:
- Add foreign key constraint to course.lesson (if needed)
- Add indexes on createdAt for date range queries

Safe for: H2, PostgreSQL
Estimated downtime: 0 seconds (table doesn't exist yet)
```

---

### 2. Add Column to Existing Table

**Request:** "Add helpfulCount column to CL_LESSON_REVIEW"

**Generated:** `V20250504121500__add_helpful_count_to_lesson_review.sql`

```sql
-- Add helpful count column to track review usefulness
ALTER TABLE CL_LESSON_REVIEW
ADD COLUMN rf_helpful_count INTEGER NOT NULL DEFAULT 0;

-- Create index for sorting by helpfulness
CREATE INDEX idx_cl_lesson_review_helpful_count ON CL_LESSON_REVIEW(rf_helpful_count DESC);
```

**Validation - Warnings:**
```
⚠️ WARNING: Null Safety Concern

Migration: ALTER TABLE CL_LESSON_REVIEW ADD COLUMN rf_helpful_count ...

Issue: Adding NOT NULL column to table with existing data
Risk: Migration will fail if table has rows (can't populate existing rows)
Impact: Deployment will abort; manual intervention needed

Fix Options:
1. Make column nullable initially: NOT NULL DEFAULT 0 is OK only if DEFAULT provided
2. Backfill in two steps:
   a. Add column nullable: ADD COLUMN rf_helpful_count INTEGER DEFAULT 0
   b. Then: ALTER COLUMN rf_helpful_count SET NOT NULL
3. If you know all existing rows should have 0: Use DEFAULT 0 (shown above)

Status: ✅ SAFE (with DEFAULT 0, migration handles existing data)
Safe for: H2, PostgreSQL
Estimated downtime: ~0-5 seconds (index creation may lock table briefly)
```

---

### 3. Create Index for Performance

**Request:** "Add index on lesson_id for faster list queries"

**Generated:** `V20250504130000__add_index_lesson_review_lesson_id.sql`

```sql
-- Index to optimize getReviewsForLesson() query
-- Reduces query time from O(n) to O(log n) on large datasets
CREATE INDEX idx_cl_lesson_review_lesson_id 
ON CL_LESSON_REVIEW(rf_lesson_id);
```

**Validation:**
```
✅ PASS: Index migration validated

Checks:
- ✅ Index name follows convention (idx_cl_table_column)
- ✅ Single column index (simple, maintainable)
- ✅ Indexed column matches query filter (rf_lesson_id)
- ✅ Index creation is idempotent (can be re-run safely)

Performance impact:
- Query: SELECT * FROM CL_LESSON_REVIEW WHERE rf_lesson_id = ? 
- Before: Full table scan (1000 rows = 1000 comparisons)
- After: B-tree lookup (1000 rows = ~10 comparisons)
- Speed improvement: ~100x faster

Storage impact: ~2-5% table size per index

Safe for: H2, PostgreSQL
Estimated downtime: ~0-2 seconds (index creation may lock table briefly)
Rollback safety: ✅ Safe (DROP INDEX is fast)
```

---

### 4. Add Foreign Key Constraint

**Request:** "Link reviews to lessons with foreign key"

**Generated:** `V20250504140000__add_fk_lesson_review_to_lesson.sql`

```sql
-- Add foreign key: reviews → lessons
-- Ensures reviews can only be created for existing lessons
ALTER TABLE CL_LESSON_REVIEW
ADD CONSTRAINT fk_cl_lesson_review_lesson_id
FOREIGN KEY (rf_lesson_id)
REFERENCES CL_LESSON(id)
ON DELETE CASCADE
ON UPDATE RESTRICT;
```

**Validation - Error:**
```
🔴 ERROR: Foreign Key Validation Failed

Migration: ADD CONSTRAINT fk_cl_lesson_review_lesson_id ...

Issue: Attempting to add FK to table CL_LESSON that may not exist
Check: Does course module have CL_LESSON table with column 'id'?

Questions to verify:
1. Does CL_LESSON table exist? (check course module migrations)
2. Is CL_LESSON.id a UUID? (must match rf_lesson_id type)
3. Do existing reviews have valid lesson IDs? (data integrity check)

Recommendations:
- Add DEFERRABLE INITIALLY DEFERRED if you need to backfill data
- Test: ./mvnw quarkus:dev (will verify FK on startup)
- If adding FK to table with data: Ensure all data is valid first

Status: ⏸️ REQUIRES VERIFICATION
Safe for: H2, PostgreSQL (once verified)
Estimated downtime: ~1-5 seconds (table lock for constraint creation)
Rollback: DROP CONSTRAINT works, but may leave orphaned data exposed
```

---

### 5. Change Column Type (Dangerous!)

**Request:** "Change comment column from VARCHAR(500) to TEXT"

**Generated:** `V20250504150000__expand_comment_field_to_text.sql`

```sql
-- Expand comment field from 500 chars to unlimited
-- H2 compatible syntax
ALTER TABLE CL_LESSON_REVIEW
MODIFY COLUMN rf_comment TEXT;

-- PostgreSQL compatible syntax (alternative if H2 doesn't work)
-- ALTER TABLE CL_LESSON_REVIEW
-- ALTER COLUMN rf_comment TYPE TEXT;
```

**Validation - Danger Zone:**
```
🔴 CRITICAL: Type Change Requires Testing

Migration: MODIFY COLUMN rf_comment TEXT

Risks:
1. ⚠️ Data loss: If shrinking (TEXT → VARCHAR(100)), data > 100 chars is lost
2. ⚠️ Application breakage: Code expecting VARCHAR may behave differently
3. ⚠️ Index incompatibility: Indexes on VARCHAR may not work with TEXT
4. ⚠️ Database compatibility: Some databases handle type changes differently

This migration is safe because:
- ✅ Expanding VARCHAR(500) → TEXT (never loses data)
- ✅ No dependent indexes
- ✅ TEXT is superset of VARCHAR

But for other type changes:
- Create new column with new type
- Copy data with transformation
- Drop old column
- Rename new column to old name

EXAMPLE (for VARCHAR → VARCHAR(100) reduction):
  ALTER TABLE CL_LESSON_REVIEW
  ADD COLUMN rf_comment_new VARCHAR(100);
  
  UPDATE CL_LESSON_REVIEW
  SET rf_comment_new = SUBSTRING(rf_comment, 1, 100);
  
  ALTER TABLE CL_LESSON_REVIEW
  DROP COLUMN rf_comment;
  
  ALTER TABLE CL_LESSON_REVIEW
  RENAME COLUMN rf_comment_new TO rf_comment;

Status: ✅ SAFE (expanding, not shrinking)
Safe for: H2, PostgreSQL
Estimated downtime: ~2-10 seconds (table lock for type change)
Rollback: Create new migration to change back (not automatic)
```

---

### 6. Backfill Data Before Constraint

**Request:** "Make helpful_count NOT NULL after backfill"

**Generated:** `V20250504160000__backfill_and_constrain_helpful_count.sql`

```sql
-- Step 1: Backfill existing rows with default value
UPDATE CL_LESSON_REVIEW
SET rf_helpful_count = 0
WHERE rf_helpful_count IS NULL;

-- Step 2: Now add the NOT NULL constraint
ALTER TABLE CL_LESSON_REVIEW
MODIFY COLUMN rf_helpful_count INTEGER NOT NULL;

-- Step 3: Verify no nulls remain
-- SELECT COUNT(*) FROM CL_LESSON_REVIEW WHERE rf_helpful_count IS NULL;
-- Result should be 0
```

**Validation:**
```
✅ PASS: Backfill validated

Checks:
- ✅ UPDATE query backfills all rows
- ✅ WHERE clause prevents unnecessary updates
- ✅ Value (0) matches application defaults
- ✅ Constraint added AFTER backfill (won't fail)

Data verification:
- SELECT COUNT(*) FROM CL_LESSON_REVIEW WHERE rf_helpful_count IS NULL;
- Expected: 0 (no nulls remain)

Safe for: H2, PostgreSQL
Estimated downtime: ~5-30 seconds (depends on row count)
Tested on: 1000 rows = 0.5 seconds

Rollback safety: ⚠️ RISKY
- Removing the constraint is fast
- But undoing UPDATE requires another migration
- If you need to rollback: Create reverse migration manually
```

---

### 7. Add Timestamp Columns with UTC Default

**Request:** "Add updatedAt timestamp to reviews"

**Generated:** `V20250504170000__add_updated_at_timestamp.sql`

```sql
-- Add updated_at timestamp (tracks when review was last modified)
ALTER TABLE CL_LESSON_REVIEW
ADD COLUMN rf_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Update existing rows to match created_at
UPDATE CL_LESSON_REVIEW
SET rf_updated_at = rf_created_at;

-- Add check to ensure updated >= created
ALTER TABLE CL_LESSON_REVIEW
ADD CONSTRAINT ck_lesson_review_timestamp_order
CHECK (rf_updated_at >= rf_created_at);
```

**Validation:**
```
✅ PASS: Timestamp migration validated

Checks:
- ✅ DEFAULT CURRENT_TIMESTAMP works in H2 and PostgreSQL
- ✅ Timestamps in UTC (database default)
- ✅ Backfill sets updatedAt = createdAt for existing rows
- ✅ CHECK constraint prevents illogical data (updatedAt < createdAt)

Notes on UTC:
- H2: CURRENT_TIMESTAMP is UTC by default ✅
- PostgreSQL: CURRENT_TIMESTAMP is session timezone (⚠️ beware!)
  - Fix: Use CURRENT_TIMESTAMP AT TIME ZONE 'UTC'
  - Or: Quarkus handles conversion in code (recommended)

Application code handles:
- Insert: Database sets CURRENT_TIMESTAMP automatically
- Update: Service must explicitly SET rf_updated_at = CURRENT_TIMESTAMP

Safe for: H2, PostgreSQL
Estimated downtime: ~1-5 seconds
Rollback: Fast (DROP CONSTRAINT, DROP COLUMN)
```

---

## Common Migration Patterns

### Pattern 1: Safe Column Addition

```sql
-- ✅ ALWAYS provide DEFAULT for NOT NULL columns on populated tables
ALTER TABLE CL_LESSON_REVIEW
ADD COLUMN rf_status VARCHAR(20) NOT NULL DEFAULT 'published';
```

### Pattern 2: Safe Column Removal

```sql
-- ✅ First, remove constraints
ALTER TABLE CL_LESSON_REVIEW
DROP CONSTRAINT fk_lesson_review_something;

-- Then, drop column
ALTER TABLE CL_LESSON_REVIEW
DROP COLUMN rf_deprecated_field;
```

### Pattern 3: Safe Index Addition

```sql
-- ✅ Indexes are safe to add at any time
-- Existing queries benefit, no data risk
CREATE INDEX idx_cl_lesson_review_status 
ON CL_LESSON_REVIEW(rf_status);
```

### Pattern 4: Safe Constraint Addition with Validation

```sql
-- ✅ Only add constraint if you've verified data
-- Step 1: Add constraint DEFERRABLE (allows batch fixes)
ALTER TABLE CL_LESSON_REVIEW
ADD CONSTRAINT ck_comment_not_blank
CHECK (rf_comment IS NOT NULL AND rf_comment != '')
DEFERRABLE INITIALLY DEFERRED;

-- Step 2: Validate all data passes constraint
-- SELECT COUNT(*) FROM CL_LESSON_REVIEW WHERE rf_comment IS NULL OR rf_comment = '';
-- Result must be 0
```

### Pattern 5: Rename Column (PostgreSQL)

```sql
-- PostgreSQL
ALTER TABLE CL_LESSON_REVIEW
RENAME COLUMN rf_helpful_votes TO rf_helpful_count;

-- H2 (more complex, may need workaround)
-- CREATE new column, copy data, drop old, rename
ALTER TABLE CL_LESSON_REVIEW
ADD COLUMN rf_helpful_count INTEGER;

UPDATE CL_LESSON_REVIEW
SET rf_helpful_count = rf_helpful_votes;

ALTER TABLE CL_LESSON_REVIEW
DROP COLUMN rf_helpful_votes;
```

---

## Migration Testing

### Local Testing

```bash
# Start dev mode - Flyway auto-applies migrations
./mvnw quarkus:dev

# Check migration status
# (Flyway creates schema_version table)
SELECT * FROM FLYWAY_SCHEMA_HISTORY;

# Test against PostgreSQL locally
docker run -e POSTGRES_PASSWORD=password postgres:15
# Update application.properties to PostgreSQL URL
./mvnw quarkus:dev
```

### Validation Checklist

Before committing a migration:

- [ ] Filename follows `V{TIMESTAMP}__{description}.sql`
- [ ] Works on both H2 (dev) and PostgreSQL (prod)
- [ ] Existing data is handled (backfill, defaults, or nullable)
- [ ] No data loss (test the rollback scenario)
- [ ] Indexes created on foreign key columns
- [ ] Comments explain purpose
- [ ] CHECK constraints enforce business rules
- [ ] Timestamps use CURRENT_TIMESTAMP (UTC)
- [ ] Table/column names follow CL_* convention
- [ ] No synthetic migrations (combine logical changes)

---

## Rollback Strategy

Flyway doesn't auto-rollback. Manual recovery needed:

### Option 1: Create Reverse Migration

```sql
-- V20250504180000__rollback_helpful_count.sql
ALTER TABLE CL_LESSON_REVIEW
DROP COLUMN rf_helpful_count;
```

### Option 2: Fix-Forward Migration

```sql
-- If migration introduced bad data, fix it forward instead of reverting
UPDATE CL_LESSON_REVIEW
SET rf_rating = 3
WHERE rf_rating < 1 OR rf_rating > 5;

ALTER TABLE CL_LESSON_REVIEW
ADD CONSTRAINT ck_valid_rating
CHECK (rf_rating BETWEEN 1 AND 5);
```

---

## Dangerous Migrations to Avoid

❌ **Never do this:**

```sql
-- ❌ NO: Dropping columns without backup
DROP COLUMN rf_deprecated_field;

-- ❌ NO: Renaming without testing both DBs
RENAME TABLE old_name TO new_name;

-- ❌ NO: Shrinking column size
ALTER COLUMN comment VARCHAR(100); -- was 500

-- ❌ NO: Changing column type without copy strategy
ALTER COLUMN status INT; -- was VARCHAR

-- ❌ NO: Making column NOT NULL without backfill
ALTER COLUMN helpful_count SET NOT NULL; -- has NULLs!

-- ❌ NO: Large data updates in production
UPDATE CL_LESSON_REVIEW SET rf_rating = 5; -- 1M rows!

-- ❌ NO: No foreign key constraints
-- (creates orphaned data)
INSERT INTO CL_LESSON_REVIEW(rf_lesson_id, ...) VALUES (fake_id, ...);
```

✅ **Do this instead:**

```sql
-- ✅ YES: Add column with default, then constrain
ALTER TABLE ADD COLUMN status VARCHAR(20) DEFAULT 'active';
ALTER TABLE ADD CONSTRAINT CHECK (status IN ('active', 'deleted'));

-- ✅ YES: Create new column, copy, drop old, rename
ALTER TABLE ADD COLUMN helpful_count_new INTEGER DEFAULT 0;
UPDATE helpful_count_new = helpful_votes;
DROP COLUMN helpful_votes;
RENAME helpful_count_new TO helpful_votes;

-- ✅ YES: Add FK constraint with ON DELETE CASCADE
ALTER TABLE ADD CONSTRAINT FOREIGN KEY (lesson_id) 
REFERENCES lesson(id) ON DELETE CASCADE;

-- ✅ YES: Batch large updates
UPDATE CL_LESSON_REVIEW 
SET rf_rating = 5 
WHERE rf_lesson_id = ? AND rf_rating IS NULL
LIMIT 1000; -- Repeat in batches
```

---

## Next Steps

1. **After entity design:** Create migration with this skill
2. **Run locally:** `./mvnw quarkus:dev` applies migrations
3. **Test on PostgreSQL:** Verify DDL syntax works
4. **Commit with code:** Include migration in PR
5. **Review:** `codelevel-architecture-reviewer` checks schema naming
6. **Deploy:** Flyway runs migrations automatically
