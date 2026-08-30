import { FormEvent, ImgHTMLAttributes, useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from './services/api';
import { loadPublicHomeData, loadPublicProduct, ProductDetail, PublicHomeData } from './services/publicApi';
import { createOrder, Order, reportOrderPayment } from './services/orderApi';
import { InquirySource, submitConsultation } from './services/inquiryApi';

type ImageProps = Omit<ImgHTMLAttributes<HTMLImageElement>, 'src'> & {
  src: string;
  fill?: boolean;
  priority?: boolean;
};

function Image({ fill, priority, style, ...props }: ImageProps) {
  return (
    <img
      {...props}
      loading={priority ? 'eager' : 'lazy'}
      style={
        fill
          ? {
              position: 'absolute',
              inset: 0,
              width: '100%',
              height: '100%',
              ...style,
            }
          : style
      }
    />
  );
}

const AMAP_POSITION: [number, number] = [113.324247, 35.345578];
const AMAP_SHARE_URL = 'https://surl.amap.com/kfIn9ZYM8vC';
const AMAP_WEB_KEY = (import.meta.env.VITE_AMAP_WEB_KEY ?? '').trim();
const AMAP_SECURITY_CODE = (import.meta.env.VITE_AMAP_SECURITY_CODE ?? '').trim();
let amapJsPromise: Promise<void> | undefined;
let amapUiPromise: Promise<void> | undefined;

function loadMapScript(id: string, src: string, ready: () => boolean): Promise<void> {
  if (ready()) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const existing = document.getElementById(id) as HTMLScriptElement | null;
    const script = existing ?? document.createElement('script');
    const onLoad = () => ready() ? resolve() : reject(new Error('高德地图资源加载失败'));
    script.addEventListener('load', onLoad, { once: true });
    script.addEventListener('error', () => reject(new Error('高德地图资源加载失败')), { once: true });
    if (!existing) {
      script.id = id;
      script.src = src;
      script.async = true;
      document.head.appendChild(script);
    }
  });
}

function AmapLocationMap() {
  const containerRef = useRef<HTMLDivElement>(null);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const configured = Boolean(AMAP_WEB_KEY && AMAP_SECURITY_CODE);

  useEffect(() => {
    if (!configured || !containerRef.current) return;
    let disposed = false;
    let map: { destroy: () => void } | undefined;
    const amapWindow = window as Window & {
      AMap?: any;
      AMapUI?: any;
      _AMapSecurityConfig?: { securityJsCode: string };
    };

    amapWindow._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE };
    amapJsPromise ??= loadMapScript(
      'nanpo-amap-js',
      `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(AMAP_WEB_KEY)}`,
      () => Boolean(amapWindow.AMap),
    );
    amapUiPromise ??= amapJsPromise.then(() => loadMapScript(
      'nanpo-amap-ui',
      'https://webapi.amap.com/ui/1.1/main.js',
      () => Boolean(amapWindow.AMapUI),
    ));

    amapUiPromise.then(() => {
      if (disposed || !containerRef.current) return;
      map = new amapWindow.AMap.Map(containerRef.current, {
        center: AMAP_POSITION,
        zoom: 15,
        viewMode: '2D',
        mapStyle: 'amap://styles/whitesmoke',
      });
      amapWindow.AMapUI.loadUI(['overlay/SimpleMarker'], (SimpleMarker: any) => {
        if (disposed) return;
        new SimpleMarker({
          iconLabel: { innerHTML: '南', style: { color: '#fff', fontSize: '15px' } },
          iconStyle: 'red',
          label: { content: '大南坡村', offset: new amapWindow.AMap.Pixel(30, 4) },
          map,
          position: AMAP_POSITION,
          title: '大南坡村',
        });
        setStatus('ready');
      });
    }).catch(() => setStatus('error'));

    return () => {
      disposed = true;
      map?.destroy();
    };
  }, [configured]);

  if (!configured || status === 'error') {
    return <div className="map-card amap-card-fallback"><div className="amap-fallback-art"><span className="amap-road-line road-one"/><span className="amap-road-line road-two"/><i className="amap-fallback-pin"><span>南</span></i></div><div className="amap-fallback-copy"><small>高德地图定位</small><strong>大南坡村</strong><span>点击查看实时位置与路线规划</span></div><a className="amap-open" href={AMAP_SHARE_URL} target="_blank" rel="noreferrer">在高德地图中打开 ↗</a></div>;
  }

  return <div className="map-card amap-card-live"><div ref={containerRef} className="amap-canvas" aria-label="大南坡村高德地图"/><div className="amap-location"><small>目的地</small><strong>大南坡村</strong></div>{status === 'loading' && <span className="amap-loading">地图加载中…</span>}<a className="amap-open" href={AMAP_SHARE_URL} target="_blank" rel="noreferrer">在高德地图中打开 ↗</a></div>;
}

type RouteType = 'drive' | 'rail' | 'taxi';
type AdminTab = 'overview' | 'orders' | 'stay' | 'goods' | 'experience' | 'contact';
type NearbyPlanId = string;
type PublicNavId = 'top' | 'route' | 'nearby' | 'stay' | 'goods';
type StayCard = { id?: number; name: string; type: string; desc: string; price: string; image: string; beds: string; externalUrl?: string };
type ProductCard = { id?: number; name: string; icon: string; season: string; desc: string; price: string; image?: string };
type ExperienceCard = { id?: number; name: string; type: string; season: string; duration: string; desc: string; price: string; image: string; hasVideo: boolean; video?: string };
type InquiryTarget = { sourceType: InquirySource; sourceId: number; name: string };
type NearbySpot = { name: string; range: string; time: string; type: string; mark: string; tone: string; image: string; desc: string; highlights: string[]; map: string };
type NearbyPlan = { eyebrow: string; name: string; days: string; fit: string; distance: string; summary: string; color: string; stops: { time: string; title: string; detail: string }[]; tips: string[] };

const routes: Record<RouteType, { title: string; time: string; steps: string[]; note: string }> = {
  drive: { title: '从郑州自驾出发', time: '约 1.5 小时', steps: ['郑云高速 S87', '云台山站下高速', '焦辉路 S306', '青云大道 X006', '大南坡村'], note: '006 县道穿村而过，建议直接导航“大南坡艺术中心”。' },
  rail: { title: '高铁到修武西站后打车', time: '高铁约 30 分钟，到站后打车进村', steps: ['郑州 / 郑州东', '修武西站', '网约车或出租车', '大南坡艺术中心'], note: '到站后建议直接打车进村；山区返程车辆较少，可提前与司机约定返程时间。' },
  taxi: { title: '从焦作市区打车前往', time: '约 50—70 分钟，以实时导航为准', steps: ['焦作市区 / 焦作站', '网约车或出租车', '青云大道 X006', '大南坡艺术中心'], note: '进村及返程车辆可能较少，建议提前预约车辆，并与司机确认返程安排。' },
};

const stayCards: StayCard[] = [
  { name: '牛大爷的院子', type: '乡土院落 · 示例房源', desc: '老院落、核桃树与山里清晨，适合一家人慢住两晚。', price: '价格待录入', image: '/images/homestay.jpg', beds: '2—4 人' },
  { name: '南坡山居 · 一号院', type: '整院包住 · 示例房源', desc: '灰砖院落保留北方村居尺度，步行可达艺术中心。', price: '价格待录入', image: '/images/nanpo-courtyard.png', beds: '4—6 人' },
  { name: '松风小院', type: '双床客房 · 示例房源', desc: '面向太行山南麓，适合周末、研学与小型团队。', price: '价格待录入', image: '/images/nanpo-architecture.png', beds: '2 人' },
  { name: '石榴树下的小院', type: '家庭套房 · 示例房源', desc: '院里保留果树和石桌，适合带孩子体验村庄日常。', price: '价格待录入', image: '/images/walnut-yard.jpg', beds: '3—5 人' },
  { name: '山里人家 · 二号院', type: '整院包住 · 示例房源', desc: '独立客厅与小厨房，适合朋友结伴或两家人同住。', price: '价格待录入', image: '/images/homestay.jpg', beds: '6—8 人' },
  { name: '南坡研学客舍', type: '团队客房 · 示例房源', desc: '面向研学与小型团建，可由后台配置餐食与活动套餐。', price: '价格待录入', image: '/images/nanpo-architecture.png', beds: '8—12 人' },
];

const goodsCards: ProductCard[] = [
  { name: '太行山核桃', icon: '核', season: '秋季新收', desc: '山地自然生长，壳薄仁香。', price: '¥ 29.9 起' },
  { name: '南坡山花椒', icon: '椒', season: '农户晒制', desc: '香气清亮，适合家常炖煮。', price: '¥ 19.9 起' },
  { name: '石磨小米', icon: '米', season: '当季谷物', desc: '颗粒饱满，煮粥米香自然。', price: '¥ 16.8 起' },
  { name: '山野百花蜜', icon: '蜜', season: '限量采收', desc: '来自太行山脚的四季花香。', price: '¥ 39.0 起' },
  { name: '四大怀药山药', icon: '药', season: '冬季采挖', desc: '粉糯细密，适合蒸煮与煲汤。', price: '¥ 36.0 起' },
  { name: '南坡手作柿饼', icon: '柿', season: '秋晒冬成', desc: '自然晾晒，保留果肉的软糯甜香。', price: '¥ 24.9 起' },
  { name: '农家手工挂面', icon: '面', season: '日常制作', desc: '慢醒慢晾，适合作为村庄伴手礼。', price: '¥ 18.0 起' },
  { name: '山地散养土鸡蛋', icon: '蛋', season: '每周上新', desc: '由农户按周汇总，数量以实际上架为准。', price: '¥ 22.0 起' },
];

