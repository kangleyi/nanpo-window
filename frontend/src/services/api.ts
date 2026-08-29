export type ApiEnvelope<T> = {
  requestId: string;
  code: string;
  message: string;
  data: T;
};

export class ApiError extends Error {
  readonly code: string;
  readonly requestId?: string;
  readonly status: number;

  constructor(message: string, code: string, status: number, requestId?: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
    this.requestId = requestId;
  }
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (init?.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const accessToken = window.localStorage.getItem('nanpo.accessToken');
  if (accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }

  const response = await fetch(path, { ...init, headers });
  const envelope = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;
  if (!response.ok || !envelope) {
    throw new ApiError(
      envelope?.message || '请求失败，请稍后重试',
      envelope?.code || 'NETWORK_ERROR',
      response.status,
      envelope?.requestId,
    );
  }
  return envelope.data;
}

export async function openProtectedMedia(path: string): Promise<void> {
  const accessToken = window.localStorage.getItem('nanpo.accessToken');
  const response = await fetch(path, {
    headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
  });
  if (!response.ok) {
    const envelope = (await response.json().catch(() => null)) as ApiEnvelope<unknown> | null;
    throw new ApiError(envelope?.message || '素材读取失败', envelope?.code || 'NETWORK_ERROR', response.status);
  }
  const objectUrl = URL.createObjectURL(await response.blob());
  window.open(objectUrl, '_blank', 'noopener,noreferrer');
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
}
