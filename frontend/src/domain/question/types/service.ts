import { SortOrder } from '../../../base/types'
import { QuestionEntity } from './types.ts'
export type QuestionSort = 'view' | 'difficulty'
export type QuestionQueryParams = {
  categoryId?: number
  sort?: QuestionSort
  order?: SortOrder
  page: number
  pageSize: number
}
export type CreateQuestionBody = Omit<
  QuestionEntity,
  'questionId' | 'createdAt' | 'updatedAt' | 'viewCount'
>
export type CreateQuestionResponse = {
  questionId: number
}
export type CreateQuestionBatchBody = {
  markdown: string
}
export type UpdateQuestionBody = Partial<CreateQuestionBody> & {
  questionId: number
}
export type QuestionOptMode = 'update' | 'create'
export interface UserQuestionStatus {
  finished: boolean
}
export type UserNote = {
  noteId: number
  content: string
} & UserQuestionStatus
