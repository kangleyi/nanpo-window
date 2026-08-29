'use client';

import Image from 'next/image';
import { FormEvent, useState } from 'react';

type RouteType = 'drive' | 'rail' | 'bus';
type AdminTab = 'overview' | 'stay' | 'goods' | 'contact';

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

function PublicWindow({ onManage }: { onManage: () => void }) {
  const [routeType, setRouteType] = useState<RouteType>('drive');
  const [toast, setToast] = useState('');
  const route = routes[routeType];
  const notify = (message: string) => { setToast(message); window.setTimeout(() => setToast(''), 2400); };

  return (
    <main>
      <header className="site-header">
        <a className="brand" href="#top"><span className="brand-seal">南</span><span><b>南坡之窗</b><small>WINDOW OF NANPO</small></span></a>
        <nav aria-label="主要导航"><a href="#about">走进南坡</a><a href="#route">行前指南</a><a href="#stay">山居一晚</a><a href="#goods">山野好物</a></nav>
        <div className="header-actions"><button className="weather" onClick={() => notify('访客服务电话将在后台录入后展示')}>◌ 访客服务</button><button className="manage" onClick={onManage}>内容管理 ↗</button></div>
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
        <div className="goods-list">{goodsCards.map((item,index)=><article key={item.name}><span className="goods-index">0{index+1}</span><div className="goods-icon">{item.icon}</div><div><small>{item.season}</small><h3>{item.name}</h3><p>{item.desc}</p></div><strong>{item.price}</strong><button onClick={() => notify(`${item.name}已加入心愿单`)}>＋</button></article>)}</div>
      </section>

      <section className="day-trip"><div className="day-photo"><Image src="/images/village-pond.jpg" alt="大南坡村院落生活" fill sizes="40vw"/><span>ONE DAY IN NANPO</span></div><div className="day-copy"><span>一日南坡建议</span><h2>不赶路，去感受。</h2><div className="timeline"><div><b>09:30</b><p><strong>抵达大南坡</strong><small>从艺术中心开始认识村庄</small></p></div><div><b>11:00</b><p><strong>方所乡村文化</strong><small>在老戏台改成的书店慢慢读</small></p></div><div><b>13:30</b><p><strong>老村散步</strong><small>沿灰砖院落与古树寻找乡土日常</small></p></div><div><b>16:00</b><p><strong>碧山工销社</strong><small>挑一份山野物产带回家</small></p></div></div><button onClick={() => notify('一日游路线已保存')}>收藏这条路线 →</button></div></section>

      <footer className="site-footer"><div className="footer-brand"><span className="brand-seal">南</span><h2>南坡之窗</h2><p>太行山下，一座会生长的村庄。</p></div><div><small>来南坡</small><a href="#route">出行路线</a><a href="#stay">民宿山居</a><a href="#goods">乡野好物</a></div><div><small>认识南坡</small><a href="#about">村庄故事</a><a href="#about">文化空间</a><button onClick={onManage}>内容管理</button></div><div className="footer-contact"><small>访客服务</small><strong>电话待后台录入</strong><p>河南省焦作市修武县<br/>西村乡大南坡村</p></div><div className="source-note">路线与村庄资料参考文化和旅游部、修武县及焦作市公开信息，更新时间：2026 年 8 月。出行前请再次核实班次。</div></footer>
      {toast&&<div className="toast">✓ {toast}</div>}
    </main>
  );
}

