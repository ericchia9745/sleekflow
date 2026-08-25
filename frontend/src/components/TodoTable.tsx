import { Fragment } from 'react'

import { STATUSES, STATUS_LABELS } from '../types'
import type { SortDirection, SortKey, Todo, TodoStatus } from '../types'
import { DependencyPanel } from './DependencyPanel'

interface Props {
  todos: Todo[]
  expandedId: number | null
  sortKey: SortKey
  sortDirection: SortDirection
  busyId: number | null
  onSort: (key: SortKey) => void
  onToggleExpand: (id: number) => void
  onStatusChange: (todo: Todo, status: TodoStatus) => void
  onEdit: (todo: Todo) => void
  onDelete: (todo: Todo) => void
  onRestore: (todo: Todo) => void
}

const COLUMNS: { key: SortKey; label: string }[] = [
  { key: 'name', label: 'Name' },
  { key: 'status', label: 'Status' },
  { key: 'priority', label: 'Priority' },
  { key: 'dueDate', label: 'Due' },
]

function overdue(todo: Todo): boolean {
  if (!todo.dueDate || todo.status === 'COMPLETED' || todo.status === 'ARCHIVED') return false
  return todo.dueDate < new Date().toISOString().slice(0, 10)
}

export function TodoTable(props: Props) {
  const { todos, sortKey, sortDirection, onSort } = props

  if (todos.length === 0) {
    return <p className="empty">No TODOs match these filters.</p>
  }

  return (
    <table className="todo-table">
      <thead>
        <tr>
          <th aria-label="Expand" />
          {COLUMNS.map((column) => (
            <th key={column.key}>
              <button type="button" className="sort" onClick={() => onSort(column.key)}>
                {column.label}
                {sortKey === column.key && <span aria-hidden> {sortDirection === 'asc' ? '▲' : '▼'}</span>}
              </button>
            </th>
          ))}
          <th>Repeats</th>
          <th>Blocked</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {todos.map((todo) => {
          const expanded = props.expandedId === todo.id
          const busy = props.busyId === todo.id
          return (
            <Fragment key={todo.id}>
              <tr className={todo.deletedAt ? 'deleted' : undefined}>
                <td>
                  <button
                    type="button"
                    className="link"
                    aria-expanded={expanded}
                    onClick={() => props.onToggleExpand(todo.id)}
                  >
                    {expanded ? '▾' : '▸'}
                  </button>
                </td>
                <td>
                  <span className="name">{todo.name}</span>
                  {todo.recurrenceSourceId && <span className="tag" title="Part of a repeating series">series</span>}
                  {todo.description && <div className="muted small">{todo.description}</div>}
                </td>
                <td>
                  <select
                    value={todo.status}
                    disabled={busy || Boolean(todo.deletedAt)}
                    onChange={(e) => props.onStatusChange(todo, e.target.value as TodoStatus)}
                  >
                    {STATUSES.map((status) => (
                      <option key={status} value={status}>{STATUS_LABELS[status]}</option>
                    ))}
                  </select>
                </td>
                <td><span className={`priority priority-${todo.priority.toLowerCase()}`}>{todo.priority}</span></td>
                <td className={overdue(todo) ? 'overdue' : undefined}>
                  {todo.dueDate ?? <span className="muted">—</span>}
                </td>
                <td>
                  {todo.recurrence.type === 'NONE'
                    ? <span className="muted">—</span>
                    : `${todo.recurrence.type}${todo.recurrence.interval && todo.recurrence.interval > 1 ? ` ×${todo.recurrence.interval}` : ''}`}
                </td>
                <td>
                  {todo.blocked
                    ? <span className="tag tag-blocked" title="Waiting on a dependency">blocked</span>
                    : <span className="muted">—</span>}
                </td>
                <td className="actions-cell">
                  {todo.deletedAt ? (
                    <button type="button" onClick={() => props.onRestore(todo)}>Restore</button>
                  ) : (
                    <>
                      <button type="button" onClick={() => props.onEdit(todo)}>Edit</button>
                      <button type="button" className="danger" onClick={() => props.onDelete(todo)}>Delete</button>
                    </>
                  )}
                </td>
              </tr>
              {expanded && (
                <tr className="detail-row">
                  <td colSpan={8}>
                    <DependencyPanel todo={todo} />
                  </td>
                </tr>
              )}
            </Fragment>
          )
        })}
      </tbody>
    </table>
  )
}
