import { FormEvent, useCallback, useEffect, useState } from 'react';
import { ApiError, openProtectedMedia } from '../../services/api';
import { logout } from '../../services/authApi';
import {
  AiCopy,
  confirmAiCopy,
  createFarmRecord,
  FarmRecord,
  FarmerDashboard,
  FarmerOrder,
  FarmerProduct,
  generateAiCopy,
  loadFarmerWorkspace,
  markFarmerOrderReady,
  submitFarmRecord,
  uploadRecordMedia,
} from '../../services/farmerApi';

const stageNames: Record<string, string> = {
  PREPARATION: '整地备耕', SOWING: '播种', FERTILIZING: '施肥', GROWING: '生长',
  HARVEST: '采收', PROCESSING: '加工', PACKING: '包装', SHIPPING: '发货',
};
const statusNames: Record<string, string> = {
  DRAFT: '草稿', PENDING_REVIEW: '待审核', PUBLISHED: '已公开', REJECTED: '已驳回',
};

export function FarmerPortalPage({ onExit }: { onExit: () => void }) {
  const [dashboard, setDashboard] = useState<FarmerDashboard | null>(null);
  const [products, setProducts] = useState<FarmerProduct[]>([]);
  const [records, setRecords] = useState<FarmRecord[]>([]);
  const [orders, setOrders] = useState<FarmerOrder[]>([]);
  const [aiCopies, setAiCopies] = useState<AiCopy[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');

  const notify = (message: string) => {
    setToast(message);
    window.setTimeout(() => setToast(''), 2400);
  };
  const reload = useCallback(() => {
    setError('');
    loadFarmerWorkspace()
      .then((data) => {
        setDashboard(data.dashboard);
        setProducts(data.products);
        setRecords(data.records);
        setOrders(data.orders);
        setAiCopies(data.aiCopies);
      })
      .catch((reason) => setError(reason instanceof ApiError ? reason.message : '农户数据加载失败'));
  }, []);
  useEffect(() => reload(), [reload]);

  const saveRecord = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setBusy(true);
    setError('');
    try {
      const product = products.find((item) => item.id === Number(form.get('productId')));
      const record = await createFarmRecord({
        productId: Number(form.get('productId')),
        plotId: product?.plotId,
        stage: String(form.get('stage')),
        occurredAt: String(form.get('occurredAt')),
        originalText: String(form.get('originalText')),
        truthConfirmed: form.get('truthConfirmed') === 'on',
      });
      const media = form.get('media');
      if (media instanceof File && media.size > 0) {
        const uploaded = await uploadRecordMedia(record.id, media);
        if (uploaded.status !== 'READY') throw new Error(uploaded.failureReason || '媒体校验失败');
      }
      const intent = (event.nativeEvent as SubmitEvent).submitter?.getAttribute('value');
      if (intent === 'submit') {
        await submitFarmRecord(record.id);
        notify('生产记录已提交，等待村级审核');
      } else {
        notify('生产记录已保存为草稿');
      }
      setShowForm(false);
      reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '生产记录保存失败');
    } finally {
      setBusy(false);
    }
  };

  const submitDraft = async (recordId: number) => {
    setBusy(true);
    try {
      await submitFarmRecord(recordId);
      notify('已提交审核');
      reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '提交失败');
    } finally {
      setBusy(false);
    }
  };

  const prepareOrder = async (orderId: number) => {
    setBusy(true);
    try {
      await markFarmerOrderReady(orderId);
      notify('已确认备货完成，等待运营人员发货');
      reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '备货状态更新失败');
    } finally {
      setBusy(false);
    }
  };

  const generateCopy = async (productId: number) => {
    setBusy(true);
    setError('');
    try {
      await generateAiCopy(productId);
      notify('已依据审核记录生成草稿，请人工确认');
      reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '文案生成失败');
    } finally {
      setBusy(false);
    }
  };

  const confirmCopy = async (event: FormEvent<HTMLFormElement>, copyId: number) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const form = new FormData(event.currentTarget);
      await confirmAiCopy(copyId, String(form.get('confirmedText')));
      notify('文案已由农户确认并留痕');
      reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '文案确认失败');
    } finally {
      setBusy(false);
    }
  };

  if (!dashboard) {
    return <main className="app-state">{error ? <><h1>无法打开农户经营台</h1><p>{error}</p><button onClick={reload}>重新加载</button></> : <><span className="state-spinner"/><h1>正在读取农户档案…</h1></>}</main>;
  }

  return <main className="farmer-portal">
    <header><button onClick={onExit}>← 返回南坡之窗</button><div className="farmer-logo"><span>南</span><div><b>农户经营台</b><small>{dashboard.farmer.name} · {dashboard.farmer.villageGroup}</small></div></div><button className="farmer-help" onClick={async()=>{await logout();onExit()}}>退出登录</button></header>
    <section className="farmer-main">
      <div className="farmer-welcome"><div><small>{dashboard.farmer.certificationStatus === 'APPROVED' ? '身份已认证' : '身份待认证'}</small><h1>今天地里有什么新变化？</h1><p>记录真实生产过程，审核通过后才会对游客公开。</p></div><button onClick={()=>setShowForm(true)} disabled={!products.length}><span>＋</span><b>添加生产记录</b><small>{products.length ? '可保存草稿或直接提交审核' : '请先创建农品'}</small></button></div>
      <div className="farmer-summary"><article><span>田</span><div><small>我的地块</small><strong>{dashboard.plotCount} 块</strong></div><i>归属已校验</i></article><article><span>记</span><div><small>生产记录</small><strong>{dashboard.recordCount} 条</strong></div><i>{dashboard.publishedRecordCount} 条已公开</i></article><article><span>审</span><div><small>等待审核</small><strong>{dashboard.pendingReviewCount} 条</strong></div><i>{dashboard.productCount} 件农品</i></article></div>
      {error&&<div className="login-error" role="alert">{error}</div>}
      <section className="farmer-content-grid"><article className="farm-diary"><header><div><h2>真实生产记录</h2><p>按时间排列，状态由后端状态机管理</p></div><button onClick={()=>setShowForm(true)} disabled={!products.length}>＋ 添加记录</button></header><div className="diary-list">
        {records.length ? records.map((record)=><div key={record.id}><span className="diary-date">{new Date(record.occurredAt).toLocaleDateString('zh-CN',{month:'2-digit',day:'2-digit'})}</span><div className="diary-photo empty">{stageNames[record.stage]?.slice(0,1) || '记'}</div><div><small>{stageNames[record.stage] || record.stage} · {statusNames[record.status] || record.status}</small><h3>{record.productName}</h3><p>{record.confirmedText || record.originalText}</p><div><b>真实性已确认</b>{record.media.map((media)=><button key={media.id} disabled={!media.contentUrl} onClick={()=>media.contentUrl&&openProtectedMedia(media.contentUrl)}>{media.originalName} · {media.status}</button>)}{record.reviewNote&&<b>审核意见：{record.reviewNote}</b>}{['DRAFT','REJECTED'].includes(record.status)&&<button disabled={busy} onClick={()=>submitDraft(record.id)}>提交审核</button>}</div></div></div>) : <div className="draft-entry"><span className="diary-date">今天</span><div className="diary-photo empty">＋</div><div><small>暂无记录</small><h3>从第一条真实生产记录开始</h3></div></div>}
      </div></article><aside className="share-builder"><span>数据边界</span><h2>只能管理<br/>属于自己的数据。</h2><p>地块、农品和生产记录在每次读写时都会校验农户归属，未审核记录不会出现在公开页。</p><div className="share-score"><div><span>认证状态</span><b>{dashboard.farmer.certificationStatus}</b></div><div><span>已公开记录</span><b>{dashboard.publishedRecordCount}</b></div></div></aside></section>
      <article className="farm-diary"><header><div><h2>待备货订单</h2><p>只展示包含本人农品的已付款订单</p></div><button onClick={reload}>刷新</button></header><div className="diary-list">{orders.length ? orders.map((order)=><div key={order.id}><span className="diary-date">{order.orderNo.slice(-4)}</span><div className="diary-photo empty">单</div><div><small>{order.status}</small><h3>{order.items.map((item)=>`${item.productName} × ${item.quantity}`).join('、')}</h3><p>订单 {order.orderNo} · {new Date(order.createdAt).toLocaleString('zh-CN')}</p><div>{order.status==='PAID'&&<button disabled={busy} onClick={()=>prepareOrder(order.id)}>确认备货完成</button>}{order.status==='READY_TO_SHIP'&&<b>等待统一发货</b>}{order.status==='SHIPPED'&&<b>已发货</b>}</div></div></div>) : <div className="draft-entry"><span className="diary-date">单</span><div className="diary-photo empty">✓</div><div><small>暂无待处理订单</small><h3>已付款订单会出现在这里</h3></div></div>}</div></article>
      <article className="farm-diary"><header><div><h2>有来源的文案助手</h2><p>只读取已审核生产记录；生成结果必须人工确认，不会自动发布</p></div></header><div className="diary-list">{products.map((product)=><div key={`ai-product-${product.id}`}><span className="diary-date">AI</span><div className="diary-photo empty">文</div><div><small>{product.name}</small><h3>根据真实记录整理产品介绍</h3><p>系统会保存引用的生产记录编号、生成器版本、原始输出和人工确认稿。</p><div><button disabled={busy} onClick={()=>generateCopy(product.id)}>生成事实草稿</button></div></div></div>)}{aiCopies.map((copy)=><div key={`ai-copy-${copy.id}`}><span className="diary-date">#{copy.id}</span><div className="diary-photo empty">稿</div><div><small>{copy.status} · 来源记录 {copy.sourceRecordIds.join('、')} · {copy.modelName}</small><h3>{copy.scene}</h3>{copy.status==='DRAFT'?<form onSubmit={(event)=>confirmCopy(event,copy.id)}><textarea name="confirmedText" required maxLength={5000} defaultValue={copy.outputText}/><button disabled={busy} type="submit">人工确认并留痕</button></form>:<p>{copy.confirmedText}</p>}</div></div>)}</div></article>
    </section>
    {showForm&&<div className="modal-backdrop"><form className="farm-upload" onSubmit={saveRecord}><header><div><small>NEW FARM RECORD</small><h2>记录今天的生产过程</h2></div><button type="button" onClick={()=>setShowForm(false)}>×</button></header><label>选择农品<select name="productId" required>{products.map((product)=><option key={product.id} value={product.id}>{product.name}</option>)}</select></label><label>生产阶段<select name="stage" defaultValue="HARVEST">{Object.entries(stageNames).map(([value,label])=><option value={value} key={value}>{label}</option>)}</select></label><label>发生时间<input name="occurredAt" type="datetime-local" required defaultValue={new Date(Date.now()-new Date().getTimezoneOffset()*60000).toISOString().slice(0,16)}/></label><label>现场情况<textarea name="originalText" required maxLength={5000} placeholder="用自己的话说明今天做了什么…"/></label><label>现场素材（可选）<input name="media" type="file" accept="image/jpeg,image/png,image/webp,audio/mpeg,audio/wav,audio/mp4,video/mp4,video/webm"/><small>图片 ≤ 10MB，音频 ≤ 30MB，视频 ≤ 100MB；上传后校验类型、大小和 SHA-256。</small></label><label className="truth-check"><input name="truthConfirmed" type="checkbox" required/> 我确认以上记录来自本人真实生产过程。</label><footer><button type="button" onClick={()=>setShowForm(false)}>取消</button><button name="intent" value="draft" type="submit" disabled={busy}>保存草稿</button><button name="intent" value="submit" type="submit" disabled={busy}>{busy?'正在保存…':'保存并提交审核 →'}</button></footer></form></div>}
    {toast&&<div className="toast">✓ {toast}</div>}
  </main>;
}
