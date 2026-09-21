import { UserEntity } from '../../user/types/types'
export type Author = Pick<UserEntity, 'userId' | 'username' | 'avatarUrl'>
export type UserActions = {
  isLiked: boolean
  isCollected: boolean
}
export interface NoteEntity {
  noteId: number
  authorId: string
  questionId: number
  content: string
  likeCount: number
  commentCount: number
  collectCount: number
  createdAt: string
  updatedAt: string
}
