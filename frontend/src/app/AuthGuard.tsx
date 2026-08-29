import { ReactNode, useEffect, useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { CurrentUser, getCurrentUser, logout } from '../services/authApi';

type GuardState =
  | { status: 'loading' }
  | { status: 'guest' }
  | { status: 'ready'; user: CurrentUser };

export function AuthGuard({ roles, children }: { roles: string[]; children: ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();
  const [state, setState] = useState<GuardState>({ status: 'loading' });
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    let active = true;
    getCurrentUser()
      .then((user) => active && setState({ status: 'ready', user }))
      .catch(() => active && setState({ status: 'guest' }));
    return () => {
      active = false;
    };
  }, []);

  if (state.status === 'loading') {
    return <main className="app-state"><span className="state-spinner"/><h1>正在确认账号权限…</h1></main>;
  }
  if (state.status === 'guest') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (!state.user.roles.some((role) => roles.includes(role))) {
    const exitAccount = async () => {
      setLoggingOut(true);
      await logout();
      navigate('/login', { replace: true });
    };
    return <main className="app-state"><h1>当前账号无权访问</h1><p>请联系村庄管理员配置角色与数据范围，或退出后更换账号。</p><div className="app-state-actions"><button disabled={loggingOut} onClick={exitAccount}>{loggingOut ? '正在退出…' : '退出当前账号'}</button><a className="secondary" href="/">返回乡见西村</a></div></main>;
  }
  return children;
}
