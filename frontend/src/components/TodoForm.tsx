import { useState } from 'react'

import type { TodoPayload } from '../api/todos'
import { PRIORITIES, RECURRENCE_TYPES } from '../types'
import type { Todo, TodoPriority, RecurrenceType } from '../types'

interface Props {
  initial?: Todo
  busy: boolean
  error: string | null
  onSubmit: (payload: TodoPayload) => void
  onCancel: () => void
}

export function TodoForm({ initial, busy, error, onSubmit, onCancel }: Props) {
  const [name, setName] = useState(initial?.name ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [dueDate, setDueDate] = useState(initial?.dueDate ?? '')
  const [priority, setPriority] = useState<TodoPriority>(initial?.priority ?? 'MEDIUM')
  const [recurrenceType, setRecurrenceType] = useState<RecurrenceType>(initial?.recurrence.type ?? 'NONE')
  const [interval, setInterval] = useState<string>(initial?.recurrence.interval?.toString() ?? '')

  const recurs = recurrenceType !== 'NONE'

  function submit(event: React.FormEvent) {
    event.preventDefault()
    onSubmit({
      name: name.trim(),
      description: description.trim() ? description.trim() : null,
      dueDate: dueDate || null,
      priority,
      recurrence: recurs ? { type: recurrenceType, interval: interval ? Number(interval) : null } : null,
    })
  }

  return (
    <form className="todo-form" onSubmit={submit}>
      <h2>{initial ? `Edit “${initial.name}”` : 'New TODO'}</h2>

      {error && <p className="error" role="alert">{error}</p>}

      <label>
        Name
        <input value={name} onChange={(e) => setName(e.target.value)} maxLength={200} required autoFocus />
      </label>

      <label>
        Description
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} maxLength={2000} rows={3} />
      </label>

      <div className="row">
        <label>
          Due date
          <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
        </label>

        <label>
          Priority
          <select value={priority} onChange={(e) => setPriority(e.target.value as TodoPriority)}>
            {PRIORITIES.map((p) => (
              <option key={p} value={p}>{p}</option>
            ))}
          </select>
        </label>
      </div>

      <div className="row">
        <label>
          Repeats
          <select value={recurrenceType} onChange={(e) => setRecurrenceType(e.target.value as RecurrenceType)}>
            {RECURRENCE_TYPES.map((type) => (
              <option key={type} value={type}>{type === 'NONE' ? 'Does not repeat' : type}</option>
            ))}
          </select>
        </label>

        {recurs && (
          <label>
            {recurrenceType === 'CUSTOM' ? 'Every N days' : 'Every N periods'}
            <input
              type="number"
              min={1}
              max={365}
              value={interval}
              placeholder={recurrenceType === 'CUSTOM' ? 'required' : '1'}
              onChange={(e) => setInterval(e.target.value)}
              required={recurrenceType === 'CUSTOM'}
            />
          </label>
        )}
      </div>

      {recurs && !dueDate && (
        <p className="hint">A repeating TODO needs a due date so the next occurrence can be scheduled.</p>
      )}

      <div className="actions">
        <button type="submit" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        <button type="button" className="secondary" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}
