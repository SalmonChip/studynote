import { httpClient } from '../../../request'
import { Category, CategoryTree } from '../types/types.ts'
import { adminCategoryApiList } from '../api/categoryApi.ts'
import { CreateCategoryBody, CreateCategoryResponse } from '../types/categoryService.ts'
export const categoryService = {
  list: () => httpClient.request<CategoryTree[]>(['GET', '/api/categories']),
}
export const adminCategoryService = {
  categoriesService: () => {
    return httpClient.request<CategoryTree[]>(adminCategoryApiList.categories, {
      queryParams: {},
    })
  },
  createCategoryService: (body: CreateCategoryBody) => {
    return httpClient.request<CreateCategoryResponse>(adminCategoryApiList.createCategory, {
      body: body,
    })
  },
  deleteCategoryService: (categoryId: number) => {
    return httpClient.request<null>(adminCategoryApiList.deleteCategory, {
      pathParams: [categoryId],
    })
  },
  updateCategoryService: (category: Category) => {
    return httpClient.request<null>(adminCategoryApiList.updateCategory, {
      body: {
        name: category.name,
      },
      pathParams: [category.categoryId],
    })
  },
}
