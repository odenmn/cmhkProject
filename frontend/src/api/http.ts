import axios from 'axios'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface BusinessType {
  id: number
  code: string
  name: string
  description: string
  sortOrder: number
  enabled: number
  createdAt: string
  updatedAt: string
}

export interface MobilePlan {
  id: number
  planCode: string
  planName: string
  planType: string
  monthlyFee: number
  channelPriceText: string
  effectiveMonthlyFee?: number
  effectivePriceText?: string
  officialMonthlyFee?: number
  officialPriceText?: string
  dataQuota: string
  voiceQuota: string
  roamingBenefit?: string
  contractPeriod: string
  promotionEndDate?: string
  sourceVersion?: string
  discountFormula?: string
  description: string
  sortOrder: number
  enabled: number
  offers?: MobilePlanOffer[]
}

export interface MobilePlanOffer {
  id: number
  planCode: string
  offerType: string
  offerName: string
  offerValue: string
  sortOrder: number
  enabled: number
}

export interface MobilePlanOrderCreateRequest {
  planCode: string
  customerName: string
  contactPhone: string
  remark: string
}

export interface MobilePlanOrder {
  id?: number
  orderNo: string
  planCode: string
  planName: string
  planType?: string
  monthlyFee: number
  channelPriceText?: string
  effectiveMonthlyFee?: number
  effectivePriceText?: string
  officialMonthlyFee?: number
  officialPriceText?: string
  dataQuota?: string
  voiceQuota?: string
  roamingBenefit?: string
  contractPeriod?: string
  promotionEndDate?: string
  discountFormula?: string
  customerName: string
  contactPhone: string
  remark: string
  status: string
}

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export async function fetchBusinessTypes() {
  const response = await http.get<ApiResponse<BusinessType[]>>('/business-types')
  return response.data
}

export async function fetchMobilePlans() {
  const response = await http.get<ApiResponse<MobilePlan[]>>('/mobile-plans')
  return response.data
}

export async function createMobilePlanOrder(request: MobilePlanOrderCreateRequest) {
  const response = await http.post<ApiResponse<MobilePlanOrder>>('/mobile-plans/orders', request)
  return response.data
}
