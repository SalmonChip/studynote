import { ApiList } from '../../../request'
export const adminQuestionApiList: ApiList = {
  getQuestionList: ['GET', '/api/admin/questions'],
  createQuestion: ['POST', '/api/admin/questions'],
  createQuestionBatch: ['POST', '/api/admin/questions/batch'],
  updateQuestion: ['PATCH', '/api/admin/questions/{questionId}'],
  deleteQuestion: ['DELETE', '/api/admin/questions/{questionId}'],
}
export const questionApiList: ApiList = {
  getQuestionList: ['GET', '/api/questions'],
  getQuestionById: ['GET', '/api/questions/{questionId}'],
  searchQuestion: ['POST', '/api/questions/search'],
}
