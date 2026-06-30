export interface BalanceHistoryResponse {
  items: PlatformBalanceItem[]
  total: number
}

export interface PlatformBalanceItem {
  platformId: number
  platformName: string
  platformType: string
  baseUrl: string
  accounts: AccountBalanceItem[]
}

export interface AccountBalanceItem {
  accountId?: number | null
  accountIdentity?: string | null
  currentBalance?: number | null
  todayConsumption?: number | null
  todayRecharge?: number | null
  points: BalancePoint[]
}

export interface BalancePoint {
  collectedAt: string
  balance: number
}
