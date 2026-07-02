export interface CollectGroupResponse {
  items: PlatformGroupItem[]
  total: number
}

export interface PlatformGroupItem {
  platformId: number
  platformName: string
  platformType: string
  baseUrl: string
  enabled: boolean
  groupCount: number
  rechargeRatio?: number | null
  discountRatio?: number | null
  lastCollectedAt?: string | null
  groups: GroupItem[]
}

export interface GroupItem {
  id: number
  groupName: string
  description?: string | null
  platformRate?: number | null
  actualRate?: number | null
  status?: string | null
  keyCount?: number | null
  usedByKey?: boolean | null
  lastCollectedAt?: string | null
}
