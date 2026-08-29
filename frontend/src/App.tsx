import { FormEvent, ImgHTMLAttributes, useCallback, useEffect, useState } from 'react';
import { ApiError } from './services/api';
import { loadPublicHomeData, loadPublicProduct, ProductDetail, PublicHomeData } from './services/publicApi';
import { createOrder, Order, reportOrderPayment } from './services/orderApi';

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

type RouteType = 'drive' | 'rail' | 'bus';
type AdminTab = 'overview' | 'orders' | 'stay' | 'goods' | 'experience' | 'contact';
type NearbyPlanId = string;
type StayCard = { name: string; type: string; desc: string; price: string; image: string; beds: string };
type ProductCard = { id?: number; name: string; icon: string; season: string; desc: string; price: string; image?: string };
type ExperienceCard = { name: string; type: string; season: string; duration: string; desc: string; price: string; image: string; hasVideo: boolean; video?: string };
type NearbySpot = { name: string; range: string; time: string; type: string; mark: string; tone: string; image: string; desc: string; highlights: string[]; map: string };
type NearbyPlan = { eyebrow: string; name: string; days: string; fit: string; distance: string; summary: string; color: string; stops: { time: string; title: string; detail: string }[]; tips: string[] };

const routes: Record<RouteType, { title: string; time: string; steps: string[]; note: string }> = {
  drive: { title: '从郑州自驾出发', time: '约 1.5 小时', steps: ['郑云高速 S87', '云台山站下高速', '焦辉路 S306', '青云大道 X006', '大南坡村'], note: '006 县道穿村而过，建议直接导航“大南坡艺术中心”。' },
  rail: { title: '高铁到修武西站', time: '郑州出发约 30 分钟', steps: ['郑州 / 郑州东', '修武西站', '修武至西村公交', '大南坡站'], note: '公交班次可能随季节调整，请在出发前向车站或村庄服务点确认。' },
  bus: { title: '从焦作市区乘公交', time: '约 50—70 分钟', steps: ['焦作站南广场', '37 路公交', '山阳建国站', '换乘 29 路', '大南坡站'], note: '公开资料显示也可乘 29 路抵达，实际站点与班次请以当天信息为准。' },
};

const stayCards: StayCard[] = [
  { name: '牛大爷的院子', type: '乡土院落 · 示例房源', desc: '老院落、核桃树与山里清晨，适合一家人慢住两晚。', price: '价格待录入', image: '/images/homestay.jpg', beds: '2—4 人' },
  { name: '南坡山居 · 一号院', type: '整院包住 · 示例房源', desc: '灰砖院落保留北方村居尺度，步行可达艺术中心。', price: '价格待录入', image: '/images/village-detail.jpg', beds: '4—6 人' },
  { name: '松风小院', type: '双床客房 · 示例房源', desc: '面向太行山南麓，适合周末、研学与小型团队。', price: '价格待录入', image: '/images/village-pond.jpg', beds: '2 人' },
  { name: '石榴树下的小院', type: '家庭套房 · 示例房源', desc: '院里保留果树和石桌，适合带孩子体验村庄日常。', price: '价格待录入', image: '/images/walnut-yard.jpg', beds: '3—5 人' },
  { name: '山里人家 · 二号院', type: '整院包住 · 示例房源', desc: '独立客厅与小厨房，适合朋友结伴或两家人同住。', price: '价格待录入', image: '/images/homestay.jpg', beds: '6—8 人' },
  { name: '南坡研学客舍', type: '团队客房 · 示例房源', desc: '面向研学与小型团建，可由后台配置餐食与活动套餐。', price: '价格待录入', image: '/images/village-detail.jpg', beds: '8—12 人' },
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
  { name: '村庄美学导览', type: '文化讲解', season: '全年可约', duration: '约 2 小时', desc: '从老礼堂到艺术中心，听村庄空间更新与乡土生活的故事。', price: '价格待配置', image: '/images/village-detail.jpg', hasVideo: true },
  { name: '石磨小米体验', type: '手作课堂', season: '全年可约', duration: '约 1 小时', desc: '认识谷物、体验传统石磨，把一袋现磨小米带回家。', price: '¥ 39 / 人起', image: '/images/products.jpg', hasVideo: false },
  { name: '亲子农耕课堂', type: '亲子研学', season: '春夏秋', duration: '半日', desc: '按节气配置播种、除草或收获任务，适合家庭和小团队。', price: '¥ 98 / 组起', image: '/images/village-pond.jpg', hasVideo: true },
  { name: '南坡秋兴手作', type: '节气活动', season: '秋季', duration: '约 2 小时', desc: '围绕在地物产开展拓印、编织与村庄市集体验。', price: '以活动公告为准', image: '/images/homestay.jpg', hasVideo: false },
];

