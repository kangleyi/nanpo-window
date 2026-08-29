import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { PublicWindow } from '../App';
import { AdminConsolePage } from '../pages/admin/AdminConsolePage';
import { LoginPage } from '../pages/auth/LoginPage';
import { FarmerPortalPage } from '../pages/farmer/FarmerPortalPage';
import { AuthGuard } from './AuthGuard';

function RouteContent() {
  const navigate = useNavigate();
  return <Routes>
    <Route path="/" element={<PublicWindow onManage={() => navigate('/admin')} onFarmer={() => navigate('/farmer')} onLogin={() => navigate('/login', { state: { from: '/' } })}/>} />
    <Route path="/login" element={<LoginPage/>} />
    <Route path="/farmer/*" element={<AuthGuard roles={['FARMER', 'SUPER_ADMIN']}><FarmerPortalPage onExit={() => navigate('/')}/></AuthGuard>} />
    <Route path="/admin/*" element={<AuthGuard roles={['CONTENT_OPERATOR', 'REVIEWER', 'ORDER_OPERATOR', 'SUPER_ADMIN']}><AdminConsolePage onExit={() => navigate('/')}/></AuthGuard>} />
    <Route path="*" element={<Navigate to="/" replace/>} />
  </Routes>;
}

export default function AppRouter() {
  return <BrowserRouter><RouteContent/></BrowserRouter>;
}
