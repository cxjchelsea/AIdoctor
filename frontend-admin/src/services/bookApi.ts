import axios from 'axios'
import type { BookInfo, BookRegistry, BookStructure } from '@/types/book'

// 书籍管理API客户端
const bookApiClient = axios.create({
  baseURL: '/api/v1/books',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export const bookApi = {
  /**
   * 获取所有已注册的书籍列表
   */
  getAllBooks: async (): Promise<BookInfo[]> => {
    const response = await bookApiClient.get<BookRegistry>('/registry')
    const registry = response.data || {}
    return Object.values(registry)
  },

  /**
   * 根据book_id获取书籍信息
   */
  getBookById: async (bookId: string): Promise<BookInfo | null> => {
    try {
      const response = await bookApiClient.get<BookInfo>(`/registry/${bookId}`)
      return response.data
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null
      }
      throw error
    }
  },

  /**
   * 获取书籍的结构解析结果
   */
  getBookStructure: async (bookId: string): Promise<BookStructure | null> => {
    try {
      const response = await bookApiClient.get<BookStructure>(`/structure/${bookId}`)
      return response.data
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null
      }
      throw error
    }
  },

  /**
   * 更新书籍状态
   */
  updateBookStatus: async (bookId: string, status: '启用' | '停用' | '重跑中'): Promise<void> => {
    await bookApiClient.patch(`/registry/${bookId}/status`, { status })
  },
}

