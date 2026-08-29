import { apiRequest } from './api';
import { PageData } from './publicApi';
import { FarmerPlot, FarmerProduct, FarmerProfile, FarmRecord } from './farmerApi';
import { Order } from './orderApi';

export type ContentKind = 'homestays' | 'experiences';

export type ManagedContent = {
  id: number;
  name: string;
  type: string;
  summary: string;
  price: string;
  coverUrl: string;
  sortOrder: number;
  status: string;
  consultationPhone?: string;
  externalUrl?: string;
  capacity?: string;
  season?: string;
  duration?: string;
  videoUrl?: string;
  bookingNotes?: string;
};

export type ContentCommand = {
  name: string;
  type: string;
  summary: string;
  price: string;
  coverUrl: string;
  sortOrder: number;
  consultationPhone?: string;
  externalUrl?: string;
  capacity?: string;
  season?: string;
  duration?: string;
  videoUrl?: string;
  bookingNotes?: string;
};

export function loadManagedContent(kind: ContentKind, status = 'ALL'): Promise<PageData<ManagedContent>> {
  return apiRequest<PageData<ManagedContent>>(`/api/admin/content/${kind}?page=1&size=100&status=${status}`);
}

export function createManagedContent(kind: ContentKind, command: ContentCommand): Promise<ManagedContent> {
  return apiRequest<ManagedContent>(`/api/admin/content/${kind}`, {
    method: 'POST',
    body: JSON.stringify(command),
  });
}

export function updateManagedContent(kind: ContentKind, id: number, command: ContentCommand): Promise<ManagedContent> {
  return apiRequest<ManagedContent>(`/api/admin/content/${kind}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(command),
  });
}

export function setManagedContentPublished(kind: ContentKind, id: number, published: boolean): Promise<void> {
  return apiRequest(`/api/admin/content/${kind}/${id}/${published ? 'publish' : 'unpublish'}`, { method: 'POST' });
}

export type ConsultationInquiry = {
  id: number;
  sourceType: 'HOMESTAY' | 'EXPERIENCE';
  sourceId: number;
  targetName: string;
  visitAt: string;
  partySize: number;
  callbackPhone: string;
  note?: string;
  status: 'NEW' | 'CONTACTED' | 'CLOSED';
  createdAt: string;
  updatedAt: string;
};

export function loadConsultationInquiries(status = 'ALL', sourceType = 'ALL'): Promise<ConsultationInquiry[]> {
  const params = new URLSearchParams({ status, sourceType });
  return apiRequest<ConsultationInquiry[]>(`/api/admin/inquiries?${params}`);
}

export function updateConsultationInquiryStatus(
  id: number,
  action: 'contacted' | 'closed',
): Promise<ConsultationInquiry> {
  return apiRequest<ConsultationInquiry>(`/api/admin/inquiries/${id}/${action}`, { method: 'POST' });
}

export function loadFarmReviewQueue(): Promise<FarmRecord[]> {
  return apiRequest<FarmRecord[]>('/api/admin/reviews/records?status=PENDING_REVIEW');
}

export function approveFarmRecord(recordId: number, confirmedText?: string): Promise<FarmRecord> {
  return apiRequest<FarmRecord>(`/api/admin/reviews/records/${recordId}/approve`, {
    method: 'POST',
    body: JSON.stringify({ confirmedText }),
  });
}

export function rejectFarmRecord(recordId: number, reviewNote: string): Promise<FarmRecord> {
  return apiRequest<FarmRecord>(`/api/admin/reviews/records/${recordId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reviewNote }),
  });
}

export type ProductCommand = {
  name: string;
  category: string;
  season: string;
  summary: string;
  coverUrl: string;
  skus: { id?: number; specification: string; unitPrice: number; stockNote?: string }[];
};

export function loadFarmers(): Promise<FarmerProfile[]> {
  return apiRequest<FarmerProfile[]>('/api/admin/farmers');
}

export function loadFarmerPlots(farmerId: number): Promise<FarmerPlot[]> {
  return apiRequest<FarmerPlot[]>(`/api/admin/farmers/${farmerId}/plots`);
}

export function loadFarmerProducts(farmerId: number): Promise<FarmerProduct[]> {
  return apiRequest<FarmerProduct[]>(`/api/admin/farmers/${farmerId}/products`);
}

export function createFarmerProduct(farmerId: number, command: ProductCommand): Promise<FarmerProduct> {
  return apiRequest<FarmerProduct>(`/api/admin/farmers/${farmerId}/products`, {
    method: 'POST', body: JSON.stringify(command),
  });
}

export function updateFarmerProduct(farmerId: number, productId: number, command: ProductCommand): Promise<FarmerProduct> {
  return apiRequest<FarmerProduct>(`/api/admin/farmers/${farmerId}/products/${productId}`, {
    method: 'PUT', body: JSON.stringify(command),
  });
}

export function setFarmerProductPublished(farmerId: number, productId: number, published: boolean): Promise<FarmerProduct> {
  return apiRequest<FarmerProduct>(
    `/api/admin/farmers/${farmerId}/products/${productId}/${published ? 'publish' : 'unpublish'}`,
    { method: 'POST' },
  );
}

export function createFarmerRecord(farmerId: number, command: {
  productId: number;
  stage: string;
  occurredAt: string;
  originalText: string;
  truthConfirmed: boolean;
}): Promise<FarmRecord> {
  return apiRequest<FarmRecord>(`/api/admin/farmers/${farmerId}/records`, {
    method: 'POST', body: JSON.stringify(command),
  });
}

export function submitFarmerRecord(farmerId: number, recordId: number): Promise<FarmRecord> {
  return apiRequest<FarmRecord>(`/api/admin/farmers/${farmerId}/records/${recordId}/submit`, { method: 'POST' });
}

export function loadAdminOrders(status = 'ALL', farmerId?: number): Promise<Order[]> {
  const params = new URLSearchParams({ status });
  if (farmerId) params.set('farmerId', String(farmerId));
  return apiRequest<Order[]>(`/api/admin/orders?${params}`);
}

export function markAdminOrderReady(orderId: number): Promise<Order> {
  return apiRequest<Order>(`/api/admin/orders/${orderId}/prepare`, { method: 'POST' });
}

export function confirmOrderPayment(orderId: number): Promise<Order> {
  return apiRequest<Order>(`/api/admin/orders/${orderId}/confirm-payment`, { method: 'POST' });
}

export function rejectOrderPayment(orderId: number, reason: string): Promise<Order> {
  return apiRequest<Order>(`/api/admin/orders/${orderId}/reject-payment`, {
    method: 'POST', body: JSON.stringify({ reason }),
  });
}

export function shipAdminOrder(orderId: number, shippingCompany: string, trackingNo: string): Promise<Order> {
  return apiRequest<Order>(`/api/admin/orders/${orderId}/ship`, {
    method: 'POST', body: JSON.stringify({ shippingCompany, trackingNo }),
  });
}