const experienceCards: ExperienceCard[] = [
  { name: '核桃采收体验', type: '农事采摘', season: '8—9 月', duration: '约 2 小时', desc: '跟着农户认树、采收、分选，把劳动过程变成一堂自然课。', price: '¥ 68 / 人起', image: '/images/walnut-yard.jpg', hasVideo: true },
  { name: '山花椒采摘', type: '季节限定', season: '7—8 月', duration: '约 1.5 小时', desc: '学习辨认成熟花椒、体验手工采摘，并了解晾晒过程。', price: '¥ 48 / 人起', image: '/images/products.jpg', hasVideo: false },
  { name: '村庄美学导览', type: '文化讲解', season: '全年可约', duration: '约 2 小时', desc: '从老礼堂到艺术中心，听村庄空间更新与乡土生活的故事。', price: '价格待配置', image: '/images/nanpo-sign.png', hasVideo: true },
  { name: '石磨小米体验', type: '手作课堂', season: '全年可约', duration: '约 1 小时', desc: '认识谷物、体验传统石磨，把一袋现磨小米带回家。', price: '¥ 39 / 人起', image: '/images/products.jpg', hasVideo: false },
  { name: '亲子农耕课堂', type: '亲子研学', season: '春夏秋', duration: '半日', desc: '按节气配置播种、除草或收获任务，适合家庭和小团队。', price: '¥ 98 / 组起', image: '/images/nanpo-autumn.png', hasVideo: true },
  { name: '南坡秋兴手作', type: '节气活动', season: '秋季', duration: '约 2 小时', desc: '围绕在地物产开展拓印、编织与村庄市集体验。', price: '以活动公告为准', image: '/images/homestay.jpg', hasVideo: false },
];

const nearbySpots: NearbySpot[] = [
  { name: '大南坡村', range: '村内出发', time: '建议慢游 2—3 小时', type: '乡村 · 艺术', mark: '南', tone: 'clay', image: '/images/nanpo-architecture.png', desc: '从老大队部到乡村书店，在灰砖院落、古树与公共文化空间之间读懂村庄更新。', highlights: ['艺术中心', '方所乡村文化', '碧山工销社'], map: 'https://surl.amap.com/kfIn9ZYM8vC' },
  { name: '圆融寺', range: '约 7—17 km', time: '驾车约 25 分钟', type: '古寺 · 石刻', mark: '融', tone: 'sand', image: '/images/spots/yuanrong-new.jpg', desc: '太行山前的千年古刹，院落依山展开，适合静心漫游并感受山寺建筑。', highlights: ['古寺院落', '石刻碑塔', '山门远眺'], map: 'https://uri.amap.com/search?keyword=圆融无碍禅寺&city=焦作&callnative=0' },
  { name: '青龙峡', range: '约 11—21 km', time: '驾车约 35 分钟', type: '峡谷 · 瀑溪', mark: '青', tone: 'moss', image: '/images/spots/qinglong.jpg', desc: '峡谷幽深、潭瀑相连，适合避暑徒步，也可把沿途山路当作太行风景的一部分。', highlights: ['峡谷瀑溪', '爱情一号公路', '清凉徒步'], map: 'https://uri.amap.com/search?keyword=青龙峡景区&city=焦作&callnative=0' },
  { name: '峰林峡', range: '约 17—27 km', time: '驾车约 45 分钟', type: '天池 · 峡谷', mark: '峰', tone: 'pine', image: '/images/spots/fenglin.jpg', desc: '峰林与碧水相拥，高峡平湖色彩清透，适合乘船观景与亲子山水体验。', highlights: ['云台天池', '高峡平湖', '山水游乐'], map: 'https://uri.amap.com/search?keyword=峰林峡&city=焦作&callnative=0' },
  { name: '当阳峪绞胎瓷博物馆', range: '约 19—29 km', time: '驾车约 45 分钟', type: '非遗 · 博物馆', mark: '瓷', tone: 'ochre', image: '/images/spots/dangyangyu.jpg', desc: '走近“表里如一”的绞胎纹理，看多色瓷泥如何经过拉坯、修坯与烧制成为独一无二的器物。', highlights: ['国家级非遗', '绞胎瓷器', '工艺体验'], map: 'https://uri.amap.com/search?keyword=当阳峪绞胎瓷博物馆&city=焦作&callnative=0' },
  { name: '圆通寺', range: '约 23—33 km', time: '驾车约 50 分钟', type: '古寺 · 山麓', mark: '通', tone: 'stone', image: '/images/spots/yuantong.jpg', desc: '巡返村旁的山麓寺院，殿宇沿地势铺开，观音像与太行山背景构成醒目的远观点位。', highlights: ['巡返古寺', '观音像', '太行山麓'], map: 'https://uri.amap.com/search?keyword=巡返圆通寺&city=焦作&callnative=0' },
  { name: '恩州驿', range: '约 27—37 km', time: '驾车约 55 分钟', type: '古街 · 夜游', mark: '驿', tone: 'clay', image: '/images/spots/enzhou.jpg', desc: '近千米古风街区串联老建筑、非遗手作与地方小吃，傍晚亮灯后更有烟火气。', highlights: ['古风街区', '非遗市集', '夜景演艺'], map: 'https://uri.amap.com/search?keyword=恩州驿&city=焦作&callnative=0' },
  { name: '云台山', range: '约 33—43 km', time: '驾车约 65 分钟', type: '峡谷 · 飞瀑', mark: '云', tone: 'blue', image: '/images/spots/yuntai-new.jpg', desc: '红石峡、潭瀑峡与茱萸峰构成经典山水组合，适合为第一次到焦作留出完整一天。', highlights: ['红石峡', '潭瀑峡', '茱萸峰'], map: 'https://uri.amap.com/search?keyword=云台山景区&city=焦作&callnative=0' },
  { name: '宝泉', range: '约 53—63 km', time: '驾车约 85 分钟', type: '峡谷 · 瀑布', mark: '泉', tone: 'moss', image: '/images/spots/baoquan.jpg', desc: '高峡、碧水与成群瀑布是这里的主角，适合安排一日亲水游并预留充足步行时间。', highlights: ['翡翠湖色', '峡谷瀑布', '亲水步道'], map: 'https://uri.amap.com/search?keyword=河南宝泉旅游区&city=新乡&callnative=0' },
];

const nearbyPlans: Record<NearbyPlanId, NearbyPlan> = {
  canyon: {
    eyebrow: '一村一峡 · 山水慢游', name: '南坡村落 + 青龙峡', days: '1 DAY', fit: '家庭 / 摄影 / 避暑', distance: '单程约 16 km', color: 'green',
    summary: '上午沿村内公共文化空间慢慢走，午后进入青龙峡，把乡村更新与太行山水放进同一天。',
    stops: [
      { time: '09:00', title: '大南坡村', detail: '艺术中心、乡村书店与老村散步' },
      { time: '12:00', title: '村中午餐', detail: '提前向民宿或村庄服务点预约' },
      { time: '13:30', title: '青龙峡', detail: '沿峡谷、潭瀑与山路轻徒步' },
      { time: '17:30', title: '返回南坡', detail: '住进山居，留一晚看山间暮色' },
    ],
    tips: ['山区弯道较多，建议白天行车', '景区开放安排可能调整，出发前确认'],
  },
  yuntai: {
    eyebrow: '经典一日 · 山水首选', name: '云台山峡谷一日', days: '1 DAY', fit: '初访 / 山水 / 徒步', distance: '单程约 35—45 km', color: 'blue',
    summary: '从大南坡出发，把红石峡与潭瀑峡安排在同一天；想登高可用茱萸峰替换一个峡谷。',
    stops: [
      { time: '07:30', title: '大南坡出发', detail: '早餐后自驾前往云台山游客中心' },
      { time: '09:00', title: '红石峡', detail: '峡谷步道较集中，建议错峰进入' },
      { time: '12:00', title: '岸上服务区', detail: '午餐、补水并确认下午交通' },
      { time: '13:30', title: '潭瀑峡 / 茱萸峰', detail: '亲水轻徒步或登高观景二选一' },
      { time: '18:30', title: '返回南坡', detail: '也可住岸上小镇，次日继续深度游' },
    ],
    tips: ['景区面积大，不建议一天塞满所有点位', '景区内交通以当日观光车安排为准'],
  },
  culture: {
    eyebrow: '人文两日 · 古村与非遗', name: '怀川文化打卡线', days: '2 DAYS', fit: '亲子 / 研学 / 非遗', distance: '单程最远约 32 km', color: 'orange',
    summary: '第一天认识大南坡的乡村更新与当阳峪绞胎瓷，第二天串联圆融寺、圆通寺和恩州驿。',
    stops: [
      { time: 'D1 上午', title: '大南坡村', detail: '乡村更新、艺术空间与在地午餐' },
      { time: 'D1 下午', title: '当阳峪绞胎瓷博物馆', detail: '看绞胎纹理、窑火与非遗工艺' },
      { time: 'D1 晚间', title: '恩州驿', detail: '逛古街、非遗市集与夜间灯景' },
      { time: 'D2 上午', title: '圆融寺', detail: '沿山寺院落慢走，看碑塔与古建' },
      { time: 'D2 下午', title: '圆通寺', detail: '到巡返村山麓看寺院与太行远景' },
    ],
    tips: ['两日线路建议自驾或包车', '研学与讲解项目建议提前预约'],
  },
};

