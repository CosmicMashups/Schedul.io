import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LogIn, Stethoscope } from 'lucide-react';
import { login } from '../api/auth';
import { ApiError, getTenantId } from '../api/client';
import { Field } from '../components/Field';
import { Spinner } from '../components/Spinner';
import { useToast } from '../components/Toast';

export default function Login() {
  const [tenantId, setTenantId] = useState(getTenantId() ?? '');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const toast = useToast();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      await login(tenantId.trim(), email.trim(), password);
      navigate('/');
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Sign-in failed.', 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate">
      <div className="panel w-full max-w-sm p-8 animate-scale-in">
        <div className="mb-6 flex items-center gap-2.5">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-teal/15 text-teal">
            <Stethoscope size={18} />
          </span>
          <div>
            <p className="text-[10px] font-mono uppercase tracking-widest text-teal leading-none mb-1">Doctor portal</p>
            <h1 className="text-lg font-semibold text-ink leading-none">Sign in</h1>
          </div>
        </div>
        <form onSubmit={onSubmit} className="space-y-4">
          <Field label="Clinic ID" value={tenantId} onChange={setTenantId} placeholder="Tenant UUID" mono />
          <Field label="Email" type="email" value={email} onChange={setEmail} />
          <Field label="Password" type="password" value={password} onChange={setPassword} />
          <button
            type="submit"
            disabled={loading}
            className="btn-press w-full flex items-center justify-center gap-2 rounded-lg bg-teal py-2.5 text-slate text-sm font-semibold hover:bg-teal-light focus-ring disabled:opacity-50"
          >
            {loading ? <Spinner size={16} /> : <LogIn size={16} />}
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  );
}
