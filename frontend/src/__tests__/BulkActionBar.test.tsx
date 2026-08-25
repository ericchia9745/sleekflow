import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { BulkActionBar } from '../components/BulkActionBar'
import type { Todo } from '../types'

function todo(overrides: Partial<Todo> = {}): Todo {
  return {
    id: 1,
    name: 'bake bread',
    description: null,
    dueDate: null,
    status: 'NOT_STARTED',
    priority: 'MEDIUM',
    recurrence: { type: 'NONE', interval: null },
    recurrenceSourceId: null,
    dependencies: [],
    owner: { id: 7, username: 'eric', displayName: 'Eric' },
    blocked: false,
    completedAt: null,
    deletedAt: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    version: 0,
    ...overrides,
  }
}

function renderBar(selected: Todo[], selectedCount = selected.length) {
  return render(
    <BulkActionBar
      selected={selected}
      selectedCount={selectedCount}
      busy={false}
      onChangeStatus={vi.fn()}
      onDelete={vi.fn()}
      onRestore={vi.fn()}
      onClear={vi.fn()}
    />,
  )
}

describe('BulkActionBar', () => {
  it('stays out of the way when nothing is selected', () => {
    const { container } = renderBar([])
    expect(container).toBeEmptyDOMElement()
  })

  it('says how much of the selection is off the current page', () => {
    // A selection survives paging, but a batch can only carry what is on screen.
    renderBar([todo()], 40)
    expect(screen.getByText(/39 on other pages, not acted on/)).toBeInTheDocument()
  })

  it('offers Restore instead of Delete once the selection includes deleted TODOs', () => {
    renderBar([todo({ deletedAt: '2026-01-02T00:00:00Z' })])
    expect(screen.getByRole('button', { name: 'Restore' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument()
  })

  it('offers Delete for a selection of live TODOs', () => {
    renderBar([todo()])
    expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
  })
})
