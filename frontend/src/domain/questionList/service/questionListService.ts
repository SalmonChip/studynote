import { httpClient } from '../../../request'
import { adminQuestionListApi, questionListApi } from '../api/questionListApi.ts'
import {
  CreateOrUpDateQuestionListBody,
  QuestionListEntity,
  QuestionListItemQueryParams,
  QuestionListItemUserVO,
  QuestionListItemVO,
  SortQuestionListItemBody,
} from '../types/types.ts'
export const adminQuestionListService = {
  getQuestionListByIdService: (questionListId: number) => {
    return httpClient.request<QuestionListEntity>(adminQuestionListApi.getQuestionList, {
      pathParams: [questionListId],
    })
  },
  getQuestionListService: () => {
    return httpClient.request<QuestionListEntity[]>(adminQuestionListApi.getQuestionLists)
  },
  createQuestionListService: (params: CreateOrUpDateQuestionListBody) => {
    return httpClient.request<{
      questionListId: number
    }>(adminQuestionListApi.createQuestionList, {
      body: params,
    })
  },
  deleteQuestionListService: (questionListId: number) => {
    return httpClient.request<{
      questionListId: number
    }>(adminQuestionListApi.deleteQuestionList, {
      pathParams: [questionListId],
    })
  },
  updateQuestionListService: (questionListId: number, params: CreateOrUpDateQuestionListBody) => {
    return httpClient.request<null>(adminQuestionListApi.updateQuestionList, {
      pathParams: [questionListId],
      body: params,
    })
  },
  getQuestionListItemService: (questionListId: number) => {
    return httpClient.request<QuestionListItemVO[]>(adminQuestionListApi.getQuestionListItems, {
      pathParams: [questionListId],
    })
  },
  createQuestionListItemService: (questionListId: number, questionId: number) => {
    return httpClient.request<{
      rank: number
    }>(adminQuestionListApi.createQuestionListItem, {
      body: {
        questionListId,
        questionId,
      },
    })
  },
  deleteQuestionListItemService: (questionListId: number, questionId: number) => {
    return httpClient.request(adminQuestionListApi.deleteQuestionListItem, {
      pathParams: [questionListId, questionId],
    })
  },
  sortQuestionListItemService: (body: SortQuestionListItemBody) => {
    return httpClient.request(adminQuestionListApi.sortQuestionListItems, {
      body: body,
    })
  },
}
export const userQuestionListService = {
  getQuestionListByIdService: (query: QuestionListItemQueryParams) => {
    return httpClient.request<QuestionListItemUserVO[]>(questionListApi.getQuestionListItems, {
      queryParams: query,
    })
  },
}
