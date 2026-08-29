import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  approveFarmRecord,
  confirmOrderPayment,
  ConsultationInquiry,
  ContentCommand,
  ContentKind,
  GoodsSectionCommand,
  GoodsSectionSettings,
  createFarmerRecord,
  createFarmerProduct,
  createManagedContent,
  loadAdminOrders,
  loadConsultationInquiries,
  loadFarmerProducts,
  loadFarmers,
  loadFarmReviewQueue,
  loadGoodsSectionSettings,
  loadManagedContent,
  ManagedContent,
  markAdminOrderReady,
  ProductCommand,
  rejectFarmRecord,
  rejectOrderPayment,
  setFarmerProductPublished,
  setManagedContentPublished,
  shipAdminOrder,
  submitFarmerRecord,
  updateFarmerProduct,
  updateConsultationInquiryStatus,
  updateGoodsSectionSettings,
  updateManagedContent,
} from '../../services/adminApi';
import { MediaUploadField } from '../../components/MediaUploadField';
import { ProductImageUploadField } from '../../components/ProductImageUploadField';
import { ApiError, openProtectedMedia } from '../../services/api';
import { logout } from '../../services/authApi';
import { FarmerProduct, FarmerProfile, FarmRecord, uploadRecordMedia } from '../../services/farmerApi';
import { Order } from '../../services/orderApi';

type AdminSection = 'orders' | 'products' | 'reviews' | 'inquiries' | 'goodsSection' | ContentKind;
type SkuDraft = { key: string; id?: number; code?: string; specification: string; unitPrice: string; stockNote: string };

const emptySkuDraft = (): SkuDraft => ({
  key: crypto.randomUUID(), specification: '', unitPrice: '', stockNote: '',
});

const orderStatusNames: Record<string, string> = {
  ALL: '全部状态', CREATED: '待付款', PAYMENT_REPORTED: '待核款', PAID: '待备货',
  READY_TO_SHIP: '待发货', SHIPPED: '已发货', COMPLETED: '已完成',
  CANCELLED: '已取消', REFUNDED: '已退款',
};

const orderStatuses = Object.keys(orderStatusNames);

