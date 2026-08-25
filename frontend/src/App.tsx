import { useState } from 'react'

import { describeError } from './api/todos'
import type { TodoPayload } from './api/todos'
import { FilterBar } from './components/FilterBar'
import { TodoForm } from './components/TodoForm'
import { TodoTable } from './components/TodoTable'
import {
  useChangeStatus,
  useCreateTodo,
  useDeleteTodo,
  useRestoreTodo,
  useTodoList,
  useUpdateTodo,
} from './hooks/useTodos'
import { EMPTY_FILTERS } from './types'
import type { SortDirection, SortKey, Todo, TodoFilters, TodoStatus } from './types'

const PAGE_SIZE = 25

export default function App() {
  const [filters, setFilters] = useState<TodoFilters>(EMPTY_FILTERS)
  const [page, setPage] = useState(0)
  const [sortKey, setSortKey] = useState<SortKey>('dueDate')
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc')
  const [editing, setEditing] = useState<Todo | null>(null)
  const [creating, setCreating] = useState(false)
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [message, setMessage] = useState<{ tone: 'error' | 'info'; text: string } | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  const list = useTodoList({ ...filters, page, size: PAGE_SIZE, sortKey, sortDirection })
  const create = useCreateTodo()
  const update = useUpdateTodo()
  const changeStatus = useChangeStatus()
  const remove = useDeleteTodo()
  const restore = useRestoreTodo()

  const todos = list.data?.content ?? []
  const pageInfo = list.data?.page

  function applyFilters(next: TodoFilters) {
    setFilters(next)
    setPage(0)
  }

  function sortBy(key: SortKey) {
    if (key === sortKey) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')
    } else {
      setSortKey(key)
      // Highest priority and latest status first is the more useful default for
      // those two; dates and names read better ascending.
      setSortDirection(key === 'priority' ? 'desc' : 'asc')
    }
    setPage(0)
  }

  async function submitNew(payload: TodoPayload) {
    setMessage(null)
    try {
      await create.mutateAsync(payload)
      setCreating(false)
    } catch (error) {
      setMessage({ tone: 'error', text: describeError(error) })
    }
  }

  async function submitEdit(payload: TodoPayload) {
    if (!editing) return
    setMessage(null)
    try {
      await update.mutateAsync({ ...payload, id: editing.id, version: editing.version })
      setEditing(null)
    } catch (error) {
      setMessage({ tone: 'error', text: describeError(error) })
    }
  }

  async function moveTo(todo: Todo, status: TodoStatus) {
    setMessage(null)
    setBusyId(todo.id)
    try {
      const result = await changeStatus.mutateAsync({ id: todo.id, status, version: todo.version })
      if (result.nextOccurrence) {
        setMessage({
          tone: 'info',
          text: `Scheduled the next “${result.nextOccurrence.name}” for ${result.nextOccurrence.dueDate}.`,
        })
      }
    } catch (error) {
      setMessage({ tone: 'error', text: describeError(error) })
    } finally {
      setBusyId(null)
    }
  }

  async function softDelete(todo: Todo) {
    setMessage(null)
    try {
      await remove.mutateAsync(todo.id)
      setMessage({ tone: 'info', text: `Deleted “${todo.name}”. Find it under “Show deleted only” to restore it.` })
    } catch (error) {
      setMessage({ tone: 'error', text: describeError(error) })
    }
  }

  async function undelete(todo: Todo) {
    setMessage(null)
    try {
      await restore.mutateAsync(todo.id)
    } catch (error) {
      setMessage({ tone: 'error', text: describeError(error) })
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>TODOs</h1>
        <button type="button" onClick={() => { setCreating(true); setEditing(null) }}>
          New TODO
        </button>
      </header>

      {message && (
        <p className={message.tone === 'error' ? 'banner error' : 'banner info'} role="status">
          {message.text}
          <button type="button" className="link" onClick={() => setMessage(null)}>dismiss</button>
        </p>
      )}

      {creating && (
        <TodoForm
          busy={create.isPending}
          error={null}
          onSubmit={submitNew}
          onCancel={() => setCreating(false)}
        />
      )}

      {editing && (
        <TodoForm
          initial={editing}
          busy={update.isPending}
          error={null}
          onSubmit={submitEdit}
          onCancel={() => setEditing(null)}
        />
      )}

      <FilterBar filters={filters} onChange={applyFilters} onReset={() => applyFilters(EMPTY_FILTERS)} />

      {list.isError && <p className="banner error">{describeError(list.error)}</p>}

      <TodoTable
        todos={todos}
        expandedId={expandedId}
        sortKey={sortKey}
        sortDirection={sortDirection}
        busyId={busyId}
        onSort={sortBy}
        onToggleExpand={(id) => setExpandedId(expandedId === id ? null : id)}
        onStatusChange={moveTo}
        onEdit={(todo) => { setEditing(todo); setCreating(false) }}
        onDelete={softDelete}
        onRestore={undelete}
      />

      {pageInfo && (
        <nav className="pagination" aria-label="Pagination">
          <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
          <span>
            Page {pageInfo.number + 1} of {Math.max(pageInfo.totalPages, 1)} · {pageInfo.totalElements} TODOs
            {list.isFetching && <span className="muted"> · updating…</span>}
          </span>
          <button
            type="button"
            disabled={pageInfo.number + 1 >= pageInfo.totalPages}
            onClick={() => setPage(page + 1)}
          >
            Next
          </button>
        </nav>
      )}
    </div>
  )
}
