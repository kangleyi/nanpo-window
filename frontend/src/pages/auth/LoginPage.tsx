import { FormEvent, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../../services/api';
import { loginWithPassword, registerWithPassword } from '../../services/authApi';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (mode === 'register' && password !== confirmPassword) {
      setError('两次输入的密码不一致');
      return;
    }
    setBusy(true);
    setError('');
    try {
      const user = mode === 'register'
        ? await registerWithPassword(phone, password)
        : await loginWithPassword(phone, password);
      const requested = (location.state as { from?: string } | null)?.from;
      const defaultPath = user.roles.some((role) => ['CONTENT_OPERATOR', 'REVIEWER', 'ORDER_OPERATOR', 'SUPER_ADMIN'].includes(role))
          ? '/admin'
          : '/';
      navigate(mode === 'register' ? '/' : requested || defaultPath, { replace: true });
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : mode === 'register' ? '注册失败' : '登录失败');
    } finally {
      setBusy(false);
    }
  };

  const switchMode = (nextMode: 'login' | 'register') => {
    setMode(nextMode);
    setPassword('');
    setConfirmPassword('');
    setError('');
  };

  return <main className="login-shell"><section className="login-card"><a href="/">返回乡见西村</a><span>账号与权限</span><h1>{mode === 'login' ? '手机号密码登录' : '注册顾客账号'}</h1><p>{mode === 'login' ? '使用手机号和密码登录，管理账号会自动进入对应后台。' : '注册后可直接下单，手机号将作为登录用户名。'}</p><div className="auth-mode-switch" role="tablist"><button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => switchMode('login')}>登录</button><button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => switchMode('register')}>注册</button></div><form onSubmit={submit}><label>手机号<input required pattern="1\d{10}" inputMode="tel" autoComplete="tel" value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="11 位手机号"/></label><label className="password-field">密码<input required minLength={8} maxLength={64} type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} placeholder="8—64 位密码"/></label>{mode === 'register' && <label className="password-field">确认密码<input required minLength={8} maxLength={64} type="password" autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} placeholder="再次输入密码"/></label>}{error && <div className="login-error" role="alert">{error}</div>}<button className="login-submit" disabled={busy} type="submit">{busy ? '正在提交…' : mode === 'login' ? '登录' : '注册并登录'}</button></form><footer>密码经安全哈希后保存；请勿与支付密码或其他重要账号共用。</footer></section></main>;
}
