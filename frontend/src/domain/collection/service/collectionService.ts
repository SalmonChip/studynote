import { httpClient } from '../../../request'
import { collectionApiList } from '../api/collectionApiList.ts'
import {
  BatchUpdateCollectionBody,
  CollectionQueryParams,
  CollectionVO,
  CreateCollectionBody,
} from '../types/types.ts'
export const collectionService = {
  getCollectionListService: (query: CollectionQueryParams) => {
    return httpClient.request<CollectionVO[]>(collectionApiList.getCollectionList, {
      queryParams: query,
    })
  },
  createCollectionService: (body: CreateCollectionBody) => {
    return httpClient.request<{
      collectionId: number
    }>(collectionApiList.createCollection, {
      body: body,
    })
  },
  deleteCollectionService: (collectionId: number) => {
    return httpClient.request(collectionApiList.deleteCollection, {
      pathParams: [collectionId],
    })
  },
  batchUpdateCollectionService: (body: BatchUpdateCollectionBody) => {
    return httpClient.request(collectionApiList.batchUpdateCollection, {
      body: body,
    })
  },
}
