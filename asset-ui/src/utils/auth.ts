const TOKEN_KEY         = 'asset_token'
const REFRESH_TOKEN_KEY = 'asset_refresh_token'

// ─── Access Token ─────────────────────────────────────────────────────────────

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

// ─── Refresh Token ────────────────────────────────────────────────────────────

export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_TOKEN_KEY) ?? ''
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

export function removeRefreshToken(): void {
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}
