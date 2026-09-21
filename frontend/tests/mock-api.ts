import type { Plugin } from 'vite'
import type { IncomingMessage, ServerResponse } from 'node:http'

export function mockApi(): Plugin {
  const user = { userId: '1', account: 'demo_admin', username: '测试用户', isAdmin: 1, gender: 3, email: 'demo@example.test', birthday: '2000-01-01', avatarUrl: '', school: '测试学校', signature: '仅用于本地交互验收' }
  let categories = [{ categoryId: 1, parentCategoryId: 0, name: 'JavaScript', children: [{ categoryId: 2, parentCategoryId: 1, name: '基础', children: [] }] }]
  let questions = Array.from({ length: 14 }, (_, index) => ({ questionId: index + 1, categoryId: index > 10 ? 3 : 2, title: index ? `前端练习题 ${index + 1}` : '解释 JavaScript 闭包', difficulty: index % 3 + 1, examPoint: '作用域与函数', viewCount: 100 - index }))
  const author = { userId: '1', username: '测试用户', avatarUrl: '' }
  let notes = [{ noteId: 1, questionId: 1, content: '## 闭包\n\n函数可以保留对外层作用域的访问。\n\n```js\nconst count = () => 1\n```', likeCount: 2, commentCount: 1, collectCount: 0, createdAt: '2026-09-16T10:00:00', author, userActions: { isLiked: false, isCollected: false } }]
  let collections = [{ collectionId: 1, name: '复习清单', description: '重点题目', collected: false }]
  let comments = [{ commentId: 1, noteId: 1, parentId: undefined as number | undefined, content: '欢迎补充学习思路', author, likeCount: 0, replyCount: 0, createdAt: '2026-09-16T10:01:00', userActions: { isLiked: false } }]
  let lists = [{ questionListId: 1, name: '前端基础题单', description: '循序渐进复习基础知识', type: 1 }]
  const listItems = new Map<number, number[]>([[1, [1, 2, 3]]])
  let messages = [{ messageId: 1, sender: { ...author, avatar: '' }, type: 2, content: '你的笔记收到一条评论', isRead: false, createdAt: '2026-09-16T10:01:00', target: { type: 1, targetId: 1, question: { questionId: 1, title: questions[0].title } } }]
  let nextId = 100
  return {
    name: 'isolated-smoke-api',
    apply: 'serve',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (!req.url?.startsWith('/api/')) return next()
        void handle(req, res).catch(error => {
          res.statusCode = 500
          res.end(JSON.stringify({ code: 500, message: String(error) }))
        })
      })
    },
  }
  async function handle(req: IncomingMessage, res: ServerResponse) {
    const url = new URL(req.url!, 'http://localhost')
    const path = url.pathname
    const method = req.method
    const query = url.searchParams
    const loggedIn = req.headers.authorization === 'Bearer smoke-token'
    const chunks: Buffer[] = []
    for await (const chunk of req) chunks.push(Buffer.from(chunk))
    const text = Buffer.concat(chunks).toString()
    const body = text && req.headers['content-type']?.includes('application/json') ? JSON.parse(text) as Record<string, unknown> : {}
    const send = (data: unknown, extra = {}) => { res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify({ code: 200, message: 'mock', data, ...extra })) }
    const paged = (items: unknown[]) => { const page = Number(query.get('page') || 1); const pageSize = Number(query.get('pageSize') || 10); send(items.slice((page - 1) * pageSize, page * pageSize), { pagination: { page, pageSize, total: items.length } }) }
    const id = Number(path.split('/').at(-1))
    if (path === '/api/users/login' || path === '/api/users') {
      if (body.password !== 'demo_pass') { res.statusCode = 400; res.end(JSON.stringify({ code: 400, message: '测试密码为 demo_pass' })); return }
      return send(user, { token: 'smoke-token' })
    }
    if (path === '/api/users/whoami') return send(user, { token: 'smoke-token' })
    if (path === '/api/users/me') { Object.assign(user, body); return send(null) }
    if (/^\/api\/users\/\d+$/.test(path)) return send(user)
    if (path === '/api/admin/users') return paged([user])
    if (path === '/api/email/verify-code') return send(null)
    if (path === '/api/upload/image' || path === '/api/users/avatar') return send({ url: 'https://example.test/image.png' })
    if (path === '/api/categories' || path === '/api/admin/categories') {
      if (method === 'POST') categories.push({ categoryId: nextId++, name: String(body.name), parentCategoryId: Number(body.parentCategoryId), children: [] })
      return send(categories)
    }
    if (path.startsWith('/api/admin/categories/')) { if (method === 'DELETE') categories = categories.filter(item => item.categoryId !== id); else Object.assign(categories.find(item => item.categoryId === id) || {}, body); return send(null) }
    if (path === '/api/questions/search') return send(questions.filter(item => item.title.includes(String(body.keyword))))
    if (/^\/api\/questions\/\d+$/.test(path)) { const note = loggedIn ? notes.find(item => item.questionId === id) : undefined; return send({ ...questions.find(item => item.questionId === id), userNote: { finished: !!note, noteId: note?.noteId, content: note?.content || '' } }) }
    if (path === '/api/questions' || path === '/api/admin/questions') {
      if (method === 'POST') { const question = { questionId: nextId++, categoryId: Number(body.categoryId), title: String(body.title), difficulty: Number(body.difficulty), examPoint: String(body.examPoint || ''), viewCount: 0 }; questions.push(question); return send({ questionId: question.questionId }) }
      let items = questions.filter(item => !query.get('categoryId') || item.categoryId === Number(query.get('categoryId')) || Number(query.get('categoryId')) === 1)
      if (query.has('sort')) { const key = query.get('sort') === 'view' ? 'viewCount' : 'difficulty'; items = [...items].sort((a, b) => (a[key] - b[key]) * (query.get('order') === 'asc' ? 1 : -1)) }
      return paged(items.map(item => ({ ...item, userQuestionStatus: { finished: loggedIn && notes.some(note => note.questionId === item.questionId) } })))
    }
    if (path === '/api/admin/questions/batch') return send(null)
    if (path.startsWith('/api/admin/questions/')) { if (method === 'DELETE') questions = questions.filter(item => item.questionId !== id); else Object.assign(questions.find(item => item.questionId === id) || {}, body); return send(null) }
    if (path === '/api/notes/ranklist') return send([{ ...author, rank: 1, noteCount: notes.length }])
    if (path === '/api/notes/heatmap') return send([{ date: '2026-09-16', count: 2, rank: 1 }])
    if (path === '/api/notes/top3count') return send({ thisMonthTop3Count: 2, lastMonthTop3Count: 1 })
    if (path === '/api/notes/download') return send({ markdown: notes.map(item => item.content).join('\n\n') })
    if (path === '/api/notes') {
      if (method === 'POST') { const note = { ...notes[0], noteId: nextId++, content: String(body.content), questionId: Number(body.questionId), likeCount: 0, commentCount: 0, collectCount: 0, userActions: { isLiked: false, isCollected: false } }; notes.push(note); return send({ noteId: note.noteId }) }
      return paged(notes.filter(item => (!query.get('questionId') || item.questionId === Number(query.get('questionId'))) && (!query.get('collectionId') || item.userActions.isCollected)).map(item => ({ ...item, question: questions.find(question => question.questionId === item.questionId), needCollapsed: false, displayContent: item.content })))
    }
    if (path.startsWith('/api/notes/')) { if (method === 'DELETE') notes = notes.filter(item => item.noteId !== id); else Object.assign(notes.find(item => item.noteId === id) || {}, body); return send(null) }
    if (path.startsWith('/api/like/note/')) { const note = notes.find(item => item.noteId === id)!; note.userActions.isLiked = method === 'POST'; note.likeCount += method === 'POST' ? 1 : -1; return send(null) }
    if (path === '/api/collections/batch') {
      for (const change of body.collections as { collectionId: number; action: string }[]) { const item = collections.find(item => item.collectionId === change.collectionId); if (item) item.collected = change.action === 'create' }
      const note = notes.find(item => item.noteId === Number(body.noteId))!; note.userActions.isCollected = collections.some(item => item.collected); note.collectCount = collections.filter(item => item.collected).length; return send(null)
    }
    if (path === '/api/collections') {
      if (method === 'POST') collections.push({ collectionId: nextId++, name: String(body.name), description: String(body.description || ''), collected: false })
      return send(collections.map(item => ({ ...item, noteStatus: { noteId: Number(query.get('noteId')), isCollected: item.collected } })))
    }
    if (path.startsWith('/api/collections/')) { collections = collections.filter(item => item.collectionId !== id); return send(null) }
    if (path === '/api/comments') {
      if (method === 'POST') { const comment = { ...comments[0], commentId: nextId++, noteId: Number(body.noteId), parentId: body.parentId ? Number(body.parentId) : undefined, content: String(body.content), likeCount: 0, userActions: { isLiked: false } }; comments.push(comment); const note = notes.find(item => item.noteId === comment.noteId); if (note) note.commentCount++; return send(comment.commentId) }
      const current = comments.filter(item => item.noteId === Number(query.get('noteId')))
      return paged(current.filter(item => !item.parentId).map(item => ({ ...item, replies: current.filter(child => child.parentId === item.commentId) })))
    }
    if (path.startsWith('/api/comments/')) {
      const commentId = Number(path.split('/')[3]); const comment = comments.find(item => item.commentId === commentId)
      if (path.endsWith('/like') && comment) { comment.userActions.isLiked = method === 'POST'; comment.likeCount += method === 'POST' ? 1 : -1 }
      else if (method === 'DELETE') comments = comments.filter(item => item.commentId !== commentId)
      else Object.assign(comment || {}, body)
      return send(null)
    }
    if (path === '/api/admin/questionlists') { if (method === 'POST') lists.push({ questionListId: nextId++, name: String(body.name), description: String(body.description || ''), type: Number(body.type) }); return send(lists) }
    if (path.startsWith('/api/admin/questionlists/')) { if (method === 'DELETE') lists = lists.filter(item => item.questionListId !== id); else if (method === 'PATCH') Object.assign(lists.find(item => item.questionListId === id) || {}, body); return send(lists.find(item => item.questionListId === id)) }
    if (path === '/api/admin/questionlist-items/sort') { listItems.set(Number(body.questionListId), body.questionIds as number[]); return send(null) }
    if (path === '/api/admin/questionlist-items') { const listId = Number(body.questionListId); listItems.set(listId, [...(listItems.get(listId) || []), Number(body.questionId)]); return send({ rank: 1 }) }
    if (path.startsWith('/api/admin/questionlist-items/') || path === '/api/questionlist-items') {
      const listId = path === '/api/questionlist-items' ? Number(query.get('questionListId')) : Number(path.split('/')[4])
      if (method === 'DELETE') { listItems.set(listId, (listItems.get(listId) || []).filter(questionId => questionId !== id)); return send(null) }
      const items = (listItems.get(listId) || []).map((questionId, rank) => ({ questionListId: listId, rank, question: questions.find(item => item.questionId === questionId), userQuestionStatus: { finished: loggedIn && notes.some(item => item.questionId === questionId) } }))
      return path === '/api/questionlist-items' ? paged(items) : send(items)
    }
    if (path === '/api/messages') return send(messages)
    if (path === '/api/messages/unread/count') return send(messages.filter(item => !item.isRead).length)
    if (path === '/api/messages/all/read') { messages.forEach(item => item.isRead = true); return send(null) }
    if (path === '/api/messages/batch/read') { messages.forEach(item => { if ((body.messageIds as number[]).includes(item.messageId)) item.isRead = true }); return send(null) }
    if (path.startsWith('/api/messages/') && method === 'DELETE') { messages = messages.filter(item => item.messageId !== id); return send(null) }
    if (path === '/api/statistic') return paged([{ id: 1, date: '2026-09-16', loginCount: 10, registerCount: 3, totalRegisterCount: 20, noteCount: 5, submitNoteCount: 8, totalNoteCount: 50 }])
    res.statusCode = 404
    res.end(JSON.stringify({ code: 404, message: `未实现测试接口 ${method} ${path}` }))
  }
}
