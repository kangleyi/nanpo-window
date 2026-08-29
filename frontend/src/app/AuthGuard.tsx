import { ReactNode, useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { CurrentUser, getCurrentUser } from '../services/authApi';

type GuardState =
  | { status: 'loading' }
  | { status: 'guest' }
  | { status: 'ready'; user: CurrentUser };

export function AuthGuard({ roles, children }: { roles: string[]; children: ReactNode }) {
  const location = useLocation();
  const [state, setState] = useState<GuardState>({ status: 'loading' });

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
    return <main className="app-state"><h1>当前账号无权访问</h1><p>请联系村庄管理员配置角色与数据范围。</p><a href="/">返回南坡之窗</a></main>;
  }
  return children;
}