const nearbySpots: NearbySpot[] = [
  { name: '青龙峡 · 峰林峡', range: '约 20—30 km', time: '驾车约 40—60 分钟', type: '峡谷 · 湖泊', mark: '近', tone: 'moss', image: '/images/spots/qinglong.jpg', desc: '峡谷、瀑溪与高峡平湖，适合清凉徒步和山水摄影。', highlights: ['峡谷瀑溪', '翡翠湖色', '山地体验'], map: 'https://uri.amap.com/search?keyword=青龙峡景区峰林峡&city=焦作&callnative=0' },
  { name: '圆融无碍禅寺', range: '约 20—35 km', time: '驾车约 45—60 分钟', type: '古寺 · 文化', mark: '寺', tone: 'sand', image: '/images/spots/yuanrong.jpg', desc: '太行山前的古寺文化空间，适合与焦作市区安排成轻松半日。', highlights: ['古寺建筑', '山门远眺', '静心漫游'], map: 'https://uri.amap.com/search?keyword=圆融无碍禅寺&city=焦作&callnative=0' },
  { name: '焦作城市漫游', range: '约 25—35 km', time: '驾车约 50—60 分钟', type: '古街 · 夜游', mark: '城', tone: 'clay', image: '/images/spots/jiaozuo-city.jpg', desc: '恩州驿、南水北调天河公园与焦作夜市，适合轻松逛吃。', highlights: ['恩州驿', '天河公园', '夜市烟火'], map: 'https://uri.amap.com/search?keyword=恩州驿&city=焦作&callnative=0' },
  { name: '焦作影视城', range: '约 30—40 km', time: '驾车约 55—70 分钟', type: '古风 · 亲子', mark: '影', tone: 'ochre', image: '/images/spots/film-city.png', desc: '以古代建筑场景和影视文化为特色，适合亲子拍照与城市一日游。', highlights: ['古风城楼', '影视场景', '亲子打卡'], map: 'https://uri.amap.com/search?keyword=焦作影视城&city=焦作&callnative=0' },
  { name: '云台山', range: '约 35—45 km', time: '驾车约 60—75 分钟', type: '峡谷 · 飞瀑', mark: '云', tone: 'pine', image: '/images/spots/yuntai.jpg', desc: '红石峡、潭瀑峡与茱萸峰，第一次来焦作的经典山水选择。', highlights: ['红石峡', '潭瀑峡', '茱萸峰'], map: 'https://uri.amap.com/search?keyword=云台山景区&city=焦作&callnative=0' },
  { name: '陈家沟', range: '约 65—80 km', time: '驾车约 90 分钟', type: '太极 · 非遗', mark: '拳', tone: 'sand', image: '/images/spots/chenjiagou.jpg', desc: '从太极祖祠到传统拳法体验，适合亲子研学与文化旅行。', highlights: ['太极祖祠', '太极文化园', '拳法体验'], map: 'https://uri.amap.com/search?keyword=陈家沟景区&city=焦作&callnative=0' },
  { name: '嘉应观', range: '约 70—85 km', time: '驾车约 90—110 分钟', type: '黄河 · 古建', mark: '河', tone: 'ochre', image: '/images/spots/jiayingguan.jpg', desc: '走近黄河治理历史与清代建筑群，适合与陈家沟串联。', highlights: ['治黄行宫', '清代古建', '黄河文化'], map: 'https://uri.amap.com/search?keyword=嘉应观景区&city=焦作&callnative=0' },
  { name: '神农山', range: '约 75—95 km', time: '驾车约 100—120 分钟', type: '登山 · 地质', mark: '峰', tone: 'stone', image: '/images/spots/shennong.jpg', desc: '龙脊长城与白皮松景观，适合体力充足的登山爱好者。', highlights: ['紫金顶', '龙脊长城', '白皮松'], map: 'https://uri.amap.com/search?keyword=神农山景区&city=焦作&callnative=0' },
  { name: '青天河', range: '约 85—100 km', time: '驾车约 110—130 分钟', type: '游船 · 红叶', mark: '湖', tone: 'blue', image: '/images/spots/qingtianhe.jpg', desc: '高峡平湖、十里画廊与秋日红叶，适合安排一整天慢慢游览。', highlights: ['高峡平湖', '游船画廊', '秋日红叶'], map: 'https://uri.amap.com/search?keyword=青天河景区&city=焦作&callnative=0' },
];

const nearbyPlans: Record<NearbyPlanId, NearbyPlan> = {
  canyon: {
    eyebrow: '轻松半日 · 最近山水', name: '南坡慢游 + 太行双峡', days: '0.5 DAY', fit: '家庭 / 摄影 / 避暑', distance: '单程约 20—30 km', color: 'green',
    summary: '上午在村里慢慢走，午后去青龙峡或峰林峡二选一，把路程留短，把时间留给山风。',
    stops: [
      { time: '09:00', title: '大南坡村', detail: '艺术中心、乡村书店与老村散步' },
      { time: '12:00', title: '村中午餐', detail: '提前向民宿或村庄服务点预约' },
      { time: '13:30', title: '青龙峡 / 峰林峡', detail: '根据开放情况与体力二选一游览' },
      { time: '17:30', title: '返回南坡', detail: '住进山居，留一晚看山间暮色' },
    ],
    tips: ['山区弯道较多，建议白天行车', '两景区开放安排可能调整，出发前确认'],
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
    eyebrow: '人文两日 · 山河与太极', name: '怀川文化环线', days: '2 DAYS', fit: '亲子 / 研学 / 非遗', distance: '各点均在 100 km 圈层', color: 'orange',
    summary: '第一天认识南坡与焦作城市，第二天串联陈家沟、嘉应观，在太极和黄河故事里读懂怀川。',
    stops: [
      { time: 'D1 上午', title: '大南坡村', detail: '乡村更新、艺术空间与在地午餐' },
      { time: 'D1 下午', title: '焦作城市漫游', detail: '恩州驿或南水北调天河公园' },
      { time: 'D1 晚间', title: '焦作市区住宿', detail: '缩短第二天向南出发的路程' },
      { time: 'D2 上午', title: '陈家沟', detail: '太极祖祠、拳法体验与非遗研学' },
      { time: 'D2 下午', title: '嘉应观', detail: '黄河治理历史与清代古建群' },
    ],
    tips: ['两日线路建议自驾或包车', '研学与讲解项目建议提前预约'],
  },
};

