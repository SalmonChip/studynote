import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import FetchClient, { AUTH_EXPIRED, TOKEN_KEY } from './fetchClient'

const response = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
describe('API transport contract', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.useRealTimers()
  })
  it('encodes paths and filters absent query values while retaining zero/false', async () => {
    vi.mocked(fetch).mockResolvedValue(response({ code: 200, data: [] }))
    localStorage.setItem(TOKEN_KEY, 'test-token')
    await new FetchClient('http://localhost:8080').request(['GET', '/api/users/{id}'], {
      pathParams: ['a/b'],
      queryParams: { page: 1, empty: '', absent: undefined, zero: 0, flag: false },
    })
    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(String(url)).toBe('http://localhost:8080/api/users/a%2Fb?page=1&zero=0&flag=false')
    expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer test-token')
    expect(init?.body).toBeUndefined()
  })
  it('preserves response pagination and token', async () => {
    const payload = {
      code: 200,
      message: 'ok',
      data: [],
      pagination: { page: 2, pageSize: 10, total: 30 },
      token: 'new-token',
    }
    vi.mocked(fetch).mockResolvedValue(response(payload))
    expect(await new FetchClient().request(['GET', '/api/notes'])).toEqual(payload)
  })
  it('serializes mutation payloads and lets the browser set multipart boundaries', async () => {
    vi.mocked(fetch).mockImplementation(async () => response({ code: 200, data: null }))
    const client = new FetchClient()
    await client.request(['PATCH', '/api/messages/batch/read'], { body: { messageIds: [1, 2] } })
    expect(vi.mocked(fetch).mock.calls[0][1]?.body).toBe('{"messageIds":[1,2]}')
    const body = new FormData()
    body.append('file', new Blob(['test']), 'test.png')
    await client.request(['POST', '/api/upload/image'], { body })
    const init = vi.mocked(fetch).mock.calls[1][1]
    expect(init?.body).toBe(body)
    expect(new Headers(init?.headers).has('Content-Type')).toBe(false)
  })
  it.each([200, 401])('expires the session for business/HTTP unauthorized (%s)', async (status) => {
    const expired = vi.fn()
    window.addEventListener(AUTH_EXPIRED, expired)
    localStorage.setItem(TOKEN_KEY, 'expired')
    vi.mocked(fetch).mockResolvedValue(response({ code: 401, message: '请登录' }, status))
    await expect(new FetchClient().request(['GET', '/api/notes'])).rejects.toThrow('请登录')
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    expect(expired).toHaveBeenCalledOnce()
    window.removeEventListener(AUTH_EXPIRED, expired)
  })
  it('rejects business errors and non-JSON gateway errors', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(response({ code: 400, message: '标题重复' }))
      .mockResolvedValueOnce(new Response('Bad gateway', { status: 502 }))
    const client = new FetchClient()
    await expect(client.request(['POST', '/api/admin/questions'])).rejects.toThrow('标题重复')
    await expect(client.request(['GET', '/api/notes'])).rejects.toThrow('502')
  })
  it('aborts hung requests after 15 seconds', async () => {
    vi.useFakeTimers()
    vi.mocked(fetch).mockImplementation(
      (_url, options) =>
        new Promise((_resolve, reject) => {
          options?.signal?.addEventListener('abort', () =>
            reject(new DOMException('Aborted', 'AbortError')),
          )
        }),
    )
    const assertion = expect(
      new FetchClient().request(['GET', '/api/notes']),
    ).rejects.toMatchObject({ name: 'AbortError' })
    await vi.advanceTimersByTimeAsync(15000)
    await assertion
  })
})
