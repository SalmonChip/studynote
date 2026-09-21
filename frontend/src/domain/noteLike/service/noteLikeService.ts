import { httpClient } from '../../../request'
import { noteLikeApiList } from '../api/noteLikeApi.ts'
export const noteLikeService = {
  likeService: (noteId: number) => {
    return httpClient.request(noteLikeApiList.like, {
      pathParams: [noteId],
    })
  },
  unLikeService: (noteId: number) => {
    return httpClient.request(noteLikeApiList.unLike, {
      pathParams: [noteId],
    })
  },
}
