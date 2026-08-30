import { apiRequest } from './api';

export type QualityReport = {
  score: number;
  characterCount: number;
  minLength: number;
  maxLength: number;
  checks: Record<string, boolean>;
  warnings: string[];
};

export type VisualAnalysis = {
  description: string;
  detectedProductName: string;
  detectedCategory: string;
  detectedSellingPoints: string[];
  visibleText: string[];
  productNameConfidence: 'high' | 'medium' | 'low';
  productCandidates: string[];
  resolvedProductName: string;
  nameSource: 'vision' | 'user';
  nameConflict: boolean;
  agriculturalProduct: boolean;
  productStage: string;
  scene: string;
  visibleFeatures: string[];
  generationBlocked: boolean;
  conflictMessage: string;
};

export type SellingPointCandidate = {
  text: string;
  kind: 'fact' | 'marketing' | 'confirmation_required';
  dimension: string;
  basis: string;
  needsConfirmation: boolean;
};

export type MarketingCopyResult = {
  requestId: string;
  headline: string;
  optimizedCopy: string;
  sellingPoints: string[];
  sellingPointCandidates: SellingPointCandidate[];
  qualityReport: QualityReport;
  visualAnalysis?: VisualAnalysis;
  meta: { source: string; channel: string; tone: string; provider: string; engine: string };
};

export type OptimizeMarketingCopyCommand = {
  productName: string;
  category: string;
  originalCopy: string;
  sellingPoints: string[];
  audience: string;
  tone: string;
  channel: string;
  maxLength: number;
  prohibitedTerms: string[];
};

export type ImageMarketingCopyCommand = Omit<OptimizeMarketingCopyCommand, 'originalCopy' | 'sellingPoints'> & {
  imageDataUrl: string;
  season: string;
  originalCopy: string;
  confirmedFacts: string[];
  confirmedSellingPoints: string[];
  visualHint: string;
};

export type MarketingCopyStatus = {
  configured: boolean;
  provider: string;
  textModel: string;
  visionModel: string;
};

export type PreparedMarketingImage = {
  dataUrl: string;
  width: number;
  height: number;
  bytes: number;
};

export async function prepareMarketingImage(file: File): Promise<PreparedMarketingImage> {
  if (!/^image\/(jpeg|png|webp)$/.test(file.type) || file.size > 10 * 1024 * 1024) {
    throw new Error('请选择 10MB 以内的 JPG、PNG 或 WebP 图片');
  }
  const bitmap = await createImageBitmap(file);
  try {
    const scale = Math.min(1, 1600 / Math.max(bitmap.width, bitmap.height));
    const width = Math.max(1, Math.round(bitmap.width * scale));
    const height = Math.max(1, Math.round(bitmap.height * scale));
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (!context) throw new Error('浏览器不支持图片处理');
    context.fillStyle = '#fff';
    context.fillRect(0, 0, width, height);
    context.drawImage(bitmap, 0, 0, width, height);
    const dataUrl = canvas.toDataURL('image/jpeg', 0.84);
    const bytes = Math.floor((dataUrl.split(',')[1] || '').length * 3 / 4);
    if (bytes > 6 * 1024 * 1024) throw new Error('图片处理后仍超过 6MB，请更换图片');
    return { dataUrl, width, height, bytes };
  } finally {
    bitmap.close();
  }
}

export function loadMarketingCopyStatus(): Promise<MarketingCopyStatus> {
  return apiRequest<MarketingCopyStatus>('/api/admin/marketing-copy/status');
}

export function optimizeMarketingCopy(command: OptimizeMarketingCopyCommand): Promise<MarketingCopyResult> {
  return apiRequest<MarketingCopyResult>('/api/admin/marketing-copy/optimize', {
    method: 'POST', body: JSON.stringify(command),
  });
}

export function generateMarketingCopyFromImage(command: ImageMarketingCopyCommand): Promise<MarketingCopyResult> {
  return apiRequest<MarketingCopyResult>('/api/admin/marketing-copy/from-image', {
    method: 'POST', body: JSON.stringify(command),
  });
}
