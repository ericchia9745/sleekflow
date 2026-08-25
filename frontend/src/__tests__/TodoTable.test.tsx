import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { TodoTable } from '../components/TodoTable'
import type { Todo, TodoSummary } from '../types'

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

  describe('the Depends on column', () => {
    const dependency = (overrides: Partial<TodoSummary> = {}): TodoSummary => ({
      id: 10,
      name: 'buy flour',
      status: 'NOT_STARTED',
      deleted: false,
      ...overrides,
    })

    it('names the prerequisite instead of only saying the task is blocked', () => {
      renderTable([todo({ blocked: true, dependencies: [dependency()] })])
      expect(screen.getByText('#10 buy flour')).toBeInTheDocument()
    })

    it('marks an outstanding prerequisite as the thing being waited on', () => {
      renderTable([todo({ blocked: true, dependencies: [dependency()] })])
      expect(screen.getByText('#10 buy flour')).toHaveClass('dependency-outstanding')
    })

    it('shows a completed prerequisite as settled rather than hiding it', () => {
      // Still worth seeing: it explains why a task with dependencies is not blocked.
      renderTable([todo({ dependencies: [dependency({ status: 'COMPLETED' })] })])
      expect(screen.getByText('#10 buy flour')).toHaveClass('dependency-done')
    })

    it('distinguishes a deleted prerequisite, which no longer blocks', () => {
      renderTable([todo({ dependencies: [dependency({ deleted: true })] })])
      expect(screen.getByText('#10 buy flour')).toHaveClass('dependency-deleted')
    })

    it('puts outstanding prerequisites ahead of settled ones', () => {
      renderTable([
        todo({
          blocked: true,
          dependencies: [
            dependency({ id: 1, name: 'done one', status: 'COMPLETED' }),
            dependency({ id: 2, name: 'still open' }),
          ],
        }),
      ])
      const shown = screen.getAllByText(/^#\d+ /).map((node) => node.textContent)
      expect(shown[0]).toBe('#2 still open')
    })

    it('collapses a long list behind a count rather than flooding the row', () => {
      renderTable([
        todo({
          dependencies: [1, 2, 3, 4].map((id) => dependency({ id, name: `task ${id}` })),
        }),
      ])
      expect(screen.getByRole('button', { name: '+2 more' })).toBeInTheDocument()
    })

    it('expands the row when the overflow count is clicked', async () => {
      const onToggleExpand = vi.fn()
      const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
      render(
        <QueryClientProvider client={client}>
          <TodoTable
            todos={[todo({ dependencies: [1, 2, 3].map((id) => dependency({ id, name: `task ${id}` })) })]}
            expandedId={null}
            sortKey="dueDate"
            sortDirection="asc"
            busyId={null}
            onSort={vi.fn()}
            onToggleExpand={onToggleExpand}
            onStatusChange={vi.fn()}
            onEdit={vi.fn()}
            onDelete={vi.fn()}
            onRestore={vi.fn()}
          />
        </QueryClientProvider>,
      )
      screen.getByRole('button', { name: '+1 more' }).click()
      expect(onToggleExpand).toHaveBeenCalledWith(1)
    })

    it('shows a dash when a TODO has no prerequisites', () => {
      renderTable([todo({ dependencies: [] })])
      const row = screen.getByText('bake bread').closest('tr')!
      expect(within(row).getAllByText('—').length).toBeGreaterThan(0)
    })
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
