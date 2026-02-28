import axios from 'axios'
import { getToken } from '@/utils/auth'

/**
 * 上传单个文件到 asset-file 服务
 * @returns 文件访问 URL（如 /file/2026/02/xxx.jpg）
 */
export async function uploadFile(file: File): Promise<string> {
  const fd = new FormData()
  fd.append('file', file)
  const res = await axios.post<{ url: string }>('/file/upload', fd, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  })
  return res.data.url
}
