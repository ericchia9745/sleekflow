import { describe, expect, it } from 'vitest'

import { describeBulkResult, describeError } from '../api/todos'
import type { BulkResult, ProblemDetail, Todo } from '../types'

function asAxiosError(problem: ProblemDetail) {
  return { response: { data: problem } }
}

describe('describeError', () => {
  it('names the blockers when a task cannot start', () => {
    const message = describeError(
      asAxiosError({
        type: 'https://sleekflow.example/problems/dependencies-not-satisfied',
        title: 'Dependencies not satisfied',
        status: 409,
        detail: 'TODO 2 is blocked by 2 unfinished dependencies',
        outstandingDependencies: ['buy flour', 'preheat oven'],
      }),
    )

    expect(message).toContain('buy flour, preheat oven')
  })

  it('lists every field error rather than only the first', () => {
    const message = describeError(
      asAxiosError({
        type: 'https://sleekflow.example/problems/validation-failed',
        title: 'Validation failed',
        status: 400,
        detail: 'One or more fields are invalid.',
        errors: [
          { field: 'name', message: 'must not be blank' },
          { field: 'recurrenceSchedulable', message: 'needs a due date' },
        ],
      }),
    )

    expect(message).toBe('name: must not be blank; recurrenceSchedulable: needs a due date')
  })

  it('falls back to the problem detail for other failures', () => {
    const message = describeError(
      asAxiosError({
        type: 'https://sleekflow.example/problems/stale-version',
        title: 'Concurrent modification',
        status: 409,
        detail: 'TODO 1 has moved on: you sent version 0 but the current version is 2',
      }),
    )

    expect(message).toContain('current version is 2')
  })

  it('handles a network failure with no problem body', () => {
    expect(describeError(new Error('Network Error'))).toBe('Network Error')
  })

  it('never returns an empty string for an unknown throwable', () => {
    expect(describeError({})).toBeTruthy()
  })
})

describe('describeBulkResult', () => {
  function result(overrides: Partial<BulkResult> = {}): BulkResult {
    return { requested: 3, succeeded: [1, 2, 3], failed: [], createdOccurrences: [], ...overrides }
  }

  it('reports a clean batch as a plain count', () => {
    expect(describeBulkResult('completed', result())).toBe('3 of 3 completed.')
  })

  it('groups the reasons items were skipped rather than listing each one', () => {
    const message = describeBulkResult(
      'updated',
      result({
        succeeded: [1],
        failed: [
          { id: 2, type: 'dependencies-not-satisfied', detail: 'Still waiting on: shop.' },
          { id: 3, type: 'dependencies-not-satisfied', detail: 'Still waiting on: prep.' },
        ],
      }),
    )

    expect(message).toBe('1 of 3 updated. Skipped: 2 still blocked.')
  })

  it('distinguishes a conflict from a permission problem', () => {
    const message = describeBulkResult(
      'deleted',
      result({
        succeeded: [1],
        failed: [
          { id: 2, type: 'stale-version', detail: 'Someone else changed this TODO first.' },
          { id: 3, type: 'not-todo-owner', detail: 'TODO 3 belongs to someone else.' },
        ],
      }),
    )

    expect(message).toContain('1 changed by someone else')
    expect(message).toContain('1 owned by someone else')
  })

  it('mentions occurrences a batch of completions scheduled', () => {
    const message = describeBulkResult(
      'completed',
      result({ createdOccurrences: [{ id: 9, name: 'water the plants' } as Todo] }),
    )

    expect(message).toContain('1 next occurrence(s) scheduled')
  })
})
