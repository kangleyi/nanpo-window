import { apiRequest } from './api';

export type CurrentUser = { id: number; phone: string; displayName: string; roles: string[] };
type AuthTokens = { tokenType: string; accessToken: string; refreshToken: string; accessExpiresAt: string; refreshExpiresAt: string; user: CurrentUser };

function saveTokens(tokens: AuthTokens): CurrentUser {
  window.localStorage.setItem('nanpo.accessToken', tokens.accessToken);
  window.localStorage.setItem('nanpo.refreshToken', tokens.refreshToken);
  return tokens.user;
}

export async function loginWithPassword(phone: string, password: string): Promise<CurrentUser> {
  const tokens = await apiRequest<AuthTokens>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ phone, password }),
  });
  return saveTokens(tokens);
}

export async function registerWithPassword(phone: string, password: string): Promise<CurrentUser> {
  const tokens = await apiRequest<AuthTokens>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ phone, password }),
  });
  return saveTokens(tokens);
}

export async function getCurrentUser(): Promise<CurrentUser> {
  return apiRequest<CurrentUser>('/api/me');
}

export async function logout(): Promise<void> {
  try {
    await apiRequest('/api/auth/logout', { method: 'POST' });
  } catch {
    // A stale token or a temporary network failure must not trap the user in
    // an authenticated screen. Server-side revocation is best effort; local
    // credentials are always cleared below.
  } finally {
    window.localStorage.removeItem('nanpo.accessToken');
    window.localStorage.removeItem('nanpo.refreshToken');
  }
}
