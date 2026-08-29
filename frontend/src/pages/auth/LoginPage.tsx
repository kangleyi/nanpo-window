import { FormEvent, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../../services/api';
import { loginWithSms, sendLoginCode } from '../../services/authApi';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const sendCode = async () => {
    setBusy(true);
    setError('');
    try {
      await sendLoginCode(phone);
      setSent(true);
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '验证码发送失败');
    } finally {
      setBusy(false);
    }
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const user = await loginWithSms(phone, code);
      const requested = (location.state as { from?: string } | null)?.from;
      const defaultPath = user.roles.includes('FARMER')
        ? '/farmer'
        : user.roles.some((role) => ['CONTENT_OPERATOR', 'REVIEWER', 'ORDER_OPERATOR', 'SUPER_ADMIN'].includes(role))
          ? '/admin'
          : '/';
      navigate(requested || defaultPath, { replace: true });
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '登录失败');
    } finally {
      setBusy(false);
    }
  };

  return <main className="login-shell"><section className="login-card"><a href="/">← 返回南坡之窗</a><span>账号与权限</span><h1>手机验证码登录</h1><p>农户和运营人员使用由村级管理员绑定的手机号登录。</p><form onSubmit={submit}><label>手机号<input required pattern="1\d{10}" inputMode="tel" value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="11 位手机号"/></label><div className="code-row"><label>验证码<input required pattern="\d{6}" inputMode="numeric" value={code} onChange={(event) => setCode(event.target.value)} placeholder="6 位验证码"/></label><button type="button" disabled={busy || phone.length !== 11} onClick={sendCode}>{sent ? '重新发送' : '获取验证码'}</button></div>{import.meta.env.DEV && sent && <small>本地开发验证码：123456</small>}{error && <div className="login-error" role="alert">{error}</div>}<button className="login-submit" disabled={busy} type="submit">{busy ? '正在验证…' : '登录 →'}</button></form><footer>登录、刷新和退出均由后端会话令牌控制，管理接口不依赖前端隐藏按钮。</footer></section></main>;
}
