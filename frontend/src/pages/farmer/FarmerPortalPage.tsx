import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../services/api';
import { logout } from '../../services/authApi';
import { FarmerOrder, loadFarmerOrders } from '../../services/farmerApi';

const orderStatusNames: Record<string, string> = {
  PAID: '已收款，等待统一备货',
  READY_TO_SHIP: '已备货，等待统一发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
};

export function FarmerPortalPage({ onExit }: { onExit: () => void }) {
  const [orders, setOrders] = useState<FarmerOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reload = useCallback(() => {
    setLoading(true);
    setError('');
    loadFarmerOrders()
      .then(setOrders)
      .catch((reason) => setError(reason instanceof ApiError ? reason.message : '订单加载失败'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => reload(), [reload]);

  const summary = useMemo(() => ({
    total: orders.length,
    pending: orders.filter((order) => ['PAID', 'READY_TO_SHIP'].includes(order.status)).length,
    shipped: orders.filter((order) => ['SHIPPED', 'COMPLETED'].includes(order.status)).length,
  }), [orders]);

  return <main className="farmer-portal farmer-orders-only">
    <header><button onClick={onExit}>← 返回乡见西村</button><div className="farmer-logo"><span>乡</span><div><b>村民订单</b><small>简单查看 · 无需维护商品</small></div></div><button className="farmer-help" onClick={async()=>{await logout();onExit()}}>退出登录</button></header>
    <section className="farmer-main">
      <div className="farmer-welcome"><div><small>我的订单</small><h1>这里仅查看与你有关的订单</h1><p>农产品上架、信息维护、收款确认和发货由村庄运营人员统一处理。</p></div><button onClick={reload} disabled={loading}><span>↻</span><b>{loading?'正在刷新':'刷新订单'}</b><small>查看最新订单状态</small></button></div>
      <div className="farmer-summary"><article><span>单</span><div><small>相关订单</small><strong>{summary.total} 单</strong></div><i>仅本人农品</i></article><article><span>待</span><div><small>处理中</small><strong>{summary.pending} 单</strong></div><i>由运营统一处理</i></article><article><span>成</span><div><small>已发货/完成</small><strong>{summary.shipped} 单</strong></div><i>状态实时更新</i></article></div>
      {error&&<div className="login-error" role="alert">{error} <button onClick={reload}>重试</button></div>}
      <article className="farm-diary farmer-order-board"><header><div><h2>订单列表</h2><p>不需要填写或修改任何内容，有新状态时刷新即可。</p></div><button onClick={reload} disabled={loading}>刷新</button></header><div className="diary-list">
        {orders.length ? orders.map((order)=><div key={order.id}><span className="diary-date">{order.orderNo.slice(-4)}</span><div className="diary-photo empty">单</div><div><small>{orderStatusNames[order.status] || order.status}</small><h3>{order.items.map((item)=>`${item.productName} ${item.specification} × ${item.quantity}`).join('、')}</h3><p>订单号：{order.orderNo}<br/>下单时间：{new Date(order.createdAt).toLocaleString('zh-CN')}</p><div><b>后续由村庄运营中心统一处理</b></div></div></div>) : !loading&&<div className="draft-entry"><span className="diary-date">单</span><div className="diary-photo empty">✓</div><div><small>暂无相关订单</small><h3>有新的已付款订单时会显示在这里</h3></div></div>}
      </div></article>
    </section>
  </main>;
}
