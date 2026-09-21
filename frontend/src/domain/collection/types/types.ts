export interface CollectionEntity {
  collectionId: number
  name: string
  description: string
  creatorId: string
  createdAt: string
  updatedAt: string
}
interface NoteStatus {
  noteId: number
  isCollected: boolean
}
export type CollectionVO = Omit<CollectionEntity, 'createdAt' | 'updatedAt' | 'creatorId'> & {
  noteStatus?: NoteStatus
}
export interface CollectionQueryParams {
  noteId: number | undefined
  creatorId: string | undefined
}
export interface CreateCollectionBody {
  name: string
  description?: string
}
export interface UpdateItem {
  collectionId: number
  action: 'delete' | 'create'
}
export interface BatchUpdateCollectionBody {
  noteId: number
  collections: UpdateItem[]
}
