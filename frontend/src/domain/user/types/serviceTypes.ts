export type RegisterBody = {
  username: string
  account: string
  password: string
  email: string
  verifyCode: string
}
export type RegisterData = {
  userId: string
}
export type LoginBody = {
  account?: string
  email?: string
  password: string
}
export type SendVerifyCodeBody = {
  email: string
  type: 'REGISTER' | 'RESET_PASSWORD'
}
export type UploadImageData = {
  url: string
}
export type UserListQueryParams = {
  userId?: string
  username?: string
  account?: string
  isAdmin?: number
  isBanned?: number
  page: number
  pageSize: number
}
