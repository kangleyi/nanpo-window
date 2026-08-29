'use client';

import Image from 'next/image';
import { FormEvent, useState } from 'react';

type RouteType = 'drive' | 'rail' | 'bus';
type AdminTab = 'overview' | 'orders' | 'stay' | 'goods' | 'contact';
type AppMode = 'public' | 'admin' | 'farmer';

const routes: Record<RouteType, { title: string; time: string; steps: string[]; note: string }> = {
  drive: { title: '从郑州自驾出发', time: '约 1.5 小时', steps: ['郑云高速 S87', '云台山站下高速', '焦辉路 S306', '青云大道 X006', '大南坡村'], note: '006 县道穿村而过，建议直接导航“大南坡艺术中心”。' },
  rail: { title: '高铁到修武西站', time: '郑州出发约 30 分钟', steps: ['郑州 / 郑州东', '修武西站', '修武至西村公交', '大南坡站'], note: '公交班次可能随季节调整，请在出发前向车站或村庄服务点确认。' },
  bus: { title: '从焦作市区乘公交', time: '约 50—70 分钟', steps: ['焦作站南广场', '37 路公交', '山阳建国站', '换乘 29 路', '大南坡站'], note: '公开资料显示也可乘 29 路抵达，实际站点与班次请以当天信息为准。' },
};

const stayCards = [
  { name: '牛大爷的院子', type: '乡土院落 · 示例房源', desc: '老院落、核桃树与山里清晨，适合一家人慢住两晚。', price: '价格待录入', image: '/images/homestay.jpg', beds: '2—4 人' },
  { name: '南坡山居 · 一号院', type: '整院包住 · 示例房源', desc: '灰砖院落保留北方村居尺度，步行可达艺术中心。', price: '价格待录入', image: '/images/village-detail.jpg', beds: '4—6 人' },
  { name: '松风小院', type: '双床客房 · 示例房源', desc: '面向太行山南麓，适合周末、研学与小型团队。', price: '价格待录入', image: '/images/village-pond.jpg', beds: '2 人' },
];

const goodsCards = [
  { name: '太行山核桃', icon: '核', season: '秋季新收', desc: '山地自然生长，壳薄仁香。', price: '¥ 29.9 起' },
  { name: '南坡山花椒', icon: '椒', season: '农户晒制', desc: '香气清亮，适合家常炖煮。', price: '¥ 19.9 起' },
  { name: '石磨小米', icon: '米', season: '当季谷物', desc: '颗粒饱满，煮粥米香自然。', price: '¥ 16.8 起' },
  { name: '山野百花蜜', icon: '蜜', season: '限量采收', desc: '来自太行山脚的四季花香。', price: '¥ 39.0 起' },
];

