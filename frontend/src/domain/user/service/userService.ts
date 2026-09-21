import { httpClient } from '../../../request'
import { adminUserApiList, userApiList } from '../api/userApi.ts'
import {
  LoginBody,
  RegisterBody,
  RegisterData,
  SendVerifyCodeBody,
  UploadImageData,
  UserListQueryParams,
} from '../types/serviceTypes.ts'
import { UserEntity, UserState, UserVO } from '../types/types.ts'
export const userService = {
  registerService: (body: RegisterBody) => {
    return httpClient.request<RegisterData>(userApiList.register, {
      body: body,
    })
  },
  loginService: (body: LoginBody) => {
    return httpClient.request<UserState>(userApiList.login, {
      body: body,
    })
  },
  whoamiService: () => {
    return httpClient.request<UserState>(userApiList.whoami)
  },
  updateMeService: (body: Partial<UserState>) => {
    return httpClient.request<null>(userApiList.updateMe, {
      body: body,
    })
  },
  getUserService: (userId: string) => {
    return httpClient.request<UserVO>(userApiList.getUser, {
      pathParams: [userId],
    })
  },
  uploadImageService: (body: FormData) => {
    return httpClient.request<UploadImageData>(userApiList.uploadImage, {
      body: body,
    })
  },
  sendVerifyCode: (body: SendVerifyCodeBody) => {
    return httpClient.request<void>(userApiList.sendVerifyCode, {
      queryParams: body,
    })
  },
}
export const adminUserService = {
  getUserListService: (params: UserListQueryParams) => {
    return httpClient.request<UserEntity[]>(adminUserApiList.getUserList, {
      queryParams: params,
    })
  },
}
