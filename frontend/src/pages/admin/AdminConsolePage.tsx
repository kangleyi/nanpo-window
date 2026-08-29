import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  approveFarmRecord,
  confirmOrderPayment,
  ContentCommand,
  ContentKind,
  createManagedContent,
  loadAdminOrders,
  loadFarmReviewQueue,
  loadManagedContent,
  ManagedContent,
  rejectFarmRecord,
  rejectOrderPayment,
  setManagedContentPublished,
  shipAdminOrder,
  updateManagedContent,
} from '../../services/adminApi';
import { ApiError, openProtectedMedia } from '../../services/api';
import { logout } from '../../services/authApi';
import { FarmRecord } from '../../services/farmerApi';
import { Order } from '../../services/orderApi';

type AdminSection = 'orders' | 'reviews' | ContentKind;

export function AdminConsolePage({ onExit }: { onExit: () => void }) {
  const [section, setSection] = useState<AdminSection>('reviews');
  const [items, setItems] = useState<ManagedContent[]>([]);
  const [reviews, setReviews] = useState<FarmRecord[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [editing, setEditing] = useState<ManagedContent | null>(null);
  const [shippingOrder, setShippingOrder] = useState<Order | null>(null);
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
    if (section === 'reviews') {
      loadFarmReviewQueue().then(setReviews)
        .catch((reason) => setError(reason instanceof ApiError ? reason.message : '审核队列加载失败'));
    } else if (section === 'orders') {
      loadAdminOrders().then(setOrders)
        .catch((reason) => setError(reason instanceof ApiError ? reason.message : '订单加载失败'));
    } else {
      loadManagedContent(section).then((page) => setItems(page.items))
        .catch((reason) => setError(reason instanceof ApiError ? reason.message : '内容加载失败'));
    }
  }, [section]);
  useEffect(() => reload(), [reload]);

  const saveContent = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (section === 'reviews' || section === 'orders') return;
    const form = new FormData(event.currentTarget);
    const command: ContentCommand = {
      name: String(form.get('name')),
      type: String(form.get('type')),
      summary: String(form.get('summary')),
      price: String(form.get('price')),
      coverUrl: String(form.get('coverUrl')),
      sortOrder: Number(form.get('sortOrder') || 0),
      ...(section === 'homestays' ? {
        capacity: String(form.get('capacity')),
        consultationPhone: String(form.get('consultationPhone') || ''),
      } : {
        season: String(form.get('season')),
        duration: String(form.get('duration')),
        videoUrl: String(form.get('videoUrl') || ''),
        bookingNotes: String(form.get('bookingNotes') || ''),
      }),
    };
    setBusy(true);
    try {
      if (editing) {
        await updateManagedContent(section, editing.id, command);
        notify('内容已更新');
      } else {
        await createManagedContent(section, command);
        notify('内容已保存为草稿');
      }
      setEditing(null);
      setShowForm(false);
      reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '内容保存失败');
    } finally {
      setBusy(false);
    }
  };

  const togglePublished = async (item: ManagedContent) => {
    if (section === 'reviews' || section === 'orders') return;
    setBusy(true);
    try {
      await setManagedContentPublished(section, item.id, item.status !== 'PUBLISHED');
      notify(item.status === 'PUBLISHED' ? '内容已下线' : '内容已发布，公开页已生效');
      reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '状态更新失败');
    } finally {
      setBusy(false);
    }
  };

  const review = async (record: FarmRecord, approved: boolean) => {
    setBusy(true);
    try {
      if (approved) {
        await approveFarmRecord(record.id, record.originalText);
        notify('记录已审核发布');
      } else {
        await rejectFarmRecord(record.id, '记录信息不完整，请补充后重新提交。');
        notify('记录已驳回农户补充');
      }
      reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '审核操作失败');
    } finally {
      setBusy(false);
    }
  };

  const updateOrder = async (order: Order, action: 'confirm' | 'reject') => {
    setBusy(true);
    try {
      if (action === 'confirm') {
        await confirmOrderPayment(order.id);
        notify('收款已确认，订单进入备货');
      } else {
        await rejectOrderPayment(order.id, '未核实到该笔付款，请顾客核对付款信息。');
        notify('付款申报已驳回');
      }
      reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '订单状态更新失败');
    } finally {
      setBusy(false);
    }
  };

  const shipOrder = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!shippingOrder) return;
    const form = new FormData(event.currentTarget);
    setBusy(true);
    try {
      await shipAdminOrder(shippingOrder.id, String(form.get('shippingCompany')), String(form.get('trackingNo')));
      setShippingOrder(null);
      notify('订单已发货');
      reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '发货失败');
    } finally {
      setBusy(false);
    }
  };

  return <main className="admin-shell">
    <aside className="admin-sidebar"><div className="brand admin-brand"><span className="brand-seal">南</span><span><b>南坡之窗</b><small>村庄运营中心</small></span></div><nav><button className={section==='orders'?'active':''} onClick={()=>setSection('orders')}>单 <span>订单履约</span></button><button className={section==='reviews'?'active':''} onClick={()=>setSection('reviews')}>田 <span>生产记录审核</span>{reviews.length>0&&<i className="alert-badge">{reviews.length}</i>}</button><button className={section==='homestays'?'active':''} onClick={()=>setSection('homestays')}>宿 <span>民宿管理</span></button><button className={section==='experiences'?'active':''} onClick={()=>setSection('experiences')}>游 <span>游玩采摘</span></button></nav><div className="admin-bottom"><button onClick={onExit}>← 返回公开页</button><div><span>管</span><p><b>村庄管理员</b><small><button onClick={async()=>{await logout();onExit()}}>退出登录</button></small></p></div></div></aside>
    <section className="admin-main"><header><div><small>南坡之窗 / 村庄运营中心</small><h1>{section==='orders'?'订单履约':section==='reviews'?'生产记录审核':section==='homestays'?'民宿管理':'游玩采摘管理'}</h1></div><div><button onClick={onExit}>↗ 预览公开页</button>{section!=='reviews'&&section!=='orders'&&<button className="primary" onClick={()=>{setEditing(null);setShowForm(true)}}>＋ 新增内容</button>}</div></header>
      {error&&<div className="login-error" role="alert">{error} <button onClick={reload}>重试</button></div>}
      {section==='orders' ? <section className="manage-list"><div className="manage-toolbar"><div><button className="active">全部订单 {orders.length}</button></div><button onClick={reload}>刷新订单</button></div>{orders.length ? orders.map((order)=><article key={order.id}><span className="row-avatar">单</span><div><h3>{order.orderNo} · ¥{order.totalAmount.toFixed(2)}</h3><p>{order.items.map((item)=>`${item.productName} ${item.specification} ×${item.quantity}`).join('；')}</p><small className="record-count">{order.recipientName} · {order.recipientPhone} · {order.recipientAddress}{order.trackingNo&&` · ${order.shippingCompany} ${order.trackingNo}`}</small></div><span className={order.status==='COMPLETED'?'published':'draft'}>{order.status}</span>{order.status==='PAYMENT_REPORTED'&&<><button disabled={busy} onClick={()=>updateOrder(order,'reject')}>驳回付款</button><button disabled={busy} onClick={()=>updateOrder(order,'confirm')}>确认收款</button></>}{order.status==='READY_TO_SHIP'&&<button disabled={busy} onClick={()=>setShippingOrder(order)}>填写发货</button>}</article>) : <div className="section-empty"><span>单</span><h3>暂无订单</h3></div>}</section>
      : section==='reviews' ? <section className="manage-list"><div className="manage-toolbar"><div><button className="active">待审核 {reviews.length}</button></div><button onClick={reload}>刷新队列</button></div>{reviews.length ? reviews.map((record)=><article key={record.id}><span className="row-avatar">记</span><div><h3>{record.productName} · {record.stage}</h3><p>{record.originalText}</p><small className="record-count">农户确认真实 · {new Date(record.occurredAt).toLocaleString('zh-CN')}</small>{record.media.map((media)=><button key={media.id} disabled={!media.contentUrl} onClick={()=>media.contentUrl&&openProtectedMedia(media.contentUrl)}>查看素材：{media.originalName} · {media.status}</button>)}</div><span className="draft">待审核</span><button disabled={busy} onClick={()=>review(record,false)}>驳回</button><button disabled={busy} onClick={()=>review(record,true)}>审核发布</button></article>) : <div className="section-empty"><span>✓</span><h3>暂无待审生产记录</h3></div>}</section>
      : <section className="manage-list"><div className="manage-toolbar"><div><button className="active">全部 {items.length}</button></div><button onClick={()=>{setEditing(null);setShowForm(true)}}>＋ 新增{section==='homestays'?'民宿':'游玩项目'}</button></div>{items.map((item)=><article key={item.id}><span className="row-avatar">{section==='homestays'?'宿':'游'}</span><div><h3>{item.name}</h3><p>{item.type} · {item.summary}</p><small className="record-count">{item.price}</small></div><span className={item.status==='PUBLISHED'?'published':'draft'}>{item.status==='PUBLISHED'?'展示中':'草稿'}</span><button onClick={()=>{setEditing(item);setShowForm(true)}}>编辑</button><button disabled={busy} onClick={()=>togglePublished(item)}>{item.status==='PUBLISHED'?'下线':'发布'}</button></article>)}</section>}
    </section>
    {showForm&&section!=='reviews'&&section!=='orders'&&<div className="modal-backdrop"><form className="content-form" onSubmit={saveContent}><header><div><small>CONTENT MANAGEMENT</small><h2>{editing?'编辑':'新增'}{section==='homestays'?'民宿':'游玩项目'}</h2></div><button type="button" onClick={()=>setShowForm(false)}>×</button></header><label>名称<input name="name" required maxLength={160} defaultValue={editing?.name}/></label><div className="form-grid"><label>类型<input name="type" required defaultValue={editing?.type}/></label><label>价格说明<input name="price" required defaultValue={editing?.price}/></label></div>{section==='homestays'?<div className="form-grid"><label>容纳人数<input name="capacity" required defaultValue={editing?.capacity}/></label><label>咨询电话<input name="consultationPhone" defaultValue={editing?.consultationPhone}/></label></div>:<><div className="form-grid"><label>开放季节<input name="season" required defaultValue={editing?.season}/></label><label>时长<input name="duration" required defaultValue={editing?.duration}/></label></div><label>视频地址<input name="videoUrl" defaultValue={editing?.videoUrl}/></label><label>预约说明<textarea name="bookingNotes" defaultValue={editing?.bookingNotes}/></label></>}<label>简介<textarea name="summary" required maxLength={2000} defaultValue={editing?.summary}/></label><label>封面地址<input name="coverUrl" required defaultValue={editing?.coverUrl || (section==='homestays'?'/images/homestay.jpg':'/images/walnut-yard.jpg')}/></label><label>排序<input name="sortOrder" type="number" min="0" defaultValue={editing?.sortOrder || 0}/></label><footer><button type="button" onClick={()=>setShowForm(false)}>取消</button><button className="primary" type="submit" disabled={busy}>{busy?'正在保存…':'保存为草稿'}</button></footer></form></div>}
    {shippingOrder&&<div className="modal-backdrop"><form className="content-form" onSubmit={shipOrder}><header><div><small>ORDER FULFILMENT</small><h2>填写发货信息</h2></div><button type="button" onClick={()=>setShippingOrder(null)}>×</button></header><p>{shippingOrder.orderNo} · {shippingOrder.recipientName}</p><label>物流公司<input name="shippingCompany" required maxLength={80} placeholder="例如：邮政快递"/></label><label>物流单号<input name="trackingNo" required maxLength={120}/></label><footer><button type="button" onClick={()=>setShippingOrder(null)}>取消</button><button className="primary" type="submit" disabled={busy}>{busy?'正在提交…':'确认发货'}</button></footer></form></div>}
    {toast&&<div className="toast">✓ {toast}</div>}
  </main>;
}
