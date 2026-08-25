import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { TodoTable } from '../components/TodoTable'
import type { Todo } from '../types'

function todo(overrides: Partial<Todo> = {}): Todo {
  return {
    id: 1,
    name: 'bake bread',
    description: null,
    dueDate: '2026-03-10',
    status: 'NOT_STARTED',
    priority: 'MEDIUM',
    recurrence: { type: 'NONE', interval: null },
    recurrenceSourceId: null,
    dependencies: [],
    blocked: false,
    completedAt: null,
    deletedAt: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    version: 0,
    ...overrides,
  }
}

function renderTable(todos: Todo[]) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <TodoTable
        todos={todos}
        expandedId={null}
        sortKey="dueDate"
        sortDirection="asc"
        busyId={null}
        onSort={vi.fn()}
        onToggleExpand={vi.fn()}
        onStatusChange={vi.fn()}
        onEdit={vi.fn()}
        onDelete={vi.fn()}
        onRestore={vi.fn()}
      />
    </QueryClientProvider>,
  )
}

describe('TodoTable', () => {
  it('flags a blocked TODO so the user knows why it will not start', () => {
    renderTable([todo({ blocked: true })])
    expect(screen.getByTitle('Waiting on a dependency')).toHaveTextContent('blocked')
  })

  it('marks a past due date as overdue', () => {
    renderTable([todo({ dueDate: '2020-01-01' })])
    expect(screen.getByText('2020-01-01')).toHaveClass('overdue')
  })

  it('does not mark a completed TODO overdue, however old', () => {
    renderTable([todo({ dueDate: '2020-01-01', status: 'COMPLETED' })])
    expect(screen.getByText('2020-01-01')).not.toHaveClass('overdue')
  })

  it('offers Restore instead of Edit and Delete for a deleted TODO', () => {
    renderTable([todo({ deletedAt: '2026-01-02T00:00:00Z' })])
    expect(screen.getByRole('button', { name: 'Restore' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument()
  })

  it('shows the repeat schedule with its interval', () => {
    renderTable([todo({ recurrence: { type: 'CUSTOM', interval: 10 } })])
    expect(screen.getByText('CUSTOM ×10')).toBeInTheDocument()
  })

  it('tells the user when nothing matches instead of showing an empty grid', () => {
    renderTable([])
    expect(screen.getByText('No TODOs match these filters.')).toBeInTheDocument()
  })

  it('disables the status control for a deleted TODO', () => {
    renderTable([todo({ deletedAt: '2026-01-02T00:00:00Z' })])
    const row = screen.getByText('bake bread').closest('tr')!
    expect(within(row).getByRole('combobox')).toBeDisabled()
  })
})
