import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { PublicWindow } from '../App';
import { AdminConsolePage } from '../pages/admin/AdminConsolePage';
import { LoginPage } from '../pages/auth/LoginPage';
import { CustomerOrdersPage } from '../pages/customer/CustomerOrdersPage';
import { AuthGuard } from './AuthGuard';

function RouteContent() {
  const navigate = useNavigate();
  return <Routes>
    <Route path="/" element={<PublicWindow onManage={() => navigate('/admin')} onLogin={() => navigate('/login', { state: { from: '/' } })} onOrders={() => navigate('/orders')}/>} />
    <Route path="/login" element={<LoginPage/>} />
    <Route path="/orders" element={<AuthGuard roles={['CUSTOMER']}><CustomerOrdersPage/></AuthGuard>} />
    <Route path="/admin/*" element={<AuthGuard roles={['CONTENT_OPERATOR', 'REVIEWER', 'ORDER_OPERATOR', 'SUPER_ADMIN']}><AdminConsolePage onExit={() => navigate('/')}/></AuthGuard>} />
    <Route path="*" element={<Navigate to="/" replace/>} />
  </Routes>;
}

export default function AppRouter() {
  return <BrowserRouter><RouteContent/></BrowserRouter>;
}