function AdminConsole({ onExit }: { onExit: () => void }) {
  const [tab,setTab]=useState<AdminTab>('overview');
  const [showForm,setShowForm]=useState(false);
  const [toast,setToast]=useState('');
  const notify=(message:string)=>{setToast(message);window.setTimeout(()=>setToast(''),2200)};
  const submit=(event:FormEvent)=>{event.preventDefault();setShowForm(false);notify('内容已保存为草稿，可预览后发布')};
  return <main className="admin-shell">
    <aside className="admin-sidebar"><div className="brand admin-brand"><span className="brand-seal">南</span><span><b>南坡之窗</b><small>内容管理台</small></span></div><nav><button className={tab==='overview'?'active':''} onClick={()=>setTab('overview')}>⌂ <span>总览</span></button><button className={tab==='stay'?'active':''} onClick={()=>setTab('stay')}>▦ <span>民宿管理</span><i>3</i></button><button className={tab==='goods'?'active':''} onClick={()=>setTab('goods')}>◇ <span>农品管理</span><i>4</i></button><button className={tab==='contact'?'active':''} onClick={()=>setTab('contact')}>◌ <span>联系信息</span></button></nav><div className="admin-bottom"><button onClick={onExit}>← 返回南坡之窗</button><div><span>管</span><p><b>村庄管理员</b><small>内容运营账号</small></p></div></div></aside>
    <section className="admin-main"><header><div><small>南坡之窗 / 内容管理</small><h1>{tab==='overview'?'运营总览':tab==='stay'?'民宿管理':tab==='goods'?'农品管理':'联系信息'}</h1></div><div><button onClick={onExit}>↗ 预览公开页面</button><button className="primary" onClick={()=>setShowForm(true)}>＋ 上架新内容</button></div></header>
      {tab==='overview'&&<><div className="admin-stats"><article><span>本周访问</span><strong>1,286</strong><small>↑ 18.4%</small></article><article><span>路线收藏</span><strong>96</strong><small>↑ 7.2%</small></article><article><span>民宿咨询</span><strong>23</strong><small>待接入电话</small></article><article><span>农品意向</span><strong>48</strong><small>4 件在展示</small></article></div><div className="admin-grid"><article className="content-status"><div className="admin-card-head"><div><h2>内容完善进度</h2><p>补齐资料后即可正式对外推广</p></div><strong>68%</strong></div><div className="progress"><i/></div><ul><li className="done">✓ 出行路线 <span>已核实</span></li><li>○ 民宿电话与价格 <button onClick={()=>setTab('stay')}>去完善</button></li><li>○ 农产品库存与联系人 <button onClick={()=>setTab('goods')}>去完善</button></li><li className="done">✓ 村庄故事 <span>已发布</span></li></ul></article><article className="admin-preview"><div><span>公开页预览</span><button onClick={onExit}>打开 ↗</button></div><div className="mini-page"><Image src="/images/village-pond.jpg" alt="公开页预览" fill sizes="30vw"/><h3>山在这里，风也在这里。</h3></div></article></div><article className="recent-table"><div className="admin-card-head"><div><h2>最近内容</h2><p>民宿、农品与联系方式统一维护</p></div><button onClick={()=>setShowForm(true)}>添加内容</button></div><div className="table-row head"><span>内容名称</span><span>类型</span><span>状态</span><span>最近更新</span><span/></div>{['牛大爷的院子|民宿|待完善|刚刚','太行山核桃|农产品|展示中|昨天','访客服务电话|联系信息|待录入|2 天前'].map(row=>{const [a,b,c,d]=row.split('|');return <div className="table-row" key={a}><strong>{a}</strong><span>{b}</span><span className={c==='展示中'?'published':'draft'}>{c}</span><span>{d}</span><button>•••</button></div>})}</article></>}
      {tab!=='overview'&&<section className="manage-list"><div className="manage-toolbar"><div><button className="active">全部</button><button>已发布</button><button>草稿</button></div><button onClick={()=>setShowForm(true)}>＋ 新增{tab==='stay'?'民宿':tab==='goods'?'农产品':'联系人'}</button></div>{(tab==='stay'?stayCards:tab==='goods'?goodsCards:[{name:'村庄访客服务',type:'电话待录入',desc:'用于公开页顶部与底部联系咨询入口',price:'未发布'}]).map((item,index)=><article key={item.name}><span className="row-avatar">{tab==='stay'?'宿':tab==='goods'?'品':'联'}</span><div><h3>{item.name}</h3><p>{'type' in item?item.type:''} · {'desc' in item?item.desc:''}</p></div><span className={index===1?'published':'draft'}>{index===1?'展示中':'待完善'}</span><button>编辑</button><button>•••</button></article>)}</section>}
    </section>
    {showForm&&<div className="modal-backdrop"><form className="content-form" onSubmit={submit}><header><div><small>CONTENT PUBLISH</small><h2>上架新内容</h2></div><button type="button" onClick={()=>setShowForm(false)}>×</button></header><div className="type-options"><label><input type="radio" name="type" defaultChecked/> 民宿</label><label><input type="radio" name="type"/> 农产品</label><label><input type="radio" name="type"/> 联系方式</label></div><label>名称<input required placeholder="例如：南坡山居一号院"/></label><div className="form-grid"><label>价格 / 说明<input placeholder="例如：¥368 / 晚"/></label><label>联系电话<input placeholder="请填写真实联系电话"/></label></div><label>简介<textarea placeholder="用一两句话介绍特色、服务与注意事项"/></label><label className="upload-box"><input type="file" accept="image/*"/><span>＋ 上传封面图片</span><small>支持 JPG、PNG，建议横版图片</small></label><footer><button type="button" onClick={()=>setShowForm(false)}>取消</button><button type="submit">保存为草稿</button><button type="submit" className="primary">保存并发布</button></footer></form></div>}
    {toast&&<div className="toast">✓ {toast}</div>}
  </main>
}

export default function Home(){const [admin,setAdmin]=useState(false);return admin?<AdminConsole onExit={()=>setAdmin(false)}/>:<PublicWindow onManage={()=>setAdmin(true)}/>}
