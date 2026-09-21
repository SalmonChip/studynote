export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'OPTIONS' | 'HEAD'
export type RequestTuple = [HttpMethod, string]
export type ApiList = Record<string, RequestTuple>
export type Options = {
  headers?: Record<string, string>
  body?: unknown
  queryParams?: object
  pathParams?: (string | number)[]
  signal?: AbortSignal
}
export interface Pagination {
  page: number
  pageSize: number
  total: number
}
export interface Response<T> {
  code: number
  message?: string
  msg?: string
  data: T
  pagination?: Pagination
  token?: string
}
export interface HttpClient {
  request<T>(tuple: RequestTuple, options?: Options): Promise<Response<T>>
}