function PublicWindow({ onManage, onFarmer }: { onManage: () => void; onFarmer: () => void }) {
  const [routeType, setRouteType] = useState<RouteType>('drive');
  const [toast, setToast] = useState('');
  const [orderItem, setOrderItem] = useState<string | null>(null);
  const [storyItem, setStoryItem] = useState<string | null>(null);
  const route = routes[routeType];
  const notify = (message: string) => { setToast(message); window.setTimeout(() => setToast(''), 2400); };

  return (
    <main>
      <header className="site-header">
        <a className="brand" href="#top"><span className="brand-seal">南</span><span><b>南坡之窗</b><small>WINDOW OF NANPO</small></span></a>
        <nav aria-label="主要导航"><a href="#about">走进南坡</a><a href="#route">行前指南</a><a href="#stay">山居一晚</a><a href="#goods">山野好物</a></nav>
        <div className="header-actions"><button className="weather farmer-entry" onClick={onFarmer}>农户入口</button><button className="weather" onClick={() => notify('访客服务电话将在后台录入后展示')}>◌ 访客服务</button><button className="manage" onClick={onManage}>内容管理 ↗</button></div>
      </header>

      <section className="hero" id="top">
        <div className="hero-copy">
          <div className="location-line"><span>HENAN · JIAOZUO · XIUWU</span><i /></div>
          <h1>山在这里，<br/>风也在这里，<br/><em>日子慢下来。</em></h1>
          <p>在太行山南麓，有一座把老礼堂变成艺术中心、把乡土日常写成美学故事的村庄。欢迎来到大南坡。</p>
          <div className="hero-buttons"><button onClick={() => document.getElementById('route')?.scrollIntoView({behavior:'smooth'})}>规划我的南坡之行 <span>→</span></button><button className="play" onClick={() => notify('南坡声音故事即将上线')}>▶ <span>听一段南坡的声音</span></button></div>
          <div className="hero-facts"><div><b>20<sup>km</sup></b><small>距焦作市区约</small></div><div><b>4—9<sup>月</sup></b><small>推荐到访季节</small></div><div><b>4<sup>处</sup></b><small>公共文化空间</small></div></div>
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
        <a href="#about"><span className="quick-no">04</span><div><small>VILLAGE STORY</small><b>读懂南坡故事</b></div><i>↗</i></a>
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
        <div className="route-head"><div><span>行前指南</span><h2>从城市出发，<br/>向山的方向走。</h2></div><p>目的地：河南省焦作市修武县西村乡大南坡村<br/>建议导航至“大南坡艺术中心”</p></div>
        <div className="route-planner">
          <div className="route-tabs"><button className={routeType==='drive'?'active':''} onClick={() => setRouteType('drive')}>自驾前往</button><button className={routeType==='rail'?'active':''} onClick={() => setRouteType('rail')}>高铁换乘</button><button className={routeType==='bus'?'active':''} onClick={() => setRouteType('bus')}>市区公交</button></div>
          <div className="route-content"><div className="route-summary"><small>RECOMMENDED ROUTE</small><h3>{route.title}</h3><strong>{route.time}</strong><p>{route.note}</p><button onClick={() => notify('已复制目的地：大南坡艺术中心')}>复制目的地地址 ↗</button></div><div className="route-line">{route.steps.map((step,index)=><div key={step}><span>{index+1}</span><b>{step}</b>{index<route.steps.length-1&&<i/>}</div>)}</div><div className="map-card"><div className="map-mountains"><i/><i/><i/></div><span className="map-city">焦作市区</span><span className="map-road">X006</span><span className="map-pin">南<small>大南坡村</small></span><span className="map-north">N ↑</span></div></div>
        </div>
        <div className="travel-note"><span>出发提醒</span><p>公交线路、班次和开放时间可能临时调整；节假日建议提前一天确认，并优先选择白天进村。</p><button onClick={() => notify('行前提醒已保存')}>保存提醒</button></div>
      </section>

      <section className="stay-section" id="stay">
        <div className="section-kicker"><span>03</span><small>STAY IN NANPO</small></div>
        <div className="section-title-row"><div><span>在村里住一晚</span><h2>推开院门，听见山里的清晨。</h2></div><p>现有公开资料显示村内已建设多套山居民宿。以下房源内容为高保真示例，具体名称、价格与联系方式将在管理后台录入后公开。</p></div>
        <div className="stay-grid">{stayCards.map((item,index)=><article key={item.name}><div className="stay-image"><Image src={item.image} alt={item.name} fill sizes="33vw"/><span>0{index+1}</span><button onClick={() => notify(`${item.name}的咨询入口待后台录入`)}>收藏 ♡</button></div><div className="stay-info"><small>{item.type}</small><h3>{item.name}</h3><p>{item.desc}</p><div><span>住 {item.beds}</span><strong>{item.price}</strong><button onClick={() => notify('联系方式录入后即可咨询预订')}>了解详情 →</button></div></div></article>)}</div>
        <div className="operator-cta"><div><span>你是南坡民宿经营者？</span><h3>把你的院子，也放进这扇窗。</h3></div><button onClick={onManage}>去后台上架房源 →</button></div>
      </section>

      <section className="goods-section" id="goods">
        <div className="goods-intro"><div className="section-kicker light"><span>04</span><small>LOCAL HARVEST</small></div><span>山野好物</span><h2>每一份收成都有<br/>自己的时节。</h2><p>山核桃、山花椒、小米与蜂蜜，是公开旅游资料中推荐的焦作山野物产。具体商品、价格和村民联系方式由后台上架。</p><div className="season"><b>八月</b><span><i style={{width:'72%'}}/>核桃与花椒陆续成熟</span></div></div>
        <div className="goods-visual"><Image src="/images/products.jpg" alt="大南坡工销社陈列的农产品" fill sizes="35vw"/><span>工销社里的山野收成</span></div>
        <div className="goods-list">{goodsCards.map((item,index)=><article key={item.name}><span className="goods-index">0{index+1}</span><div className="goods-icon">{item.icon}</div><div><small>{item.season}</small><h3>{item.name}</h3><p>{item.desc}</p></div><strong>{item.price}</strong><button className="trace-button" onClick={() => setStoryItem(item.name)}>看过程</button><button onClick={() => setOrderItem(item.name)}>购买</button></article>)}</div>
      </section>

      <section className="day-trip"><div className="day-photo"><Image src="/images/village-pond.jpg" alt="大南坡村院落生活" fill sizes="40vw"/><span>ONE DAY IN NANPO</span></div><div className="day-copy"><span>一日南坡建议</span><h2>不赶路，去感受。</h2><div className="timeline"><div><b>09:30</b><p><strong>抵达大南坡</strong><small>从艺术中心开始认识村庄</small></p></div><div><b>11:00</b><p><strong>方所乡村文化</strong><small>在老戏台改成的书店慢慢读</small></p></div><div><b>13:30</b><p><strong>老村散步</strong><small>沿灰砖院落与古树寻找乡土日常</small></p></div><div><b>16:00</b><p><strong>碧山工销社</strong><small>挑一份山野物产带回家</small></p></div></div><button onClick={() => notify('一日游路线已保存')}>收藏这条路线 →</button></div></section>

      <footer className="site-footer"><div className="footer-brand"><span className="brand-seal">南</span><h2>南坡之窗</h2><p>太行山下，一座会生长的村庄。</p></div><div><small>来南坡</small><a href="#route">出行路线</a><a href="#stay">民宿山居</a><a href="#goods">乡野好物</a></div><div><small>认识南坡</small><a href="#about">村庄故事</a><a href="#about">文化空间</a><button onClick={onManage}>内容管理</button></div><div className="footer-contact"><small>访客服务</small><strong>电话待后台录入</strong><p>河南省焦作市修武县<br/>西村乡大南坡村</p></div><div className="source-note">路线与村庄资料参考文化和旅游部、修武县及焦作市公开信息，更新时间：2026 年 8 月。出行前请再次核实班次。</div></footer>
      {storyItem&&<ProductStory product={storyItem} onClose={()=>setStoryItem(null)} onBuy={()=>{setStoryItem(null);setOrderItem(storyItem)}}/>}
      {orderItem&&<CheckoutFlow product={orderItem} onClose={()=>setOrderItem(null)}/>} 
      {toast&&<div className="toast">✓ {toast}</div>}
    </main>
  );
}

