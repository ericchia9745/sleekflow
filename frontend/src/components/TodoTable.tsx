import { Fragment } from 'react'

import { STATUSES, STATUS_LABELS } from '../types'
import type { SortDirection, SortKey, Todo, TodoStatus, TodoSummary } from '../types'
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

/** How many dependency names to show before collapsing the rest into a count. */
const DEPENDENCIES_SHOWN = 2

/**
 * The prerequisites of a TODO, most relevant first.
 *
 * Outstanding ones are what actually hold the task up, so they lead; completed
 * and deleted ones are still worth showing -- they explain why a task that
 * looks blocked no longer is -- but they belong behind.
 */
function orderedDependencies(todo: Todo): TodoSummary[] {
  const weight = (dependency: TodoSummary) =>
    dependency.deleted ? 2 : dependency.status === 'COMPLETED' ? 1 : 0
  return [...todo.dependencies].sort((a, b) => weight(a) - weight(b))
}

function dependencyState(dependency: TodoSummary): string {
  if (dependency.deleted) return 'dependency dependency-deleted'
  return dependency.status === 'COMPLETED' ? 'dependency dependency-done' : 'dependency dependency-outstanding'
}

function dependencyTitle(dependency: TodoSummary): string {
  if (dependency.deleted) return `#${dependency.id} was deleted, so it no longer blocks this TODO`
  return dependency.status === 'COMPLETED'
    ? `#${dependency.id} is completed`
    : `Waiting on #${dependency.id} (${STATUS_LABELS[dependency.status]})`
}

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
          <th>Depends on</th>
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
                <td className="depends-cell">
                  {todo.dependencies.length === 0 ? (
                    <span className="muted">—</span>
                  ) : (
                    <>
                      {orderedDependencies(todo)
                        .slice(0, DEPENDENCIES_SHOWN)
                        .map((dependency) => (
                          <span
                            key={dependency.id}
                            className={dependencyState(dependency)}
                            title={dependencyTitle(dependency)}
                          >
                            #{dependency.id} {dependency.name}
                          </span>
                        ))}
                      {todo.dependencies.length > DEPENDENCIES_SHOWN && (
                        <button
                          type="button"
                          className="link small"
                          onClick={() => props.onToggleExpand(todo.id)}
                        >
                          +{todo.dependencies.length - DEPENDENCIES_SHOWN} more
                        </button>
                      )}
                    </>
                  )}
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
                  <td colSpan={9}>
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
