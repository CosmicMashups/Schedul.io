import { FormEvent, useEffect, useState } from 'react';
import { Mail, Stethoscope, UserPlus, Send } from 'lucide-react';
import { createPractitioner, createUser, linkPractitionerUser, listClinics, listSpecialties, searchPractitioners } from '../api/clinic';
import { ApiError } from '../api/client';
import type { ClinicResponse, PractitionerResponse, SpecialtyResponse } from '../api/types';
import { Field } from '../components/Field';
import { EmptyState } from '../components/EmptyState';
import { Spinner } from '../components/Spinner';
import { useToast } from '../components/Toast';

export default function Practitioners() {
  const [doctors, setDoctors] = useState<PractitionerResponse[]>([]);
  const [clinics, setClinics] = useState<ClinicResponse[]>([]);
  const [specialties, setSpecialties] = useState<SpecialtyResponse[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [invitingFor, setInvitingFor] = useState<PractitionerResponse | null>(null);
  const toast = useToast();

  async function load() {
    try {
      const [docs, cls, specs] = await Promise.all([searchPractitioners(), listClinics(), listSpecialties()]);
      setDoctors(docs);
      setClinics(cls);
      setSpecialties(specs);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not load doctors.', 'error');
    }
  }
  useEffect(() => { load(); }, []);

  return (
    <div className="max-w-2xl animate-page">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Stethoscope size={20} className="text-teal" />
          <h1 className="text-xl font-semibold text-ink">Doctors</h1>
        </div>
        <button onClick={() => setShowForm((v) => !v)} className="btn-press flex items-center gap-1.5 rounded-lg border border-line px-3.5 py-2 text-sm font-medium hover:border-teal focus-ring">
          <UserPlus size={15} /> {showForm ? 'Cancel' : 'New doctor'}
        </button>
      </div>

      {showForm ? (
        <PractitionerForm clinics={clinics} specialties={specialties} onDone={() => { setShowForm(false); load(); toast.show('Doctor added.'); }} />
      ) : invitingFor ? (
        <InviteForm
          practitioner={invitingFor}
          onDone={() => { setInvitingFor(null); toast.show('Doctor Portal access granted.'); }}
          onCancel={() => setInvitingFor(null)}
        />
      ) : doctors.length === 0 ? (
        <div className="panel"><EmptyState icon={Stethoscope} title="No doctors yet" /></div>
      ) : (
        <div className="panel divide-y divide-line/60">
          {doctors.map((d) => (
            <div key={d.id} className="px-4 py-3 flex items-center gap-3">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-teal/10 text-teal">
                <Stethoscope size={16} />
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-medium text-ink">Dr. {d.firstName} {d.lastName} {d.credentials && <span className="text-ink/40 font-normal">· {d.credentials}</span>}</p>
                <p className="text-xs text-ink/50 mt-0.5">{d.specialties.join(', ') || 'No specialty set'} · {d.clinics.join(', ')}</p>
              </div>
              <button
                onClick={() => setInvitingFor(d)}
                className="btn-press flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs font-medium text-teal hover:bg-teal/10 focus-ring shrink-0"
              >
                <Send size={13} /> Invite to Doctor Portal
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/**
 * Closes the "invite to Doctor Portal" gap: creates a login account (POST /api/v1/staff/users
 * with roleCodes=['DOCTOR']) and links it to this Practitioner record
 * (POST /staff/practitioners/{id}/link-user) in two calls, since that's how the backend
 * models it — a login account is a general identity concern, not practitioner-specific data.
 */
function InviteForm({
  practitioner, onDone, onCancel,
}: { practitioner: PractitionerResponse; onDone: () => void; onCancel: () => void }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      const user = await createUser({
        email, password, firstName: practitioner.firstName, lastName: practitioner.lastName,
        roleCodes: ['DOCTOR'],
      });
      await linkPractitionerUser(practitioner.id, user.id);
      onDone();
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not grant Doctor Portal access.', 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="panel p-5 space-y-4 animate-scale-in">
      <div className="flex items-center gap-2 mb-1">
        <Mail size={16} className="text-teal" />
        <h2 className="text-sm font-medium text-ink">
          Invite Dr. {practitioner.firstName} {practitioner.lastName} to the Doctor Portal
        </h2>
      </div>
      <p className="text-xs text-ink/50 -mt-2">
        Creates a login account and links it to this doctor's schedule and queue.
      </p>
      <Field label="Email" type="email" value={email} onChange={setEmail} placeholder="doctor@example.com" />
      <Field label="Temporary password" type="text" value={password} onChange={setPassword} placeholder="At least 8 characters" mono />
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={loading}
          className="btn-press flex items-center gap-2 rounded-lg bg-teal px-5 py-2.5 text-sm font-medium text-white hover:bg-teal-dark focus-ring disabled:opacity-50"
        >
          {loading ? <Spinner size={15} /> : <Send size={15} />}
          {loading ? 'Sending invite…' : 'Grant access'}
        </button>
        <button type="button" onClick={onCancel} className="rounded-lg px-4 py-2.5 text-sm text-ink/60 hover:text-ink focus-ring">
          Cancel
        </button>
      </div>
    </form>
  );
}

function PractitionerForm({
  clinics, specialties, onDone,
}: { clinics: ClinicResponse[]; specialties: SpecialtyResponse[]; onDone: () => void }) {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [credentials, setCredentials] = useState('');
  const [selectedSpecialties, setSelectedSpecialties] = useState<Set<string>>(new Set());
  const [clinicId, setClinicId] = useState(clinics[0]?.id ?? '');
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  function toggleSpecialty(id: string) {
    setSelectedSpecialties((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      await createPractitioner({
        firstName, lastName, credentials: credentials || undefined,
        specialtyIds: Array.from(selectedSpecialties),
        clinicIds: [clinicId],
      });
      onDone();
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not create this doctor.', 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="panel p-5 space-y-4 animate-scale-in">
      <div className="grid grid-cols-2 gap-4">
        <Field label="First name" value={firstName} onChange={setFirstName} />
        <Field label="Last name" value={lastName} onChange={setLastName} />
      </div>
      <Field label="Credentials" value={credentials} onChange={setCredentials} placeholder="MD, FPCP" required={false} />

      <div>
        <span className="block text-xs font-medium text-ink/60 mb-1.5">Specialties</span>
        {specialties.length === 0 ? (
          <p className="text-xs text-ink/40">No specialties configured yet.</p>
        ) : (
          <div className="flex flex-wrap gap-1.5">
            {specialties.map((s) => (
              <button
                key={s.id}
                type="button"
                onClick={() => toggleSpecialty(s.id)}
                className={`btn-press rounded-full px-3 py-1.5 text-xs font-medium border focus-ring transition-colors ${
                  selectedSpecialties.has(s.id) ? 'bg-teal text-white border-teal' : 'border-line text-ink/70 hover:border-teal'
                }`}
              >
                {s.name}
              </button>
            ))}
          </div>
        )}
      </div>

      <label className="block">
        <span className="block text-xs font-medium text-ink/60 mb-1.5">Clinic</span>
        <select value={clinicId} onChange={(e) => setClinicId(e.target.value)} required className="w-full rounded-lg border border-line bg-paper px-3.5 py-2.5 text-sm focus-ring">
          {clinics.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </label>

      <button type="submit" disabled={loading} className="btn-press rounded-lg bg-teal px-5 py-2.5 text-sm font-medium text-white hover:bg-teal-dark focus-ring disabled:opacity-50">
        {loading ? 'Saving…' : 'Create doctor'}
      </button>
    </form>
  );
}