export function PublicWindow({ onManage, onFarmer, onLogin }: { onManage: () => void; onFarmer: () => void; onLogin: () => void }) {
  const [routeType, setRouteType] = useState<RouteType>('drive');
  const [toast, setToast] = useState('');
  const [orderItem, setOrderItem] = useState<ProductCard | null>(null);
  const [storyItem, setStoryItem] = useState<ProductCard | null>(null);
  const [detailItem, setDetailItem] = useState<ProductCard | null>(null);
  const [videoItem, setVideoItem] = useState<ExperienceCard | null>(null);
  const [inquiryTarget, setInquiryTarget] = useState<InquiryTarget | null>(null);
  const [stayPage, setStayPage] = useState(1);
  const [goodsPage, setGoodsPage] = useState(1);
  const [experiencePage, setExperiencePage] = useState(1);
  const [homeData, setHomeData] = useState<PublicHomeData | null>(null);
  const [catalogError, setCatalogError] = useState('');
  const [activeNav, setActiveNav] = useState<PublicNavId>('top');
  const stayPageSize = 3;
  const goodsPageSize = 4;
  const experiencePageSize = 3;
  const notify = (message: string) => { setToast(message); window.setTimeout(() => setToast(''), 2400); };
  const reloadCatalog = useCallback(() => {
    setCatalogError('');
    loadPublicHomeData()
      .then(setHomeData)
      .catch((reason) => setCatalogError(reason instanceof ApiError ? reason.message : '公开内容加载失败'));
  }, []);

  useEffect(() => reloadCatalog(), [reloadCatalog]);

  useEffect(() => {
    if (!homeData) return;
    const sectionIds: PublicNavId[] = ['top', 'route', 'nearby', 'stay', 'goods'];
    const observer = new IntersectionObserver((entries) => {
      const visible = entries
        .filter((entry) => entry.isIntersecting)
        .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0];
      if (visible) setActiveNav(visible.target.id as PublicNavId);
    }, { rootMargin: '-18% 0px -62% 0px', threshold: [0, 0.05, 0.25] });
    sectionIds.forEach((id) => {
      const section = document.getElementById(id);
      if (section) observer.observe(section);
    });
    return () => observer.disconnect();
  }, [homeData]);

  if (!homeData) {
    return <main className="app-state">{catalogError ? <><h1>暂时无法打开乡见西村</h1><p>{catalogError}</p><button onClick={reloadCatalog}>重新加载</button></> : <><span className="state-spinner"/><h1>正在打开乡见西村…</h1><p>读取村庄、行程、民宿与农品的最新公开信息。</p></>}</main>;
  }

  const routeMap = Object.fromEntries(homeData.routes.map((item) => [item.kind, { title: item.title, time: item.duration, steps: item.steps, note: item.note }]));
  const route = routeMap[routeType];
  const publicStayCards: StayCard[] = homeData.homestays.items.map((item) => ({ id: item.id, name: item.name, type: item.type, desc: item.summary, price: item.price, image: item.coverUrl, beds: item.capacity, externalUrl: item.externalUrl }));
  const publicGoodsCards: ProductCard[] = homeData.products.items.map((item) => ({ id: item.id, name: item.name, icon: item.name.slice(0, 1), season: item.season, desc: item.summary, price: `¥ ${Number(item.startingPrice).toFixed(2)} 起`, image: item.coverUrl }));
  const publicExperienceCards: ExperienceCard[] = homeData.experiences.items.map((item) => ({ id: item.id, name: item.name, type: item.type, season: item.season, duration: item.duration, desc: item.summary, price: item.price, image: item.coverUrl, hasVideo: Boolean(item.videoUrl), video: item.videoUrl }));
  const spotTones = ['moss', 'sand', 'clay', 'ochre', 'pine', 'stone', 'blue'];
  const publicNearbySpots: NearbySpot[] = homeData.attractions.items.map((item, index) => ({ name: item.name, range: item.distanceKm <= 1 ? '村内出发' : `约 ${Math.max(0, Math.round(item.distanceKm - 5))}—${Math.round(item.distanceKm + 5)} km`, time: item.driveMinutes <= 0 ? '建议慢游 2—3 小时' : `驾车约 ${item.driveMinutes} 分钟`, type: item.category, mark: item.name.slice(0, 1), tone: spotTones[index % spotTones.length], image: item.coverUrl, desc: item.summary, highlights: item.highlights, map: item.mapUrl }));
  const publicNearbyPlans = Object.fromEntries(homeData.travelPlans.map((item, index) => [item.slug, { eyebrow: '从南坡出发', name: item.name, days: item.duration, fit: item.suitableFor, distance: item.distance, summary: item.summary, color: ['green', 'blue', 'orange'][index % 3], stops: item.stops, tips: item.tips }])) as Record<string, NearbyPlan>;

  return (
    <main className="public-window">
      <header className="site-header">
        <a className="brand" href="#top"><span className="brand-seal">乡</span><span><b>乡见西村</b><small>DISCOVER XICUN</small></span></a>
        <nav aria-label="主要导航"><a href="#about">走进南坡</a><a href="#route">行前指南</a><a href="#nearby">特色周边游</a><a href="#experience">游玩采摘</a><a href="#stay">山居一晚</a><a href="#goods">山野好物</a></nav>
        <div className="header-actions"><button className="weather" onClick={onLogin}>客户登录</button><button className="weather farmer-entry" onClick={onFarmer}>村民订单</button><button className="manage" onClick={onManage}>内容管理 ↗</button></div>
      </header>

      <section className="hero" id="top">
        <div className="hero-copy">
          <div className="location-line"><span>HENAN · JIAOZUO · XIUWU</span><i /></div>
          <h1>山在这里，<br/>风也在这里，<br/><em>日子慢下来。</em></h1>
          <p>{homeData.site.summary}</p>
          <div className="hero-buttons"><button onClick={() => document.getElementById('route')?.scrollIntoView({behavior:'smooth'})}>规划我的南坡之行 <span>→</span></button><button className="play" onClick={() => notify('南坡声音故事即将上线')}>▶ <span>听一段南坡的声音</span></button></div>
          <div className="hero-facts"><div><b>20<sup>km</sup></b><small>距焦作市区约</small></div><div><b>{homeData.site.recommendedSeason}</b><small>推荐到访季节</small></div><div><b>4<sup>处</sup></b><small>公共文化空间</small></div></div>
        </div>
        <div className="hero-visual">
          <Image src="/images/nanpo-architecture.png" alt="乡见西村传统建筑与老树" fill priority sizes="50vw" />
          <div className="image-caption"><span>01</span><p>旧日大队部的院落<br/><small>大南坡 · 西村乡</small></p></div>
          <div className="postcard"><b>太行山下</b><span>一座会生长的村庄</span><i>大南坡</i></div>
        </div>
        <aside className="hero-rail"><span>SCROLL TO EXPLORE</span><i /></aside>
      </section>

      <section className="quick-window" aria-label="南坡信息概览">
        <a href="#route"><span className="quick-no">01</span><div><small>HOW TO ARRIVE</small><b>怎么来南坡</b></div><i>↗</i></a>
        <a href="#stay"><span className="quick-no">02</span><div><small>STAY IN VILLAGE</small><b>住进山居院落</b></div><i>↗</i></a>
        <a href="#goods"><span className="quick-no">03</span><div><small>LOCAL HARVEST</small><b>把山野带回家</b></div><i>↗</i></a>
        <a href="#nearby"><span className="quick-no">04</span><div><small>FEATURED DAY TRIPS</small><b>特色周边游</b></div><i>↗</i></a>
      </section>

      <section className="about-section" id="about">
        <div className="section-kicker"><span>01</span><small>THE VILLAGE</small></div>
        <div className="about-grid">
          <div className="about-copy"><span>走进南坡 · 太行山下的村庄新生</span><h2>旧砖墙没有被推倒，<br/>它们只是长出了新的故事。</h2><p>大南坡位于修武县西村乡东北部浅山区，由四个自然村组成。这里曾因煤而兴，也经历过资源退去后的沉寂。2020 年起，老大队部、旧礼堂、粮库和供销社在保留原有尺度与材料的基础上被重新使用，陆续成为艺术中心、乡村书店、社区营造中心和工销社。今天的南坡既看得见太行山前的古树、田园和灰砖院落，也能在公共文化空间里遇见展览、阅读、怀梆戏与在地物产。</p><div className="quote">“不修饰、不掩盖，让时间在空间里沉淀。”</div></div>
          <div className="about-collage"><figure className="large"><Image src="/images/nanpo-courtyard.png" alt="乡见西村绿意院落与石板小径" fill sizes="42vw" /></figure><figure className="small"><Image src="/images/nanpo-sign.png" alt="秋日银杏下的大南坡村牌" fill sizes="18vw" /></figure><span className="year-mark">2026<small>乡村美学更新启程</small></span></div>
        </div>
        <div className="village-checkin-head"><span>村内风景与打卡点</span><h3>第一次来，沿着这些地方慢慢走。</h3><p>建议从艺术中心进入村庄，步行串联书店、工销社、戏台与老村院落；具体开放与活动安排以现场为准。</p></div>
        <div className="culture-spaces"><article><span>01</span><h3>大南坡艺术中心</h3><p>老大队部办公室和粮库更新成展览空间，旧梁架、灰砖墙和院落尺度都被保留下来。</p></article><article><span>02</span><h3>方所乡村文化</h3><p>由老礼堂与戏台更新而来，阅读、展览和村庄日常在同一个屋檐下相遇。</p></article><article><span>03</span><h3>碧山工销社</h3><p>在旧供销社里认识民间百工、当代设计和标注着农户名字的南坡物产。</p></article><article><span>04</span><h3>社区营造中心</h3><p>村民议事、公共活动与访客交流的共享空间，也是理解南坡社区生活的一扇窗口。</p></article><article><span>05</span><h3>怀梆戏台与老礼堂</h3><p>寻找地方戏曲与集体记忆留下的痕迹，在节庆和活动时感受村庄声音。</p></article><article><span>06</span><h3>古树、村牌与灰砖院落</h3><p>从秋日银杏下的村牌走进老村，在石板路、树影和太行田园间留下南坡照片。</p></article></div>
      </section>

      <section className="route-section" id="route">
        <div className="section-kicker light"><span>02</span><small>HOW TO ARRIVE</small></div>
        <div className="route-head"><div><span>行前指南</span><h2>从城市出发，<br/>向山的方向走。</h2></div><p>目的地：{homeData.site.address}<br/>建议导航至“{homeData.site.mapKeyword}”</p></div>
        <div className="route-planner">
          <div className="route-tabs"><button disabled={!routeMap.drive} className={routeType==='drive'?'active':''} onClick={() => setRouteType('drive')}>自驾前往</button><button disabled={!routeMap.rail} className={routeType==='rail'?'active':''} onClick={() => setRouteType('rail')}>高铁 + 打车</button><button disabled={!routeMap.taxi} className={routeType==='taxi'?'active':''} onClick={() => setRouteType('taxi')}>市区打车</button></div>
          {route ? <div className="route-content"><div className="route-summary"><small>RECOMMENDED ROUTE</small><h3>{route.title}</h3><strong>{route.time}</strong><p>{route.note}</p><button onClick={() => notify(`已复制目的地：${homeData.site.mapKeyword}`)}>复制目的地地址 ↗</button></div><div className="route-line">{route.steps.map((step,index)=><div key={step}><span>{index+1}</span><b>{step}</b>{index<route.steps.length-1&&<i/>}</div>)}</div><AmapLocationMap/></div> : <EmptyState label="出行路线"/>}
        </div>
        <div className="travel-note"><span>出发提醒</span><p>山区叫车和道路情况可能临时变化；节假日建议提前预约车辆，确认返程安排，并优先选择白天进村。</p><button onClick={() => notify('行前提醒已保存')}>保存提醒</button></div>
      </section>

      <NearbyTravel notify={notify} spots={publicNearbySpots} plans={publicNearbyPlans} />

      <section className="experience-section" id="experience">
        <div className="section-kicker light"><span>04</span><small>PLAY & HARVEST</small></div>
        <div className="experience-head"><div><span>跟着节气来玩</span><h2>不只看风景，<br/>也亲手参与一场收成。</h2></div><p>采摘、农耕、手作与村庄导览都可由后台持续上架；项目可配置季节、价格、名额、图集和视频。</p></div>
        {publicExperienceCards.length ? <><div className="experience-grid">{publicExperienceCards.slice((experiencePage-1)*experiencePageSize,experiencePage*experiencePageSize).map((item)=><article key={item.name}><div className="experience-media"><Image src={item.image} alt={item.name} fill sizes="33vw"/>{item.hasVideo?<button onClick={()=>setVideoItem(item)} aria-label={`播放${item.name}视频`}><i>▶</i><span>视频看现场</span></button>:<span className="photo-badge">图集</span>}<small>{item.season}</small></div><div className="experience-info"><span>{item.type} · {item.duration}</span><h3>{item.name}</h3><p>{item.desc}</p><footer><strong>{item.price}</strong><button disabled={!item.id} onClick={()=>item.id&&setInquiryTarget({sourceType:'EXPERIENCE',sourceId:item.id,name:item.name})}>留言咨询 →</button></footer></div></article>)}</div><Pagination page={experiencePage} total={publicExperienceCards.length} pageSize={experiencePageSize} onChange={setExperiencePage} label="游玩采摘项目" /></> : <EmptyState label="游玩采摘项目"/>}
        <div className="experience-manage"><div><span>村庄运营方</span><h3>季节变了，项目也可以随时更新。</h3><p>后台可设置开放日期、每日名额、预约电话、封面图与介绍视频。</p></div><button onClick={onManage}>去后台配置项目 →</button></div>
      </section>

      <section className="stay-section" id="stay">
        <div className="section-kicker"><span>05</span><small>STAY IN NANPO</small></div>
        <div className="section-title-row"><div><span>在村里住一晚</span><h2>推开院门，听见山里的清晨。</h2></div><p>现有公开资料显示村内已建设多套山居民宿。以下房源内容为高保真示例，具体名称、价格与联系方式将在管理后台录入后公开。</p></div>
        {publicStayCards.length ? <><div className="stay-grid">{publicStayCards.slice((stayPage-1)*stayPageSize,stayPage*stayPageSize).map((item,index)=><article key={item.name}><div className="stay-image"><Image src={item.image} alt={item.name} fill sizes="33vw"/><span>{String((stayPage-1)*stayPageSize+index+1).padStart(2,'0')}</span><button onClick={() => notify(`${item.name}已加入收藏`)}>收藏 ♡</button></div><div className="stay-info"><small>{item.type}</small><h3>{item.name}</h3><p>{item.desc}</p><div><span>住 {item.beds}</span><strong>{item.price}</strong><span className="stay-actions">{item.externalUrl&&<a href={item.externalUrl} target="_blank" rel="noreferrer">民宿主页 ↗</a>}<button disabled={!item.id} onClick={()=>item.id&&setInquiryTarget({sourceType:'HOMESTAY',sourceId:item.id,name:item.name})}>留言咨询 →</button></span></div></div></article>)}</div><Pagination page={stayPage} total={publicStayCards.length} pageSize={stayPageSize} onChange={setStayPage} label="民宿" /></> : <EmptyState label="民宿"/>}
        <div className="operator-cta"><div><span>你是南坡民宿经营者？</span><h3>把你的院子，也放进这扇窗。</h3></div><button onClick={onManage}>去后台上架房源 →</button></div>
      </section>

      <section className="goods-section" id="goods">
        <div className="goods-intro"><div className="section-kicker light"><span>06</span><small>LOCAL HARVEST</small></div><span>{homeData.site.goodsSection?.eyebrow || '山野好物'}</span><h2>{homeData.site.goodsSection?.title || '每一份收成都有自己的时节。'}</h2><p>{homeData.site.goodsSection?.description || '山核桃、山花椒、小米与蜂蜜，是公开旅游资料中推荐的焦作山野物产。具体商品、价格和村民联系方式由后台上架。'}</p><div className="season"><b>{homeData.site.goodsSection?.seasonLabel || '八月'}</b><span><i style={{width:'72%'}}/>{homeData.site.goodsSection?.seasonNote || '核桃与花椒陆续成熟'}</span></div></div>
        <div className="goods-visual"><Image src={homeData.site.goodsSection?.imageUrl || '/images/nanpo-workshop.png'} alt={homeData.site.goodsSection?.imageCaption || '乡见西村工销社与乡土手作陈列'} fill sizes="35vw"/><span>{homeData.site.goodsSection?.imageCaption || '工销社里的山野收成'}</span></div>
        <div className="goods-list">{publicGoodsCards.length ? <>{publicGoodsCards.slice((goodsPage-1)*goodsPageSize,goodsPage*goodsPageSize).map((item,index)=><article key={item.name}><span className="goods-index">{String((goodsPage-1)*goodsPageSize+index+1).padStart(2,'0')}</span><div className="goods-icon">{item.icon}</div><div><small>{item.season}</small><h3><button className="goods-name" disabled={!item.id} onClick={() => setDetailItem(item)} aria-label={`查看${item.name}商品详情`}>{item.name}</button></h3><p>{item.desc}</p></div><strong>{item.price}</strong><button className="trace-button" onClick={() => setStoryItem(item)}>看过程</button><button onClick={() => setOrderItem(item)}>购买</button></article>)}<Pagination page={goodsPage} total={publicGoodsCards.length} pageSize={goodsPageSize} onChange={setGoodsPage} label="农产品" dark /></> : <EmptyState label="农产品"/>}</div>
      </section>

      <section className="day-trip"><div className="day-photo"><Image src="/images/nanpo-autumn.png" alt="乡见西村秋日古建与金色树影" fill sizes="40vw"/><span>ONE DAY IN NANPO</span></div><div className="day-copy"><span>一日南坡建议</span><h2>不赶路，去感受。</h2><div className="timeline"><div><b>09:30</b><p><strong>抵达大南坡</strong><small>从艺术中心开始认识村庄</small></p></div><div><b>11:00</b><p><strong>方所乡村文化</strong><small>在老戏台改成的书店慢慢读</small></p></div><div><b>13:30</b><p><strong>老村散步</strong><small>沿灰砖院落与古树寻找乡土日常</small></p></div><div><b>16:00</b><p><strong>碧山工销社</strong><small>挑一份山野物产带回家</small></p></div></div><button onClick={() => notify('一日游路线已保存')}>收藏这条路线 →</button></div></section>

      <footer className="site-footer"><div className="footer-brand"><span className="brand-seal">乡</span><h2>乡见西村</h2><p>{homeData.site.summary}</p></div><div><small>来南坡</small><a href="#route">出行路线</a><a href="#nearby">特色周边游</a><a href="#experience">游玩与采摘</a><a href="#stay">民宿山居</a><a href="#goods">乡野好物</a></div><div><small>认识南坡</small><a href="#about">村庄故事</a><a href="#about">文化空间</a><button onClick={onManage}>内容管理</button></div><div className="footer-contact"><small>访客服务</small><strong>{homeData.site.visitorService?.phone || '暂未开通'}</strong><p>{homeData.site.address}<br/>{homeData.site.visitorService?.businessHours}</p></div><div className="source-note">路线与村庄资料来自后台已发布数据。页面距离、车程为从大南坡村出发的规划估算，不代表实时导航；出发前请复核路况、班次、票务与开放安排。</div></footer>
      <nav className="mobile-floating-nav" aria-label="手机快捷导航">
        {([
          ['top', '⌂', '首页'],
          ['route', '行', '路线'],
          ['nearby', '游', '特色'],
          ['stay', '宿', '民宿'],
          ['goods', '物', '农品'],
        ] as [PublicNavId, string, string][]).map(([id, icon, label]) => <a
          key={id}
          href={`#${id}`}
          className={activeNav === id ? 'active' : ''}
          aria-current={activeNav === id ? 'location' : undefined}
          onClick={() => setActiveNav(id)}
        ><span aria-hidden="true">{icon}</span><b>{label}</b></a>)}
      </nav>
      {detailItem&&<ProductDetailModal product={detailItem} onClose={()=>setDetailItem(null)} onStory={()=>{setDetailItem(null);setStoryItem(detailItem)}} onBuy={()=>{setDetailItem(null);setOrderItem(detailItem)}}/>}
      {storyItem&&<ProductStory product={storyItem} onClose={()=>setStoryItem(null)} onBuy={()=>{setStoryItem(null);setOrderItem(storyItem)}}/>}
      {orderItem&&<CheckoutFlow product={orderItem} onClose={()=>setOrderItem(null)} onLogin={onLogin}/>}
      {videoItem&&<VideoPreview item={videoItem} onClose={()=>setVideoItem(null)}/>}
      {inquiryTarget&&<InquiryModal target={inquiryTarget} onClose={()=>setInquiryTarget(null)} onSuccess={()=>{setInquiryTarget(null);notify('留言已提交，村庄运营人员会电话回访')}}/>}
      {toast&&<div className="toast">✓ {toast}</div>}
    </main>
  );
}

