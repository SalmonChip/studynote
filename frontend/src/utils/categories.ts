import type { CategoryTree } from '@/domain/category/types/types'
export function flattenCategories(
  items: CategoryTree[],
  prefix = '',
): { value: number; label: string }[] {
  return items.flatMap((item) => [
    { value: item.categoryId, label: prefix + item.name },
    ...flattenCategories(item.children || [], prefix + item.name + ' / '),
  ])
}
