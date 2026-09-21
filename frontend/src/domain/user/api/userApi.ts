import { ApiList } from '../../../request'
export const userApiList: ApiList = {
  register: ['POST', '/api/users'],
  login: ['POST', '/api/users/login'],
  logout: ['POST', '/api/users/logout'],
  whoami: ['POST', '/api/users/whoami'],
  getUser: ['GET', '/api/users/{userId}'],
  updateMe: ['PATCH', '/api/users/me'],
  uploadImage: ['POST', '/api/upload/image'],
  sendVerifyCode: ['GET', '/api/email/verify-code'],
}
export const adminUserApiList: ApiList = {
  getUserList: ['GET', '/api/admin/users'],
}
