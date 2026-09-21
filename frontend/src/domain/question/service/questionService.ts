import { httpClient } from '../../../request'
import { adminQuestionApiList, questionApiList } from '../api/questionApi.ts'
import {
  QuestionEntity,
  QuestionVO,
  QuestionWithUserNote,
  QuestionWithUserStatus,
} from '../types/types.ts'
import {
  CreateQuestionBatchBody,
  CreateQuestionBody,
  CreateQuestionResponse,
  QuestionQueryParams,
  UpdateQuestionBody,
} from '../types/service.ts'
export const adminQuestionService = {
  getQuestionList: (params: QuestionQueryParams) => {
    return httpClient.request<QuestionEntity[]>(adminQuestionApiList.getQuestionList, {
      queryParams: params,
    })
  },
  createQuestionService: (body: CreateQuestionBody) => {
    return httpClient.request<CreateQuestionResponse>(adminQuestionApiList.createQuestion, {
      body: body,
    })
  },
  createQuestionBatchService: (body: CreateQuestionBatchBody) => {
    return httpClient.request<QuestionEntity[]>(adminQuestionApiList.createQuestionBatch, {
      body: body,
    })
  },
  deleteQuestionService: (questionId: number) => {
    return httpClient.request<{
      questionId: number
    }>(adminQuestionApiList.deleteQuestion, {
      pathParams: [questionId],
    })
  },
  updateQuestionService: (question: UpdateQuestionBody) => {
    return httpClient.request<null>(adminQuestionApiList.updateQuestion, {
      body: question,
      pathParams: [question.questionId],
    })
  },
}
export const questionService = {
  getQuestionListService: (params: QuestionQueryParams) => {
    return httpClient.request<QuestionWithUserStatus[]>(questionApiList.getQuestionList, {
      queryParams: params,
    })
  },
  getQuestionByIdService: (questionId: number) => {
    return httpClient.request<QuestionWithUserNote>(questionApiList.getQuestionById, {
      pathParams: [questionId],
    })
  },
  searchQuestionService: (body: { keyword: string }) => {
    return httpClient.request<QuestionVO[]>(questionApiList.searchQuestion, {
      body: body,
    })
  },
}
