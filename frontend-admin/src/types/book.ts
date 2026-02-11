/**
 * 书籍相关类型定义
 */

export interface BookInfo {
  book_id: string
  name: string
  version: string
  isbn?: string | null
  field?: string | null
  status: '启用' | '停用' | '重跑中'
  pdf_path: string
  file_name: string
  file_size: number
  registered_at: string
  updated_at: string
}

export interface BookRegistry {
  [bookId: string]: BookInfo
}

export interface Chapter {
  title: string
  start_page: number
  end_page: number
  level: number
}

export interface TocItem {
  title: string
  page: number
  level: number
}

export interface IndexItem {
  keyword: string
  pages: number[]
}

export interface PageChapterMap {
  [page: string]: Array<{
    title: string
    level: number
  }>
}

export interface BookStructure {
  book_id: string
  book_name: string
  total_pages: number
  chapters: Chapter[]
  toc_structure: TocItem[]
  index_info: IndexItem[]
  page_chapter_map: PageChapterMap
  summary: {
    chapter_count: number
    toc_item_count: number
    index_item_count: number
    mapped_pages: number
  }
}

