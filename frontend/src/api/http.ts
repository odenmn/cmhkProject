import axios from 'axios'
import { clearAuthSession, getAccessToken } from '../auth/session'

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
  customerIdentity: number
  hasOffer: number
  hasPassOrHkid: number
  expectedStartDate?: string
  idType: string
  idNo: string
  referrerPhone: string
  preferredContactTime: string
  remark: string
}

export interface MobilePlanOrder {
  id?: number
  orderNo: string
  customerId?: number
  planId?: number
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
  customerIdentity?: number
  hasOffer?: number
  hasPassOrHkid?: number
  expectedStartDate?: string
  idType?: string
  idNo?: string
  referrerPhone?: string
  preferredContactTime?: string
  remark: string
  status: string
}

export interface ChannelEntryContext {
  entryToken: string
  entryName: string
  channelName: string
  elderlyMode: number
}

export interface PhoneLoginResponse {
  customerId: number
  newCustomer: boolean
  channelName: string
  elderlyMode: number
  accessToken: string
  expiresAt: string
}

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const accessToken = getAccessToken()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuthSession()
      const currentRoute = window.location.hash.slice(1)
      if (!currentRoute.startsWith('/channel-auth')) {
        window.location.hash = '/channel-auth?entryToken=DEMO-ENTRY-001'
      }
    }
    return Promise.reject(error)
  }
)

export async function fetchBusinessTypes() {
  const response = await http.get<ApiResponse<BusinessType[]>>('/business-types')
  return response.data
}

export async function fetchMobilePlans() {
  const response = await http.get<ApiResponse<MobilePlan[]>>('/mobile-plans')
  return response.data
}

export async function fetchMobilePlan(planCode: string) {
  const response = await http.get<ApiResponse<MobilePlan>>(`/mobile-plans/${encodeURIComponent(planCode)}`)
  return response.data
}

export async function createMobilePlanOrder(request: MobilePlanOrderCreateRequest) {
  const response = await http.post<ApiResponse<MobilePlanOrder>>('/mobile-plans/orders', request)
  return response.data
}

export async function resolveChannelEntry(entryToken: string) {
  const response = await http.get<ApiResponse<ChannelEntryContext>>('/channel-auth/entry', {
    params: { entryToken }
  })
  return response.data
}

export async function sendMockVerificationCode(entryToken: string, phone: string) {
  const response = await http.post<ApiResponse<null>>('/channel-auth/verification-codes', { entryToken, phone })
  return response.data
}

export async function loginByPhone(entryToken: string, phone: string, verificationCode: string) {
  const response = await http.post<ApiResponse<PhoneLoginResponse>>('/channel-auth/phone-login', {
    entryToken,
    phone,
    verificationCode
  })
  return response.data
}
