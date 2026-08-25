import { STATUSES, STATUS_LABELS } from '../types'
import type { Todo, TodoStatus } from '../types'

interface Props {
  /** The selected TODOs, resolved against the page on screen. */
  selected: Todo[]
  selectedCount: number
  busy: boolean
  onChangeStatus: (status: TodoStatus) => void
  onDelete: () => void
  onRestore: () => void
  onClear: () => void
}

/**
 * Acts on the current selection.
 *
 * The count comes in separately from the resolved TODOs: a selection survives
 * paging and filtering, so the user can hold 40 TODOs selected while only 25
 * of them are on screen. Only what is on screen can be acted on, which is why
 * the bar says so rather than silently doing less than it claims.
 */
export function BulkActionBar(props: Props) {
  const { selected, selectedCount, busy } = props
  if (selectedCount === 0) return null

  const offPage = selectedCount - selected.length
  const anyDeleted = selected.some((todo) => todo.deletedAt)

  return (
    <section className="bulk-bar" aria-label="Bulk actions">
      <span>
        <strong>{selectedCount}</strong> selected
        {offPage > 0 && <span className="muted small"> ({offPage} on other pages, not acted on)</span>}
      </span>

      <label className="inline">
        Set status
        <select
          defaultValue=""
          disabled={busy || selected.length === 0}
          onChange={(e) => {
            if (e.target.value) props.onChangeStatus(e.target.value as TodoStatus)
            e.target.value = ''
          }}
        >
          <option value="" disabled>
            Choose…
          </option>
          {STATUSES.map((status) => (
            <option key={status} value={status}>
              {STATUS_LABELS[status]}
            </option>
          ))}
        </select>
      </label>

      {anyDeleted ? (
        <button type="button" disabled={busy} onClick={props.onRestore}>
          Restore
        </button>
      ) : (
        <button type="button" className="danger" disabled={busy} onClick={props.onDelete}>
          Delete
        </button>
      )}

      <button type="button" className="link" onClick={props.onClear}>
        Clear selection
      </button>
    </section>
  )
}
