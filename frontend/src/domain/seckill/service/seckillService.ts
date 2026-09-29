import { httpClient } from '../../../request'
import { adminCourseApi, adminSeckillApi, seckillApi } from '../api/seckillApi.ts'
import type {
  CourseVO,
  CreateCourseBody,
  CreateCourseVO,
  CreateSeckillActivityBody,
  CreateSeckillActivityVO,
  SeckillActivityDetailVO,
  SeckillActivityVO,
  SeckillOrderVO,
  UpdateCourseBody,
  UpdateSeckillActivityBody,
  UserCourseVO,
} from '../types/types.ts'

export const adminCourseService = {
  getCoursesService: () => {
    return httpClient.request<CourseVO[]>(adminCourseApi.getCourses)
  },
  createCourseService: (body: CreateCourseBody) => {
    return httpClient.request<CreateCourseVO>(adminCourseApi.createCourse, { body })
  },
  updateCourseService: (courseId: number, body: UpdateCourseBody) => {
    return httpClient.request<null>(adminCourseApi.updateCourse, {
      pathParams: [courseId],
      body,
    })
  },
  deleteCourseService: (courseId: number) => {
    return httpClient.request<null>(adminCourseApi.deleteCourse, {
      pathParams: [courseId],
    })
  },
}

export const adminSeckillService = {
  getActivitiesService: () => {
    return httpClient.request<SeckillActivityVO[]>(adminSeckillApi.getActivities)
  },
  createActivityService: (body: CreateSeckillActivityBody) => {
    return httpClient.request<CreateSeckillActivityVO>(adminSeckillApi.createActivity, { body })
  },
  updateActivityService: (activityId: number, body: UpdateSeckillActivityBody) => {
    return httpClient.request<null>(adminSeckillApi.updateActivity, {
      pathParams: [activityId],
      body,
    })
  },
  deleteActivityService: (activityId: number) => {
    return httpClient.request<null>(adminSeckillApi.deleteActivity, {
      pathParams: [activityId],
    })
  },
  // 预热：库存灌进 Redis
  warmUpService: (activityId: number) => {
    return httpClient.request<null>(adminSeckillApi.warmUp, {
      pathParams: [activityId],
    })
  },
}

export const userSeckillService = {
  /** 不需要登录 */
  getActivitiesService: () => {
    return httpClient.request<SeckillActivityDetailVO[]>(seckillApi.getActivities)
  },
  // 抢购，异步落库，成功无 orderId
  seckillService: (activityId: number) => {
    return httpClient.request<null>(seckillApi.seckill, {
      pathParams: [activityId],
    })
  },
  /** 我的订单 */
  getOrdersService: () => {
    return httpClient.request<SeckillOrderVO[]>(seckillApi.getOrders)
  },
  /** 支付 */
  payService: (orderId: number) => {
    return httpClient.request<null>(seckillApi.pay, {
      pathParams: [orderId],
    })
  },
}

/** 已购课程 */
export const userCourseService = {
  getMyCoursesService: () => {
    return httpClient.request<UserCourseVO[]>(seckillApi.getMyCourses)
  },
}
