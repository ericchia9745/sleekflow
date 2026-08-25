import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import * as api from '../api/todos'
import type { ListParams, TodoPayload } from '../api/todos'
import type { TodoStatus } from '../types'

const TODOS_KEY = 'todos'

export function useTodoList(params: ListParams) {
  return useQuery({
    queryKey: [TODOS_KEY, params],
    queryFn: () => api.listTodos(params),
    // Keeps the table on screen while a new page or filter loads, instead of
    // collapsing to a spinner on every keystroke.
    placeholderData: (previous) => previous,
  })
}

/** Candidate TODOs to depend on, matched by name. */
export function useTodoSearch(term: string, enabled: boolean) {
  return useQuery({
    queryKey: [TODOS_KEY, 'search', term],
    queryFn: () =>
      api.listTodos({
        page: 0,
        size: 10,
        sortKey: 'name',
        sortDirection: 'asc',
        status: [],
        priority: [],
        dueFrom: '',
        dueTo: '',
        blocked: '',
        search: term,
        deletedOnly: false,
      }),
    enabled: enabled && term.trim().length > 0,
  })
}

function useInvalidateTodos() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: [TODOS_KEY] })
}

export function useCreateTodo() {
  const invalidate = useInvalidateTodos()
  return useMutation({
    mutationFn: (payload: TodoPayload & { dependencyIds?: number[] }) => api.createTodo(payload),
    onSuccess: invalidate,
  })
}

export function useUpdateTodo() {
  const invalidate = useInvalidateTodos()
  return useMutation({
    mutationFn: ({ id, ...payload }: TodoPayload & { id: number; version: number }) =>
      api.updateTodo(id, payload),
    onSuccess: invalidate,
  })
}

export function useChangeStatus() {
  const invalidate = useInvalidateTodos()
  return useMutation({
    mutationFn: ({ id, status, version }: { id: number; status: TodoStatus; version: number }) =>
      api.changeStatus(id, status, version),
    // Refetch on failure too: a 409 means our copy is stale, so the surest fix
    // is to pull the current state rather than leave a wrong version on screen.
    onSettled: invalidate,
  })
}

export function useDeleteTodo() {
  const invalidate = useInvalidateTodos()
  return useMutation({ mutationFn: (id: number) => api.deleteTodo(id), onSuccess: invalidate })
}

export function useRestoreTodo() {
  const invalidate = useInvalidateTodos()
  return useMutation({ mutationFn: (id: number) => api.restoreTodo(id), onSuccess: invalidate })
}

export function useAddDependency() {
  const invalidate = useInvalidateTodos()
  return useMutation({
    mutationFn: ({ id, dependsOnId }: { id: number; dependsOnId: number }) =>
      api.addDependency(id, dependsOnId),
    onSuccess: invalidate,
  })
}

export function useRemoveDependency() {
  const invalidate = useInvalidateTodos()
  return useMutation({
    mutationFn: ({ id, dependsOnId }: { id: number; dependsOnId: number }) =>
      api.removeDependency(id, dependsOnId),
    onSuccess: invalidate,
  })
}