export function PublicWindow({ onManage, onFarmer, onLogin }: { onManage: () => void; onFarmer: () => void; onLogin: () => void }) {
  const [routeType, setRouteType] = useState<RouteType>('drive');
  const [toast, setToast] = useState('');
  const [orderItem, setOrderItem] = useState<ProductCard | null>(null);
  const [storyItem, setStoryItem] = useState<ProductCard | null>(null);
  const [videoItem, setVideoItem] = useState<ExperienceCard | null>(null);
  const [stayPage, setStayPage] = useState(1);
  const [goodsPage, setGoodsPage] = useState(1);
  const [experiencePage, setExperiencePage] = useState(1);
  const [homeData, setHomeData] = useState<PublicHomeData | null>(null);
  const [catalogError, setCatalogError] = useState('');
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

  if (!homeData) {
    return <main className="app-state">{catalogError ? <><h1>暂时无法打开南坡之窗</h1><p>{catalogError}</p><button onClick={reloadCatalog}>重新加载</button></> : <><span className="state-spinner"/><h1>正在打开南坡之窗…</h1><p>读取村庄、行程、民宿与农品的最新公开信息。</p></>}</main>;
  }

  const routeMap = Object.fromEntries(homeData.routes.map((item) => [item.kind, { title: item.title, time: item.duration, steps: item.steps, note: item.note }]));
  const route = routeMap[routeType];
  const publicStayCards: StayCard[] = homeData.homestays.items.map((item) => ({ name: item.name, type: item.type, desc: item.summary, price: item.price, image: item.coverUrl, beds: item.capacity }));
  const publicGoodsCards: ProductCard[] = homeData.products.items.map((item) => ({ id: item.id, name: item.name, icon: item.name.slice(0, 1), season: item.season, desc: item.summary, price: `¥ ${Number(item.startingPrice).toFixed(2)} 起`, image: item.coverUrl }));
  const publicExperienceCards: ExperienceCard[] = homeData.experiences.items.map((item) => ({ name: item.name, type: item.type, season: item.season, duration: item.duration, desc: item.summary, price: item.price, image: item.coverUrl, hasVideo: Boolean(item.videoUrl), video: item.videoUrl }));
  const spotTones = ['moss', 'sand', 'clay', 'ochre', 'pine', 'stone', 'blue'];
  const publicNearbySpots: NearbySpot[] = homeData.attractions.items.map((item, index) => ({ name: item.name, range: `约 ${Math.max(0, Math.round(item.distanceKm - 5))}—${Math.round(item.distanceKm + 5)} km`, time: `驾车约 ${item.driveMinutes} 分钟`, type: item.category, mark: item.name.slice(0, 1), tone: spotTones[index % spotTones.length], image: item.coverUrl, desc: item.summary, highlights: item.highlights, map: item.mapUrl }));
  const publicNearbyPlans = Object.fromEntries(homeData.travelPlans.map((item, index) => [item.slug, { eyebrow: '从南坡出发', name: item.name, days: item.duration, fit: item.suitableFor, distance: item.distance, summary: item.summary, color: ['green', 'blue', 'orange'][index % 3], stops: item.stops, tips: item.tips }])) as Record<string, NearbyPlan>;

  return (
    <main>
      <header className="site-header">
        <a className="brand" href="#top"><span className="brand-seal">南</span><span><b>南坡之窗</b><small>WINDOW OF NANPO</small></span></a>
        <nav aria-label="主要导航"><a href="#about">走进南坡</a><a href="#route">行前指南</a><a href="#nearby">周边游</a><a href="#experience">游玩采摘</a><a href="#stay">山居一晚</a><a href="#goods">山野好物</a></nav>
        <div className="header-actions"><button className="weather" onClick={onLogin}>客户登录</button><button className="weather farmer-entry" onClick={onFarmer}>农户入口</button><button className="weather" onClick={() => notify(homeData.site.visitorService ? `访客服务：${homeData.site.visitorService.phone}` : '访客服务暂未开通')}>◌ 访客服务</button><button className="manage" onClick={onManage}>内容管理 ↗</button></div>
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
          <Image src="/images/village-pond.jpg" alt="大南坡村古树与石砌水池" fill priority sizes="50vw" />
          <div className="image-caption"><span>01</span><p>旧日大队部的院落<br/><small>大南坡 · 西村乡</small></p></div>
          <div className="postcard"><b>太行山下</b><span>一座会生长的村庄</span><i>大南坡</i></div>
        </div>
        <aside className="hero-rail"><span>SCROLL TO EXPLORE</span><i /></aside>
      </section>

      <section className="quick-window" aria-label="南坡信息概览">
        <a href="#route"><span className="quick-no">01</span><div><small>HOW TO ARRIVE</small><b>怎么来南坡</b></div><i>↗</i></a>
        <a href="#stay"><span className="quick-no">02</span><div><small>STAY IN VILLAGE</small><b>住进山居院落</b></div><i>↗</i></a>
        <a href="#goods"><span className="quick-no">03</span><div><small>LOCAL HARVEST</small><b>把山野带回家</b></div><i>↗</i></a>
        <a href="#nearby"><span className="quick-no">04</span><div><small>WITHIN 100 KM</small><b>从南坡游向周边</b></div><i>↗</i></a>
      </section>

      <section className="about-section" id="about">
        <div className="section-kicker"><span>01</span><small>THE VILLAGE</small></div>
        <div className="about-grid">
          <div className="about-copy"><span>接下来，打开南坡</span><h2>旧砖墙没有被推倒，<br/>它们只是长出了新的故事。</h2><p>大南坡位于修武县西村乡东北部浅山区，由西小庄、东小庄、南坡老村、南坡新村四个自然村组成。这里曾因煤而兴，也曾因资源枯竭而沉寂。如今，老大队部、旧礼堂和供销社被重新激活，成为书店、艺术中心与村庄公共生活的一部分。</p><div className="quote">“不修饰、不掩盖，让时间在空间里沉淀。”</div></div>
          <div className="about-collage"><figure className="large"><Image src="/images/walnut-yard.jpg" alt="大南坡村核桃树下的老院落" fill sizes="42vw" /></figure><figure className="small"><Image src="/images/village-detail.jpg" alt="大南坡村灰砖院落细节" fill sizes="18vw" /></figure><span className="year-mark">2020<small>乡村美学更新启程</small></span></div>
        </div>
        <div className="culture-spaces"><article><span>01</span><h3>方所乡村文化</h3><p>由老戏台更新而来，阅读、展览与村庄日常在这里相遇。</p></article><article><span>02</span><h3>大南坡艺术中心</h3><p>老大队部办公室和粮库变身为面向乡村的艺术空间。</p></article><article><span>03</span><h3>碧山工销社</h3><p>连接民间百工、当代设计与村民生产的乡村商店。</p></article><article><span>04</span><h3>南坡秋兴</h3><p>让音乐、手作、在地生活与远方来客在山村共振。</p></article></div>
      </section>

      <section className="route-section" id="route">
        <div className="section-kicker light"><span>02</span><small>HOW TO ARRIVE</small></div>
        <div className="route-head"><div><span>行前指南</span><h2>从城市出发，<br/>向山的方向走。</h2></div><p>目的地：{homeData.site.address}<br/>建议导航至“{homeData.site.mapKeyword}”</p></div>
        <div className="route-planner">
          <div className="route-tabs"><button disabled={!routeMap.drive} className={routeType==='drive'?'active':''} onClick={() => setRouteType('drive')}>自驾前往</button><button disabled={!routeMap.rail} className={routeType==='rail'?'active':''} onClick={() => setRouteType('rail')}>高铁换乘</button><button disabled={!routeMap.bus} className={routeType==='bus'?'active':''} onClick={() => setRouteType('bus')}>市区公交</button></div>
          {route ? <div className="route-content"><div className="route-summary"><small>RECOMMENDED ROUTE</small><h3>{route.title}</h3><strong>{route.time}</strong><p>{route.note}</p><button onClick={() => notify(`已复制目的地：${homeData.site.mapKeyword}`)}>复制目的地地址 ↗</button></div><div className="route-line">{route.steps.map((step,index)=><div key={step}><span>{index+1}</span><b>{step}</b>{index<route.steps.length-1&&<i/>}</div>)}</div><div className="map-card"><div className="map-mountains"><i/><i/><i/></div><span className="map-city">焦作市区</span><span className="map-road">X006</span><span className="map-pin">南<small>{homeData.site.name}</small></span><span className="map-north">N ↑</span></div></div> : <EmptyState label="出行路线"/>}
        </div>
        <div className="travel-note"><span>出发提醒</span><p>公交线路、班次和开放时间可能临时调整；节假日建议提前一天确认，并优先选择白天进村。</p><button onClick={() => notify('行前提醒已保存')}>保存提醒</button></div>
      </section>

      <NearbyTravel notify={notify} spots={publicNearbySpots} plans={publicNearbyPlans} />

      <section className="experience-section" id="experience">
        <div className="section-kicker light"><span>04</span><small>PLAY & HARVEST</small></div>
        <div className="experience-head"><div><span>跟着节气来玩</span><h2>不只看风景，<br/>也亲手参与一场收成。</h2></div><p>采摘、农耕、手作与村庄导览都可由后台持续上架；项目可配置季节、价格、名额、图集和视频。</p></div>
        {publicExperienceCards.length ? <><div className="experience-grid">{publicExperienceCards.slice((experiencePage-1)*experiencePageSize,experiencePage*experiencePageSize).map((item)=><article key={item.name}><div className="experience-media"><Image src={item.image} alt={item.name} fill sizes="33vw"/>{item.hasVideo?<button onClick={()=>setVideoItem(item)} aria-label={`播放${item.name}视频`}><i>▶</i><span>视频看现场</span></button>:<span className="photo-badge">图集</span>}<small>{item.season}</small></div><div className="experience-info"><span>{item.type} · {item.duration}</span><h3>{item.name}</h3><p>{item.desc}</p><footer><strong>{item.price}</strong><button onClick={()=>notify(`${item.name}已加入咨询清单`)}>咨询预约 →</button></footer></div></article>)}</div><Pagination page={experiencePage} total={publicExperienceCards.length} pageSize={experiencePageSize} onChange={setExperiencePage} label="游玩采摘项目" /></> : <EmptyState label="游玩采摘项目"/>}
        <div className="experience-manage"><div><span>村庄运营方</span><h3>季节变了，项目也可以随时更新。</h3><p>后台可设置开放日期、每日名额、预约电话、封面图与介绍视频。</p></div><button onClick={onManage}>去后台配置项目 →</button></div>
      </section>

      <section className="stay-section" id="stay">
        <div className="section-kicker"><span>05</span><small>STAY IN NANPO</small></div>
        <div className="section-title-row"><div><span>在村里住一晚</span><h2>推开院门，听见山里的清晨。</h2></div><p>现有公开资料显示村内已建设多套山居民宿。以下房源内容为高保真示例，具体名称、价格与联系方式将在管理后台录入后公开。</p></div>
        {publicStayCards.length ? <><div className="stay-grid">{publicStayCards.slice((stayPage-1)*stayPageSize,stayPage*stayPageSize).map((item,index)=><article key={item.name}><div className="stay-image"><Image src={item.image} alt={item.name} fill sizes="33vw"/><span>{String((stayPage-1)*stayPageSize+index+1).padStart(2,'0')}</span><button onClick={() => notify(`${item.name}已加入收藏`)}>收藏 ♡</button></div><div className="stay-info"><small>{item.type}</small><h3>{item.name}</h3><p>{item.desc}</p><div><span>住 {item.beds}</span><strong>{item.price}</strong><button onClick={() => notify(homeData.site.visitorService ? `咨询电话：${homeData.site.visitorService.phone}` : '咨询方式暂未开放')}>了解详情 →</button></div></div></article>)}</div><Pagination page={stayPage} total={publicStayCards.length} pageSize={stayPageSize} onChange={setStayPage} label="民宿" /></> : <EmptyState label="民宿"/>}
        <div className="operator-cta"><div><span>你是南坡民宿经营者？</span><h3>把你的院子，也放进这扇窗。</h3></div><button onClick={onManage}>去后台上架房源 →</button></div>
      </section>

      <section className="goods-section" id="goods">
        <div className="goods-intro"><div className="section-kicker light"><span>06</span><small>LOCAL HARVEST</small></div><span>山野好物</span><h2>每一份收成都有<br/>自己的时节。</h2><p>山核桃、山花椒、小米与蜂蜜，是公开旅游资料中推荐的焦作山野物产。具体商品、价格和村民联系方式由后台上架。</p><div className="season"><b>八月</b><span><i style={{width:'72%'}}/>核桃与花椒陆续成熟</span></div></div>
        <div className="goods-visual"><Image src="/images/products.jpg" alt="大南坡工销社陈列的农产品" fill sizes="35vw"/><span>工销社里的山野收成</span></div>
        <div className="goods-list">{publicGoodsCards.length ? <>{publicGoodsCards.slice((goodsPage-1)*goodsPageSize,goodsPage*goodsPageSize).map((item,index)=><article key={item.name}><span className="goods-index">{String((goodsPage-1)*goodsPageSize+index+1).padStart(2,'0')}</span><div className="goods-icon">{item.icon}</div><div><small>{item.season}</small><h3>{item.name}</h3><p>{item.desc}</p></div><strong>{item.price}</strong><button className="trace-button" onClick={() => setStoryItem(item)}>看过程</button><button onClick={() => setOrderItem(item)}>购买</button></article>)}<Pagination page={goodsPage} total={publicGoodsCards.length} pageSize={goodsPageSize} onChange={setGoodsPage} label="农产品" dark /></> : <EmptyState label="农产品"/>}</div>
      </section>

      <section className="day-trip"><div className="day-photo"><Image src="/images/village-pond.jpg" alt="大南坡村院落生活" fill sizes="40vw"/><span>ONE DAY IN NANPO</span></div><div className="day-copy"><span>一日南坡建议</span><h2>不赶路，去感受。</h2><div className="timeline"><div><b>09:30</b><p><strong>抵达大南坡</strong><small>从艺术中心开始认识村庄</small></p></div><div><b>11:00</b><p><strong>方所乡村文化</strong><small>在老戏台改成的书店慢慢读</small></p></div><div><b>13:30</b><p><strong>老村散步</strong><small>沿灰砖院落与古树寻找乡土日常</small></p></div><div><b>16:00</b><p><strong>碧山工销社</strong><small>挑一份山野物产带回家</small></p></div></div><button onClick={() => notify('一日游路线已保存')}>收藏这条路线 →</button></div></section>

      <footer className="site-footer"><div className="footer-brand"><span className="brand-seal">南</span><h2>南坡之窗</h2><p>{homeData.site.summary}</p></div><div><small>来南坡</small><a href="#route">出行路线</a><a href="#nearby">百公里周边游</a><a href="#experience">游玩与采摘</a><a href="#stay">民宿山居</a><a href="#goods">乡野好物</a></div><div><small>认识南坡</small><a href="#about">村庄故事</a><a href="#about">文化空间</a><button onClick={onManage}>内容管理</button></div><div className="footer-contact"><small>访客服务</small><strong>{homeData.site.visitorService?.phone || '暂未开通'}</strong><p>{homeData.site.address}<br/>{homeData.site.visitorService?.businessHours}</p></div><div className="source-note">路线与村庄资料来自后台已发布数据。页面距离、车程为从大南坡村出发的规划估算，不代表实时导航；出发前请复核路况、班次、票务与开放安排。</div></footer>
      {storyItem&&<ProductStory product={storyItem} onClose={()=>setStoryItem(null)} onBuy={()=>{setStoryItem(null);setOrderItem(storyItem)}}/>}
      {orderItem&&<CheckoutFlow product={orderItem} onClose={()=>setOrderItem(null)} onLogin={onLogin}/>}
      {videoItem&&<VideoPreview item={videoItem} onClose={()=>setVideoItem(null)}/>}
      {toast&&<div className="toast">✓ {toast}</div>}
    </main>
  );
}

