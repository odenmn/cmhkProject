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
  monthlyFee: number
  dataQuota: string
  voiceQuota: string
  contractPeriod: string
  description: string
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
  monthlyFee: number
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
