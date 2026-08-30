import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../../services/api';
import { logout } from '../../services/authApi';
import { loadCustomerOrders, Order } from '../../services/orderApi';

const statusNames: Record<string, string> = {
  CREATED: '待付款',
  PAYMENT_REPORTED: '待核款',
  PAID: '已收款',
  READY_TO_SHIP: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDED: '已退款',
};

function formatTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function CustomerOrdersPage() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);
  const [error, setError] = useState('');

  const reload = useCallback(() => {
    setLoading(true);
    setError('');
    loadCustomerOrders()
      .then(setOrders)
      .catch((reason) => setError(reason instanceof ApiError ? reason.message : '订单加载失败，请稍后重试'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => reload(), [reload]);

  const exitAccount = async () => {
    setLoggingOut(true);
    await logout();
    navigate('/login', { replace: true });
  };

  return <main className="customer-orders-page">
    <header className="customer-orders-header">
      <a className="brand" href="/"><span className="brand-seal">乡</span><span><b>乡见西村</b><small>DISCOVER XICUN</small></span></a>
      <div><a href="/">返回首页</a><button disabled={loggingOut} onClick={exitAccount}>{loggingOut ? '正在退出…' : '退出登录'}</button></div>
    </header>
    <section className="customer-orders-content">
      <div className="customer-orders-title"><div><small>MY ORDERS</small><h1>我的订单</h1><p>这里只展示当前登录手机号创建的订单。</p></div><button disabled={loading} onClick={reload}>{loading ? '加载中…' : '刷新订单'}</button></div>
      {error && <div className="customer-orders-message error" role="alert"><span>{error}</span><button onClick={reload}>重新加载</button></div>}
      {!error && loading && <div className="customer-orders-message"><span className="state-spinner"/><p>正在加载订单…</p></div>}
      {!error && !loading && !orders.length && <div className="customer-orders-empty"><span>单</span><h2>还没有订单</h2><p>去首页看看村里的农产品，下单后会显示在这里。</p><a href="/#goods">去选购农产品</a></div>}
      {!error && !loading && orders.length > 0 && <div className="customer-orders-list">
        {orders.map((order) => <article key={order.id}>
          <header><div><small>订单号</small><h2>{order.orderNo}</h2><time>{formatTime(order.createdAt)}</time></div><span className={`customer-order-status status-${order.status.toLowerCase()}`}>{statusNames[order.status] || order.status}</span></header>
          <div className="customer-order-items">{order.items.map((item) => <div key={item.id}><span>{item.productName.slice(0, 1)}</span><p><strong>{item.productName}</strong><small>{item.specification} × {item.quantity}</small></p><b>¥{Number(item.lineAmount).toFixed(2)}</b></div>)}</div>
          <footer><div><small>实付金额</small><strong>¥{Number(order.totalAmount).toFixed(2)}</strong></div><div><small>收货人</small><span>{order.recipientName} · {order.recipientPhone}</span></div><div><small>收货地址</small><span>{order.recipientAddress}</span></div>{order.trackingNo && <div><small>物流信息</small><span>{order.shippingCompany} · {order.trackingNo}</span></div>}</footer>
        </article>)}
      </div>}
    </section>
  </main>;
}