function ProductDetailModal({ product, onClose, onStory, onBuy }: { product: ProductCard; onClose: () => void; onStory: () => void; onBuy: () => void }) {
  const [detail, setDetail] = useState<ProductDetail | null>(null);
  const [error, setError] = useState('');
  const reload = useCallback(() => {
    if (!product.id) {
      setError('该商品还没有可公开的详情');
      return;
    }
    setError('');
    loadPublicProduct(product.id)
      .then(setDetail)
      .catch((reason) => setError(reason instanceof ApiError ? reason.message : '商品详情加载失败'));
  }, [product.id]);

  useEffect(() => reload(), [reload]);

  return <div className="modal-backdrop product-detail-backdrop"><section className="product-detail-modal" aria-modal="true" role="dialog" aria-labelledby="product-detail-title">
    <header><div><small>LOCAL HARVEST · 商品详情</small><h2 id="product-detail-title">{product.name}</h2></div><button onClick={onClose} aria-label="关闭商品详情">×</button></header>
    {!detail&&!error&&<div className="product-detail-state"><span className="state-spinner"/><p>正在读取商品信息…</p></div>}
    {error&&<div className="product-detail-state"><strong>{error}</strong><button onClick={reload}>重新加载</button></div>}
    {detail&&<>
      <div className="product-detail-overview">
        <ProductImageGallery product={detail.product} fallbackUrl={product.image}/>
        <div className="product-detail-copy"><div className="product-detail-tags"><span>{detail.product.category}</span><span>{detail.product.season}</span></div><h3>{detail.product.name}</h3><p>{detail.product.summary}</p><div className="product-detail-farmer"><span>{detail.farmer.name.slice(0,1)}</span><div><small>提交农户</small><strong>{detail.farmer.name}</strong></div><i>{detail.farmer.certificationStatus === 'APPROVED' ? '身份已审核 ✓' : '身份待审核'}</i></div></div>
      </div>
      <div className="product-detail-specs"><header><div><small>AVAILABLE OPTIONS</small><h3>可售规格</h3></div><span>共 {detail.skus.length} 种</span></header>{detail.skus.length ? <div>{detail.skus.map((sku) => <article key={sku.id}><div><strong>{sku.specification}</strong><small>规格编号：{sku.code}</small></div><b>¥ {Number(sku.unitPrice).toFixed(2)}</b><p>{sku.stockNote || '库存以提交订单时为准'}</p></article>)}</div> : <EmptyState label="可售规格"/>}</div>
      <footer><button className="detail-story" onClick={onStory}>查看真实生产过程</button><button className="detail-buy" disabled={!detail.skus.length} onClick={onBuy}>选择规格并购买 →</button></footer>
    </>}
  </section></div>;
}

