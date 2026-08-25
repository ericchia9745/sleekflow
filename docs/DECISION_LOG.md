# Decision Log

## 1. Ambiguities, and how they were resolved

**"Due Date" — a date, or a moment in time?**
Stored as a calendar date (`LocalDate` / `DATE`). Recurrence arithmetic is the
deciding factor: "monthly" is only well-defined on a calendar, and a timestamp
would force a timezone decision the requirements never mention — with the usual
consequence that a task due "today" flips to yesterday for a colleague in
another office. The cost is that time-of-day deadlines are not expressible. If
they were needed, the fix is a nullable `due_time` column beside the date, not a
change of type.

**"Data should not be permanently lost when a TODO is deleted."**
Read as: delete is reversible, not as: keep an audit trail of every change.
`DELETE` sets `deleted_at`; the row stays, disappears from the default list, and
`POST /{id}/restore` brings it back. The UI exposes this as "Show deleted only".
This also solves a second problem for free: a TODO that others depend on can be
deleted without leaving dangling references.

Note that this is distinct from the `ARCHIVED` **status**, which the spec lists
separately. Archiving is a user's decision that a task is no longer active;
deleting is a user's decision that it should not have existed. Conflating them
would have made "archive" irreversible or "delete" visible, and neither reads
right.

**"A dependent task cannot be moved to In Progress until all of its dependencies
are Completed."**
Enforced exactly as written — the gate is on `IN_PROGRESS` only. Completing is
deliberately *not* gated: a task can turn out to be unnecessary and deserve
closing without ever being started, and the requirement is specific about which
transition it constrains. This is the interpretation I would most want to
confirm with a product owner; it is a one-line change if the intent was broader.

Two follow-on cases the requirement does not cover:
- A **soft-deleted** dependency stops blocking. The work it represented is gone.
- An **archived** dependency still blocks, because `ARCHIVED` is not
  `COMPLETED`. The user must either complete it or drop the link. The
  alternative — treating archived as satisfied — lets a task slip through a gate
  that was put there on purpose.

**"Recurring: daily, weekly, monthly, or custom."**
Modelled as a type plus an interval. The named schedules take an optional
multiplier (every 2 weeks); `CUSTOM` means "every N days" and requires an
explicit N. `CUSTOM` with N days therefore overlaps `DAILY` with interval N.
That redundancy is deliberate: a single `(type, interval)` pair covers every
case in the requirement without a parser, and the alternative — a cron
expression or an RRULE — is a large amount of machinery for a feature nobody
asked to be that flexible.

The next occurrence is created **without** the completed one's dependencies. A
recurring chore's dependencies were satisfied for *this* cycle, and there is no
general way to tell which of them also recur. Copying them would leave the new
occurrence blocked by tasks that are already done.

**"Multiple users accessing the same TODO list concurrently."**
Read as correctness under concurrent writes, not as real-time collaboration.
Handled with optimistic locking: every write carries the version the client last
saw, and a mismatch returns `409` naming both versions instead of silently
overwriting someone's edit. Pessimistic locking would serialise unrelated edits;
last-write-wins would lose them silently.

## 2. Architectural decisions and trade-offs

**Sorting priority and status in the database.** Enum names sort alphabetically,
which puts `HIGH` before `LOW` and `ARCHIVED` before `NOT_STARTED` — correct as
text, useless to a user. Two stored generated columns (`priority_rank`,
`status_rank`) hold the meaningful order and are indexed. The alternative,
sorting in application code, breaks as soon as results are paged, since page 1
can only be ordered against the rows it already fetched. The cost is a
MySQL-specific DDL feature; on a database without generated columns the same
effect needs a trigger or an application-maintained column.

**Computing "blocked" per page, not per row.** The blocked flag needs the state
of every dependency. Deriving it row by row is an N+1 query, which is exactly
what falls over at 10,000 items. The list endpoint issues one extra query for
the whole page (`findBlockedIdsAmong`), and the blocked/unblocked *filter* is an
`EXISTS` subquery so the database does the work and the count stays accurate.
Measured over 12,006 rows, every filter and sort returns in under 25 ms.

**Offset paging with a stable tie-breaker.** Every sort has `id` appended, so
paging cannot repeat or skip rows when values tie. Page size is capped at 200
(`PAGE_SIZE_MAX`) so no single request can pull the whole list. Offset paging
does degrade at very deep offsets; keyset paging would fix that but complicates
arbitrary multi-column sorting, and at the stated scale the measurements did not
justify it.

**RFC 9457 problem responses.** Failures carry a stable `type` URI plus
structured fields — `outstandingDependencies`, `expectedVersion`/`actualVersion`
— so the UI can name the blocking task rather than print a sentence. This is why
the "cannot start" message in the UI lists the specific dependency.

**Rules in the entity, coordination in the service.** `isBlocked`,
`nextOccurrence`, and the soft-delete flag live on `Todo`, so a future caller
cannot route around them. The service owns what needs a repository: version
checks, cycle detection, and loading.

**Configuration is entirely environment-driven** with working defaults, and the
`prod` profile ships none for credentials — it refuses to start and names what
is missing. Full reference in [CONFIGURATION.md](CONFIGURATION.md).

## 3. What I chose not to build

- **Authentication and users.** The core requirements describe a single shared
  list, and the concurrency requirement is about simultaneous access, not about
  per-user data. Adding auth would have meant an ownership model touching every
  query, for a feature listed as nice-to-have.
- **Real-time updates.** Instead, the client treats cached data as immediately
  stale and refetches on window focus, which covers the realistic case (a second
  tab) without a WebSocket layer.
- **Bulk operations.** Straightforward to add on the existing service, but they
  raise a design question — partial failure semantics — that deserves more than
  a quick answer.
- **An audit trail.** Soft delete satisfies the stated requirement. Full history
  is a different feature with different storage costs.
- **Testcontainers.** Tests run against a real MySQL schema, which is the point;
  but this machine has no Docker, so they use a separate `sleekflow_todo_test`
  database instead. Testcontainers would make CI cleaner and is the first thing
  I would change in an environment that has Docker.
- **A tested Docker Compose path for the app itself.** Compose is provided for
  MySQL only, and is written-but-unverified since Docker is not installed here.
  I would rather say so than imply it has been run.

## 4. What I would do differently with more time

- **Keyset pagination** for the common sorts, so deep pages stay flat.
- **A concurrent test that actually races two writers** against the same TODO.
  The current test proves the version check rejects a stale write; it does not
  prove two simultaneous completions of a recurring task cannot both spawn an
  occurrence. The version check makes that safe, but "makes it safe" is an
  argument, not a test.
- **Property-based tests for recurrence arithmetic.** The month-end and leap-day
  cases are covered by hand-picked examples; generated dates would cover more.
- **Richer dependency visualisation.** The UI shows what a task depends on, but
  not what it blocks, and there is no graph view.
- **Extract the frontend's filter state into the URL**, so a filtered view can be
  shared or reloaded.

## 5. A bug worth recording

The first version of the service built its response DTO before the transaction
flushed. Hibernate increments `@Version` at flush, so the API returned the
version the client *already had* — and the client's next write then failed with
a conflict that had never happened. Two consecutive edits always broke.

It was caught by the integration tests rather than by hand, which is the case
for writing them: every manual check had passed, because a single edit works
fine. The fix is one flush before mapping, with a comment explaining why it is
not incidental.
