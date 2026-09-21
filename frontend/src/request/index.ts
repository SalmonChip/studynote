import FetchClient from './fetchClient'
export type { HttpMethod, RequestTuple, Options, Response, Pagination, ApiList } from './types'
export const httpClient = new FetchClient()
