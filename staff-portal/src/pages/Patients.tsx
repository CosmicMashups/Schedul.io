import { FormEvent, useEffect, useState } from 'react';
import { Bell, Search, UserPlus, Users } from 'lucide-react';
import { registerPatient, searchPatients } from '../api/patients';
import { getNotificationHistory } from '../api/notifications';
import { ApiError } from '../api/client';
import type { NotificationResponse, PatientResponse } from '../api/types';
import { Field, SelectField } from '../components/Field';
import { EmptyState } from '../components/EmptyState';
import { Skeleton } from '../components/Skeleton';
import { useToast } from '../components/Toast';

export default function Patients() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<PatientResponse[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [historyFor, setHistoryFor] = useState<PatientResponse | null>(null);
  const [searched, setSearched] = useState(false);
  // Fixed: without a loading flag, clicking Search gave no feedback at all until the results
  // (or the "No matches" empty state) arrived — a real gap on a slow connection.
  const [searching, setSearching] = useState(false);
  const toast = useToast();

  async function search(e?: FormEvent) {
    e?.preventDefault();
    setSearching(true);
    try {
      setResults(await searchPatients(query));
      setSearched(true);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Search failed.', 'error');
    } finally {
      setSearching(false);
    }
  }

  return (
    <div className="max-w-2xl animate-page">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Users size={20} className="text-teal" />
          <h1 className="text-xl font-semibold text-ink">Patients</h1>
        </div>
        <button onClick={() => setShowForm((v) => !v)} className="btn-press flex items-center gap-1.5 rounded-lg border border-line px-3.5 py-2 text-sm font-medium hover:border-teal focus-ring">
          <UserPlus size={15} /> {showForm ? 'Cancel' : 'New patient'}
        </button>
      </div>

      {showForm ? (
        <RegisterForm onDone={() => { setShowForm(false); search(); toast.show('Patient registered.'); }} />
      ) : historyFor ? (
        <NotificationHistory patient={historyFor} onBack={() => setHistoryFor(null)} />
      ) : (
        <>
          <form onSubmit={search} className="relative mb-6">
            <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-ink/30" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Name, mobile number, or email…"
              className="w-full rounded-lg border border-line bg-panel pl-10 pr-24 py-2.5 text-sm focus-ring"
            />
            <button type="submit" disabled={searching} className="btn-press absolute right-1.5 top-1/2 -translate-y-1/2 rounded-md bg-teal px-3.5 py-1.5 text-xs text-white font-medium hover:bg-teal-dark focus-ring disabled:opacity-50">
              {searching ? 'Searching…' : 'Search'}
            </button>
          </form>

          {searching && (
            <div className="panel p-4 space-y-3">
              {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-11" />)}
            </div>
          )}
          {!searching && !searched && <div className="panel"><EmptyState icon={Search} title="Search to find a patient" /></div>}
          {!searching && searched && results.length === 0 && <div className="panel"><EmptyState icon={Users} title="No matches" /></div>}

          {!searching && results.length > 0 && (
            <div className="panel divide-y divide-line/60">
              {results.map((p) => (
                <div key={p.id} className="px-4 py-3 flex items-center justify-between">
                  <div>
                    <p className="font-medium text-ink">{p.firstName} {p.lastName}</p>
                    <p className="text-xs text-ink/50 tabular-nums">{p.mobileNumber ?? p.email ?? '—'} · DOB {new Date(p.birthDate).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}</p>
                  </div>
                  <button
                    onClick={() => setHistoryFor(p)}
                    className="btn-press flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs font-medium text-teal hover:bg-teal/10 focus-ring"
                  >
                    <Bell size={13} /> Notifications
                  </button>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}

function NotificationHistory({ patient, onBack }: { patient: PatientResponse; onBack: () => void }) {
  const [notifications, setNotifications] = useState<NotificationResponse[] | null>(null);
  const toast = useToast();

  useEffect(() => {
    getNotificationHistory(patient.id)
      .then(setNotifications)
      .catch((err) => toast.show(err instanceof ApiError ? err.message : 'Could not load notification history.', 'error'));
  }, [patient.id]);

  const STATUS_TONE: Record<string, string> = {
    SENT: 'text-teal bg-teal/10', FAILED: 'text-coral bg-coral/10',
    SKIPPED_NO_CONTACT: 'text-amber bg-amber/10', SKIPPED_NO_TEMPLATE: 'text-amber bg-amber/10',
  };

  return (
    <div>
      <button onClick={onBack} className="text-sm text-teal hover:text-teal-dark focus-ring mb-4">← Back to search</button>
      <p className="text-sm text-ink/60 mb-4">Notification history for <span className="font-medium text-ink">{patient.firstName} {patient.lastName}</span></p>

      {notifications === null && (
        <div className="panel divide-y divide-line/60">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="px-4 py-3 space-y-2">
              <Skeleton className="h-3.5 w-1/3" />
              <Skeleton className="h-3 w-2/3" />
            </div>
          ))}
        </div>
      )}
      {notifications && notifications.length === 0 && <div className="panel"><EmptyState icon={Bell} title="No notifications sent yet" /></div>}

      {notifications && notifications.length > 0 && (
        <div className="panel divide-y divide-line/60">
          {notifications.map((n) => (
            <div key={n.id} className="px-4 py-3">
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs font-mono uppercase text-ink/50">{n.eventCode.replace(/_/g, ' ')}</span>
                <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${STATUS_TONE[n.status] ?? 'text-ink/50 bg-ink/5'}`}>{n.status}</span>
              </div>
              <p className="text-sm text-ink/70">{n.renderedBody || <span className="text-ink/30 italic">No message rendered</span>}</p>
              <p className="text-xs text-ink/40 mt-1">{n.channel} · {n.recipientContact ?? 'no contact on file'}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function RegisterForm({ onDone }: { onDone: () => void }) {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [sex, setSex] = useState('');
  const [mobileNumber, setMobileNumber] = useState('');
  const [source, setSource] = useState('FRONT_DESK');
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      await registerPatient({
        firstName, lastName, birthDate, sex: sex as 'MALE' | 'FEMALE',
        mobileNumber: mobileNumber || undefined,
        registrationSource: source as 'FRONT_DESK' | 'PHONE' | 'WALK_IN' | 'STAFF',
        privacyNoticeVersion: 'v1',
        dataProcessingConsentGranted: true,
      });
      onDone();
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Registration failed.', 'error');
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
      <div className="grid grid-cols-2 gap-4">
        <Field label="Date of birth" type="date" value={birthDate} onChange={setBirthDate} />
        <SelectField label="Sex" value={sex} onChange={setSex} options={[{ value: 'MALE', label: 'Male' }, { value: 'FEMALE', label: 'Female' }]} />
      </div>
      <Field label="Mobile number" value={mobileNumber} onChange={setMobileNumber} required={false} />
      <SelectField
        label="How did they register?"
        value={source}
        onChange={setSource}
        options={[
          { value: 'FRONT_DESK', label: 'Front desk' },
          { value: 'PHONE', label: 'Phone' },
          { value: 'WALK_IN', label: 'Walk-in' },
        ]}
      />
      <button type="submit" disabled={loading} className="btn-press rounded-lg bg-teal px-5 py-2.5 text-sm font-medium text-white hover:bg-teal-dark focus-ring disabled:opacity-50">
        {loading ? 'Saving…' : 'Register patient'}
      </button>
    </form>
  );
}
