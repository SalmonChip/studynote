import { ApiList } from '../../../request'
export const adminQuestionListApi: ApiList = {
  getQuestionList: ['GET', '/api/admin/questionlists/{questionListId}'],
  getQuestionLists: ['GET', '/api/admin/questionlists'],
  createQuestionList: ['POST', '/api/admin/questionlists'],
  deleteQuestionList: ['DELETE', '/api/admin/questionlists/{questionListId}'],
  updateQuestionList: ['PATCH', '/api/admin/questionlists/{questionListId}'],
  getQuestionListItems: ['GET', '/api/admin/questionlist-items/{questionListId}'],
  createQuestionListItem: ['POST', '/api/admin/questionlist-items'],
  deleteQuestionListItem: ['DELETE', '/api/admin/questionlist-items/{questionListId}/{questionId}'],
  sortQuestionListItems: ['PATCH', '/api/admin/questionlist-items/sort'],
}
export const questionListApi: ApiList = {
  getQuestionListItems: ['GET', '/api/questionlist-items'],
}
