import { apiRequest } from './api';

export type InquirySource = 'HOMESTAY' | 'EXPERIENCE';

export type InquiryCommand = {
  sourceType: InquirySource;
  sourceId: number;
  visitAt: string;
  partySize: number;
  callbackPhone: string;
  note?: string;
};

export type Inquiry = InquiryCommand & {
  id: number;
  targetName: string;
  status: 'NEW' | 'CONTACTED' | 'CLOSED';
  createdAt: string;
  updatedAt: string;
};

export function submitConsultation(command: InquiryCommand): Promise<Inquiry> {
  return apiRequest<Inquiry>('/api/public/inquiries', {
    method: 'POST',
    body: JSON.stringify(command),
  });
}