function EmptyState({ label }: { label: string }) {
  return <div className="section-empty"><span>南</span><h3>暂无{label}</h3><p>运营人员发布内容后，将自动在这里展示。</p></div>;
}

function Pagination({ page, total, pageSize, onChange, label, dark = false }: { page: number; total: number; pageSize: number; onChange: (page: number) => void; label: string; dark?: boolean }) {
  const pages = Math.max(1, Math.ceil(total / pageSize));
  return <nav className={`pagination ${dark ? 'dark' : ''}`} aria-label={`${label}分页`}><span>共 {total} 项</span><div><button disabled={page<=1} onClick={()=>onChange(page-1)} aria-label={`上一页${label}`}>←</button>{Array.from({length:pages},(_,index)=>index+1).map(item=><button key={item} className={page===item?'active':''} onClick={()=>onChange(item)} aria-current={page===item?'page':undefined}>{String(item).padStart(2,'0')}</button>)}<button disabled={page>=pages} onClick={()=>onChange(page+1)} aria-label={`下一页${label}`}>→</button></div><small>{String(page).padStart(2,'0')} / {String(pages).padStart(2,'0')}</small></nav>;
}

function VideoPreview({ item, onClose }: { item: ExperienceCard; onClose: () => void }) {
  return <div className="modal-backdrop video-backdrop"><section className="video-modal"><header><div><small>FIELD VIDEO · 项目实拍</small><h2>{item.name}</h2></div><button onClick={onClose} aria-label="关闭视频">×</button></header><video controls playsInline preload="metadata" poster={item.image}><source src={item.video} type="video/mp4"/>您的浏览器暂不支持视频播放。</video><footer><div><span>{item.type}</span><strong>{item.season} · {item.duration}</strong></div><p>视频由后台审核发布，同时保留封面、标题与文字说明。</p></footer></section></div>;
}

