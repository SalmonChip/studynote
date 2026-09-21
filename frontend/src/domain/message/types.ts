import { QuestionSummary } from '@/domain/question/types/types'
enum MessageType {
  LIKE = 1,
  COMMENT = 2,
  SYSTEM = 3,
}
enum TargetType {
  NOTE = 1,
  COMMENT = 2,
}
export interface Message {
  messageId: number
  sender: {
    userId: string
    username: string
    avatar: string
  }
  type: MessageType
  target?: {
    type: TargetType
    targetId: number
    question: QuestionSummary
  }
  isRead: boolean
  content: string
  createdAt: string
}
