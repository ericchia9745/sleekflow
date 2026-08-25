import { useState } from 'react'

import { describeError } from '../api/todos'
import { useAddDependency, useRemoveDependency, useTodoSearch } from '../hooks/useTodos'
import { STATUS_LABELS } from '../types'
import type { Todo } from '../types'

interface Props {
  todo: Todo
}

/**
 * Dependencies are searched rather than listed in a dropdown: with thousands of
 * TODOs, a select of "the first N" is not a usable way to find one.
 */
export function DependencyPanel({ todo }: Props) {
  const [term, setTerm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const candidates = useTodoSearch(term, term.trim().length > 0)
  const add = useAddDependency()
  const remove = useRemoveDependency()

  const alreadyLinked = new Set(todo.dependencies.map((d) => d.id))

  async function link(dependsOnId: number) {
    setError(null)
    try {
      await add.mutateAsync({ id: todo.id, dependsOnId })
      setTerm('')
    } catch (err) {
      setError(describeError(err))
    }
  }

  async function unlink(dependsOnId: number) {
    setError(null)
    try {
      await remove.mutateAsync({ id: todo.id, dependsOnId })
    } catch (err) {
      setError(describeError(err))
    }
  }

  return (
    <div className="dependency-panel">
      <h4>Depends on</h4>
      {error && <p className="error" role="alert">{error}</p>}

      {todo.dependencies.length === 0 ? (
        <p className="muted">Nothing — this TODO is free to start.</p>
      ) : (
        <ul className="dependency-list">
          {todo.dependencies.map((dependency) => (
            <li key={dependency.id}>
              <span className={dependency.status === 'COMPLETED' ? 'done' : 'outstanding'}>
                #{dependency.id} {dependency.name}
                {dependency.deleted ? ' (deleted)' : ` — ${STATUS_LABELS[dependency.status]}`}
              </span>
              <button type="button" className="link" onClick={() => unlink(dependency.id)}>
                remove
              </button>
            </li>
          ))}
        </ul>
      )}

      <label className="add-dependency">
        Add a dependency
        <input
          type="search"
          value={term}
          placeholder="Search by name…"
          onChange={(e) => setTerm(e.target.value)}
        />
      </label>

      {term.trim() && (
        <ul className="candidates">
          {(candidates.data?.content ?? [])
            .filter((candidate) => candidate.id !== todo.id && !alreadyLinked.has(candidate.id))
            .map((candidate) => (
              <li key={candidate.id}>
                <button type="button" className="link" onClick={() => link(candidate.id)}>
                  + #{candidate.id} {candidate.name}
                </button>
              </li>
            ))}
          {candidates.isFetched && (candidates.data?.content.length ?? 0) === 0 && (
            <li className="muted">No matches.</li>
          )}
        </ul>
      )}
    </div>
  )
}