function ProductImageGallery({ product, fallbackUrl }: { product: ProductDetail['product']; fallbackUrl?: string }) {
  const images = product.imageUrls?.length
    ? product.imageUrls
    : [product.coverUrl || fallbackUrl || '/images/products.jpg'];
  const [activeIndex, setActiveIndex] = useState(0);

  return <div className="product-detail-gallery">
    <div className="product-detail-cover">
      <Image src={images[activeIndex] || images[0]} alt={`${product.name} 图片 ${activeIndex + 1}`} fill sizes="320px"/>
      {images.length > 1 && <span>{activeIndex + 1} / {images.length}</span>}
    </div>
    {images.length > 1 && <div className="product-detail-thumbnails" aria-label={`${product.name}图片列表`}>
      {images.map((url, index) => <button key={url} type="button" className={index === activeIndex ? 'active' : ''} onClick={() => setActiveIndex(index)} aria-label={`查看第${index + 1}张图片`} aria-pressed={index === activeIndex}>
        <img src={url} alt=""/>
      </button>)}
    </div>}
  </div>;
}

function EmptyState({ label }: { label: string }) {
  return <div className="section-empty"><span>乡</span><h3>暂无{label}</h3><p>运营人员发布内容后，将自动在这里展示。</p></div>;
}

function Pagination({ page, total, pageSize, onChange, label, dark = false }: { page: number; total: number; pageSize: number; onChange: (page: number) => void; label: string; dark?: boolean }) {
  const pages = Math.max(1, Math.ceil(total / pageSize));
  return <nav className={`pagination ${dark ? 'dark' : ''}`} aria-label={`${label}分页`}><span>共 {total} 项</span><div><button disabled={page<=1} onClick={()=>onChange(page-1)} aria-label={`上一页${label}`}>←</button>{Array.from({length:pages},(_,index)=>index+1).map(item=><button key={item} className={page===item?'active':''} onClick={()=>onChange(item)} aria-current={page===item?'page':undefined}>{String(item).padStart(2,'0')}</button>)}<button disabled={page>=pages} onClick={()=>onChange(page+1)} aria-label={`下一页${label}`}>→</button></div><small>{String(page).padStart(2,'0')} / {String(pages).padStart(2,'0')}</small></nav>;
}

function InquiryModal({ target, onClose, onSuccess }: {
  target: InquiryTarget;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const now = new Date();
  const localNow = new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
  const tomorrow = new Date(now.getTime() + 24 * 60 * 60_000);
  const defaultVisitAt = new Date(tomorrow.getTime() - tomorrow.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setBusy(true);
    setError('');
    try {
      await submitConsultation({
        sourceType: target.sourceType,
        sourceId: target.sourceId,
        visitAt: String(form.get('visitAt')),
        partySize: Number(form.get('partySize')),
        callbackPhone: String(form.get('callbackPhone')),
        note: String(form.get('note') || ''),
      });
      onSuccess();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '留言提交失败，请稍后重试');
    } finally {
      setBusy(false);
    }
  };
  return <div className="modal-backdrop inquiry-backdrop"><form className="inquiry-modal" onSubmit={submit}>
    <header><div><small>{target.sourceType === 'HOMESTAY' ? 'HOMESTAY INQUIRY' : 'EXPERIENCE INQUIRY'}</small><h2>咨询 {target.name}</h2></div><button type="button" onClick={onClose} aria-label="关闭留言">×</button></header>
    <p>留下到访计划，村庄运营人员会按回访电话与您确认接待安排。</p>
    {error&&<div className="login-error" role="alert">{error}</div>}
    <div className="form-grid"><label>计划到访时间<input name="visitAt" type="datetime-local" min={localNow} defaultValue={defaultVisitAt} required/></label><label>到访人数<input name="partySize" type="number" min="1" max="100" defaultValue="2" required/></label></div>
    <label>回访电话<input name="callbackPhone" type="tel" inputMode="tel" pattern="1\d{10}" maxLength={11} placeholder="请填写 11 位手机号" required/></label>
    <label>备注<textarea name="note" maxLength={1000} placeholder="例如：有老人同行、希望安排亲子采摘、预计入住两晚等（选填）"/></label>
    <footer><button type="button" onClick={onClose}>取消</button><button className="checkout-primary" type="submit" disabled={busy}>{busy?'正在提交…':'提交留言 →'}</button></footer>
  </form></div>;
}

function VideoPreview({ item, onClose }: { item: ExperienceCard; onClose: () => void }) {
  return <div className="modal-backdrop video-backdrop"><section className="video-modal"><header><div><small>FIELD VIDEO · 项目实拍</small><h2>{item.name}</h2></div><button onClick={onClose} aria-label="关闭视频">×</button></header><video controls playsInline preload="metadata" poster={item.image}><source src={item.video} type="video/mp4"/>您的浏览器暂不支持视频播放。</video><footer><div><span>{item.type}</span><strong>{item.season} · {item.duration}</strong></div><p>视频由后台审核发布，同时保留封面、标题与文字说明。</p></footer></section></div>;
}

