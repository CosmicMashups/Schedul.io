import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LogIn } from 'lucide-react';
import { login } from '../api/auth';
import { getMyPatientRecord } from '../api/booking';
import { ApiError } from '../api/client';
import { Field } from '../components/Field';
import { useToast } from '../components/Toast';
import { Spinner } from '../components/Spinner';

export default function Login() {
  const [tenantId, setTenantId] = useState(localStorage.getItem('clinic.tenantId') ?? '');
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

      // Closes a previously-flagged gap: resolve the patient record tied to this login
      // (GET /api/v1/patients/me) instead of relying solely on what a prior registration
      // may have cached in localStorage on this device.
      try {
        const patient = await getMyPatientRecord();
        localStorage.setItem('clinic.patientId', patient.id);
      } catch {
        // No linked patient record yet (e.g. a staff-created account with no self-registration) —
        // fine, the person can still browse; "My appointments" will prompt them to register.
      }

      toast.show('Welcome back!');
      navigate('/doctors');
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.', 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-sm mx-auto animate-page">
      <h1 className="font-display text-2xl text-ink mb-1">Sign in</h1>
      <p className="text-sm text-ink/60 mb-6">Access your appointments and booking history.</p>

      <form onSubmit={onSubmit} className="space-y-4">
        <Field label="Clinic ID" value={tenantId} onChange={setTenantId} placeholder="Tenant UUID" mono />
        <Field label="Email" type="email" value={email} onChange={setEmail} placeholder="you@example.com" />
        <Field label="Password" type="password" value={password} onChange={setPassword} placeholder="••••••••" />

        <button
          type="submit"
          disabled={loading}
          className="btn-press w-full flex items-center justify-center gap-2 rounded-full bg-teal py-3 text-white text-sm font-medium hover:bg-teal-dark focus-ring disabled:opacity-50 shadow-sm shadow-teal/20"
        >
          {loading ? <Spinner size={16} /> : <LogIn size={16} />}
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>
  );
}
