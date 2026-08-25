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
This is the requirement I got wrong first, and the reversal is worth recording
in full.

The phrase supports two readings: *one list that several people share*, or
*several people each using the application*. I built it shared, then — while
adding authentication — talked myself into the second reading and scoped every
query to its owner, on the reasoning that an account that cannot keep anything
private is a strange product. That was over-thinking a requirement that says
"the same TODO list" in as many words. I put it back.

So: **one list, shared by everyone, with the creator recorded on every row and
shown in an Owner column.** Ownership is attribution, not access control, with
exactly one exception — only the owner may delete or restore. Removing someone
else's work from a shared list is a different act from correcting it, and it is
the one that is hard to notice afterwards, because a soft-deleted TODO simply
stops appearing. An edit announces itself; a deletion does not.

Two costs came with going back, both paid in the same migration:

- The list indexes had been rebuilt owner-first to match owner-scoped queries. A
  shared list issues the default query with no owner predicate, and MySQL cannot
  use a `user_id`-led index for that at all, so `V4` restores the
  `deleted_at`-led ones and keeps a single owner-led index for the optional
  "only mine" filter.
- Owner names have to reach the client without an N+1. `Todo` carries a plain
  `user_id` column rather than a relation, so the service resolves the distinct
  owners of a page in one query — the same shape as the blocked flag.

Concurrency itself is unchanged and is what the requirement is really about:
every write carries the version the client last saw, and a mismatch returns
`409` naming both versions instead of silently overwriting someone's edit.
Pessimistic locking would serialise unrelated edits; last-write-wins would lose
them silently. Sharing the list is what finally makes that machinery earn its
keep — two people really can now be editing the same row.

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
Measured over 12,011 rows — re-run after the list became shared and `V4`
reshaped the indexes — every filter and sort returns in under 25 ms: 18 ms for
the default page, 22 ms for the slowest filter, 4 ms for the revision
fingerprint, and 79 ms for a full 200-item bulk operation.

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

## 3. Authentication and live updates

Both were added after the core features, and both are covered in detail in
[ARCHITECTURE.md](ARCHITECTURE.md). The decisions worth recording here:

**Authentication gates access; it does not partition data.** Signing in decides
whether you may use the list at all, not which parts of it you may see. See
section 1 for how I arrived at that, having first built the opposite.

**Password change with no verification is a placeholder, and is labelled one.**
`POST /api/auth/change-password` replaces a password given only a username: no
current password, no emailed link. Anyone who knows a username can take that
account over. It exists because no email address is collected at registration,
so there is nowhere a reset link could go, and an account with no recovery path
at all is its own kind of failure for a demo. The real fix is an email address
and a single-use expiring token; the interim state is documented rather than
quietly shipped, because the failure mode here is not obvious from the UI.

**Sessions in the database, not JWTs.** An opaque random token with a row behind
it makes revocation an `UPDATE`. A JWT would avoid the lookup but make sign-out
either a lie or a blocklist — which is a session table with extra steps. The
lookup is a primary-key hit on an indexed hash.

**SHA-256 was requested, and is a starting point.** It is the wrong choice for
storing passwords: it is fast, so a stolen table falls to brute force. I built
it as asked but designed the exit — every digest carries its algorithm as a
prefix, and a stronger hasher re-hashes users at their next sign-in, with no
migration. Both hashing steps (browser and server) are deliberate and solve
different problems; neither replaces the other.

**Polling a fingerprint, not a socket.** `GET /api/todos/revision` returns the
newest `updated_at` plus a row count: 5 ms against 12,009 rows, against ~20 ms
to return a page. Clients refetch only when it changes. A BroadcastChannel
covers same-browser tabs instantly, so the poll only has to carry the
cross-*user* case. SSE is the next step if five seconds is ever too slow, and
`useRealtimeSync` is the single place that would change.

**sessionStorage, with a handshake.** Storing the token per tab means closing
the tab ends the session and nothing hits disk — but a second tab would start
signed out, which is awkward for a multi-tab feature. New tabs ask their
siblings for the session over the same channel. The honest cost: any same-origin
tab can ask. That is no weaker than `localStorage`, weaker than strict per-tab
isolation, and all three lose to XSS. `httpOnly` cookies plus CSRF handling is
the right answer before real users.

## 4. Bulk operations, and the question that delayed them

These were deferred at first, on the grounds that partial-failure semantics
deserved more than a quick answer. They are now built, and the answer is worth
stating because it is the whole of the design.