function NearbyTravel({ notify, spots, plans }: { notify: (message: string) => void; spots: NearbySpot[]; plans: Record<string, NearbyPlan> }) {
  const [planId, setPlanId] = useState<NearbyPlanId>(Object.keys(plans)[0] || '');
  const [spotIndex, setSpotIndex] = useState(0);
  const [visibleSpotCount, setVisibleSpotCount] = useState(3);
  const [spotAutoplayEnabled, setSpotAutoplayEnabled] = useState(() => !window.matchMedia('(prefers-reduced-motion: reduce)').matches);
  const [spotInteractionPaused, setSpotInteractionPaused] = useState(false);
  const spotCarouselRef = useRef<HTMLDivElement | null>(null);
  const spotScrollFrameRef = useRef<number | null>(null);
  const plan = plans[planId] || Object.values(plans)[0];

  useEffect(() => {
    const viewport = spotCarouselRef.current;
    if (!viewport) return;
    const syncLayout = () => {
      const cards = Array.from(viewport.querySelectorAll<HTMLElement>('.spot-card'));
      if (!cards.length) return;
      const cardWidth = cards[0].offsetWidth || viewport.clientWidth;
      const count = Math.max(1, Math.round(viewport.clientWidth / cardWidth));
      setVisibleSpotCount(count);
      setSpotIndex((current) => {
        const next = Math.min(current, Math.max(0, cards.length - count));
        requestAnimationFrame(() => viewport.scrollTo({ left: cards[next]?.offsetLeft || 0 }));
        return next;
      });
    };
    const resizeObserver = new ResizeObserver(syncLayout);
    resizeObserver.observe(viewport);
    syncLayout();
    return () => {
      resizeObserver.disconnect();
      if (spotScrollFrameRef.current !== null) cancelAnimationFrame(spotScrollFrameRef.current);
    };
  }, [spots.length]);

  const moveSpots = useCallback((direction: -1 | 1) => {
    const viewport = spotCarouselRef.current;
    if (!viewport) return;
    const cards = Array.from(viewport.querySelectorAll<HTMLElement>('.spot-card'));
    if (!cards.length) return;
    setSpotIndex((current) => {
      const lastStart = Math.max(0, cards.length - visibleSpotCount);
      let next = current + direction * visibleSpotCount;
      if (next > lastStart) next = 0;
      if (next < 0) next = lastStart;
      viewport.scrollTo({ left: cards[next].offsetLeft, behavior: 'smooth' });
      return next;
    });
  }, [visibleSpotCount]);

  useEffect(() => {
    if (!spotAutoplayEnabled || spotInteractionPaused || spots.length <= visibleSpotCount) return;
    const timer = window.setInterval(() => moveSpots(1), 3000);
    return () => window.clearInterval(timer);
  }, [moveSpots, spotAutoplayEnabled, spotInteractionPaused, spots.length, visibleSpotCount]);

  const syncSpotIndex = () => {
    const viewport = spotCarouselRef.current;
    if (!viewport) return;
    if (spotScrollFrameRef.current !== null) cancelAnimationFrame(spotScrollFrameRef.current);
    spotScrollFrameRef.current = requestAnimationFrame(() => {
      const cards = Array.from(viewport.querySelectorAll<HTMLElement>('.spot-card'));
      if (!cards.length) return;
      const nextIndex = cards.reduce((closest, card, index) => (
        Math.abs(card.offsetLeft - viewport.scrollLeft) < Math.abs(cards[closest].offsetLeft - viewport.scrollLeft)
          ? index : closest
      ), 0);
      setSpotIndex(Math.min(nextIndex, Math.max(0, cards.length - visibleSpotCount)));
    });
  };
  const visibleSpotEnd = Math.min(spots.length, spotIndex + visibleSpotCount);
  const spotPositionLabel = visibleSpotCount === 1
    ? `${String(spotIndex + 1).padStart(2, '0')} / ${String(spots.length).padStart(2, '0')}`
    : `${String(spotIndex + 1).padStart(2, '0')}–${String(visibleSpotEnd).padStart(2, '0')} / ${String(spots.length).padStart(2, '0')}`;
  return <section className="nearby-section" id="nearby">
    <div className="section-kicker"><span>03</span><small>FEATURED TRIPS FROM NANPO</small></div>
    <div className="nearby-head">
      <div><span>特色周边游</span><h2>以村庄为圆心，<br/>打开周边游玩路线。</h2></div>
      <div className="radius-note"><span className="radius-rings"><i/><i/><i/><b>南坡</b></span><p><strong>100 km</strong> 旅行生活圈<small>所有目的地均按从大南坡村出发估算</small></p></div>
    </div>

    {spots.length ? <div className="nearby-carousel" onTouchStart={() => setSpotInteractionPaused(true)} onTouchEnd={() => setSpotInteractionPaused(false)} onFocusCapture={() => setSpotInteractionPaused(true)} onBlurCapture={(event) => { if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setSpotInteractionPaused(false); }}>
      <div className="nearby-carousel-toolbar"><span>每 3 秒自动切换，也可横向滑动</span><div><b aria-live="polite" aria-atomic="true">{spotPositionLabel}</b><button type="button" onClick={() => setSpotAutoplayEnabled((current) => !current)} aria-label={spotAutoplayEnabled ? '暂停自动轮播' : '继续自动轮播'}>{spotAutoplayEnabled ? 'Ⅱ' : '▶'}</button><button type="button" onClick={() => moveSpots(-1)} aria-label="上一组周边景点">←</button><button type="button" onClick={() => moveSpots(1)} aria-label="下一组周边景点">→</button></div></div>
      <div className="nearby-spots" ref={spotCarouselRef} onScroll={syncSpotIndex} tabIndex={0} role="region" aria-roledescription="轮播" aria-label="特色周边景点" onKeyDown={(event) => { if (event.key === 'ArrowLeft') { event.preventDefault(); moveSpots(-1); } else if (event.key === 'ArrowRight') { event.preventDefault(); moveSpots(1); } }}>
        {spots.map((spot, index) => <article key={spot.name} className={`spot-card ${spot.tone}`}><Image className="spot-bg" src={spot.image} alt={`${spot.name}实景`} fill sizes="(max-width: 760px) 100vw, (max-width: 1050px) 50vw, 33vw" />
          <header><span>{spot.mark}</span><small>{String(index + 1).padStart(2, '0')} · {spot.type}</small></header>
          <h3>{spot.name}</h3><p>{spot.desc}</p><ul>{spot.highlights.map(item => <li key={item}>{item}</li>)}</ul>
          <div><strong>{spot.range}</strong><small>{spot.time}</small><a href={spot.map} target="_blank" rel="noreferrer" aria-label={`在地图中查看${spot.name}`}>地图导航 ↗</a></div>
        </article>)}
      </div>
    </div> : <EmptyState label="特色周边景点"/>}

    {plan ? <div className="plan-studio">
      <div className="plan-aside">
        <small>TRIP PLANNER</small><h3>选一条适合你的路线</h3><p>以南坡为起点，按时间、体力和兴趣来安排。</p>
        <div className="plan-tabs">
          {(Object.entries(plans) as [NearbyPlanId, NearbyPlan][]).map(([id, item]) => <button key={id} className={planId === id ? 'active' : ''} onClick={() => setPlanId(id)}><span>{item.days}</span><b>{item.name}</b><small>{item.fit}</small><i>→</i></button>)}
        </div>
      </div>
      <div className={`plan-detail ${plan.color}`}>
        <header><div><small>{plan.eyebrow}</small><h3>{plan.name}</h3></div><span>{plan.distance}</span></header>
        <p className="plan-summary">{plan.summary}</p>
        <div className="plan-schedule">{plan.stops.map((stop, index) => <div key={stop.time + stop.title}><time>{stop.time}</time><i><b>{index + 1}</b></i><p><strong>{stop.title}</strong><small>{stop.detail}</small></p></div>)}</div>
        <footer><div>{plan.tips.map(tip => <span key={tip}>✓ {tip}</span>)}</div><button onClick={() => notify(`已收藏：${plan.name}`)}>收藏这条路线 →</button></footer>
      </div>
    </div> : <EmptyState label="旅行规划"/>}

    <div className="planning-disclaimer"><span>行前复核</span><p>山区道路、景区开放与观光车安排可能随天气和季节调整。建议出发前使用地图重新规划，并向景区或村庄服务点确认。</p><a href="https://wglj.jiaozuo.gov.cn/2026/08-04/610066.html" target="_blank" rel="noreferrer">查看焦作文旅推荐线路 ↗</a></div>
  </section>;
}

function ProductStory({ product, onClose, onBuy }: { product: ProductCard; onClose: () => void; onBuy: () => void }) {
  const [detail, setDetail] = useState<ProductDetail | null>(null);
  const [error, setError] = useState('');
  const reload = useCallback(() => {
    if (!product.id) {
      setError('该农品还没有可公开的溯源编号');
      return;
    }
    setError('');
    loadPublicProduct(product.id)
      .then(setDetail)
      .catch((reason) => setError(reason instanceof ApiError ? reason.message : '生产档案加载失败'));
  }, [product.id]);

  useEffect(() => reload(), [reload]);

  const stageNames: Record<string, string> = {
    PREPARATION: '整地备耕', SOWING: '播种', GROWING: '自然生长',
    HARVEST: '成熟采收', PROCESSING: '加工', PACKING: '分选包装', SHIPPING: '出库发货',
  };
  const records = detail?.productionRecords ?? [];
  const lastUpdated = records.length ? records[records.length - 1].publishedAt || records[records.length - 1].occurredAt : undefined;
  const dateLabel = (value: string) => new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' }).format(new Date(value));
  const fullDateLabel = (value: string) => new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value));

  return <div className="modal-backdrop story-backdrop"><section className="story-modal"><header><div><small>FARM TO TABLE · 真实生产档案</small><h2>{product.name}的一生</h2><p>仅展示农户提交且经后台审核发布的记录</p></div><button onClick={onClose}>×</button></header>
    {detail&&<div className="farmer-proof"><span>{detail.farmer.name.slice(0,1)}</span><div><strong>{detail.farmer.name} · {detail.farmer.villageGroup}</strong><small>{detail.farmer.introduction}</small></div><i>{detail.farmer.certificationStatus === 'APPROVED' ? '身份已审核 ✓' : '身份待审核'}</i></div>}
    <div className="process-timeline">
      {!detail&&!error&&<div className="section-empty"><span className="state-spinner"/><h3>正在读取生产档案…</h3></div>}
      {error&&<div className="section-empty"><span>乡</span><h3>{error}</h3><button onClick={reload}>重新加载</button></div>}
      {detail&&records.length===0&&<EmptyState label="已公开生产记录"/>}
      {records.map((record,index)=>{
        const visualMedia = record.media?.find((media) => media.mediaType === 'IMAGE' || media.mediaType === 'VIDEO');
        const audioMedia = record.media?.filter((media) => media.mediaType === 'AUDIO') ?? [];
        return <article key={record.id}><div className="process-image">{visualMedia?.mediaType === 'VIDEO'
          ? <video src={visualMedia.url} controls playsInline preload="metadata" aria-label={`${stageNames[record.stage] || record.stage}现场视频`}/>
          : <Image src={visualMedia?.url || product.image || '/images/products.jpg'} alt={`${stageNames[record.stage] || record.stage}${visualMedia ? '现场图片' : '商品图片'}`} fill sizes="150px"/>}{visualMedia&&<em>{visualMedia.mediaType === 'VIDEO' ? '现场视频' : '现场图片'}</em>}</div><span>{String(index+1).padStart(2,'0')}</span><div><small>{dateLabel(record.occurredAt)}</small><h3>{stageNames[record.stage] || record.stage}</h3><p>{record.text}</p>{audioMedia.map((media)=><audio key={media.id} className="process-audio" src={media.url} controls preload="metadata" aria-label={`${stageNames[record.stage] || record.stage}现场录音`}/>)}</div></article>;
      })}
    </div>
    <div className="story-actions"><p>{lastUpdated ? `最后发布：${fullDateLabel(lastUpdated)} · 共 ${records.length} 条已公开记录` : '暂无已公开记录'}</p><button onClick={onBuy}>信任这份收成，去购买 →</button></div></section></div>;
}

function DemoQr(){return <div className="demo-qr" aria-label="演示收款码，不可用于真实支付"><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><span>演示<br/>不可支付</span></div>}

