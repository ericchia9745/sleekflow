import { useEffect, useState } from 'react'

import { setUnauthorizedHandler } from './api/client'
import { describeBulkResult, describeError } from './api/todos'
import { useAuth } from './auth/AuthContext'
import { AuthScreen } from './components/AuthScreen'
import type { TodoPayload } from './api/todos'
import { BulkActionBar } from './components/BulkActionBar'
import { FilterBar } from './components/FilterBar'
import { TodoForm } from './components/TodoForm'
import { TodoTable } from './components/TodoTable'
import {
  useBulkChangeStatus,
  useBulkDelete,
  useBulkRestore,
  useChangeStatus,
  useCreateTodo,
  useDeleteTodo,
  useRestoreTodo,
  useTodoList,
  useUpdateTodo,
} from './hooks/useTodos'
import { useRealtimeSync } from './realtime/useRealtimeSync'
import { EMPTY_FILTERS } from './types'
import type { SortDirection, SortKey, Todo, TodoFilters, TodoStatus } from './types'

const PAGE_SIZE = 25

export default function App() {
  const { user, initialising, signOut } = useAuth()

  // A 401 from any call means this session is finished; drop it so the app
  // returns to the sign-in screen instead of failing request after request.
  useEffect(() => {
    setUnauthorizedHandler(() => { void signOut() })
  }, [signOut])

  if (initialising) {
    return <div className="app"><p className="muted">Restoring your session…</p></div>
  }
  if (!user) {
    return <AuthScreen />
  }
  return <TodoWorkspace />
}

function TodoWorkspace() {
  const { user, signOut } = useAuth()
  const [filters, setFilters] = useState<TodoFilters>(EMPTY_FILTERS)
  const [page, setPage] = useState(0)
  const [sortKey, setSortKey] = useState<SortKey>('dueDate')
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc')
  const [editing, setEditing] = useState<Todo | null>(null)
  const [creating, setCreating] = useState(false)
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [message, setMessage] = useState<{ tone: 'error' | 'info'; text: string } | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  // Held as ids rather than TODOs so a selection survives paging, filtering and
  // a refetch triggered by somebody else's edit.
  const [selectedIds, setSelectedIds] = useState<ReadonlySet<number>>(new Set())

  const { announceChange, lastCheckedAt } = useRealtimeSync(true)
  const list = useTodoList({ ...filters, page, size: PAGE_SIZE, sortKey, sortDirection })
  const create = useCreateTodo()
  const update = useUpdateTodo()
  const changeStatus = useChangeStatus()
  const remove = useDeleteTodo()
  const restore = useRestoreTodo()
  const bulkStatus = useBulkChangeStatus()
  const bulkDelete = useBulkDelete()
  const bulkRestore = useBulkRestore()

  const todos = list.data?.content ?? []
  const pageInfo = list.data?.page
  const selectedOnPage = todos.filter((todo) => selectedIds.has(todo.id))
  const bulkBusy = bulkStatus.isPending || bulkDelete.isPending || bulkRestore.isPending

  function toggleSelected(id: number) {
    const next = new Set(selectedIds)
    if (!next.delete(id)) next.add(id)
    setSelectedIds(next)
  }

  function toggleSelectedPage(select: boolean) {
    const next = new Set(selectedIds)
    for (const todo of todos) {
      if (select) next.add(todo.id)
      else next.delete(todo.id)
    }
    setSelectedIds(next)
  }

  /** Runs a batch, reports it in one line, and drops the ids that were applied. */
  async function runBulk(verb: string, action: () => Promise<import('./types').BulkResult>) {
    setMessage(null)
    try {
      const result = await action()
      announceChange()
      setMessage({
        tone: result.failed.length ? 'error' : 'info',
        text: describeBulkResult(verb, result),
      })
      const next = new Set(selectedIds)
      for (const id of result.succeeded) next.delete(id)
      setSelectedIds(next)
    } catch (error) {
      setMessage({ tone: 'error', text: describeError(error) })
    }
  }

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
      announceChange()
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
      announceChange()
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
      announceChange()
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
      announceChange()
      setMessage({ tone: 'info', text: `Deleted “${todo.name}”. Find it under “Show deleted only” to restore it.` })
    } catch (error) {
      setMessage({ tone: 'error', text: describeError(error) })
    }
  }

  async function undelete(todo: Todo) {
    setMessage(null)
    try {
      await restore.mutateAsync(todo.id)
      announceChange()
    } catch (error) {
      setMessage({ tone: 'error', text: describeError(error) })
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>TODOs</h1>
        <div className="header-actions">
          <span className="muted small">
            Signed in as <strong>{user?.displayName}</strong>
            {lastCheckedAt > 0 && (
              <> · synced {new Date(lastCheckedAt).toLocaleTimeString()}</>
            )}
          </span>
          <button type="button" onClick={() => { setCreating(true); setEditing(null) }}>
            New TODO
          </button>
          <button type="button" className="secondary" onClick={() => void signOut()}>
            Sign out
          </button>
        </div>
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

      <FilterBar
        filters={filters}
        currentUserId={user?.id ?? 0}
        onChange={applyFilters}
        onReset={() => applyFilters(EMPTY_FILTERS)}
      />

      <BulkActionBar
        selected={selectedOnPage}
        selectedCount={selectedIds.size}
        busy={bulkBusy}
        onChangeStatus={(status) =>
          void runBulk(
            'updated',
            () =>
              bulkStatus.mutateAsync({
                status,
                items: selectedOnPage.map((todo) => ({ id: todo.id, version: todo.version })),
              }),
          )
        }
        onDelete={() => void runBulk('deleted', () => bulkDelete.mutateAsync(selectedOnPage.map((t) => t.id)))}
        onRestore={() => void runBulk('restored', () => bulkRestore.mutateAsync(selectedOnPage.map((t) => t.id)))}
        onClear={() => setSelectedIds(new Set())}
      />

      {list.isError && <p className="banner error">{describeError(list.error)}</p>}

      <TodoTable
        todos={todos}
        currentUserId={user?.id ?? 0}
        selectedIds={selectedIds}
        expandedId={expandedId}
        sortKey={sortKey}
        sortDirection={sortDirection}
        busyId={busyId}
        onToggleSelect={toggleSelected}
        onToggleSelectPage={toggleSelectedPage}
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