function ProductStory({ product, onClose, onBuy }: { product: string; onClose: () => void; onBuy: () => void }) {
  const stages = [
    { date:'03月12日', title:'山地整土', text:'翻整冬土、清理石块，让土地透气。', image:'/images/village-pond.jpg' },
    { date:'04月06日', title:'春日播种', text:'选用农户自留种，记录地块与播种批次。', image:'/images/walnut-yard.jpg' },
    { date:'06月18日', title:'自然生长', text:'人工除草，本批次不使用催熟剂。', image:'/images/village-detail.jpg' },
    { date:'08月22日', title:'成熟采收', text:'由农户梁有福在清晨完成采收与初选。', image:'/images/products.jpg' },
  ];
  return <div className="modal-backdrop story-backdrop"><section className="story-modal"><header><div><small>FARM TO TABLE · 真实生产档案</small><h2>{product}的一生</h2><p>由农户上传，村庄后台审核后对外公开</p></div><button onClick={onClose}>×</button></header><div className="farmer-proof"><span>梁</span><div><strong>梁有福 · 南坡种植户</strong><small>已实名 · 地块编号 NP-03</small></div><i>村级审核 ✓</i></div><div className="process-timeline">{stages.map((stage,index)=><article key={stage.title}><div className="process-image"><Image src={stage.image} alt={stage.title} fill sizes="150px"/></div><span>0{index+1}</span><div><small>{stage.date}</small><h3>{stage.title}</h3><p>{stage.text}</p></div></article>)}</div><div className="story-actions"><p>最后更新：2026-08-22 · 共 8 条原始记录</p><button onClick={onBuy}>信任这份收成，去购买 →</button></div></section></div>;
}

