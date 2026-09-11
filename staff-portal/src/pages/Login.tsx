import { FormEvent, useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { gsap } from 'gsap';
import { LogIn } from 'lucide-react';
import { login } from '../api/auth';
import { ApiError, getTenantId } from '../api/client';
import { Field } from '../components/Field';
import { useToast } from '../components/Toast';
import { Spinner } from '../components/Spinner';

function BrandMark({ size = 18 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="3" y="5" width="18" height="16" rx="4" stroke="currentColor" strokeWidth="2" />
      <path d="M8 3v4M16 3v4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M12 10.5v7M8.5 14h7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

export default function Login() {
  const [tenantId, setTenantId] = useState(getTenantId() ?? '');
  const [email, setEmail] = useState('admin@demo-clinic.test');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const toast = useToast();
  const rootRef = useRef<HTMLDivElement>(null);

  // Entrance stagger on the one screen every staff member sees first, every shift — the
  // headline and card arrive in sequence rather than popping in fully-formed. Skipped under
  // prefers-reduced-motion.
  useEffect(() => {
    const root = rootRef.current;
    if (!root) return;
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const targets = root.querySelectorAll<HTMLElement>('[data-login-in]');
    if (reduceMotion || targets.length === 0) {
      gsap.set(targets, { opacity: 1, y: 0 });
      return;
    }
    const ctx = gsap.context(() => {
      gsap.set(targets, { opacity: 0, y: 16 });
      gsap.to(targets, { opacity: 1, y: 0, duration: 0.6, ease: 'power3.out', stagger: 0.08 });
    }, root);
    return () => ctx.revert();
  }, []);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      await login(tenantId.trim(), email.trim(), password);
      navigate('/dashboard');
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Sign-in failed.', 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div ref={rootRef} className="relative min-h-screen overflow-hidden bg-ink">
      {/* Photo slot: place login-desk.jpg at staff-portal/public/images/. Falls back to an
          on-brand dark-teal gradient (not a pale one) so white text stays legible either way. */}
      <div
        aria-hidden
        className="absolute inset-0 bg-cover bg-center"
        style={{ backgroundImage: "url('/images/login-desk.jpg'), linear-gradient(135deg, #0A5049 0%, #17262B 100%)" }}
      />
      {/* The headline sits directly on the photo (top on narrow widths, left on wide ones);
          the sign-in card doesn't need scrim since it has its own opaque background. Stacked
          (not breakpoint-swapped) layers — a flat wash plus both a vertical and horizontal
          gradient — keep the headline legible at every width instead of only the one layout
          a single directional gradient was sized for. */}
      <div aria-hidden className="absolute inset-0 bg-ink/40" />
      <div aria-hidden className="absolute inset-0 bg-gradient-to-t from-ink/55 via-ink/20 to-transparent" />
      <div aria-hidden className="absolute inset-0 bg-gradient-to-r from-ink/55 via-ink/20 to-transparent" />

      <div className="relative min-h-screen flex flex-col lg:flex-row items-center justify-center lg:justify-between gap-10 px-6 sm:px-10 lg:px-16 py-12 max-w-6xl mx-auto">
        <div className="max-w-md text-center lg:text-left">
          <p data-login-in className="font-mono text-xs uppercase tracking-widest text-teal-light mb-4">Staff console</p>
          <h1 data-login-in className="font-display text-3xl sm:text-4xl leading-[1.1] text-white text-balance">
            Everything the front desk needs, one console.
          </h1>
        </div>

        <div data-login-in className="panel w-full max-w-sm p-8 shrink-0">
          <div className="mb-6 flex items-center gap-2.5">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-teal text-white">
              <BrandMark size={18} />
            </span>
            <div>
              <p className="text-[10px] font-mono uppercase tracking-widest text-teal leading-none mb-1">Staff console</p>
              <h2 className="text-lg font-semibold text-ink leading-none">Sign in</h2>
            </div>
          </div>
          <form onSubmit={onSubmit} className="space-y-4">
            <Field label="Clinic ID" value={tenantId} onChange={setTenantId} placeholder="Tenant UUID" mono />
            <Field label="Email" type="email" value={email} onChange={setEmail} />
            <Field label="Password" type="password" value={password} onChange={setPassword} placeholder="ChangeMe123!" />
            <button
              type="submit"
              disabled={loading}
              className="btn-press w-full flex items-center justify-center gap-2 rounded-lg bg-teal py-2.5 text-white text-sm font-medium hover:bg-teal-dark focus-ring disabled:opacity-50"
            >
              {loading ? <Spinner size={16} /> : <LogIn size={16} />}
              {loading ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
          <p className="mt-5 text-center text-[11px] text-ink/40">
            <Link to="/terms" className="hover:text-ink focus-ring rounded">Terms of Service</Link>
            {' · '}
            <Link to="/privacy" className="hover:text-ink focus-ring rounded">Privacy Policy</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
