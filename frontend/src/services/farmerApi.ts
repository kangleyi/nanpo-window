import { apiRequest } from './api';

export type FarmerProfile = {
  id: number;
  code: string;
  name: string;
  villageGroup: string;
  introduction: string;
  certificationStatus: string;
};

export type FarmerDashboard = {
  farmer: FarmerProfile;
  plotCount: number;
  productCount: number;
  recordCount: number;
  pendingReviewCount: number;
  publishedRecordCount: number;
};

export type FarmerProduct = {
  id: number;
  plotId?: number;
  name: string;
  category: string;
  season: string;
  summary: string;
  coverUrl: string;
  status: string;
  recordCount: number;
};

export type FarmRecord = {
  id: number;
  productId: number;
  productName: string;
  plotId?: number;
  plotCode?: string;
  stage: string;
  occurredAt: string;
  originalText: string;
  confirmedText?: string;
  truthConfirmed: boolean;
  status: string;
  reviewNote?: string;
  reviewedAt?: string;
  publishedAt?: string;
  version: number;
  media: {
    id: number;
    mediaType: string;
    originalName: string;
    contentType: string;
    status: string;
    contentUrl?: string;
  }[];
};

export type FarmRecordCommand = {
  productId: number;
  plotId?: number;
  stage: string;
  occurredAt: string;
  originalText: string;
  truthConfirmed: boolean;
};

export type FarmerOrder = {
  id: number;
  orderNo: string;
  status: string;
  createdAt: string;
  items: { id: number; productName: string; specification: string; quantity: number }[];
};

export type AiCopy = {
  id: number;
  scene: string;
  sourceRecordIds: number[];
  modelName: string;
  modelVersion?: string;
  outputText: string;
  confirmedText?: string;
  status: string;
  confirmedAt?: string;
};

export type MediaAsset = {
  id: number;
  status: string;
  failureReason?: string;
  checksumSha256?: string;
};

export async function loadFarmerWorkspace() {
  const [dashboard, products, records, orders, aiCopies] = await Promise.all([
    apiRequest<FarmerDashboard>('/api/farmer/dashboard'),
    apiRequest<FarmerProduct[]>('/api/farmer/products'),
    apiRequest<FarmRecord[]>('/api/farmer/records'),
    apiRequest<FarmerOrder[]>('/api/farmer/orders'),
    apiRequest<AiCopy[]>('/api/farmer/ai-copy'),
  ]);
  return { dashboard, products, records, orders, aiCopies };
}

export function createFarmRecord(command: FarmRecordCommand): Promise<FarmRecord> {
  return apiRequest<FarmRecord>('/api/farmer/records', {
    method: 'POST',
    body: JSON.stringify(command),
  });
}

export function submitFarmRecord(recordId: number): Promise<FarmRecord> {
  return apiRequest<FarmRecord>(`/api/farmer/records/${recordId}/submit`, { method: 'POST' });
}

export function markFarmerOrderReady(orderId: number): Promise<void> {
  return apiRequest(`/api/farmer/orders/${orderId}/prepare`, { method: 'POST' });
}

export async function uploadRecordMedia(recordId: number, file: File): Promise<MediaAsset> {
  const bytes = await file.arrayBuffer();
  const checksum = Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', bytes)))
    .map((value) => value.toString(16).padStart(2, '0')).join('');
  const mediaType = file.type.startsWith('image/') ? 'IMAGE'
    : file.type.startsWith('audio/') ? 'AUDIO'
      : file.type.startsWith('video/') ? 'VIDEO' : 'UNSUPPORTED';
  const ticket = await apiRequest<{ media: MediaAsset; uploadUrl: string }>('/api/media/upload-ticket', {
    method: 'POST',
    body: JSON.stringify({
      mediaType,
      contentType: file.type,
      sizeBytes: file.size,
      originalName: file.name,
      checksumSha256: checksum,
      recordId,
    }),
  });
  await apiRequest<MediaAsset>(ticket.uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/octet-stream' },
    body: bytes,
  });
  return apiRequest<MediaAsset>(`/api/media/${ticket.media.id}/complete`, { method: 'POST' });
}

export function generateAiCopy(productId: number): Promise<AiCopy> {
  return apiRequest<AiCopy>(`/api/farmer/products/${productId}/ai-copy`, {
    method: 'POST', body: JSON.stringify({ scene: 'PRODUCT_INTRO' }),
  });
}

export function confirmAiCopy(id: number, confirmedText: string): Promise<AiCopy> {
  return apiRequest<AiCopy>(`/api/farmer/ai-copy/${id}/confirm`, {
    method: 'POST', body: JSON.stringify({ confirmedText }),
  });
}
