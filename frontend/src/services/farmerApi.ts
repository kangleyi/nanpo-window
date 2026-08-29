import { apiRequest } from './api';
import { MediaAsset, uploadMediaFile } from './mediaApi';

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
  imageUrls: string[];
  status: string;
  skus: FarmerSku[];
  recordCount: number;
  updatedAt: string;
};

export type FarmerSku = {
  id: number;
  code: string;
  specification: string;
  unitPrice: number;
  stockNote?: string;
  enabled: boolean;
};

export type FarmerPlot = {
  id: number;
  code: string;
  location: string;
  area?: string;
  mainCrop?: string;
  coverUrl?: string;
  status: string;
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

export function loadFarmerOrders(): Promise<FarmerOrder[]> {
  return apiRequest<FarmerOrder[]>('/api/farmer/orders');
}

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
  const mediaType = file.type.startsWith('image/') ? 'IMAGE'
    : file.type.startsWith('audio/') ? 'AUDIO'
      : file.type.startsWith('video/') ? 'VIDEO' : 'UNSUPPORTED';
  if (mediaType === 'UNSUPPORTED') throw new Error('不支持的媒体文件格式');
  return (await uploadMediaFile(file, mediaType, recordId)).media;
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
