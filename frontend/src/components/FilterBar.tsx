import { PRIORITIES, STATUSES, STATUS_LABELS } from '../types'
import type { TodoFilters, TodoPriority, TodoStatus } from '../types'

interface Props {
  filters: TodoFilters
  onChange: (filters: TodoFilters) => void
  onReset: () => void
}

function toggle<T>(values: T[], value: T): T[] {
  return values.includes(value) ? values.filter((v) => v !== value) : [...values, value]
}

export function FilterBar({ filters, onChange, onReset }: Props) {
  const set = (patch: Partial<TodoFilters>) => onChange({ ...filters, ...patch })

  return (
    <section className="filters" aria-label="Filters">
      <div className="filter-group">
        <label htmlFor="search">Search</label>
        <input
          id="search"
          type="search"
          placeholder="Name contains…"
          value={filters.search}
          onChange={(e) => set({ search: e.target.value })}
        />
      </div>

      <fieldset className="filter-group">
        <legend>Status</legend>
        <div className="chips">
          {STATUSES.map((status: TodoStatus) => (
            <label key={status} className={filters.status.includes(status) ? 'chip chip-on' : 'chip'}>
              <input
                type="checkbox"
                checked={filters.status.includes(status)}
                onChange={() => set({ status: toggle(filters.status, status) })}
              />
              {STATUS_LABELS[status]}
            </label>
          ))}
        </div>
      </fieldset>

      <fieldset className="filter-group">
        <legend>Priority</legend>
        <div className="chips">
          {PRIORITIES.map((priority: TodoPriority) => (
            <label key={priority} className={filters.priority.includes(priority) ? 'chip chip-on' : 'chip'}>
              <input
                type="checkbox"
                checked={filters.priority.includes(priority)}
                onChange={() => set({ priority: toggle(filters.priority, priority) })}
              />
              {priority}
            </label>
          ))}
        </div>
      </fieldset>

      <div className="filter-group">
        <label htmlFor="dueFrom">Due from</label>
        <input id="dueFrom" type="date" value={filters.dueFrom} onChange={(e) => set({ dueFrom: e.target.value })} />
      </div>

      <div className="filter-group">
        <label htmlFor="dueTo">Due to</label>
        <input id="dueTo" type="date" value={filters.dueTo} onChange={(e) => set({ dueTo: e.target.value })} />
      </div>

      <div className="filter-group">
        <label htmlFor="blocked">Dependencies</label>
        <select
          id="blocked"
          value={filters.blocked}
          onChange={(e) => set({ blocked: e.target.value as TodoFilters['blocked'] })}
        >
          <option value="">Any</option>
          <option value="true">Blocked</option>
          <option value="false">Unblocked</option>
        </select>
      </div>

      <div className="filter-group">
        <label className="inline">
          <input
            type="checkbox"
            checked={filters.deletedOnly}
            onChange={(e) => set({ deletedOnly: e.target.checked })}
          />
          Show deleted only
        </label>
      </div>

      <button type="button" className="link" onClick={onReset}>
        Reset filters
      </button>
    </section>
  )
}
