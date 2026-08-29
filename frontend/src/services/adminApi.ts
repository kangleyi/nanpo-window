import { apiRequest } from './api';
import { PageData } from './publicApi';
import { FarmRecord } from './farmerApi';
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

export function loadAdminOrders(): Promise<Order[]> {
  return apiRequest<Order[]>('/api/admin/orders?status=ALL');
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
