import { SortOrder } from '../../../base/types'
import { Author, NoteEntity, UserActions } from './types.ts'
import { QuestionSummary } from '../../question/types/types'
export interface NoteQueryParams {
  questionId?: number
  authorId?: string
  collectionId?: number
  sort?: 'create'
  order?: SortOrder
  recentDays?: number
  page: number
  pageSize: number
}
export type NoteWithRelations = Omit<NoteEntity, 'authorId' | 'questionId' | 'updatedAt'> & {
  needCollapsed: boolean
  displayContent: string
  author: Author
  question: QuestionSummary
  userActions: UserActions | undefined
}
export interface CreateNoteParams {
  content: string
  questionId: number
}
export interface NoteRankListItem {
  userId: string
  username: string
  avatarUrl: string
  noteCount: number
  rank: number
}
export interface NoteHeatMapItem {
  date: Date
  count: number
  rank: number
}
export interface NoteTop3Count {
  lastMonthTop3Count: number
  thisMonthTop3Count: number
}
export interface DownloadNote {
  markdown: string
}
