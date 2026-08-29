import { apiRequest } from './api';

export type PageData<T> = { items: T[]; page: number; size: number; total: number };
export type GoodsSection = { eyebrow: string; title: string; description: string; seasonLabel: string; seasonNote: string; imageUrl: string; imageCaption: string };
export type Site = { id: number; name: string; province: string; city: string; county: string; address: string; summary: string; mapKeyword: string; recommendedSeason: string; visitorService?: { scene: string; name: string; phone: string; businessHours?: string }; goodsSection?: GoodsSection };
export type TravelRoute = { id: number; kind: string; title: string; duration: string; note: string; steps: string[]; source?: string; verifiedAt?: string; expiresAt?: string };
export type Attraction = { id: number; name: string; category: string; distanceKm: number; driveMinutes: number; summary: string; coverUrl: string; mapUrl: string; highlights: string[] };
export type TravelPlan = { id: number; slug: string; name: string; duration: string; suitableFor: string; distance: string; summary: string; stops: { time: string; title: string; detail: string }[]; tips: string[] };
export type Homestay = { id: number; name: string; type: string; summary: string; capacity: string; price: string; coverUrl: string; consultationPhone?: string; externalUrl?: string };
export type Experience = { id: number; name: string; type: string; season: string; duration: string; summary: string; price: string; coverUrl: string; videoUrl?: string; bookingNotes?: string };
export type Product = { id: number; name: string; category: string; season: string; summary: string; coverUrl: string; imageUrls: string[]; startingPrice: number; farmerName: string; farmerId: number };
export type Farmer = { id: number; code: string; name: string; villageGroup: string; introduction: string; certificationStatus: string };
export type ProductSku = { id: number; code: string; specification: string; unitPrice: number; stockNote: string };
export type FarmRecord = { id: number; stage: string; occurredAt: string; text: string; reviewedAt?: string; publishedAt?: string };
export type ProductDetail = { product: Product; farmer: Farmer; skus: ProductSku[]; productionRecords: FarmRecord[] };

export type PublicHomeData = {
  site: Site;
  routes: TravelRoute[];
  attractions: PageData<Attraction>;
  travelPlans: TravelPlan[];
  homestays: PageData<Homestay>;
  experiences: PageData<Experience>;
  products: PageData<Product>;
};

export async function loadPublicHomeData(): Promise<PublicHomeData> {
  const [site, routes, attractions, travelPlans, homestays, experiences, products] = await Promise.all([
    apiRequest<Site>('/api/public/site'),
    apiRequest<TravelRoute[]>('/api/public/travel/routes'),
    apiRequest<PageData<Attraction>>('/api/public/attractions?page=1&size=100'),
    apiRequest<TravelPlan[]>('/api/public/travel-plans'),
    apiRequest<PageData<Homestay>>('/api/public/homestays?page=1&size=100'),
    apiRequest<PageData<Experience>>('/api/public/experiences?page=1&size=100'),
    apiRequest<PageData<Product>>('/api/public/products?page=1&size=100'),
  ]);
  return { site, routes, attractions, travelPlans, homestays, experiences, products };
}

export function loadPublicProduct(productId: number): Promise<ProductDetail> {
  return apiRequest<ProductDetail>(`/api/public/products/${productId}`);
}