function DemoQr(){return <div className="demo-qr" aria-label="演示收款码，不可用于真实支付"><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><span>演示<br/>不可支付</span></div>}

function CheckoutFlow({ product, onClose }: { product: string; onClose: () => void }) {
  const [step,setStep]=useState<'form'|'pay'|'done'>('form');
  const price=goodsCards.find(item=>item.name===product)?.price.replace(' 起','')||'¥29.9';
  return <div className="modal-backdrop checkout-backdrop"><section className="checkout-modal"><header><div><small>ORDER REQUEST</small><h2>{step==='form'?'提交购买信息':step==='pay'?'扫码转账':'订单已提交'}</h2></div><button onClick={onClose}>×</button></header><div className="checkout-steps"><span className="active">1 填写信息</span><i/><span className={step!=='form'?'active':''}>2 扫码转账</span><i/><span className={step==='done'?'active':''}>3 后台确认</span></div>{step==='form'&&<form onSubmit={(event)=>{event.preventDefault();setStep('pay')}}><div className="order-product"><span>{product.slice(0,1)}</span><div><strong>{product}</strong><small>来自南坡农户 · 具体规格后台确认</small></div><b>{price}</b></div><label>收货人<input required placeholder="请输入姓名"/></label><label>联系电话<input required inputMode="tel" placeholder="用于确认订单与发货通知"/></label><label>收货地址<textarea required placeholder="省 / 市 / 区县 / 街道及详细地址"/></label><div className="checkout-notice">本页面暂不接入在线支付。提交后展示村庄统一收款码，由后台人工确认到账并安排发货。</div><button className="checkout-primary" type="submit">确认信息，下一步 →</button></form>}{step==='pay'&&<div className="pay-panel"><span className="demo-label">原型演示</span><DemoQr/><h3>请转账 {price}</h3><p>收款方：南坡村农产品服务中心（示例）<br/>转账时请备注：手机尾号 + 商品名称</p><div className="pay-warning">此为模拟收款码，不可用于真实支付</div><button className="checkout-primary" onClick={()=>setStep('done')}>我已完成转账</button><button className="secondary" onClick={()=>setStep('form')}>返回修改信息</button></div>}{step==='done'&&<div className="order-success"><span>✓</span><h3>已通知后台核对收款</h3><p>订单号 NP202608290018<br/>后台确认到账后，将进入备货与发货流程。</p><div className="customer-status"><div className="active"><i/>已提交<small>刚刚</small></div><div><i/>待确认收款<small>预计 2 小时内</small></div><div><i/>待发货<small>确认后处理</small></div><div><i/>运输中<small>录入快递单号</small></div></div><button className="checkout-primary" onClick={onClose}>完成</button></div>}<footer>村庄统一受理 · 人工核款 · 农户备货 · 统一发货</footer></section></div>;
}

