import { useEffect, useRef } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'

import { apiClient } from '../api/client'
import { openChannel, publish } from './channel'
import type { ChannelMessage } from './channel'

interface Revision {
  lastModifiedAt: string | null
  total: number
}

const POLL_INTERVAL_MS = 5000

async function fetchRevision(): Promise<Revision> {
  const { data } = await apiClient.get<Revision>('/todos/revision')
  return data
}

/**
 * Keeps this tab's TODO list current.
 *
 * Two mechanisms, because they cover different cases:
 *  - polling a small revision endpoint catches changes made by other *users*,
 *    at the cost of up to one interval of staleness;
 *  - a BroadcastChannel message catches changes made in another *tab* of this
 *    browser immediately, with no request at all.
 *
 * Both end in the same place: invalidate the TODO queries and let React Query
 * refetch whatever is actually on screen.
 */
export function useRealtimeSync(enabled: boolean) {
  const queryClient = useQueryClient()
  const lastSeen = useRef<string | null>(null)
  const channelRef = useRef<BroadcastChannel | null>(null)

  const revision = useQuery({
    queryKey: ['todos', 'revision'],
    queryFn: fetchRevision,
    enabled,
    refetchInterval: POLL_INTERVAL_MS,
    refetchIntervalInBackground: false,
  })

  useEffect(() => {
    const channel = openChannel()
    channelRef.current = channel
    if (channel) {
      channel.onmessage = (event: MessageEvent<ChannelMessage>) => {
        if (event.data.type === 'todos-changed') {
          queryClient.invalidateQueries({ queryKey: ['todos'] })
        }
      }
    }
    return () => channel?.close()
  }, [queryClient])

  useEffect(() => {
    if (!revision.data) return
    const fingerprint = `${revision.data.lastModifiedAt ?? ''}:${revision.data.total}`
    if (lastSeen.current === null) {
      // First reading is the baseline, not a change.
      lastSeen.current = fingerprint
      return
    }
    if (lastSeen.current !== fingerprint) {
      lastSeen.current = fingerprint
      queryClient.invalidateQueries({ queryKey: ['todos'], refetchType: 'active' })
    }
  }, [revision.data, queryClient])

  return {
    /** Tell sibling tabs that this one just changed something. */
    announceChange: () => publish(channelRef.current, { type: 'todos-changed', at: Date.now() }),
    lastCheckedAt: revision.dataUpdatedAt,
  }
}