function CheckoutFlow({ product, onClose, onLogin }: { product: ProductCard; onClose: () => void; onLogin: () => void }) {
  const [step,setStep]=useState<'form'|'pay'|'done'>('form');
  const [detail,setDetail]=useState<ProductDetail|null>(null);
  const [order,setOrder]=useState<Order|null>(null);
  const [busy,setBusy]=useState(false);
  const [loadingDetail,setLoadingDetail]=useState(true);
  const [error,setError]=useState('');
  const [needsLogin,setNeedsLogin]=useState(false);
  const [staleProduct,setStaleProduct]=useState(false);
  const [idempotencyKey]=useState(()=>`checkout-${crypto.randomUUID()}`);
  const loadDetail=useCallback(async()=>{
    setLoadingDetail(true);setDetail(null);setError('');setNeedsLogin(false);setStaleProduct(false);
    if(!product.id){setError('该商品已下架或商品列表已更新，请刷新商品列表后再试');setStaleProduct(true);setLoadingDetail(false);return}
    try{
      const loaded=await loadPublicProduct(product.id);
      setDetail(loaded);
      if(!loaded.skus.length)setError('当前商品暂时没有可售规格，请稍后再试');
    }catch(reason){
      if(reason instanceof ApiError&&reason.status===404){setStaleProduct(true);setError('该商品已下架或商品列表已更新，请刷新商品列表后再试')}
      else setError(reason instanceof ApiError?reason.message:'商品规格加载失败，请稍后重试');
    }finally{setLoadingDetail(false)}
  },[product.id]);
  useEffect(()=>{void loadDetail()},[loadDetail]);
  const submit=async(event:FormEvent<HTMLFormElement>)=>{event.preventDefault();const sku=detail?.skus[0];if(!sku){setError('当前商品暂时没有可售规格，请稍后再试');return}const form=new FormData(event.currentTarget);setBusy(true);setError('');setNeedsLogin(false);try{const created=await createOrder({recipientName:String(form.get('recipientName')),recipientPhone:String(form.get('recipientPhone')),recipientAddress:String(form.get('recipientAddress')),customerNote:String(form.get('customerNote')||''),items:[{skuId:sku.id,quantity:1}]},idempotencyKey);setOrder(created);setStep('pay')}catch(reason){if(reason instanceof ApiError&&reason.status===401){setNeedsLogin(true);setError('请先使用手机号登录，再提交订单')}else{setError(reason instanceof ApiError?reason.message:'订单创建失败')}}finally{setBusy(false)}};
  const report=async()=>{if(!order)return;setBusy(true);setError('');try{const updated=await reportOrderPayment(order.orderNo,`${order.recipientPhone.slice(-4)} ${product.name}`);setOrder(updated);setStep('done')}catch(reason){setError(reason instanceof ApiError?reason.message:'转账报告提交失败')}finally{setBusy(false)}};
  const price=order?`¥ ${Number(order.totalAmount).toFixed(2)}`:product.price.replace(' 起','');
  return <div className="modal-backdrop checkout-backdrop"><section className="checkout-modal"><header><div><small>ORDER WORKFLOW</small><h2>{step==='form'?'提交购买信息':step==='pay'?'扫码转账':'等待后台核款'}</h2></div><button onClick={onClose}>×</button></header><div className="checkout-steps"><span className="active">1 创建订单</span><i/><span className={step!=='form'?'active':''}>2 报告转账</span><i/><span className={step==='done'?'active':''}>3 人工核款</span></div>{error&&<div className="login-error checkout-error" role="alert"><span>{error}</span>{needsLogin&&<button onClick={onLogin}>去登录</button>}{staleProduct&&<button onClick={()=>window.location.reload()}>刷新商品</button>}{!needsLogin&&!staleProduct&&!detail&&<button onClick={()=>void loadDetail()}>重新加载</button>}</div>}{step==='form'&&<form onSubmit={submit}><div className="order-product"><span>{product.name.slice(0,1)}</span><div><strong>{product.name}</strong><small>{loadingDetail?'正在读取规格…':detail?.skus[0]?.specification||'暂无可售规格'}</small></div><b>{price}</b></div><label>收货人<input name="recipientName" required maxLength={100} placeholder="请输入姓名"/></label><label>联系电话<input name="recipientPhone" required pattern="1\d{10}" inputMode="tel" placeholder="11 位手机号"/></label><label>收货地址<textarea name="recipientAddress" required maxLength={500} placeholder="省 / 市 / 区县 / 街道及详细地址"/></label><label>备注<input name="customerNote" maxLength={500} placeholder="可选"/></label><div className="checkout-notice">后端会生成唯一订单号、锁定价格和收款配置；重复点击使用同一幂等键，不会重复建单。</div><button className="checkout-primary" disabled={busy||loadingDetail||!detail||!detail.skus.length} type="submit">{busy?'正在创建订单…':loadingDetail?'正在读取规格…':'确认信息，下一步 →'}</button></form>}{step==='pay'&&order&&<div className="pay-panel">{order.payment.demo&&<span className="demo-label">本地演示</span>}<DemoQr/><h3>请转账 {price}</h3><p>收款方：{order.payment.payeeName}<br/>转账备注：{order.recipientPhone.slice(-4)} {product.name}<br/>订单号：{order.orderNo}</p>{order.payment.demo&&<div className="pay-warning">此为演示收款配置，不可用于真实支付</div>}<button className="checkout-primary" disabled={busy} onClick={report}>{busy?'正在提交…':'我已完成转账'}</button></div>}{step==='done'&&order&&<div className="order-success"><span>✓</span><h3>转账报告已留痕</h3><p>订单号 {order.orderNo}<br/>当前状态：{order.status}。运营人员核对实际到账后，才会通知农户备货。</p><button className="checkout-primary" onClick={onClose}>完成</button></div>}<footer>村庄统一受理 · 人工核款 · 农户备货 · 统一发货</footer></section></div>;
}

export function FarmerPortal({ onExit }: { onExit: () => void }) {
  const [showUpload,setShowUpload]=useState(false);
  const [toast,setToast]=useState('');
  const [generated,setGenerated]=useState(false);
  const notify=(message:string)=>{setToast(message);window.setTimeout(()=>setToast(''),2200)};
  const submit=(event:FormEvent)=>{event.preventDefault();setShowUpload(false);notify('生产记录已提交，等待村庄后台审核')};
  return <main className="farmer-portal"><header><button onClick={onExit}>← 返回南坡之窗</button><div className="farmer-logo"><span>南</span><div><b>农户经营台</b><small>真实记录，让好产品被看见</small></div></div><button className="farmer-help" onClick={()=>notify('村庄服务人员会协助您完成上传')}>不会用？找人帮忙</button></header><section className="farmer-main"><div className="farmer-welcome"><div><small>上午好，梁叔</small><h1>今天地里有什么新变化？</h1><p>拍张照片、说几句话，就能留下真实生产过程。</p></div><button onClick={()=>setShowUpload(true)}><span>＋</span><b>上传今日记录</b><small>照片、视频、语音都可以</small></button></div><div className="farmer-summary"><article><span>田</span><div><small>我的地块</small><strong>核桃坡 3 号地</strong></div><i>已认证</i></article><article><span>记</span><div><small>本季真实记录</small><strong>8 条</strong></div><i>6 条已公开</i></article><article><span>品</span><div><small>正在展示</small><strong>2 件农品</strong></div><i>48 人想买</i></article></div><section className="farmer-content-grid"><article className="farm-diary"><header><div><h2>生产过程</h2><p>按照时间记录，顾客看得更放心</p></div><button onClick={()=>setShowUpload(true)}>＋ 添加记录</button></header><div className="diary-list"><div><span className="diary-date">08/22</span><div className="diary-photo"><Image src="/images/products.jpg" alt="核桃采收记录" fill sizes="120px"/></div><div><small>成熟采收 · 已公开</small><h3>第一批核桃开始采收</h3><p>今早六点和家里人一起摘的，挑出外壳完整的先晾晒。</p><div><b>图片 3</b><b>语音 00:36</b><b>村级已审核</b></div></div></div><div><span className="diary-date">06/18</span><div className="diary-photo"><Image src="/images/walnut-yard.jpg" alt="核桃自然生长记录" fill sizes="120px"/></div><div><small>自然生长 · 已公开</small><h3>今年雨水足，核桃长势不错</h3><p>这几天人工除了草，没有打催熟药，叶子和果子都很精神。</p><div><b>图片 2</b><b>村级已审核</b></div></div></div><div className="draft-entry"><span className="diary-date">今天</span><div className="diary-photo empty">＋</div><div><small>待记录</small><h3>继续记录，故事才完整</h3><p>建议拍摄：晾晒、分选、装袋过程</p></div></div></div></article><aside className="share-builder"><span>AI 对外表达助手</span><h2>您讲种植，<br/>我帮您讲给顾客听。</h2><p>小禾会读取已审核的真实记录，整理成不夸大、有出处的产品故事。</p>{!generated?<button onClick={()=>setGenerated(true)}>✦ 生成一段对外介绍</button>:<div className="generated-copy"><small>根据 6 条公开记录生成</small><p>“梁叔家的山核桃，生长在南坡村核桃坡 3 号地。从春天整土，到夏季人工除草，再到清晨采收，每一步都有照片和日期可查。没有漂亮话，只有这一季真实的生长。”</p><div><button onClick={()=>notify('文案已复制，可发到微信')}>复制文案</button><button onClick={onExit}>预览公开页</button></div></div>}<div className="share-score"><div><span>真实度</span><b>100%</b></div><div><span>记录完整度</span><b>75%</b></div></div></aside></section></section>{showUpload&&<div className="modal-backdrop"><form className="farm-upload" onSubmit={submit}><header><div><small>NEW FARM RECORD</small><h2>记录今天的生产过程</h2></div><button type="button" onClick={()=>setShowUpload(false)}>×</button></header><label>选择农品<select><option>太行山核桃 · 核桃坡 3 号地</option><option>南坡山花椒 · 东坡 1 号地</option></select></label><label>今天做了什么？<div className="stage-choices"><span>整地</span><span>播种</span><span>施肥</span><span>生长</span><span className="active">采收</span><span>包装</span></div></label><label className="farm-media"><input type="file" accept="image/*,video/*" multiple/><span>＋</span><b>拍照或上传视频</b><small>保留拍摄时间，便于形成真实档案</small></label><label>说说现场情况<textarea placeholder="可以打字，也可以点右侧话筒直接说…"/><button type="button">● 语音</button></label><label className="truth-check"><input type="checkbox" required/> 我确认以上记录来自本人真实生产过程，同意后台审核后对外展示。</label><footer><button type="button" onClick={()=>setShowUpload(false)}>先不上传</button><button type="submit">提交审核 →</button></footer></form></div>}{toast&&<div className="toast">✓ {toast}</div>}</main>;
}