function FarmerPortal({ onExit }: { onExit: () => void }) {
  const [showUpload,setShowUpload]=useState(false);
  const [toast,setToast]=useState('');
  const [generated,setGenerated]=useState(false);
  const notify=(message:string)=>{setToast(message);window.setTimeout(()=>setToast(''),2200)};
  const submit=(event:FormEvent)=>{event.preventDefault();setShowUpload(false);notify('生产记录已提交，等待村庄后台审核')};
  return <main className="farmer-portal"><header><button onClick={onExit}>← 返回南坡之窗</button><div className="farmer-logo"><span>南</span><div><b>农户经营台</b><small>真实记录，让好产品被看见</small></div></div><button className="farmer-help" onClick={()=>notify('村庄服务人员会协助您完成上传')}>不会用？找人帮忙</button></header><section className="farmer-main"><div className="farmer-welcome"><div><small>上午好，梁叔</small><h1>今天地里有什么新变化？</h1><p>拍张照片、说几句话，就能留下真实生产过程。</p></div><button onClick={()=>setShowUpload(true)}><span>＋</span><b>上传今日记录</b><small>照片、视频、语音都可以</small></button></div><div className="farmer-summary"><article><span>田</span><div><small>我的地块</small><strong>核桃坡 3 号地</strong></div><i>已认证</i></article><article><span>记</span><div><small>本季真实记录</small><strong>8 条</strong></div><i>6 条已公开</i></article><article><span>品</span><div><small>正在展示</small><strong>2 件农品</strong></div><i>48 人想买</i></article></div><section className="farmer-content-grid"><article className="farm-diary"><header><div><h2>生产过程</h2><p>按照时间记录，顾客看得更放心</p></div><button onClick={()=>setShowUpload(true)}>＋ 添加记录</button></header><div className="diary-list"><div><span className="diary-date">08/22</span><div className="diary-photo"><Image src="/images/products.jpg" alt="核桃采收记录" fill sizes="120px"/></div><div><small>成熟采收 · 已公开</small><h3>第一批核桃开始采收</h3><p>今早六点和家里人一起摘的，挑出外壳完整的先晾晒。</p><div><b>图片 3</b><b>语音 00:36</b><b>村级已审核</b></div></div></div><div><span className="diary-date">06/18</span><div className="diary-photo"><Image src="/images/walnut-yard.jpg" alt="核桃自然生长记录" fill sizes="120px"/></div><div><small>自然生长 · 已公开</small><h3>今年雨水足，核桃长势不错</h3><p>这几天人工除了草，没有打催熟药，叶子和果子都很精神。</p><div><b>图片 2</b><b>村级已审核</b></div></div></div><div className="draft-entry"><span className="diary-date">今天</span><div className="diary-photo empty">＋</div><div><small>待记录</small><h3>继续记录，故事才完整</h3><p>建议拍摄：晾晒、分选、装袋过程</p></div></div></div></article><aside className="share-builder"><span>AI 对外表达助手</span><h2>您讲种植，<br/>我帮您讲给顾客听。</h2><p>小禾会读取已审核的真实记录，整理成不夸大、有出处的产品故事。</p>{!generated?<button onClick={()=>setGenerated(true)}>✦ 生成一段对外介绍</button>:<div className="generated-copy"><small>根据 6 条公开记录生成</small><p>“梁叔家的山核桃，生长在南坡村核桃坡 3 号地。从春天整土，到夏季人工除草，再到清晨采收，每一步都有照片和日期可查。没有漂亮话，只有这一季真实的生长。”</p><div><button onClick={()=>notify('文案已复制，可发到微信')}>复制文案</button><button onClick={onExit}>预览公开页</button></div></div>}<div className="share-score"><div><span>真实度</span><b>100%</b></div><div><span>记录完整度</span><b>75%</b></div></div></aside></section></section>{showUpload&&<div className="modal-backdrop"><form className="farm-upload" onSubmit={submit}><header><div><small>NEW FARM RECORD</small><h2>记录今天的生产过程</h2></div><button type="button" onClick={()=>setShowUpload(false)}>×</button></header><label>选择农品<select><option>太行山核桃 · 核桃坡 3 号地</option><option>南坡山花椒 · 东坡 1 号地</option></select></label><label>今天做了什么？<div className="stage-choices"><span>整地</span><span>播种</span><span>施肥</span><span>生长</span><span className="active">采收</span><span>包装</span></div></label><label className="farm-media"><input type="file" accept="image/*,video/*" multiple/><span>＋</span><b>拍照或上传视频</b><small>保留拍摄时间，便于形成真实档案</small></label><label>说说现场情况<textarea placeholder="可以打字，也可以点右侧话筒直接说…"/><button type="button">● 语音</button></label><label className="truth-check"><input type="checkbox" required/> 我确认以上记录来自本人真实生产过程，同意后台审核后对外展示。</label><footer><button type="button" onClick={()=>setShowUpload(false)}>先不上传</button><button type="submit">提交审核 →</button></footer></form></div>}{toast&&<div className="toast">✓ {toast}</div>}</main>;
}

