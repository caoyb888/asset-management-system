import request from '@/api/request'
import type { PageResult } from './project'

export interface NewsVO {
  id: number
  title: string
  content?: string
  category: number
  categoryName: string
  status: number
  statusName: string
  publishTime?: string
  createdAt: string
  updatedAt: string
}

export interface NewsQuery {
  pageNum?: number
  pageSize?: number
  title?: string
  category?: number | ''
  status?: number | ''
}

export interface NewsSaveDTO {
  title: string
  content?: string
  category?: number | null
  status?: number
  publishTime?: string
}

/** 分页查询新闻资讯列表 */
export function getNewsPage(params: NewsQuery) {
  return request.get<PageResult<NewsVO>, PageResult<NewsVO>>('/base/news', { params })
}

/** 查询新闻资讯详情 */
export function getNewsById(id: number) {
  return request.get<NewsVO, NewsVO>(`/base/news/${id}`)
}

/** 新增新闻资讯 */
export function createNews(data: NewsSaveDTO) {
  return request.post<number, number>('/base/news', data)
}

/** 编辑新闻资讯 */
export function updateNews(id: number, data: NewsSaveDTO) {
  return request.put<void, void>(`/base/news/${id}`, data)
}

/** 删除新闻资讯 */
export function deleteNews(id: number) {
  return request.delete<void, void>(`/base/news/${id}`)
}

/** 上架新闻资讯 */
export function publishNews(id: number) {
  return request.put<void, void>(`/base/news/${id}/publish`)
}

/** 下架新闻资讯 */
export function unpublishNews(id: number) {
  return request.put<void, void>(`/base/news/${id}/unpublish`)
}
