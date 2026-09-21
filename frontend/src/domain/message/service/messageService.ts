import { httpClient } from '@/request'
import { messageApi } from '@/domain/message/api/messageApi.ts'
import { Message } from '@/domain/message/types.ts'
export const messageService = {
  getMessages: () => {
    return httpClient.request<Message[]>(messageApi.messages, {})
  },
  getUnreadCount: () => {
    return httpClient.request<number>(messageApi.unreadCount, {})
  },
  readMessages: (messageIds: number[]) => {
    return httpClient.request<null>(messageApi.readMessageBatch, {
      body: {
        messageIds,
      },
    })
  },
  readAllMessages: () => {
    return httpClient.request<null>(messageApi.readAll, {})
  },
  deleteMessage: (messageId: number) => {
    return httpClient.request<null>(messageApi.deleteMessage, {
      pathParams: [messageId],
    })
  },
}