function NearbyTravel({ notify, spots, plans }: { notify: (message: string) => void; spots: NearbySpot[]; plans: Record<string, NearbyPlan> }) {
  const [planId, setPlanId] = useState<NearbyPlanId>(Object.keys(plans)[0] || '');
  const plan = plans[planId] || Object.values(plans)[0];
  return <section className="nearby-section" id="nearby">
    <div className="section-kicker"><span>03</span><small>EXPLORE WITHIN 100 KM</small></div>
    <div className="nearby-head">
      <div><span>从南坡，再走远一点</span><h2>以村庄为圆心，<br/>打开百公里山河。</h2></div>
      <div className="radius-note"><span className="radius-rings"><i/><i/><i/><b>南坡</b></span><p><strong>100 km</strong> 旅行生活圈<small>所有目的地均按从大南坡村出发估算</small></p></div>
    </div>

    {spots.length ? <div className="nearby-spots">
      {spots.map((spot, index) => <article key={spot.name} className={`spot-card ${spot.tone}`}><Image className="spot-bg" src={spot.image} alt={`${spot.name}实景`} fill sizes="(max-width: 760px) 100vw, 33vw" />
        <header><span>{spot.mark}</span><small>0{index + 1} · {spot.type}</small></header>
        <h3>{spot.name}</h3><p>{spot.desc}</p><ul>{spot.highlights.map(item => <li key={item}>{item}</li>)}</ul>
        <div><strong>{spot.range}</strong><small>{spot.time}</small><a href={spot.map} target="_blank" rel="noreferrer" aria-label={`在地图中查看${spot.name}`}>地图导航 ↗</a></div>
      </article>)}
    </div> : <EmptyState label="周边景点"/>}

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
      {error&&<div className="section-empty"><span>南</span><h3>{error}</h3><button onClick={reload}>重新加载</button></div>}
      {detail&&records.length===0&&<EmptyState label="已公开生产记录"/>}
      {records.map((record,index)=><article key={record.id}><div className="process-image"><Image src={product.image || '/images/products.jpg'} alt={stageNames[record.stage] || record.stage} fill sizes="150px"/></div><span>{String(index+1).padStart(2,'0')}</span><div><small>{dateLabel(record.occurredAt)}</small><h3>{stageNames[record.stage] || record.stage}</h3><p>{record.text}</p></div></article>)}
    </div>
    <div className="story-actions"><p>{lastUpdated ? `最后发布：${fullDateLabel(lastUpdated)} · 共 ${records.length} 条已公开记录` : '暂无已公开记录'}</p><button onClick={onBuy}>信任这份收成，去购买 →</button></div></section></div>;
}