export function AdminConsolePage({ onExit }: { onExit: () => void }) {
  const [section, setSection] = useState<AdminSection>('orders');
  const [items, setItems] = useState<ManagedContent[]>([]);
  const [reviews, setReviews] = useState<FarmRecord[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [inquiries, setInquiries] = useState<ConsultationInquiry[]>([]);
  const [farmers, setFarmers] = useState<FarmerProfile[]>([]);
  const [products, setProducts] = useState<FarmerProduct[]>([]);
  const [goodsSection, setGoodsSection] = useState<GoodsSectionSettings | null>(null);
  const [selectedFarmerId, setSelectedFarmerId] = useState(0);
  const [orderStatus, setOrderStatus] = useState('ALL');
  const [inquiryStatus, setInquiryStatus] = useState('ALL');
  const [inquiryType, setInquiryType] = useState('ALL');
  const [editing, setEditing] = useState<ManagedContent | null>(null);
  const [editingProduct, setEditingProduct] = useState<FarmerProduct | null>(null);
  const [recordingProduct, setRecordingProduct] = useState<FarmerProduct | null>(null);
  const [recordMediaFile, setRecordMediaFile] = useState<File | null>(null);
  const [recordError, setRecordError] = useState('');
  const [shippingOrder, setShippingOrder] = useState<Order | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [showProductForm, setShowProductForm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [uploadsInProgress, setUploadsInProgress] = useState(0);
  const [skuDrafts, setSkuDrafts] = useState<SkuDraft[]>([emptySkuDraft()]);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');

  const notify = (message: string) => {
    setToast(message);
    window.setTimeout(() => setToast(''), 2400);
  };

  useEffect(() => {
    loadFarmers().then((data) => {
      setFarmers(data);
      setSelectedFarmerId((current) => current || data[0]?.id || 0);
    }).catch((reason) => setError(reason instanceof ApiError ? reason.message : '村民列表加载失败'));
  }, []);

  const reload = useCallback(() => {
    setError('');
    if (section === 'inquiries') {
      loadConsultationInquiries(inquiryStatus, inquiryType).then(setInquiries)
        .catch((reason) => setError(reason instanceof ApiError ? reason.message : '咨询留言加载失败'));
    } else if (section === 'reviews') {
      loadFarmReviewQueue().then(setReviews)
        .catch((reason) => setError(reason instanceof ApiError ? reason.message : '审核队列加载失败'));
    } else if (section === 'orders') {
      loadAdminOrders(orderStatus, selectedFarmerId || undefined).then(setOrders)
        .catch((reason) => setError(reason instanceof ApiError ? reason.message : '订单加载失败'));
    } else if (section === 'products') {
      if (!selectedFarmerId) {
        setProducts([]);
        return;
      }
      loadFarmerProducts(selectedFarmerId)
        .then(setProducts)
        .catch((reason) => setError(reason instanceof ApiError ? reason.message : '农产品加载失败'));
    } else if (section === 'goodsSection') {
      loadGoodsSectionSettings().then(setGoodsSection)
        .catch((reason) => setError(reason instanceof ApiError ? reason.message : '好物版块配置加载失败'));
    } else {
      loadManagedContent(section).then((page) => setItems(page.items))
        .catch((reason) => setError(reason instanceof ApiError ? reason.message : '内容加载失败'));
    }
  }, [inquiryStatus, inquiryType, orderStatus, section, selectedFarmerId]);

  useEffect(() => reload(), [reload]);

  useEffect(() => {
    if (!showProductForm) return;
    setSkuDrafts(editingProduct?.skus.filter((sku) => sku.enabled).map((sku) => ({
      key: crypto.randomUUID(), id: sku.id, code: sku.code, specification: sku.specification,
      unitPrice: String(sku.unitPrice), stockNote: sku.stockNote || '',
    })) || [emptySkuDraft()]);
  }, [editingProduct, showProductForm]);

  const selectedFarmer = useMemo(
    () => farmers.find((farmer) => farmer.id === selectedFarmerId),
    [farmers, selectedFarmerId],
  );

  const trackUpload = useCallback((active: boolean) => {
    setUploadsInProgress((current) => Math.max(0, current + (active ? 1 : -1)));
  }, []);

  const saveContent = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (section === 'reviews' || section === 'orders' || section === 'products' || section === 'inquiries' || section === 'goodsSection') return;
    const form = new FormData(event.currentTarget);
    const coverUrl = String(form.get('coverUrl') || '');
    if (!coverUrl) {
      setError('请先上传封面图片');
      return;
    }
    const command: ContentCommand = {
      name: String(form.get('name')), type: String(form.get('type')),
      summary: String(form.get('summary')), price: String(form.get('price')),
      coverUrl, sortOrder: Number(form.get('sortOrder') || 0),
      ...(section === 'homestays' ? {
        capacity: String(form.get('capacity')), consultationPhone: String(form.get('consultationPhone') || ''),
        externalUrl: String(form.get('externalUrl') || ''),
      } : {
        season: String(form.get('season')), duration: String(form.get('duration')),
        videoUrl: String(form.get('videoUrl') || ''), bookingNotes: String(form.get('bookingNotes') || ''),
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
      setEditing(null); setShowForm(false); reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '内容保存失败');
    } finally { setBusy(false); }
  };

  const saveGoodsSection = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const imageUrl = String(form.get('imageUrl') || '');
    if (!imageUrl) { setError('请先上传版块展示图片'); return; }
    const command: GoodsSectionCommand = {
      eyebrow: String(form.get('eyebrow') || '').trim(),
      title: String(form.get('title') || '').trim(),
      description: String(form.get('description') || '').trim(),
      seasonLabel: String(form.get('seasonLabel') || '').trim(),
      seasonNote: String(form.get('seasonNote') || '').trim(),
      imageUrl,
      imageCaption: String(form.get('imageCaption') || '').trim(),
    };
    setBusy(true); setError('');
    try {
      const updated = await updateGoodsSectionSettings(command);
      setGoodsSection(updated); notify('好物版块已保存并发布');
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '好物版块保存失败');
    } finally { setBusy(false); }
  };

  const saveProduct = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedFarmerId) return;
    const form = new FormData(event.currentTarget);
    try {
      const skus = skuDrafts.map((draft, index) => {
        const specification = draft.specification.trim();
        const unitPrice = Number(draft.unitPrice);
        const stockNote = draft.stockNote.trim();
        if (!specification || !Number.isFinite(unitPrice) || unitPrice <= 0) {
          throw new Error(`第 ${index + 1} 条规格未填写完整，售价必须大于 0`);
        }
        return { id: draft.id, specification, unitPrice, stockNote };
      });
      if (!skus.length) throw new Error('至少填写一个可售规格');
      const imageUrls = form.getAll('imageUrls').map(String).filter(Boolean);
      if (!imageUrls.length) throw new Error('请至少上传一张农产品图片');
      const command: ProductCommand = {
        name: String(form.get('name')), category: String(form.get('category')),
        season: String(form.get('season')), summary: String(form.get('summary')),
        coverUrl: imageUrls[0], imageUrls, skus,
      };
      setBusy(true);
      if (editingProduct) {
        await updateFarmerProduct(selectedFarmerId, editingProduct.id, command);
        notify('农产品信息已更新');
      } else {
        await createFarmerProduct(selectedFarmerId, command);
        notify('农产品已保存为草稿');
      }
      setEditingProduct(null); setShowProductForm(false); reload();
    } catch (reason) {
      setError(reason instanceof ApiError || reason instanceof Error ? reason.message : '农产品保存失败');
    } finally { setBusy(false); }
  };

  const saveFarmRecord = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedFarmerId || !recordingProduct) return;
    const form = new FormData(event.currentTarget);
    const stage = String(form.get('stage') || '');
    const occurredAt = String(form.get('occurredAt') || '');
    const originalText = String(form.get('originalText') || '').trim();
    const truthConfirmed = form.get('truthConfirmed') === 'on';
    setRecordError('');
    if (!stage) { setRecordError('请选择生产阶段'); return; }
    if (!occurredAt) { setRecordError('请选择生产过程的发生时间'); return; }
    const occurredTime = new Date(occurredAt).getTime();
    if (!Number.isFinite(occurredTime)) { setRecordError('发生时间格式不正确，请重新选择'); return; }
    if (occurredTime > Date.now() + 5 * 60_000) { setRecordError('发生时间不能晚于当前时间'); return; }
    if (!originalText) { setRecordError('请填写真实情况说明'); return; }
    if (!truthConfirmed) { setRecordError('请先确认该记录来自真实生产过程'); return; }
    if (recordMediaFile) {
      const limits: Record<string, number> = {
        'image/jpeg': 10 * 1024 * 1024, 'image/png': 10 * 1024 * 1024, 'image/webp': 10 * 1024 * 1024,
        'audio/mpeg': 30 * 1024 * 1024, 'audio/wav': 30 * 1024 * 1024, 'audio/mp4': 30 * 1024 * 1024,
        'video/mp4': 100 * 1024 * 1024, 'video/webm': 100 * 1024 * 1024,
      };
      if (!limits[recordMediaFile.type]) { setRecordError('素材格式不支持，请上传 JPG、PNG、WebP、MP3、WAV、M4A、MP4 或 WebM 文件'); return; }
      if (recordMediaFile.size > limits[recordMediaFile.type]) {
        const maximum = recordMediaFile.type.startsWith('image/') ? '10MB' : recordMediaFile.type.startsWith('audio/') ? '30MB' : '100MB';
        setRecordError(`所选素材超过 ${maximum} 大小限制`); return;
      }
    }
    setBusy(true); setError('');
    let recordSaved = false;
    let step: 'save' | 'upload' | 'submit' = 'save';
    try {
      const record = await createFarmerRecord(selectedFarmerId, {
        productId: recordingProduct.id,
        stage, occurredAt, originalText, truthConfirmed,
      });
      recordSaved = true;
      if (recordMediaFile) {
        step = 'upload';
        const uploaded = await uploadRecordMedia(record.id, recordMediaFile);
        if (uploaded.status !== 'READY') throw new Error(uploaded.failureReason || '素材校验失败');
      }
      step = 'submit';
      await submitFarmerRecord(selectedFarmerId, record.id);
      setRecordingProduct(null); setRecordMediaFile(null); setRecordError(''); setSection('reviews');
      notify('生产过程已提交审核，可在审核队列中确认发布');
    } catch (reason) {
      const rawMessage = reason instanceof ApiError || reason instanceof Error ? reason.message : '服务暂时不可用，请稍后重试';
      const message = rawMessage
        .replace(/^occurredAt:\s*/, '发生时间：')
        .replace(/^originalText:\s*/, '真实情况说明：')
        .replace(/^truthConfirmed:\s*/, '真实性确认：')
        .replace(/^stage:\s*/, '生产阶段：');
      const prefix = !recordSaved || step === 'save' ? '生产记录保存失败'
        : step === 'upload' ? '记录草稿已保存，但素材上传失败'
          : '记录草稿已保存，但提交审核失败';
      setRecordError(`${prefix}：${message}`);
    } finally { setBusy(false); }
  };

  const toggleContentPublished = async (item: ManagedContent) => {
    if (section === 'reviews' || section === 'orders' || section === 'products' || section === 'inquiries' || section === 'goodsSection') return;
    setBusy(true);
    try {
      await setManagedContentPublished(section, item.id, item.status !== 'PUBLISHED');
      notify(item.status === 'PUBLISHED' ? '内容已下线' : '内容已发布'); reload();
    } catch (reason) { setError(reason instanceof ApiError ? reason.message : '状态更新失败'); }
    finally { setBusy(false); }
  };

  const toggleProductPublished = async (product: FarmerProduct) => {
    if (!selectedFarmerId) return;
    setBusy(true);
    try {
      await setFarmerProductPublished(selectedFarmerId, product.id, product.status !== 'PUBLISHED');
      notify(product.status === 'PUBLISHED' ? '农产品已下架' : '农产品已上架到公开商城'); reload();
    } catch (reason) { setError(reason instanceof ApiError ? reason.message : '上下架失败'); }
    finally { setBusy(false); }
  };

  const review = async (record: FarmRecord, approved: boolean) => {
    setBusy(true);
    try {
      if (approved) { await approveFarmRecord(record.id, record.originalText); notify('记录已审核发布'); }
      else { await rejectFarmRecord(record.id, '记录信息不完整，请补充后重新提交。'); notify('记录已驳回'); }
      reload();
    } catch (reason) { setError(reason instanceof ApiError ? reason.message : '审核操作失败'); }
    finally { setBusy(false); }
  };

  const updateOrder = async (order: Order, action: 'confirm' | 'reject' | 'prepare') => {
    setBusy(true);
    try {
      if (action === 'confirm') { await confirmOrderPayment(order.id); notify('收款已确认'); }
      else if (action === 'reject') { await rejectOrderPayment(order.id, '未核实到该笔付款，请顾客核对付款信息。'); notify('付款申报已驳回'); }
      else { await markAdminOrderReady(order.id); notify('已确认备货完成'); }
      reload();
    } catch (reason) { setError(reason instanceof ApiError ? reason.message : '订单状态更新失败'); }
    finally { setBusy(false); }
  };

  const updateInquiry = async (inquiry: ConsultationInquiry, action: 'contacted' | 'closed') => {
    setBusy(true);
    try {
      await updateConsultationInquiryStatus(inquiry.id, action);
      notify(action === 'contacted' ? '已标记为已联系' : '咨询已关闭');
      reload();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '咨询状态更新失败');
    } finally { setBusy(false); }
  };

  const shipOrder = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!shippingOrder) return;
    const form = new FormData(event.currentTarget);
    setBusy(true);
    try {
      await shipAdminOrder(shippingOrder.id, String(form.get('shippingCompany')), String(form.get('trackingNo')));
      setShippingOrder(null); notify('订单已发货'); reload();
    } catch (reason) { setError(reason instanceof ApiError ? reason.message : '发货失败'); }
    finally { setBusy(false); }
  };

  const createShareImage = async () => {
    if (!orders.length) { notify('当前筛选结果没有订单'); return; }
    try {
      await document.fonts.ready;
      const visibleOrders = orders.slice(0, 10);
      const canvas = document.createElement('canvas');
      canvas.width = 1080;
      canvas.height = 460 + visibleOrders.length * 92;
      const context = canvas.getContext('2d');
      if (!context) throw new Error('浏览器不支持图片生成');
      context.fillStyle = '#f3efe3'; context.fillRect(0, 0, canvas.width, canvas.height);
      context.fillStyle = '#2f4938'; context.fillRect(0, 0, canvas.width, 260);
      context.fillStyle = '#bd7650'; context.fillRect(70, 58, 58, 58);
      context.fillStyle = '#fff'; context.font = 'bold 34px "PingFang SC", sans-serif'; context.fillText('南', 82, 99);
      context.font = 'bold 52px "PingFang SC", sans-serif'; context.fillText('村民订单清单', 155, 104);
      context.fillStyle = '#d9e2db'; context.font = '26px "PingFang SC", sans-serif';
      context.fillText(`${selectedFarmer?.name || '全部村民'} · ${orderStatusNames[orderStatus]} · 共 ${orders.length} 单`, 72, 178);
      context.font = '20px "PingFang SC", sans-serif';
      context.fillText(`生成时间 ${new Date().toLocaleString('zh-CN')}  ·  不含客户手机号与地址`, 72, 222);
      visibleOrders.forEach((order, index) => {
        const y = 320 + index * 92;
        context.strokeStyle = '#d9d4c7'; context.beginPath(); context.moveTo(72, y + 60); context.lineTo(1008, y + 60); context.stroke();
        context.fillStyle = '#9d684a'; context.font = 'bold 22px "PingFang SC", sans-serif'; context.fillText(order.orderNo.slice(-8), 72, y);
        context.fillStyle = '#263a2d'; context.font = 'bold 25px "PingFang SC", sans-serif';
        const relevantItems = selectedFarmerId ? order.items.filter((item) => item.farmerId === selectedFarmerId) : order.items;
        const productsText = relevantItems.map((item) => `${item.productName}×${item.quantity}`).join('、');
        context.fillText(productsText.length > 28 ? `${productsText.slice(0, 28)}…` : productsText, 235, y);
        context.fillStyle = '#6f786f'; context.font = '21px "PingFang SC", sans-serif'; context.fillText(orderStatusNames[order.status] || order.status, 785, y);
        context.fillText(new Date(order.createdAt).toLocaleDateString('zh-CN'), 235, y + 34);
      });
      context.fillStyle = '#7c837d'; context.font = '20px "PingFang SC", sans-serif';
      context.fillText(orders.length > 10 ? `图片展示前 10 单，完整筛选结果共 ${orders.length} 单` : '乡见西村 · 村庄运营中心', 72, canvas.height - 50);
      const blob = await new Promise<Blob>((resolve, reject) => canvas.toBlob((value) => value ? resolve(value) : reject(new Error('图片生成失败')), 'image/png'));
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a'); link.href = url; link.download = `南坡订单-${selectedFarmer?.name || '全部村民'}-${Date.now()}.png`; link.click();
      URL.revokeObjectURL(url); notify('分享图片已生成，客户隐私信息已隐藏');
    } catch (reason) { setError(reason instanceof Error ? reason.message : '分享图片生成失败'); }
  };

  const title = section === 'orders' ? '订单履约' : section === 'products' ? '农产品管理'
    : section === 'reviews' ? '生产记录审核' : section === 'inquiries' ? '咨询留言'
      : section === 'goodsSection' ? '好物版块设置' : section === 'homestays' ? '民宿管理' : '游玩采摘管理';
  const defaultOccurredAt = new Date(Date.now() - new Date().getTimezoneOffset() * 60_000).toISOString().slice(0, 16);

  return <main className="admin-shell">
    <aside className="admin-sidebar"><div className="brand admin-brand"><span className="brand-seal">乡</span><span><b>乡见西村</b><small>村庄运营中心</small></span></div><nav>
      <button className={section==='orders'?'active':''} onClick={()=>setSection('orders')}>单 <span>订单履约</span></button>
      <button className={section==='products'?'active':''} onClick={()=>setSection('products')}>品 <span>农产品管理</span></button>
      <button className={section==='goodsSection'?'active':''} onClick={()=>setSection('goodsSection')}>展 <span>好物版块</span></button>
      <button className={section==='reviews'?'active':''} onClick={()=>setSection('reviews')}>田 <span>生产记录审核</span>{reviews.length>0&&<i className="alert-badge">{reviews.length}</i>}</button>
      <button className={section==='homestays'?'active':''} onClick={()=>setSection('homestays')}>宿 <span>民宿管理</span></button>
      <button className={section==='experiences'?'active':''} onClick={()=>setSection('experiences')}>游 <span>游玩采摘</span></button>
      <button className={section==='inquiries'?'active':''} onClick={()=>setSection('inquiries')}>询 <span>咨询留言</span>{inquiries.filter((item)=>item.status==='NEW').length>0&&<i className="alert-badge">{inquiries.filter((item)=>item.status==='NEW').length}</i>}</button>
    </nav><div className="admin-bottom"><button onClick={onExit}>← 返回公开页</button><div><span>管</span><p><b>村庄管理员</b><small><button onClick={async()=>{await logout();onExit()}}>退出登录</button></small></p></div></div></aside>
    <section className="admin-main"><header><div><small>乡见西村 / 村庄运营中心</small><h1>{title}</h1></div><div><button onClick={onExit}>↗ 预览公开页</button>{section==='products'&&<button className="primary" disabled={!selectedFarmerId} onClick={()=>{setEditingProduct(null);setShowProductForm(true)}}>＋ 新增农产品</button>}{(section==='homestays'||section==='experiences')&&<button className="primary" onClick={()=>{setEditing(null);setShowForm(true)}}>＋ 新增内容</button>}</div></header>
      {error&&<div className="login-error" role="alert">{error} <button onClick={reload}>重试</button></div>}
      {section==='orders' ? <section className="manage-list"><div className="manage-toolbar order-filter-toolbar"><div><label>村民<select value={selectedFarmerId} onChange={(event)=>setSelectedFarmerId(Number(event.target.value))}><option value="0">全部村民</option>{farmers.map((farmer)=><option key={farmer.id} value={farmer.id}>{farmer.name}</option>)}</select></label><label>状态<select value={orderStatus} onChange={(event)=>setOrderStatus(event.target.value)}>{orderStatuses.map((status)=><option key={status} value={status}>{orderStatusNames[status]}</option>)}</select></label></div><div><button onClick={reload}>刷新订单</button><button className="share-image-button" onClick={createShareImage}>生成分享图片</button></div></div><p className="filter-summary">当前结果：{selectedFarmer?.name || '全部村民'} · {orderStatusNames[orderStatus]} · {orders.length} 单</p>{orders.length ? orders.map((order)=><article key={order.id}><span className="row-avatar">单</span><div><h3>{order.orderNo} · ¥{order.totalAmount.toFixed(2)}</h3><p>{order.items.map((item)=>`${item.productName} ${item.specification} ×${item.quantity}`).join('；')}</p><small className="record-count">{order.recipientName} · {order.recipientPhone} · {order.recipientAddress}{order.trackingNo&&` · ${order.shippingCompany} ${order.trackingNo}`}</small></div><span className={['COMPLETED','SHIPPED'].includes(order.status)?'published':'draft'}>{orderStatusNames[order.status] || order.status}</span>{order.status==='PAYMENT_REPORTED'&&<><button disabled={busy} onClick={()=>updateOrder(order,'reject')}>驳回付款</button><button disabled={busy} onClick={()=>updateOrder(order,'confirm')}>确认收款</button></>}{order.status==='PAID'&&<button disabled={busy} onClick={()=>updateOrder(order,'prepare')}>确认备货</button>}{order.status==='READY_TO_SHIP'&&<button disabled={busy} onClick={()=>setShippingOrder(order)}>填写发货</button>}</article>) : <div className="section-empty"><span>单</span><h3>当前筛选条件下暂无订单</h3></div>}</section>
      : section==='products' ? <section className="manage-list"><div className="manage-toolbar product-toolbar"><div><label>选择村民<select value={selectedFarmerId} onChange={(event)=>setSelectedFarmerId(Number(event.target.value))}>{farmers.map((farmer)=><option key={farmer.id} value={farmer.id}>{farmer.name}</option>)}</select></label></div><button disabled={!selectedFarmerId} onClick={()=>{setEditingProduct(null);setShowProductForm(true)}}>＋ 新增农产品</button></div>{selectedFarmer&&<p className="filter-summary">正在维护：{selectedFarmer.name}（{selectedFarmer.code}）</p>}{products.length ? products.map((product)=><article key={product.id}><span className="row-avatar">品</span><div><h3>{product.name}</h3><p>{product.category} · {product.season} · {product.summary}</p><small className="record-count">{product.skus.filter((sku)=>sku.enabled).map((sku)=>`${sku.specification} ¥${sku.unitPrice}`).join('；')} · 生产记录 {product.recordCount} 条</small></div><span className={product.status==='PUBLISHED'?'published':'draft'}>{product.status==='PUBLISHED'?'展示中':'草稿'}</span><button onClick={()=>{setRecordingProduct(product);setRecordMediaFile(null);setRecordError('');setError('')}}>添加过程</button><button onClick={()=>{setEditingProduct(product);setShowProductForm(true)}}>编辑</button><button disabled={busy} onClick={()=>toggleProductPublished(product)}>{product.status==='PUBLISHED'?'下架':'上架'}</button></article>) : <div className="section-empty"><span>品</span><h3>{selectedFarmer?'该村民暂无农产品':'请先选择村民'}</h3></div>}</section>
      : section==='reviews' ? <section className="manage-list"><div className="manage-toolbar"><div><button className="active">待审核 {reviews.length}</button></div><button onClick={reload}>刷新队列</button></div>{reviews.length ? reviews.map((record)=><article key={record.id}><span className="row-avatar">记</span><div><h3>{record.productName} · {record.stage}</h3><p>{record.originalText}</p><small className="record-count">来源已确认 · {new Date(record.occurredAt).toLocaleString('zh-CN')}</small>{record.media.map((media)=><button key={media.id} disabled={!media.contentUrl} onClick={()=>media.contentUrl&&openProtectedMedia(media.contentUrl)}>查看素材：{media.originalName}</button>)}</div><span className="draft">待审核</span><button disabled={busy} onClick={()=>review(record,false)}>驳回</button><button disabled={busy} onClick={()=>review(record,true)}>审核发布</button></article>) : <div className="section-empty"><span>✓</span><h3>暂无待审生产记录</h3></div>}</section>
      : section==='inquiries' ? <section className="manage-list inquiry-list"><div className="manage-toolbar order-filter-toolbar"><div><label>咨询类型<select value={inquiryType} onChange={(event)=>setInquiryType(event.target.value)}><option value="ALL">全部类型</option><option value="HOMESTAY">民宿</option><option value="EXPERIENCE">游玩采摘</option></select></label><label>状态<select value={inquiryStatus} onChange={(event)=>setInquiryStatus(event.target.value)}><option value="ALL">全部状态</option><option value="NEW">待联系</option><option value="CONTACTED">已联系</option><option value="CLOSED">已关闭</option></select></label></div><button onClick={reload}>刷新留言</button></div>{inquiries.length ? inquiries.map((inquiry)=><article key={inquiry.id}><span className="row-avatar">{inquiry.sourceType==='HOMESTAY'?'宿':'游'}</span><div><h3>{inquiry.targetName} · {inquiry.partySize} 人</h3><p>计划到访：{new Date(inquiry.visitAt).toLocaleString('zh-CN')} · 回访电话：<a href={`tel:${inquiry.callbackPhone}`}>{inquiry.callbackPhone}</a></p><small className="record-count">{inquiry.note||'未填写备注'} · 留言于 {new Date(inquiry.createdAt).toLocaleString('zh-CN')}</small></div><span className={inquiry.status==='CLOSED'?'published':'draft'}>{inquiry.status==='NEW'?'待联系':inquiry.status==='CONTACTED'?'已联系':'已关闭'}</span>{inquiry.status==='NEW'&&<button disabled={busy} onClick={()=>updateInquiry(inquiry,'contacted')}>标记已联系</button>}{inquiry.status!=='CLOSED'&&<button disabled={busy} onClick={()=>updateInquiry(inquiry,'closed')}>关闭</button>}</article>) : <div className="section-empty"><span>询</span><h3>当前筛选下暂无咨询留言</h3></div>}</section>
      : section==='goodsSection' ? goodsSection ? <form className="content-form goods-section-form" onSubmit={saveGoodsSection}><header><div><small>PUBLIC PAGE · LOCAL HARVEST</small><h2>山野好物版块</h2><p>这里的内容会展示在公开首页农产品列表上方，保存后立即生效。</p></div></header><div className="form-grid"><label>版块小标题<input name="eyebrow" required maxLength={80} defaultValue={goodsSection.eyebrow}/></label><label>主标题<input name="title" required maxLength={255} defaultValue={goodsSection.title}/></label></div><label>介绍文案<textarea name="description" required maxLength={1000} defaultValue={goodsSection.description}/></label><div className="form-grid"><label>时节月份<input name="seasonLabel" required maxLength={50} defaultValue={goodsSection.seasonLabel}/></label><label>时节说明<input name="seasonNote" required maxLength={255} defaultValue={goodsSection.seasonNote}/></label></div><MediaUploadField name="imageUrl" label="展示图片" mediaType="IMAGE" initialUrl={goodsSection.imageUrl} required onBusyChange={trackUpload}/><label>图片说明<input name="imageCaption" required maxLength={160} defaultValue={goodsSection.imageCaption}/></label><footer><small>最后更新：{new Date(goodsSection.updatedAt).toLocaleString('zh-CN')}</small><button className="primary" type="submit" disabled={busy||uploadsInProgress>0}>{uploadsInProgress>0?'正在上传…':busy?'正在保存…':'保存并发布'}</button></footer></form> : <div className="section-empty"><span>展</span><h3>正在加载版块配置</h3></div>
      : <section className="manage-list"><div className="manage-toolbar"><div><button className="active">全部 {items.length}</button></div><button onClick={()=>{setEditing(null);setShowForm(true)}}>＋ 新增{section==='homestays'?'民宿':'游玩项目'}</button></div>{items.map((item)=><article key={item.id}><span className="row-avatar">{section==='homestays'?'宿':'游'}</span><div><h3>{item.name}</h3><p>{item.type} · {item.summary}</p><small className="record-count">{item.price}</small></div><span className={item.status==='PUBLISHED'?'published':'draft'}>{item.status==='PUBLISHED'?'展示中':'草稿'}</span><button onClick={()=>{setEditing(item);setShowForm(true)}}>编辑</button><button disabled={busy} onClick={()=>toggleContentPublished(item)}>{item.status==='PUBLISHED'?'下线':'发布'}</button></article>)}</section>}
    </section>
    {showForm&&(section==='homestays'||section==='experiences')&&<div className="modal-backdrop"><form className="content-form" onSubmit={saveContent}>
      <header><div><small>CONTENT MANAGEMENT</small><h2>{editing?'编辑':'新增'}{section==='homestays'?'民宿':'游玩项目'}</h2></div><button type="button" onClick={()=>setShowForm(false)}>×</button></header>
      <label>名称<input name="name" required maxLength={160} defaultValue={editing?.name}/></label>
      <div className="form-grid"><label>类型<input name="type" required defaultValue={editing?.type}/></label><label>价格说明<input name="price" required defaultValue={editing?.price}/></label></div>
      {section==='homestays'?<><div className="form-grid"><label>容纳人数<input name="capacity" required defaultValue={editing?.capacity}/></label><label>咨询电话<input name="consultationPhone" defaultValue={editing?.consultationPhone}/></label></div><label>民宿跳转链接<input name="externalUrl" type="url" maxLength={1000} defaultValue={editing?.externalUrl} placeholder="https://example.com/homestay"/><small>填写后，公开页面会显示“民宿主页”链接并在新窗口打开。</small></label></>:<><div className="form-grid"><label>开放季节<input name="season" required defaultValue={editing?.season}/></label><label>时长<input name="duration" required defaultValue={editing?.duration}/></label></div><MediaUploadField name="videoUrl" label="介绍视频" mediaType="VIDEO" initialUrl={editing?.videoUrl} onBusyChange={trackUpload}/><label>预约说明<textarea name="bookingNotes" defaultValue={editing?.bookingNotes}/></label></>}
      <label>简介<textarea name="summary" required maxLength={2000} defaultValue={editing?.summary}/></label>
      <MediaUploadField name="coverUrl" label="封面图片" mediaType="IMAGE" initialUrl={editing?.coverUrl} required onBusyChange={trackUpload}/>
      <label>排序<input name="sortOrder" type="number" min="0" defaultValue={editing?.sortOrder || 0}/></label>
      <footer><button type="button" onClick={()=>setShowForm(false)}>取消</button><button className="primary" type="submit" disabled={busy||uploadsInProgress>0}>{uploadsInProgress>0?'正在上传…':busy?'正在保存…':'保存为草稿'}</button></footer>
    </form></div>}
    {showProductForm&&<div className="modal-backdrop"><form className="content-form product-form" onSubmit={saveProduct}>
      <header><div><small>PRODUCT MANAGEMENT · {selectedFarmer?.name}</small><h2>{editingProduct?'维护':'新增'}农产品</h2></div><button type="button" onClick={()=>setShowProductForm(false)}>×</button></header>
      <div className="form-grid"><label>农产品名称<input name="name" required maxLength={160} defaultValue={editingProduct?.name}/></label><label>分类<input name="category" required maxLength={100} defaultValue={editingProduct?.category}/></label></div>
      <label>上市季节<input name="season" required maxLength={100} defaultValue={editingProduct?.season}/></label>
      <label>农产品介绍<textarea name="summary" required maxLength={2000} defaultValue={editingProduct?.summary}/></label>
      <ProductImageUploadField name="imageUrls" initialUrls={editingProduct?.imageUrls?.length ? editingProduct.imageUrls : editingProduct?.coverUrl ? [editingProduct.coverUrl] : []} onBusyChange={trackUpload}/>
      <fieldset className="sku-fields"><legend>可售规格</legend><div className="sku-field-head"><span>系统编码</span><span>规格名称</span><span>销售价（元）</span><span>库存说明</span><i/></div>{skuDrafts.map((sku,index)=><div className="sku-field-row" key={sku.key}><span className={`sku-generated-code ${sku.code?'ready':''}`}><small>系统编码</small>{sku.code||'保存后自动生成'}</span><label><span>规格名称</span><input aria-label={`第${index+1}条规格名称`} value={sku.specification} onChange={(event)=>setSkuDrafts((current)=>current.map((item)=>item.key===sku.key?{...item,specification:event.target.value}:item))} required maxLength={160} placeholder="如 500克/袋"/></label><label><span>销售价（元）</span><input aria-label={`第${index+1}条销售价`} value={sku.unitPrice} onChange={(event)=>setSkuDrafts((current)=>current.map((item)=>item.key===sku.key?{...item,unitPrice:event.target.value}:item))} required type="number" min="0.01" step="0.01" placeholder="29.90"/></label><label><span>库存说明</span><input aria-label={`第${index+1}条库存说明`} value={sku.stockNote} onChange={(event)=>setSkuDrafts((current)=>current.map((item)=>item.key===sku.key?{...item,stockNote:event.target.value}:item))} maxLength={200} placeholder="如 当季现货"/></label><button type="button" aria-label={`删除第${index+1}条规格`} disabled={skuDrafts.length===1} onClick={()=>setSkuDrafts((current)=>current.filter((item)=>item.key!==sku.key))}>删除</button></div>)}<button className="add-sku-button" type="button" onClick={()=>setSkuDrafts((current)=>[...current,emptySkuDraft()])}>＋ 添加一条规格</button><small>规格编码由后端统一生成；已有规格保留原编码，历史订单快照不受后续修改影响。</small></fieldset>
      <footer><button type="button" onClick={()=>setShowProductForm(false)}>取消</button><button className="primary" type="submit" disabled={busy||uploadsInProgress>0}>{uploadsInProgress>0?'正在上传…':busy?'正在保存…':'保存农产品'}</button></footer>
    </form></div>}
    {recordingProduct&&<div className="modal-backdrop record-form-backdrop"><form className="record-form" noValidate onSubmit={saveFarmRecord}><header><div><small>FARM RECORD · {selectedFarmer?.name}</small><h2>添加真实生产过程</h2></div><button type="button" aria-label="关闭生产过程表单" onClick={()=>{setRecordingProduct(null);setRecordMediaFile(null);setRecordError('')}}>×</button></header><div className="record-context"><span>{recordingProduct.name.slice(0,1)}</span><div><small>当前农产品</small><strong>{recordingProduct.name}</strong><p>保存后进入审核队列，审核通过才会在公开页面展示。</p></div></div>{recordError&&<div className="record-form-error" role="alert"><span>!</span><p>{recordError}</p><button type="button" onClick={()=>setRecordError('')} aria-label="关闭错误提示">×</button></div>}<div className="record-form-body"><div className="record-grid"><label>生产阶段<select name="stage" defaultValue="GROWING"><option value="PREPARATION">整地备耕</option><option value="SOWING">播种</option><option value="FERTILIZING">施肥</option><option value="GROWING">生长</option><option value="HARVEST">采收</option><option value="PROCESSING">加工</option><option value="PACKING">包装</option><option value="SHIPPING">发货</option></select></label><label>发生时间<input name="occurredAt" type="datetime-local" defaultValue={defaultOccurredAt}/></label></div><label className="record-description">真实情况说明<textarea name="originalText" maxLength={5000} placeholder="请记录种植、采收、加工或包装的真实情况，不使用夸大表述。"/><small>最多 5000 字，请使用村民容易核实的客观表述。</small></label><div className="record-media-field"><div className="record-field-title"><span>现场素材 <small>选填</small></span><b>支持照片、视频或语音</b></div><label className="record-media-picker"><input type="file" accept="image/jpeg,image/png,image/webp,video/mp4,video/webm,audio/mpeg,audio/wav,audio/mp4" onChange={(event)=>{setRecordMediaFile(event.currentTarget.files?.[0]||null);setRecordError('')}}/><span>＋</span><div><strong>{recordMediaFile?'重新选择素材':'选择一个现场素材'}</strong><small>图片 ≤ 10MB · 音频 ≤ 30MB · 视频 ≤ 100MB</small></div></label>{recordMediaFile&&<div className="record-media-selected"><span>✓</span><div><strong>{recordMediaFile.name}</strong><small>{(recordMediaFile.size/1024/1024).toFixed(2)} MB · 将在保存记录后上传</small></div><button type="button" onClick={()=>setRecordMediaFile(null)}>移除</button></div>}</div><label className="record-truth-check"><input name="truthConfirmed" type="checkbox"/><span><strong>确认信息真实</strong><small>已向村民核实，该记录来自真实生产过程。</small></span></label></div><footer><span>提交过程：保存记录 → 上传素材 → 进入审核</span><div><button type="button" onClick={()=>{setRecordingProduct(null);setRecordMediaFile(null);setRecordError('')}}>取消</button><button className="primary" type="submit" disabled={busy}>{busy?'正在处理，请稍候…':'保存并提交审核'}</button></div></footer></form></div>}
    {shippingOrder&&<div className="modal-backdrop"><form className="content-form" onSubmit={shipOrder}><header><div><small>ORDER FULFILMENT</small><h2>填写发货信息</h2></div><button type="button" onClick={()=>setShippingOrder(null)}>×</button></header><p>{shippingOrder.orderNo} · {shippingOrder.recipientName}</p><label>物流公司<input name="shippingCompany" required maxLength={80} placeholder="例如：邮政快递"/></label><label>物流单号<input name="trackingNo" required maxLength={120}/></label><footer><button type="button" onClick={()=>setShippingOrder(null)}>取消</button><button className="primary" type="submit" disabled={busy}>{busy?'正在提交…':'确认发货'}</button></footer></form></div>}
    {toast&&<div className="toast">✓ {toast}</div>}
  </main>;
}
