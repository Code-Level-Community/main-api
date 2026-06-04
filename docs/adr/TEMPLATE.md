# ADR-NNN: Title

## Status
Accepted | Proposed | Deprecated | Superseded by ADR-NNN

## Context

[Describe the problem or question that necessitated this decision. Include relevant constraints, forces, and requirements.]

## Decision

[State the decision clearly and specifically. Include code examples, diagrams, or tables when they aid understanding.]

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Option A | Why it was ruled out |
| Option B | Why it was ruled out |

## Motivation

[Core reasoning for choosing this option over the alternatives. Reference constraints from Context.]

## Consequences

### Positive
- Benefit 1

### Negative / Trade-offs
- Drawback or trade-off 1

---

## Multi-decision ADR pattern

For module-level ADRs that document several related decisions, replace the sections above (after `## Context`) with repeating blocks:

```
## Decision N: Sub-decision Title

**What:** [The decision]

**Alternatives considered:**

| Alternative | Reason for rejection |
|---|---|

**Motivation:** [Core reasoning]

**Consequences:**
- (+) Positive outcome
- (-) Negative / trade-off
```