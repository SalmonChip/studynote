import { CommentQueryParams, Comment, CreateCommentRequest } from '@/domain/comment/types.ts'
import { commentApiList } from '@/domain/comment/api/commentApi.ts'
import { httpClient } from '@/request'
export const commentService = {
  updateComment: (commentId: number, content: string) =>
    httpClient.request(['PATCH', '/api/comments/{commentId}'], {
      pathParams: [commentId],
      body: { content },
    }),
  deleteComment: (commentId: number) =>
    httpClient.request(['DELETE', '/api/comments/{commentId}'], { pathParams: [commentId] }),
  getCommentsService: (params: CommentQueryParams) => {
    return httpClient.request<Comment[]>(commentApiList.comments, {
      queryParams: params,
    })
  },
  createCommentService: (request: CreateCommentRequest) => {
    return httpClient.request<number>(commentApiList.createComment, {
      body: request,
    })
  },
  likeCommentService: (commentId: number) => {
    return httpClient.request(commentApiList.likeComment, {
      pathParams: [commentId],
    })
  },
  unlikeCommentService: (commentId: number) => {
    return httpClient.request(commentApiList.unlikeComment, {
      pathParams: [commentId],
    })
  },
}
