import request from '@/api/request'

export interface SysPost {
  id: number
  postCode: string
  postName: string
  sortOrder?: number
  status: number
  remark?: string
}

export interface PostCreateDTO {
  id?: number
  postCode: string
  postName: string
  sortOrder?: number
  status?: number
  remark?: string
}

export const postApi = {
  page: (params?: object) => request.get('/sys/posts', { params }),
  list: () => request.get('/sys/posts/list'),
  create: (data: PostCreateDTO) => request.post('/sys/posts', data),
  update: (id: number, data: PostCreateDTO) => request.put(`/sys/posts/${id}`, data),
  delete: (id: number) => request.delete(`/sys/posts/${id}`),
  changeStatus: (id: number, status: number) => request.put(`/sys/posts/${id}/status`, { status }),
}
