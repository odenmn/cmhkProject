export interface AuthSession {
  customerId: number
  newCustomer: boolean
  channelName: string
  elderlyMode: number
  accessToken: string
  expiresAt: string
}

const SESSION_KEY = 'cmhk-auth-session'

export function saveAuthSession(session: AuthSession) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function getAuthSession(): AuthSession | null {
  const value = sessionStorage.getItem(SESSION_KEY)
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value) as AuthSession
  } catch {
    clearAuthSession()
    return null
  }
}

export function getAccessToken() {
  return getAuthSession()?.accessToken || ''
}

export function hasValidAccessToken() {
  const session = getAuthSession()
  if (!session?.accessToken || Number.isNaN(Date.parse(session.expiresAt))) {
    return false
  }
  if (Date.parse(session.expiresAt) <= Date.now()) {
    clearAuthSession()
    return false
  }
  return true
}

export function clearAuthSession() {
  sessionStorage.removeItem(SESSION_KEY)
}
