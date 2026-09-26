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
    // 后端所有 VO 的头像字段都叫 avatarUrl，MessageVO.Sender 也不例外
    avatarUrl: string
  }
  type: MessageType
  target?: {
    // 目标所属的题目。后端在目标笔记或题目已被删除时会给 null。
    question: QuestionSummary
    targetId: number
    targetType: TargetType
  }
  isRead: boolean
  content: string
  createdAt: string
}
