export interface CategoryEntity {
  categoryId: number
  name: string
  parentCategoryId: number
  createdAt: string
  updatedAt: string
}
export type Category = Omit<CategoryEntity, 'createdAt' | 'updatedAt'>
export type CategoryTree = Category & {
  key: number
  children?: CategoryTree[]
}
export type OptMode = 'create' | 'update'
export interface CategoryTreeNode {
  title: string
  key: number
  children?: CategoryTreeNode[]
}
