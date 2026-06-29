export interface PlatformItem {
  id?: number
  platformName: string
  baseUrl: string
  enabled: boolean
  type: string
  accountCount?: number
  totalBalance?: number
  platformConsumption?: number
  actualConsumption?: number
  rechargeAmount?: number
  arrivalAmount?: number
  abnormalCount?: number
  lastCollectedAt?: string | null
}

export interface PlatformSummary {
  platformCount: number
  enabledCount: number
  accountCount: number
  abnormalCount: number
  platformConsumption: number
  actualConsumption: number
}

export interface PlatformSummaryResponse {
  items: PlatformItem[]
  summary: PlatformSummary
}

export interface PlatformForm {
  platformName: string
  baseUrl: string
  type: string
  enabled: boolean
}