function AdminConsole({ onExit }: { onExit: () => void }) {
  const [tab,setTab]=useState<AdminTab>('overview');
  const [showForm,setShowForm]=useState(false);
  const [toast,setToast]=useState('');
  const [orderStage,setOrderStage]=useState(0);
  const notify=(message:string)=>{setToast(message);window.setTimeout(()=>setToast(''),2200)};
  const submit=(event:FormEvent)=>{event.preventDefault();setShowForm(false);notify('内容已保存为草稿，可预览后发布')};
  const orderStages=['待确认收款','已收款 · 待备货','待发货','运输中','已完成'];
  const advanceOrder=()=>{if(orderStage<4){setOrderStage(orderStage+1);notify(orderStage===0?'已确认到账，订单已通知农户备货':orderStage===1?'备货完成，等待录入快递单号':orderStage===2?'快递单号已录入，客户已收到通知':'订单已确认完成')}};
  return <main className="admin-shell">
    <aside className="admin-sidebar"><div className="brand admin-brand"><span className="brand-seal">南</span><span><b>南坡之窗</b><small>村庄运营中心</small></span></div><nav><button className={tab==='overview'?'active':''} onClick={()=>setTab('overview')}>⌂ <span>总览</span></button><button className={tab==='orders'?'active':''} onClick={()=>setTab('orders')}>▤ <span>订单与发货</span><i className="alert-badge">6</i></button><button className={tab==='stay'?'active':''} onClick={()=>setTab('stay')}>▦ <span>民宿管理</span><i>3</i></button><button className={tab==='goods'?'active':''} onClick={()=>setTab('goods')}>◇ <span>农品与过程</span><i>4</i></button><button className={tab==='contact'?'active':''} onClick={()=>setTab('contact')}>◌ <span>联系信息</span></button></nav><div className="admin-bottom"><button onClick={onExit}>← 返回南坡之窗</button><div><span>管</span><p><b>村庄管理员</b><small>内容与订单运营</small></p></div></div></aside>
    <section className="admin-main"><header><div><small>南坡之窗 / 村庄运营中心</small><h1>{tab==='overview'?'运营总览':tab==='orders'?'订单与发货':tab==='stay'?'民宿管理':tab==='goods'?'农品与生产过程':'联系信息'}</h1></div><div><button onClick={onExit}>↗ 预览公开页面</button>{tab!=='orders'&&<button className="primary" onClick={()=>setShowForm(true)}>＋ 上架新内容</button>}</div></header>
      {tab==='overview'&&<><div className="admin-stats"><article><span>待确认收款</span><strong>6</strong><small className="orange">需要人工核对</small></article><article><span>待发货</span><strong>3</strong><small>今日处理</small></article><article><span>农户待审核记录</span><strong>2</strong><small>照片与视频</small></article><article><span>本周成交意向</span><strong>48</strong><small>↑ 18.4%</small></article></div><div className="admin-grid"><article className="content-status"><div className="admin-card-head"><div><h2>今日需要处理</h2><p>收款、审核、备货与发货统一流转</p></div><strong>11</strong></div><ul className="ops-todo"><li><span className="op-icon money">¥</span><div><b>6 笔订单等待确认到账</b><small>客户已上传转账备注</small></div><button onClick={()=>setTab('orders')}>去核款</button></li><li><span className="op-icon farm">田</span><div><b>2 条农户生产记录待审核</b><small>确认真实后可对外公开</small></div><button onClick={()=>setTab('goods')}>去审核</button></li><li><span className="op-icon box">□</span><div><b>3 笔订单等待发货</b><small>需要填写物流公司与单号</small></div><button onClick={()=>setTab('orders')}>去发货</button></li></ul></article><article className="admin-preview"><div><span>公开页预览</span><button onClick={onExit}>打开 ↗</button></div><div className="mini-page"><Image src="/images/village-pond.jpg" alt="公开页预览" fill sizes="30vw"/><h3>真实记录，让好产品被看见。</h3></div></article></div><article className="recent-table"><div className="admin-card-head"><div><h2>最新业务动态</h2><p>订单与农户内容统一留痕</p></div><button onClick={()=>setTab('orders')}>查看全部</button></div><div className="table-row head"><span>业务内容</span><span>类型</span><span>状态</span><span>更新时间</span><span/></div>{['订单 NP0018 · 太行山核桃|订单|待核款|刚刚','梁有福 · 核桃采收记录|生产记录|待审核|8 分钟前','订单 NP0016 · 山野百花蜜|订单|待发货|1 小时前'].map(row=>{const [a,b,c,d]=row.split('|');return <div className="table-row" key={a}><strong>{a}</strong><span>{b}</span><span className={c==='待发货'?'published':'draft'}>{c}</span><span>{d}</span><button>•••</button></div>})}</article></>}
      {tab==='orders'&&<section className="orders-panel"><div className="order-flow-head"><div><small>统一订单状态流</small><h2>客户下单 → 转账 → 后台核款 → 农户备货 → 统一发货</h2></div><span>全流程留痕</span></div><div className="order-flow">{orderStages.map((stage,index)=><div className={index<=orderStage?'active':''} key={stage}><span>{index<orderStage?'✓':index+1}</span><b>{stage}</b>{index<orderStages.length-1&&<i/>}</div>)}</div><div className="orders-layout"><aside className="order-queue"><header><h3>订单队列</h3><span>6 笔待核款</span></header>{['NP202608290018|张晓宁|太行山核桃|¥29.90','NP202608290017|李敏|山野百花蜜|¥39.00','NP202608290016|王先生|石磨小米|¥33.60'].map((row,index)=>{const [id,name,goods,amount]=row.split('|');return <button className={index===0?'active':''} key={id}><span>{index===0?'待核款':index===1?'待核款':'待发货'}</span><b>{goods}</b><small>{name} · {amount}</small><i>{id.slice(-4)}</i></button>})}</aside><article className="order-detail"><header><div><small>订单 NP202608290018</small><h3>太行山核桃 × 1</h3></div><span className="order-status">{orderStages[orderStage]}</span></header><div className="order-info-grid"><div><small>客户</small><b>张晓宁 · 138****2806</b></div><div><small>应收金额</small><b className="amount">¥29.90</b></div><div><small>收货地址</small><b>河南省郑州市金水区 ×× 路 18 号</b></div><div><small>转账备注</small><b>2806 核桃</b></div></div><div className="payment-proof"><DemoQr/><div><small>客户支付信息</small><h4>已点击“我已完成转账”</h4><p>提交时间：2026-08-29 12:18<br/>收款渠道：村庄统一收款码（原型）</p></div><button onClick={()=>notify('已记录：需要人工核对实际到账')}>查看核款说明</button></div>{orderStage>=2&&<div className="shipping-form"><label>物流公司<select defaultValue="顺丰速运"><option>顺丰速运</option><option>邮政快递</option><option>中通快递</option></select></label><label>快递单号<input defaultValue="SF1234567890"/></label></div>}<footer><button onClick={()=>notify('已添加内部订单备注')}>添加备注</button><button className="primary" onClick={advanceOrder} disabled={orderStage===4}>{orderStage===0?'确认已到账':orderStage===1?'确认备货完成':orderStage===2?'录入单号并发货':orderStage===3?'确认订单完成':'订单已完成'} →</button></footer><div className="risk-note">重要：原型仅演示人工核款流程。真实上线需配置唯一订单号、转账备注、防重复确认、操作日志和退款处理。</div></article></div></section>}
      {tab!=='overview'&&tab!=='orders'&&<section className="manage-list"><div className="manage-toolbar"><div><button className="active">全部</button><button>已发布</button><button>待审核</button></div><button onClick={()=>setShowForm(true)}>＋ 新增{tab==='stay'?'民宿':tab==='goods'?'农品':'联系人'}</button></div>{(tab==='stay'?stayCards:tab==='goods'?goodsCards:[{name:'村庄访客服务',type:'电话待录入',desc:'用于公开页顶部与底部联系咨询入口',price:'未发布'}]).map((item,index)=><article key={item.name}><span className="row-avatar">{tab==='stay'?'宿':tab==='goods'?'品':'联'}</span><div><h3>{item.name}</h3><p>{'type' in item?item.type:''} · {'desc' in item?item.desc:''}</p>{tab==='goods'&&<small className="record-count">真实生产记录 {index+5} 条 · 最近更新 {index+1} 天前</small>}</div><span className={index===1?'published':'draft'}>{index===1?'展示中':tab==='goods'?'待审核':'待完善'}</span><button onClick={()=>notify(tab==='goods'?'已打开生产记录审核页':'已打开编辑页')}>{tab==='goods'?'审核过程':'编辑'}</button><button>•••</button></article>)}</section>}
    </section>
    {showForm&&<div className="modal-backdrop"><form className="content-form" onSubmit={submit}><header><div><small>CONTENT PUBLISH</small><h2>上架新内容</h2></div><button type="button" onClick={()=>setShowForm(false)}>×</button></header><div className="type-options"><label><input type="radio" name="type" defaultChecked/> 民宿</label><label><input type="radio" name="type"/> 农产品</label><label><input type="radio" name="type"/> 联系方式</label></div><label>名称<input required placeholder="例如：南坡山居一号院"/></label><div className="form-grid"><label>价格 / 说明<input placeholder="例如：¥368 / 晚"/></label><label>联系电话<input placeholder="请填写真实联系电话"/></label></div><label>简介<textarea placeholder="用一两句话介绍特色、服务与注意事项"/></label><label className="upload-box"><input type="file" accept="image/*"/><span>＋ 上传封面图片</span><small>支持 JPG、PNG，建议横版图片</small></label><footer><button type="button" onClick={()=>setShowForm(false)}>取消</button><button type="submit">保存为草稿</button><button type="submit" className="primary">保存并发布</button></footer></form></div>}
    {toast&&<div className="toast">✓ {toast}</div>}
  </main>
}

export default function Home(){const [mode,setMode]=useState<AppMode>('public');return mode==='admin'?<AdminConsole onExit={()=>setMode('public')}/>:mode==='farmer'?<FarmerPortal onExit={()=>setMode('public')}/>:<PublicWindow onManage={()=>setMode('admin')} onFarmer={()=>setMode('farmer')}/>}