function DemoQr(){return <div className="demo-qr" aria-label="演示收款码，不可用于真实支付"><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><span>演示<br/>不可支付</span></div>}

function CheckoutFlow({ product, onClose, onLogin }: { product: ProductCard; onClose: () => void; onLogin: () => void }) {
  const [step,setStep]=useState<'form'|'pay'|'done'>('form');
  const [detail,setDetail]=useState<ProductDetail|null>(null);
  const [order,setOrder]=useState<Order|null>(null);
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState('');
  const [needsLogin,setNeedsLogin]=useState(false);
  const [idempotencyKey]=useState(()=>`checkout-${crypto.randomUUID()}`);
  useEffect(()=>{if(product.id){loadPublicProduct(product.id).then(setDetail).catch((reason)=>setError(reason instanceof ApiError?reason.message:'商品规格加载失败'))}},[product.id]);
  const submit=async(event:FormEvent<HTMLFormElement>)=>{event.preventDefault();const sku=detail?.skus[0];if(!sku){setError('当前农品没有可售规格');return}const form=new FormData(event.currentTarget);setBusy(true);setError('');try{const created=await createOrder({recipientName:String(form.get('recipientName')),recipientPhone:String(form.get('recipientPhone')),recipientAddress:String(form.get('recipientAddress')),customerNote:String(form.get('customerNote')||''),items:[{skuId:sku.id,quantity:1}]},idempotencyKey);setOrder(created);setStep('pay')}catch(reason){if(reason instanceof ApiError&&reason.status===401){setNeedsLogin(true);setError('请先使用手机号登录，再提交订单')}else{setError(reason instanceof ApiError?reason.message:'订单创建失败')}}finally{setBusy(false)}};
  const report=async()=>{if(!order)return;setBusy(true);setError('');try{const updated=await reportOrderPayment(order.orderNo,`${order.recipientPhone.slice(-4)} ${product.name}`);setOrder(updated);setStep('done')}catch(reason){setError(reason instanceof ApiError?reason.message:'转账报告提交失败')}finally{setBusy(false)}};
  const price=order?`¥ ${Number(order.totalAmount).toFixed(2)}`:product.price.replace(' 起','');
  return <div className="modal-backdrop checkout-backdrop"><section className="checkout-modal"><header><div><small>ORDER WORKFLOW</small><h2>{step==='form'?'提交购买信息':step==='pay'?'扫码转账':'等待后台核款'}</h2></div><button onClick={onClose}>×</button></header><div className="checkout-steps"><span className="active">1 创建订单</span><i/><span className={step!=='form'?'active':''}>2 报告转账</span><i/><span className={step==='done'?'active':''}>3 人工核款</span></div>{error&&<div className="login-error" role="alert">{error}{needsLogin&&<button onClick={onLogin}>去登录</button>}</div>}{step==='form'&&<form onSubmit={submit}><div className="order-product"><span>{product.name.slice(0,1)}</span><div><strong>{product.name}</strong><small>{detail?.skus[0]?.specification||'正在读取规格…'}</small></div><b>{price}</b></div><label>收货人<input name="recipientName" required maxLength={100} placeholder="请输入姓名"/></label><label>联系电话<input name="recipientPhone" required pattern="1\d{10}" inputMode="tel" placeholder="11 位手机号"/></label><label>收货地址<textarea name="recipientAddress" required maxLength={500} placeholder="省 / 市 / 区县 / 街道及详细地址"/></label><label>备注<input name="customerNote" maxLength={500} placeholder="可选"/></label><div className="checkout-notice">后端会生成唯一订单号、锁定价格和收款配置；重复点击使用同一幂等键，不会重复建单。</div><button className="checkout-primary" disabled={busy||!detail} type="submit">{busy?'正在创建订单…':'确认信息，下一步 →'}</button></form>}{step==='pay'&&order&&<div className="pay-panel">{order.payment.demo&&<span className="demo-label">本地演示</span>}<DemoQr/><h3>请转账 {price}</h3><p>收款方：{order.payment.payeeName}<br/>转账备注：{order.recipientPhone.slice(-4)} {product.name}<br/>订单号：{order.orderNo}</p>{order.payment.demo&&<div className="pay-warning">此为演示收款配置，不可用于真实支付</div>}<button className="checkout-primary" disabled={busy} onClick={report}>{busy?'正在提交…':'我已完成转账'}</button></div>}{step==='done'&&order&&<div className="order-success"><span>✓</span><h3>转账报告已留痕</h3><p>订单号 {order.orderNo}<br/>当前状态：{order.status}。运营人员核对实际到账后，才会通知农户备货。</p><button className="checkout-primary" onClick={onClose}>完成</button></div>}<footer>村庄统一受理 · 人工核款 · 农户备货 · 统一发货</footer></section></div>;
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
      {tab==='overview'&&<><div className="admin-stats"><article><span>待确认收款</span><strong>6</strong><small className="orange">需要人工核对</small></article><article><span>待发货</span><strong>3</strong><small>今日处理</small></article><article><span>农户待审核记录</span><strong>2</strong><small>照片与视频</small></article><article><span>本周成交意向</span><strong>48</strong><small>↑ 18.4%</small></article></div><div className="admin-grid"><article className="content-status"><div className="admin-card-head"><div><h2>今日需要处理</h2><p>收款、审核、备货与发货统一流转</p></div><strong>11</strong></div><ul className="ops-todo"><li><span className="op-icon money">¥</span><div><b>6 笔订单等待确认到账</b><small>客户已上传转账备注</small></div><button onClick={()=>setTab('orders')}>去核款</button></li><li><span className="op-icon farm">田</span><div><b>2 条农户生产记录待审核</b><small>确认真实后可对外公开</small></div><button onClick={()=>setTab('goods')}>去审核</button></li><li><span className="op-icon box">□</span><div><b>3 笔订单等待发货</b><small>需要填写物流公司与单号</small></div><button onClick={()=>setTab('orders')}>去发货</button></li></ul></article><article className="admin-preview"><div><span>公开页预览</span><button onClick={onExit}>打开 ↗</button></div><div className="mini-page"><Image src="/images/village-pond.jpg" alt="公开页预览" fill sizes="30vw"/><h3>真实记录，让好产品被看见。</h3></div></article></div><article className="recent-table"><div className="admin-card-head"><div><h2>最新业务动态</h2><p>订单与农户内容统一留痕</p></div><button onClick={()=>setTab('orders')}>查看全部</button></div><div className="table-row head"><span>业务内容</span><span>类型</span><span>状态</span><span>更新时间</span><span/></div>{['订单 NP0018 · 太行山核桃|订单|待核款|刚刚','梁有福 · 核桃采收记录|生产记录|待审核|8 分钟前','订单 NP0016 · 山野百花蜜|订单|待发货|1 小时前'].map(row=>{const [a,b,c,d]=row.split('|');return <div className="table-row" key={a}><strong>{a}</strong><span>{b}</span><span className={c==='待发货'?'published':'draft'}>{c}</span><span>{d}</span><button>•••</button></div>})}</article></>}
      {tab==='orders'&&<section className="orders-panel"><div className="order-flow-head"><div><small>统一订单状态流</small><h2>客户下单 → 转账 → 后台核款 → 农户备货 → 统一发货</h2></div><span>全流程留痕</span></div><div className="order-flow">{orderStages.map((stage,index)=><div className={index<=orderStage?'active':''} key={stage}><span>{index<orderStage?'✓':index+1}</span><b>{stage}</b>{index<orderStages.length-1&&<i/>}</div>)}</div><div className="orders-layout"><aside className="order-queue"><header><h3>订单队列</h3><span>6 笔待核款</span></header>{['NP202608290018|张晓宁|太行山核桃|¥29.90','NP202608290017|李敏|山野百花蜜|¥39.00','NP202608290016|王先生|石磨小米|¥33.60'].map((row,index)=>{const [id,name,goods,amount]=row.split('|');return <button className={index===0?'active':''} key={id}><span>{index===0?'待核款':index===1?'待核款':'待发货'}</span><b>{goods}</b><small>{name} · {amount}</small><i>{id.slice(-4)}</i></button>})}</aside><article className="order-detail"><header><div><small>订单 NP202608290018</small><h3>太行山核桃 × 1</h3></div><span className="order-status">{orderStages[orderStage]}</span></header><div className="order-info-grid"><div><small>客户</small><b>张晓宁 · 138****2806</b></div><div><small>应收金额</small><b className="amount">¥29.90</b></div><div><small>收货地址</small><b>河南省郑州市金水区 ×× 路 18 号</b></div><div><small>转账备注</small><b>2806 核桃</b></div></div><div className="payment-proof"><DemoQr/><div><small>客户支付信息</small><h4>已点击“我已完成转账”</h4><p>提交时间：2026-08-29 12:18<br/>收款渠道：村庄统一收款码（原型）</p></div><button onClick={()=>notify('已记录：需要人工核对实际到账')}>查看核款说明</button></div>{orderStage>=2&&<div className="shipping-form"><label>物流公司<select defaultValue="顺丰速运"><option>顺丰速运</option><option>邮政快递</option><option>中通快递</option></select></label><label>快递单号<input defaultValue="SF1234567890"/></label></div>}<footer><button onClick={()=>notify('已添加内部订单备注')}>添加备注</button><button className="primary" onClick={advanceOrder} disabled={orderStage===4}>{orderStage===0?'确认已到账':orderStage===1?'确认备货完成':orderStage===2?'录入单号并发货':orderStage===3?'确认订单完成':'订单已完成'} →</button></footer><div className="risk-note">重要：原型仅演示人工核款流程。真实上线需配置唯一订单号、转账备注、防重复确认、操作日志和退款处理。</div></article></div></section>}
      {tab!=='overview'&&tab!=='orders'&&<section className="manage-list"><div className="manage-toolbar"><div><button className="active">全部</button><button>已发布</button><button>待审核</button></div><button onClick={()=>setShowForm(true)}>＋ 新增{tab==='stay'?'民宿':tab==='goods'?'农品':tab==='experience'?'游玩项目':'联系人'}</button></div>{manageItems.map((item,index)=><article key={item.name}><span className="row-avatar">{tab==='stay'?'宿':tab==='goods'?'品':tab==='experience'?'游':'联'}</span><div><h3>{item.name}</h3><p>{'type' in item?item.type:item.season} · {item.desc}</p>{tab==='goods'&&<small className="record-count">真实生产记录 {index+5} 条 · 最近更新 {index+1} 天前</small>}{tab==='experience'&&<small className="record-count">{'hasVideo' in item&&item.hasVideo?'视频 1 条 · 图集 6 张':'图集 4 张'} · 预约规则已配置</small>}</div><span className={index===1?'published':'draft'}>{index===1?'展示中':tab==='goods'?'待审核':'待完善'}</span><button onClick={()=>notify(tab==='goods'?'已打开生产记录审核页':tab==='experience'?'已打开项目与媒体配置':'已打开编辑页')}>{tab==='goods'?'审核过程':tab==='experience'?'配置媒体':'编辑'}</button><button>•••</button></article>)}</section>}
    </section>
    {showForm&&<div className="modal-backdrop"><form className="content-form" onSubmit={submit}><header><div><small>CONTENT PUBLISH</small><h2>上架新内容</h2></div><button type="button" onClick={()=>setShowForm(false)}>×</button></header><div className="type-options"><label><input type="radio" name="type" defaultChecked={tab==='stay'||tab==='overview'}/> 民宿</label><label><input type="radio" name="type" defaultChecked={tab==='goods'}/> 农产品</label><label><input type="radio" name="type" defaultChecked={tab==='experience'}/> 游玩采摘</label><label><input type="radio" name="type" defaultChecked={tab==='contact'}/> 联系方式</label></div><label>名称<input required placeholder="例如：秋季核桃采摘体验"/></label><div className="form-grid"><label>价格 / 说明<input placeholder="例如：¥68 / 人"/></label><label>联系电话<input placeholder="请填写真实联系电话"/></label></div><label>简介<textarea placeholder="用一两句话介绍特色、服务与注意事项"/></label><label className="upload-box media-upload"><input type="file" accept="image/*,video/*" multiple/><span>＋ 上传图片或视频</span><small>支持 JPG、PNG、MP4；首张图片作为封面，可拖动排序</small></label><footer><button type="button" onClick={()=>setShowForm(false)}>取消</button><button type="submit">保存为草稿</button><button type="submit" className="primary">保存并发布</button></footer></form></div>}
    {toast&&<div className="toast">✓ {toast}</div>}
  </main>
}
