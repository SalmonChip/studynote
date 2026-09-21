import { httpClient } from '@/request'
import { noteApiList } from '../api/noteApi.ts'
import {
  CreateNoteParams,
  DownloadNote,
  NoteHeatMapItem,
  NoteQueryParams,
  NoteRankListItem,
  NoteTop3Count,
  NoteWithRelations,
} from '../types/serviceTypes.ts'
export const noteService = {
  getNoteList: (params: NoteQueryParams) => {
    return httpClient.request<NoteWithRelations[]>(noteApiList.getNoteList, {
      queryParams: params,
    })
  },
  createNoteService: (params: CreateNoteParams) => {
    return httpClient.request<{
      noteId: number
    }>(noteApiList.createNote, {
      body: params,
    })
  },
  deleteNoteService: (noteId: number) => {
    return httpClient.request<{
      noteId: number
    }>(noteApiList.deleteNote, {
      pathParams: [noteId],
    })
  },
  updateNoteService: (noteId: number, params: CreateNoteParams) => {
    return httpClient.request<{
      noteId: number
    }>(noteApiList.updateNote, {
      pathParams: [noteId],
      body: params,
    })
  },
  getNoteRankListService: () => {
    return httpClient.request<NoteRankListItem[]>(noteApiList.getNoteRankList)
  },
  getHeatMapService: () => {
    return httpClient.request<NoteHeatMapItem[]>(noteApiList.getHeatMap)
  },
  getTop3CountService: () => {
    return httpClient.request<NoteTop3Count>(noteApiList.getTop3Count)
  },
  downloadNoteService: () => {
    return httpClient.request<DownloadNote>(noteApiList.downloadNote)
  },
}
