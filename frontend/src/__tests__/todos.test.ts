import { describe, expect, it } from 'vitest'

import { describeError } from '../api/todos'
import type { ProblemDetail } from '../types'

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
