import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { registerPatient } from '../api/auth';
import { ApiError, getTenantId, setTenantId } from '../api/client';
import { Field, SelectField } from '../components/Field';
import { useToast } from '../components/Toast';
import { UserPlus } from 'lucide-react';
import { Spinner } from '../components/Spinner';

// NOTE: registration itself needs the caller to already be authenticated per the backend's
// PATIENT_WRITE/SELF_REGISTER guard on POST /api/v1/patients — there is no anonymous
// self-registration endpoint. This screen therefore assumes the person already has a portal
// login (e.g. created by staff during onboarding) and is filling in their patient record for
// the first time. A true "sign up as a brand-new patient with no account yet" flow needs a
// backend endpoint that doesn't exist in this milestone — flagged rather than worked around.
export default function Register() {
  const navigate = useNavigate();
  const [tenantId, setTenantIdField] = useState(getTenantId() ?? '');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [sex, setSex] = useState('');
  const [mobileNumber, setMobileNumber] = useState('');
  const [email, setEmail] = useState('');
  const [addressLine, setAddressLine] = useState('');
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!consent) {
      setError('Please review and accept the data processing consent to continue.');
      return;
    }
    setTenantId(tenantId.trim());
    setLoading(true);
    try {
      const patient = await registerPatient({
        firstName, lastName, birthDate, sex: sex as 'MALE' | 'FEMALE',
        mobileNumber: mobileNumber || undefined,
        email: email || undefined,
        addressLine: addressLine || undefined,
        preferredContactMethod: 'SMS',
        registrationSource: 'ONLINE',
        privacyNoticeVersion: 'v1',
        dataProcessingConsentGranted: true,
      });
      localStorage.setItem('clinic.patientId', patient.id);
      toast.show('Welcome! Your record has been created.');
      navigate('/doctors');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-md mx-auto animate-page">
      <h1 className="font-display text-2xl text-ink mb-1">Create your patient record</h1>
      <p className="text-sm text-ink/60 mb-6">
        We'll use this to match you with your appointments and keep your care team informed.
      </p>

      <form onSubmit={onSubmit} className="space-y-4">
        <Field label="Clinic ID" value={tenantId} onChange={setTenantIdField} placeholder="Tenant UUID" mono />

        <div className="grid grid-cols-2 gap-4">
          <Field label="First name" value={firstName} onChange={setFirstName} />
          <Field label="Last name" value={lastName} onChange={setLastName} />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Date of birth" type="date" value={birthDate} onChange={setBirthDate} />
          <SelectField
            label="Sex"
            value={sex}
            onChange={setSex}
            options={[{ value: 'MALE', label: 'Male' }, { value: 'FEMALE', label: 'Female' }]}
          />
        </div>

        <Field label="Mobile number" value={mobileNumber} onChange={setMobileNumber} placeholder="09XX XXX XXXX" required={false} />
        <Field label="Email" type="email" value={email} onChange={setEmail} required={false} />
        <Field label="Address" value={addressLine} onChange={setAddressLine} required={false} />

        <label className="flex items-start gap-2.5 text-xs text-ink/60 pt-2">
          <input
            type="checkbox"
            checked={consent}
            onChange={(e) => setConsent(e.target.checked)}
            className="mt-0.5 focus-ring"
          />
          <span>
            I consent to Demo Clinic collecting and processing my personal and health information
            for the purpose of scheduling and delivering care, per the clinic's privacy notice.
          </span>
        </label>

        {error && <p className="text-sm text-coral">{error}</p>}

        <button
          type="submit"
          disabled={loading}
          className="btn-press w-full flex items-center justify-center gap-2 rounded-full bg-teal py-3 text-white text-sm font-medium hover:bg-teal-dark focus-ring disabled:opacity-50 shadow-sm shadow-teal/20"
        >
          {loading ? <Spinner size={16} /> : <UserPlus size={16} />}
          {loading ? 'Creating your record…' : 'Create record'}
        </button>
      </form>
    </div>
  );
}
