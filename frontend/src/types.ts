export type TodoStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'ARCHIVED'
export type TodoPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export type RecurrenceType = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM'

export const STATUSES: TodoStatus[] = ['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED']
export const PRIORITIES: TodoPriority[] = ['HIGH', 'MEDIUM', 'LOW']
export const RECURRENCE_TYPES: RecurrenceType[] = ['NONE', 'DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM']

export const STATUS_LABELS: Record<TodoStatus, string> = {
  NOT_STARTED: 'Not started',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  ARCHIVED: 'Archived',
}

export interface Recurrence {
  type: RecurrenceType
  interval: number | null
}

/** Who created a TODO. The list is shared, so this is attribution. */
export interface TodoOwner {
  id: number
  username: string | null
  displayName: string
}

export interface TodoSummary {
  id: number
  name: string
  status: TodoStatus
  deleted: boolean
}

export interface Todo {
  id: number
  name: string
  description: string | null
  dueDate: string | null
  status: TodoStatus
  priority: TodoPriority
  recurrence: Recurrence
  recurrenceSourceId: number | null
  dependencies: TodoSummary[]
  owner: TodoOwner
  blocked: boolean
  completedAt: string | null
  deletedAt: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export interface PagedResponse<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export interface StatusChangeResult {
  todo: Todo
  nextOccurrence: Todo | null
}

/** RFC 9457 problem response, as returned by the API for every failure. */
export interface ProblemDetail {
  type: string
  title: string
  status: number
  detail: string
  outstandingDependencies?: string[]
  expectedVersion?: number
  actualVersion?: number
  errors?: { field: string; message: string }[]
}

export type SortKey = 'dueDate' | 'priority' | 'status' | 'name' | 'createdAt'
export type SortDirection = 'asc' | 'desc'

export interface TodoFilters {
  status: TodoStatus[]
  priority: TodoPriority[]
  dueFrom: string
  dueTo: string
  /** A user id to narrow the shared list to one person's TODOs, or '' for everyone's. */
  owner: number | ''
  blocked: '' | 'true' | 'false'
  search: string
  deletedOnly: boolean
}

export const EMPTY_FILTERS: TodoFilters = {
  status: [],
  priority: [],
  dueFrom: '',
  dueTo: '',
  owner: '',
  blocked: '',
  search: '',
  deletedOnly: false,
}

export interface AuthenticatedUser {
  id: number
  username: string
  displayName: string
  createdAt: string
}
