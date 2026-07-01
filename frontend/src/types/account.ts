export interface AccountItem {
  id?: number
  username?: string | null
  email?: string | null
  platformId?: number
  platformName?: string | null
  platformType?: string | null
  testModel?: string | null
  isCollect?: boolean
}

export interface AccountForm {
  platformId: number | null
  username: string
  email: string
  password: string
  testModel: string
  isCollect: boolean
}
