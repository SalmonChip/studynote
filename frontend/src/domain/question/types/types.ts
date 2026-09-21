import { UserNote, UserQuestionStatus } from './service.ts'
export enum QuestionDifficulty {
  Easy = 1,
  Medium = 2,
  Hard = 3,
}
export interface QuestionEntity {
  questionId: number
  categoryId: number
  title: string
  difficulty: QuestionDifficulty
  examPoint?: string | undefined
  viewCount: number
  createdAt: string
  updatedAt: string
}
export type QuestionVO = Omit<QuestionEntity, 'createdAt' | 'updatedAt'>
export type QuestionSummary = Pick<QuestionEntity, 'questionId' | 'title'>
export type QuestionDetail = Omit<QuestionEntity, 'createdAt' | 'updatedAt'>
export type QuestionWithUserStatus = Omit<
  QuestionEntity,
  'createdAt' | 'updatedAt' | 'categoryId'
> & {
  userQuestionStatus: UserQuestionStatus
}
export type QuestionWithUserNote = Omit<
  QuestionEntity,
  'questionId' | 'createdAt' | 'updatedAt' | 'categoryId'
> & {
  userNote: UserNote
}
