import { apiRequest } from './api';

export type MediaAsset = {
  id: number;
  status: string;
  failureReason?: string;
  checksumSha256?: string;
};

type MediaType = 'IMAGE' | 'AUDIO' | 'VIDEO';

type UploadTicket = {
  media: MediaAsset;
  uploadUrl: string;
  headers: Record<string, string>;
};

export async function uploadMediaFile(file: File, mediaType: MediaType, recordId?: number) {
  const bytes = await file.arrayBuffer();
  const checksum = Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', bytes)))
    .map((value) => value.toString(16).padStart(2, '0')).join('');
  const ticket = await apiRequest<UploadTicket>('/api/media/upload-ticket', {
    method: 'POST',
    body: JSON.stringify({
      mediaType,
      contentType: file.type,
      sizeBytes: file.size,
      originalName: file.name,
      checksumSha256: checksum,
      ...(recordId ? { recordId } : {}),
    }),
  });
  if (ticket.uploadUrl.startsWith('/api/')) {
    await apiRequest<MediaAsset>(ticket.uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/octet-stream' },
      body: bytes,
    });
  } else {
    const response = await fetch(ticket.uploadUrl, {
      method: 'PUT',
      headers: ticket.headers,
      body: bytes,
    });
    if (!response.ok) throw new Error(`文件上传到对象存储失败（HTTP ${response.status}）`);
  }
  const media = await apiRequest<MediaAsset>(`/api/media/${ticket.media.id}/complete`, { method: 'POST' });
  if (media.status !== 'READY') throw new Error(media.failureReason || '上传文件校验失败');
  return { media, contentUrl: `/api/public/media/${media.id}/content` };
}
