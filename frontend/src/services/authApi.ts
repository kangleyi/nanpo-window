import { apiRequest } from './api';

export type CurrentUser = { id: number; phone: string; displayName: string; roles: string[] };
type AuthTokens = { tokenType: string; accessToken: string; refreshToken: string; accessExpiresAt: string; refreshExpiresAt: string; user: CurrentUser };

export async function sendLoginCode(phone: string): Promise<void> {
  await apiRequest('/api/auth/sms/send', { method: 'POST', body: JSON.stringify({ phone }) });
}

export async function loginWithSms(phone: string, code: string): Promise<CurrentUser> {
  const tokens = await apiRequest<AuthTokens>('/api/auth/sms/login', {
    method: 'POST',
    body: JSON.stringify({ phone, code }),
  });
  window.localStorage.setItem('nanpo.accessToken', tokens.accessToken);
  window.localStorage.setItem('nanpo.refreshToken', tokens.refreshToken);
  return tokens.user;
}

export async function getCurrentUser(): Promise<CurrentUser> {
  return apiRequest<CurrentUser>('/api/me');
}

export async function logout(): Promise<void> {
  try {
    await apiRequest('/api/auth/logout', { method: 'POST' });
  } finally {
    window.localStorage.removeItem('nanpo.accessToken');
    window.localStorage.removeItem('nanpo.refreshToken');
  }
}
