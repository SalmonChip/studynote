import { ApiList } from '../../../request'

// 管理端课程接口
export const adminCourseApi: ApiList = {
  getCourses: ['GET', '/api/admin/courses'],
  createCourse: ['POST', '/api/admin/courses'],
  updateCourse: ['PATCH', '/api/admin/courses/{courseId}'],
  deleteCourse: ['DELETE', '/api/admin/courses/{courseId}'],
}

// 管理端秒杀活动接口
export const adminSeckillApi: ApiList = {
  getActivities: ['GET', '/api/admin/seckill-activities'],
  createActivity: ['POST', '/api/admin/seckill-activities'],
  updateActivity: ['PATCH', '/api/admin/seckill-activities/{activityId}'],
  deleteActivity: ['DELETE', '/api/admin/seckill-activities/{activityId}'],
  warmUp: ['POST', '/api/admin/seckill-activities/{activityId}/warm-up'],
}

// 用户端秒杀接口
export const seckillApi: ApiList = {
  getActivities: ['GET', '/api/seckill/activities'],
  seckill: ['POST', '/api/seckill/{activityId}'],
  getOrders: ['GET', '/api/seckill/orders'],
  pay: ['POST', '/api/seckill/orders/{orderId}/pay'],
  getMyCourses: ['GET', '/api/users/courses'],
}