**A batch is best-effort, and reports on every item.** In a batch of forty, "one
of these is blocked" and "someone else edited one of these" are expected
outcomes for a row, not failures of the request. All-or-nothing would send the
user back to retry the same forty and hit a different single conflict. So the
endpoints return `200` with `succeeded` and `failed` side by side, and each
failure carries the same `type` slug the equivalent single-item endpoint would
have put in its RFC 9457 problem document — a stable code, not prose.

**Versions travel per item.** A single request-level version could not tell "one
row moved" from "everything you hold is stale". Delete and restore take bare
ids, matching their single-item endpoints, which ask for no version either:
taking a TODO off the list is not an edit of its contents.

**The dependency gate is answered once for the whole batch**, in the same
`findBlockedIdsAmong` query the list endpoint uses. That keeps a 200-item batch
off the N+1 path and makes an item's outcome independent of its position in the
request.

**What is still not built is bulk-by-filter** — "complete all 10,000 matching
this filter". Resolving the set server-side makes per-item version checks
impossible by construction, because the client never saw the rows. That is a
different feature with a different safety story. What exists is bulk-by-ids,
capped at `PAGE_SIZE_MAX`, so a client acts on exactly what it has seen.

One trap worth recording: bulk soft-delete reads like a one-line JPQL `UPDATE`.
That would skip `@Version` and `@PreUpdate`, leaving `updated_at` untouched — and
since the revision fingerprint is `MAX(updated_at)`, every other open tab would
keep showing stale data. Same family as the seed-script bug in section 6.

## 5. What I chose not to build

- **An audit trail.** Soft delete satisfies the stated requirement. Full history
  is a different feature with different storage costs.
- **Testcontainers.** Tests run against a real MySQL schema, which is the point;
  but this machine has no Docker, so they use a separate `sleekflow_schedule_note_test`
  database instead. Testcontainers would make CI cleaner and is the first thing
  I would change in an environment that has Docker.
- **A tested Docker Compose path for the app itself.** Compose is provided for
  MySQL only, and is written-but-unverified since Docker is not installed here.
  I would rather say so than imply it has been run.

- **A CI pipeline.** There is no `.github/workflows`. Both suites run with one
  command each, so this is a short file rather than a design problem, but it is
  honest to say it is absent.

Also left out with authentication in place: roles and permissions beyond
owner-only deletion, real password recovery, email verification, and any rate
limiting on sign-in. Throttling is the first thing a real deployment needs —
nothing currently slows down guessing, and change-password makes that worse
rather than better.

## 6. What I would do differently with more time

- **Keyset pagination** for the common sorts, so deep pages stay flat.
- **A concurrent test that actually races two writers** against the same TODO.
  The current test proves the version check rejects a stale write; it does not
  prove two simultaneous completions of a recurring task cannot both spawn an
  occurrence. The version check makes that safe, but "makes it safe" is an
  argument, not a test.
- **Property-based tests for recurrence arithmetic.** The month-end and leap-day
  cases are covered by hand-picked examples; generated dates would cover more.
- **Richer dependency visualisation.** The UI shows what a task depends on, but
  not what it blocks, and there is no graph view. (`TodoRepository.findDependents`
  exists for this and is currently unused.)
- **Bulk-by-filter**, with a conflict story that does not depend on the client
  having seen every row.
- **Extract the frontend's filter state into the URL**, so a filtered view can be
  shared or reloaded.

## 7. Two bugs worth recording

**A conflict that never happened.** The first version of the service built its
response DTO before the transaction flushed. Hibernate increments `@Version` at flush, so the API returned the
version the client *already had* — and the client's next write then failed with
a conflict that had never happened. Two consecutive edits always broke.

It was caught by the integration tests rather than by hand, which is the case
for writing them: every manual check had passed, because a single edit works
fine. The fix is one flush before mapping, with a comment explaining why it is
not incidental.

**A change signal that ignored changes.** The seed script stamped rows with
MySQL's `NOW(6)` — server local time — while the application writes UTC. The
seeded rows sat eight hours in the future, so `MAX(updated_at)` never moved when
a real edit landed, and the revision endpoint reported "no change" while the
list was actively being edited. The row count masked it for inserts, which is
exactly what made it easy to miss. Found by editing a TODO and watching the
fingerprint stay still. The seed now writes `UTC_TIMESTAMP(6)`; the wider lesson
is that a timestamp-derived change signal quietly assumes every writer shares a
clock, which is worth stating before adding the next writer.