export function AdminConsole({ onExit }: { onExit: () => void }) {
  const [tab,setTab]=useState<AdminTab>('overview');
  const [showForm,setShowForm]=useState(false);
  const [toast,setToast]=useState('');
  const [orderStage,setOrderStage]=useState(0);
  const notify=(message:string)=>{setToast(message);window.setTimeout(()=>setToast(''),2200)};
  const submit=(event:FormEvent)=>{event.preventDefault();setShowForm(false);notify('内容已保存为草稿，可预览后发布')};
  const orderStages=['待确认收款','已收款 · 待备货','待发货','运输中','已完成'];
  const advanceOrder=()=>{if(orderStage<4){setOrderStage(orderStage+1);notify(orderStage===0?'已确认到账，订单已通知农户备货':orderStage===1?'备货完成，等待录入快递单号':orderStage===2?'快递单号已录入，客户已收到通知':'订单已确认完成')}};
  const manageItems = tab==='stay'?stayCards:tab==='goods'?goodsCards:tab==='experience'?experienceCards:[{name:'村庄访客服务',type:'电话待录入',desc:'用于公开页顶部与底部联系咨询入口',price:'未发布'}];
  return <main className="admin-shell">
    <aside className="admin-sidebar"><div className="brand admin-brand"><span className="brand-seal">南</span><span><b>南坡之窗</b><small>村庄运营中心</small></span></div><nav><button className={tab==='overview'?'active':''} onClick={()=>setTab('overview')}>⌂ <span>总览</span></button><button className={tab==='orders'?'active':''} onClick={()=>setTab('orders')}>▤ <span>订单与发货</span><i className="alert-badge">6</i></button><button className={tab==='stay'?'active':''} onClick={()=>setTab('stay')}>▦ <span>民宿管理</span><i>6</i></button><button className={tab==='goods'?'active':''} onClick={()=>setTab('goods')}>◇ <span>农品与过程</span><i>8</i></button><button className={tab==='experience'?'active':''} onClick={()=>setTab('experience')}>◈ <span>游玩采摘</span><i>6</i></button><button className={tab==='contact'?'active':''} onClick={()=>setTab('contact')}>◌ <span>联系信息</span></button></nav><div className="admin-bottom"><button onClick={onExit}>← 返回南坡之窗</button><div><span>管</span><p><b>村庄管理员</b><small>内容与订单运营</small></p></div></div></aside>
    <section className="admin-main"><header><div><small>南坡之窗 / 村庄运营中心</small><h1>{tab==='overview'?'运营总览':tab==='orders'?'订单与发货':tab==='stay'?'民宿管理':tab==='goods'?'农品与生产过程':tab==='experience'?'游玩采摘项目':'联系信息'}</h1></div><div><button onClick={onExit}>↗ 预览公开页面</button>{tab!=='orders'&&<button className="primary" onClick={()=>setShowForm(true)}>＋ 上架新内容</button>}</div></header>
      {tab==='overview'&&<><div className="admin-stats"><article><span>待确认收款</span><strong>6</strong><small className="orange">需要人工核对</small></article><article><span>待发货</span><strong>3</strong><small>今日处理</small></article><article><span>农户待审核记录</span><strong>2</strong><small>照片与视频</small></article><article><span>本周成交意向</span><strong>48</strong><small>↑ 18.4%</small></article></div><div className="admin-grid"><article className="content-status"><div className="admin-card-head"><div><h2>今日需要处理</h2><p>收款、审核、备货与发货统一流转</p></div><strong>11</strong></div><ul className="ops-todo"><li><span className="op-icon money">¥</span><div><b>6 笔订单等待确认到账</b><small>客户已上传转账备注</small></div><button onClick={()=>setTab('orders')}>去核款</button></li><li><span className="op-icon farm">田</span><div><b>2 条农户生产记录待审核</b><small>确认真实后可对外公开</small></div><button onClick={()=>setTab('goods')}>去审核</button></li><li><span className="op-icon box">□</span><div><b>3 笔订单等待发货</b><small>需要填写物流公司与单号</small></div><button onClick={()=>setTab('orders')}>去发货</button></li></ul></article><article className="admin-preview"><div><span>公开页预览</span><button onClick={onExit}>打开 ↗</button></div><div className="mini-page"><Image src="/images/nanpo-architecture.png" alt="公开页预览" fill sizes="30vw"/><h3>真实记录，让好产品被看见。</h3></div></article></div><article className="recent-table"><div className="admin-card-head"><div><h2>最新业务动态</h2><p>订单与农户内容统一留痕</p></div><button onClick={()=>setTab('orders')}>查看全部</button></div><div className="table-row head"><span>业务内容</span><span>类型</span><span>状态</span><span>更新时间</span><span/></div>{['订单 NP0018 · 太行山核桃|订单|待核款|刚刚','梁有福 · 核桃采收记录|生产记录|待审核|8 分钟前','订单 NP0016 · 山野百花蜜|订单|待发货|1 小时前'].map(row=>{const [a,b,c,d]=row.split('|');return <div className="table-row" key={a}><strong>{a}</strong><span>{b}</span><span className={c==='待发货'?'published':'draft'}>{c}</span><span>{d}</span><button>•••</button></div>})}</article></>}
      {tab==='orders'&&<section className="orders-panel"><div className="order-flow-head"><div><small>统一订单状态流</small><h2>客户下单 → 转账 → 后台核款 → 农户备货 → 统一发货</h2></div><span>全流程留痕</span></div><div className="order-flow">{orderStages.map((stage,index)=><div className={index<=orderStage?'active':''} key={stage}><span>{index<orderStage?'✓':index+1}</span><b>{stage}</b>{index<orderStages.length-1&&<i/>}</div>)}</div><div className="orders-layout"><aside className="order-queue"><header><h3>订单队列</h3><span>6 笔待核款</span></header>{['NP202608290018|张晓宁|太行山核桃|¥29.90','NP202608290017|李敏|山野百花蜜|¥39.00','NP202608290016|王先生|石磨小米|¥33.60'].map((row,index)=>{const [id,name,goods,amount]=row.split('|');return <button className={index===0?'active':''} key={id}><span>{index===0?'待核款':index===1?'待核款':'待发货'}</span><b>{goods}</b><small>{name} · {amount}</small><i>{id.slice(-4)}</i></button>})}</aside><article className="order-detail"><header><div><small>订单 NP202608290018</small><h3>太行山核桃 × 1</h3></div><span className="order-status">{orderStages[orderStage]}</span></header><div className="order-info-grid"><div><small>客户</small><b>张晓宁 · 138****2806</b></div><div><small>应收金额</small><b className="amount">¥29.90</b></div><div><small>收货地址</small><b>河南省郑州市金水区 ×× 路 18 号</b></div><div><small>转账备注</small><b>2806 核桃</b></div></div><div className="payment-proof"><DemoQr/><div><small>客户支付信息</small><h4>已点击“我已完成转账”</h4><p>提交时间：2026-08-29 12:18<br/>收款渠道：村庄统一收款码（原型）</p></div><button onClick={()=>notify('已记录：需要人工核对实际到账')}>查看核款说明</button></div>{orderStage>=2&&<div className="shipping-form"><label>物流公司<select defaultValue="顺丰速运"><option>顺丰速运</option><option>邮政快递</option><option>中通快递</option></select></label><label>快递单号<input defaultValue="SF1234567890"/></label></div>}<footer><button onClick={()=>notify('已添加内部订单备注')}>添加备注</button><button className="primary" onClick={advanceOrder} disabled={orderStage===4}>{orderStage===0?'确认已到账':orderStage===1?'确认备货完成':orderStage===2?'录入单号并发货':orderStage===3?'确认订单完成':'订单已完成'} →</button></footer><div className="risk-note">重要：原型仅演示人工核款流程。真实上线需配置唯一订单号、转账备注、防重复确认、操作日志和退款处理。</div></article></div></section>}
      {tab!=='overview'&&tab!=='orders'&&<section className="manage-list"><div className="manage-toolbar"><div><button className="active">全部</button><button>已发布</button><button>待审核</button></div><button onClick={()=>setShowForm(true)}>＋ 新增{tab==='stay'?'民宿':tab==='goods'?'农品':tab==='experience'?'游玩项目':'联系人'}</button></div>{manageItems.map((item,index)=><article key={item.name}><span className="row-avatar">{tab==='stay'?'宿':tab==='goods'?'品':tab==='experience'?'游':'联'}</span><div><h3>{item.name}</h3><p>{'type' in item?item.type:item.season} · {item.desc}</p>{tab==='goods'&&<small className="record-count">真实生产记录 {index+5} 条 · 最近更新 {index+1} 天前</small>}{tab==='experience'&&<small className="record-count">{'hasVideo' in item&&item.hasVideo?'视频 1 条 · 图集 6 张':'图集 4 张'} · 预约规则已配置</small>}</div><span className={index===1?'published':'draft'}>{index===1?'展示中':tab==='goods'?'待审核':'待完善'}</span><button onClick={()=>notify(tab==='goods'?'已打开生产记录审核页':tab==='experience'?'已打开项目与媒体配置':'已打开编辑页')}>{tab==='goods'?'审核过程':tab==='experience'?'配置媒体':'编辑'}</button><button>•••</button></article>)}</section>}
    </section>
    {showForm&&<div className="modal-backdrop"><form className="content-form" onSubmit={submit}><header><div><small>CONTENT PUBLISH</small><h2>上架新内容</h2></div><button type="button" onClick={()=>setShowForm(false)}>×</button></header><div className="type-options"><label><input type="radio" name="type" defaultChecked={tab==='stay'||tab==='overview'}/> 民宿</label><label><input type="radio" name="type" defaultChecked={tab==='goods'}/> 农产品</label><label><input type="radio" name="type" defaultChecked={tab==='experience'}/> 游玩采摘</label><label><input type="radio" name="type" defaultChecked={tab==='contact'}/> 联系方式</label></div><label>名称<input required placeholder="例如：秋季核桃采摘体验"/></label><div className="form-grid"><label>价格 / 说明<input placeholder="例如：¥68 / 人"/></label><label>联系电话<input placeholder="请填写真实联系电话"/></label></div><label>简介<textarea placeholder="用一两句话介绍特色、服务与注意事项"/></label><label className="upload-box media-upload"><input type="file" accept="image/*,video/*" multiple/><span>＋ 上传图片或视频</span><small>支持 JPG、PNG、MP4；首张图片作为封面，可拖动排序</small></label><footer><button type="button" onClick={()=>setShowForm(false)}>取消</button><button type="submit">保存为草稿</button><button type="submit" className="primary">保存并发布</button></footer></form></div>}
    {toast&&<div className="toast">✓ {toast}</div>}
  </main>
}
