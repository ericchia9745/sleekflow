import { apiClient } from './client'
import type {
  BulkFailure,
  BulkResult,
  PagedResponse,
  ProblemDetail,
  StatusChangeResult,
  Todo,
  TodoFilters,
  TodoPriority,
  TodoStatus,
  Recurrence,
  SortDirection,
  SortKey,
} from '../types'

export interface TodoPayload {
  name: string
  description: string | null
  dueDate: string | null
  priority: TodoPriority
  recurrence: Recurrence | null
}

export interface ListParams extends TodoFilters {
  page: number
  size: number
  sortKey: SortKey
  sortDirection: SortDirection
}

/**
 * Only send parameters the user actually set. Sending empty ones would make the
 * request URL noisy and, for the array filters, ambiguous.
 */
function toQuery(params: ListParams): Record<string, string | number | string[]> {
  const query: Record<string, string | number | string[]> = {
    page: params.page,
    size: params.size,
    sort: `${params.sortKey},${params.sortDirection}`,
  }
  if (params.status.length) query.status = params.status
  if (params.priority.length) query.priority = params.priority
  if (params.dueFrom) query.dueFrom = params.dueFrom
  if (params.dueTo) query.dueTo = params.dueTo
  if (params.owner !== '') query.owner = params.owner
  if (params.blocked) query.blocked = params.blocked
  if (params.search.trim()) query.search = params.search.trim()
  if (params.deletedOnly) query.deletedOnly = 'true'
  return query
}

export async function listTodos(params: ListParams): Promise<PagedResponse<Todo>> {
  const { data } = await apiClient.get<PagedResponse<Todo>>('/todos', {
    params: toQuery(params),
    // Repeat the key for arrays (status=A&status=B) -- what Spring binds to a List.
    paramsSerializer: { indexes: null },
  })
  return data
}

export async function createTodo(payload: TodoPayload & { dependencyIds?: number[] }): Promise<Todo> {
  const { data } = await apiClient.post<Todo>('/todos', payload)
  return data
}

export async function updateTodo(id: number, payload: TodoPayload & { version: number }): Promise<Todo> {
  const { data } = await apiClient.put<Todo>(`/todos/${id}`, payload)
  return data
}

export async function changeStatus(
  id: number,
  status: TodoStatus,
  version: number,
): Promise<StatusChangeResult> {
  const { data } = await apiClient.patch<StatusChangeResult>(`/todos/${id}/status`, { status, version })
  return data
}

/**
 * A batch is best-effort: the response reports each item separately, so a
 * blocked or already-edited TODO does not sink the ones that did apply.
 */
export async function bulkChangeStatus(
  status: TodoStatus,
  items: { id: number; version: number }[],
): Promise<BulkResult> {
  const { data } = await apiClient.post<BulkResult>('/todos/bulk/status', { status, items })
  return data
}

export async function bulkDelete(ids: number[]): Promise<BulkResult> {
  const { data } = await apiClient.post<BulkResult>('/todos/bulk/delete', { ids })
  return data
}

export async function bulkRestore(ids: number[]): Promise<BulkResult> {
  const { data } = await apiClient.post<BulkResult>('/todos/bulk/restore', { ids })
  return data
}

/** Turns a bulk result into one line a user can act on. */
export function describeBulkResult(verb: string, result: BulkResult): string {
  const done = `${result.succeeded.length} of ${result.requested} ${verb}`
  const scheduled = result.createdOccurrences.length
    ? `, ${result.createdOccurrences.length} next occurrence(s) scheduled`
    : ''
  if (!result.failed.length) return `${done}${scheduled}.`
  const reasons = new Map<string, number>()
  for (const failure of result.failed) {
    reasons.set(REASONS[failure.type], (reasons.get(REASONS[failure.type]) ?? 0) + 1)
  }
  const skipped = [...reasons].map(([reason, count]) => `${count} ${reason}`).join(', ')
  return `${done}${scheduled}. Skipped: ${skipped}.`
}

const REASONS: Record<BulkFailure['type'], string> = {
  'stale-version': 'changed by someone else',
  'dependencies-not-satisfied': 'still blocked',
  'not-todo-owner': 'owned by someone else',
  'todo-not-found': 'no longer on the list',
}

export async function deleteTodo(id: number): Promise<void> {
  await apiClient.delete(`/todos/${id}`)
}

export async function restoreTodo(id: number): Promise<Todo> {
  const { data } = await apiClient.post<Todo>(`/todos/${id}/restore`)
  return data
}

export async function addDependency(id: number, dependsOnId: number): Promise<Todo> {
  const { data } = await apiClient.post<Todo>(`/todos/${id}/dependencies`, { dependsOnId })
  return data
}

export async function removeDependency(id: number, dependsOnId: number): Promise<Todo> {
  const { data } = await apiClient.delete<Todo>(`/todos/${id}/dependencies/${dependsOnId}`)
  return data
}

/** Turns any thrown error into something worth showing a user. */
export function describeError(error: unknown): string {
  const problem = (error as { response?: { data?: ProblemDetail } })?.response?.data
  if (!problem) {
    return error instanceof Error ? error.message : 'Something went wrong.'
  }
  if (problem.errors?.length) {
    return problem.errors.map((e) => `${e.field}: ${e.message}`).join('; ')
  }
  if (problem.outstandingDependencies?.length) {
    return `${problem.detail} Waiting on: ${problem.outstandingDependencies.join(', ')}.`
  }
  return problem.detail ?? problem.title ?? 'Request failed.'
}
