// 秒杀域类型，字段对齐后端 DTO / VO

/** 活动状态 */
export enum SeckillActivityStatus {
  OFFLINE = 0,
  ONLINE = 1,
}

// 订单状态：0 待支付 / 1 已支付 / 2 已取消
export enum SeckillOrderStatus {
  PENDING = 0,
  PAID = 1,
  CANCELLED = 2,
}

/** 课程 */
export interface CourseVO {
  courseId: number
  title: string
  description: string | null
  coverUrl: string | null
  /** 单位元 */
  price: number
  /** 1 上架 / 0 下架 */
  status: number
  /** ISO 字符串 */
  createdAt: string
  updatedAt: string
}

// 创建课程入参
export interface CreateCourseBody {
  title: string
  description?: string
  coverUrl?: string
  price: number
  // status 必须给值
  status?: number
}

/** 更新课程 */
export type UpdateCourseBody = Partial<CreateCourseBody>

/** 创建课程返回 */
export interface CreateCourseVO {
  courseId: number
}

/** 秒杀活动（管理端） */
export interface SeckillActivityVO {
  activityId: number
  courseId: number
  /** 单位元 */
  seckillPrice: number
  stock: number
  /** ISO 字符串 */
  startTime: string
  endTime: string
  /** 1 上架 / 0 下架 */
  status: number
  /** 乐观锁版本号 */
  version: number
  createdAt: string
  updatedAt: string
}

// 创建秒杀活动入参，时间格式 yyyy-MM-dd HH:mm:ss
export interface CreateSeckillActivityBody {
  courseId: number
  seckillPrice: number
  stock: number
  startTime: string
  endTime: string
  /** 必须显式传 */
  status?: number
}

/** 更新秒杀活动 */
export type UpdateSeckillActivityBody = Partial<CreateSeckillActivityBody>

/** 创建活动返回 */
export interface CreateSeckillActivityVO {
  activityId: number
}

// 以下是用户端

// 用户端秒杀活动（含课程信息），stock 是 MySQL 总量非实时剩余
export interface SeckillActivityDetailVO {
  activityId: number
  courseId: number
  courseTitle: string
  courseCoverUrl: string | null
  courseDescription: string | null
  /** 课程原价（元） */
  coursePrice: number
  /** 秒杀价（元） */
  seckillPrice: number
  /** 库存（MySQL 总量） */
  stock: number
  /** ISO 字符串 */
  startTime: string
  endTime: string
}

/** 用户端秒杀订单 */
export interface SeckillOrderVO {
  orderId: number
  activityId: number
  courseId: number
  courseTitle: string | null
  courseCoverUrl: string | null
  /** 成交价（元），下单快照 */
  price: number
  /** 见 SeckillOrderStatus */
  status: number
  createdAt: string
  paidAt: string | null
  cancelledAt: string | null
}

/** 已购课程来源 */
export enum UserCourseSource {
  /** 秒杀 */
  SECKILL = 1,
  /** 购买 */
  PURCHASE = 2,
  /** 后台发放 */
  GRANT = 3,
}

/** 已购课程 */
export interface UserCourseVO {
  courseId: number
  title: string
  description: string | null
  coverUrl: string | null
  /** 单位元 */
  price: number
  /** 见 UserCourseSource */
  source: number
  createTime: string
}
