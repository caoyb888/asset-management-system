import request from '@/api/request'
import type { PageResult } from './project'

export interface NoticeVO {
  id: number
  title: string
  content?: string
  noticeType: number
  noticeTypeName: string
  status: number
  statusName: string
  scheduledTime?: string
  publishTime?: string
  createdAt: string
  updatedAt: string
}

export interface NoticeQuery {
  pageNum?: number
  pageSize?: number
  title?: string
  noticeType?: number | ''
  status?: number | ''
}

export interface NoticeSaveDTO {
  title: string
  content?: string
  noticeType?: number | null
  status?: number
  scheduledTime?: string
}

/** 分页查询通知公告列表 */
export function getNoticePage(params: NoticeQuery) {
  return request.get<PageResult<NoticeVO>, PageResult<NoticeVO>>('/base/notices', { params })
}

/** 查询通知公告详情 */
export function getNoticeById(id: number) {
  return request.get<NoticeVO, NoticeVO>(`/base/notices/${id}`)
}

/** 新增通知公告 */
export function createNotice(data: NoticeSaveDTO) {
  return request.post<number, number>('/base/notices', data)
}

/** 编辑通知公告 */
export function updateNotice(id: number, data: NoticeSaveDTO) {
  return request.put<void, void>(`/base/notices/${id}`, data)
}

/** 删除通知公告 */
export function deleteNotice(id: number) {
  return request.delete<void, void>(`/base/notices/${id}`)
}

/** 发布通知公告 */
export function publishNotice(id: number) {
  return request.put<void, void>(`/base/notices/${id}/publish`)
}

/** 下架通知公告 */
export function unpublishNotice(id: number) {
  return request.put<void, void>(`/base/notices/${id}/unpublish`)
}
