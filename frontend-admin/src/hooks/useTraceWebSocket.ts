import { useEffect, useState, useRef, useCallback } from 'react'
import { Client, IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { ExecutionTraceEvent } from '@/types/trace'

interface UseTraceWebSocketOptions {
  cdpId: string
  enabled?: boolean
}

/**
 * WebSocket Hook for 执行追踪
 */
export const useTraceWebSocket = ({ cdpId, enabled = true }: UseTraceWebSocketOptions) => {
  const [events, setEvents] = useState<ExecutionTraceEvent[]>([])
  const [isConnected, setIsConnected] = useState(false)
  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    if (!enabled || !cdpId) {
      return
    }

    // 创建WebSocket客户端
    const client = new Client({
      webSocketFactory: () => new SockJS('/api/v1/trace/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setIsConnected(true)
        console.log('追踪WebSocket已连接')
        
        // 订阅CDP的追踪事件
        client.subscribe(`/topic/trace/${cdpId}`, (message: IMessage) => {
          try {
            const event: ExecutionTraceEvent = JSON.parse(message.body)
            setEvents(prev => [...prev, event])
          } catch (error) {
            console.error('解析追踪事件失败:', error)
          }
        })
      },
      onDisconnect: () => {
        setIsConnected(false)
        console.log('追踪WebSocket已断开')
      },
      onStompError: (frame) => {
        console.error('WebSocket错误:', frame)
      },
    })

    clientRef.current = client
    client.activate()

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate()
      }
    }
  }, [cdpId, enabled])

  const clearEvents = useCallback(() => {
    setEvents([])
  }, [])

  return {
    events,
    isConnected,
    clearEvents,
  }
}

