import { QuestionDetail, QuestionSummary } from '../../question/types/types'
import { UserQuestionStatus } from '../../question/types/service.ts'
export enum QuestionListType {
  COMMON_TYPE = 1,
  TRAINING_CAMP_TYPE = 2,
}
export enum QuestionListParentNode {
  COMMON = -1,
  TRAINING_CAMP = -2,
}
export interface QuestionListEntity {
  questionListId: number
  name: string
  type: QuestionListType
  description: string
  createdAt: string
  updatedAt: string
}
export interface QuestionListItemEntity {
  questionListId: number
  questionId: number
  rank: number
  createdAt: string
  updatedAt: string
}
export interface QuestionListCategory {
  key: number
  title: string
  questionListId: number | undefined
  children: QuestionListCategory[] | undefined
}
export interface QuestionListItemVO {
  questionListId: number
  question: QuestionSummary
  rank: number
}
export interface QuestionListItemUserVO {
  questionListId: number
  question: QuestionDetail
  rank: number
  userQuestionStatus: UserQuestionStatus
}
export interface CreateOrUpDateQuestionListBody {
  name: string
  description: string
  type: number
}
export interface SortQuestionListItemBody {
  questionListId: number
  questionIds: number[]
}
export interface QuestionListItemQueryParams {
  questionListId: number | undefined
  page: number
  pageSize: number
}
export type QuestionListOptType = 'create' | 'update'
