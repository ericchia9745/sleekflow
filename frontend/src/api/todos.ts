import { apiClient } from './client'
import type {
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
