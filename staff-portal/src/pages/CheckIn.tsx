import { FormEvent, useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { ScanLine, Ticket, UserCheck } from 'lucide-react';
import { checkIn } from '../api/checkin';
import { ApiError } from '../api/client';
import type { CheckInResponse } from '../api/types';
import { useToast } from '../components/Toast';
import { Spinner } from '../components/Spinner';

export default function CheckIn() {
  const location = useLocation();
  const [appointmentId, setAppointmentId] = useState('');
  const [identityVerified, setIdentityVerified] = useState(true);
  const [result, setResult] = useState<CheckInResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  // Closes a previously-flagged gap: arriving here via the "Check in" action on the
  // Appointments list now pre-fills the id instead of requiring a manual paste.
  useEffect(() => {
    const state = location.state as { appointmentId?: string } | null;
    if (state?.appointmentId) setAppointmentId(state.appointmentId);
  }, [location.state]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setResult(null);
    setLoading(true);
    try {
      setResult(await checkIn(appointmentId.trim(), 'RECEPTIONIST', identityVerified));
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Check-in failed.', 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-md animate-page">
      <div className="flex items-center gap-2 mb-1">
        <UserCheck size={20} className="text-teal" />
        <h1 className="text-xl font-semibold text-ink">Check-in</h1>
      </div>
      <p className="text-sm text-ink/60 mb-6">
        Confirm the patient has arrived and send them to the doctor queue.
      </p>

      <form onSubmit={onSubmit} className="panel p-5 space-y-4">
        <label className="block">
          <span className="block text-xs font-medium text-ink/60 mb-1.5">Appointment ID</span>
          <div className="relative">
            <ScanLine size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink/30" />
            <input
              value={appointmentId}
              onChange={(e) => setAppointmentId(e.target.value)}
              required
              className="w-full rounded-lg border border-line bg-paper pl-9 pr-3.5 py-2.5 text-sm font-mono focus-ring"
              placeholder="UUID"
            />
          </div>
        </label>

        <label className="flex items-center gap-2 text-sm text-ink/70">
          <input type="checkbox" checked={identityVerified} onChange={(e) => setIdentityVerified(e.target.checked)} className="focus-ring" />
          Identity verified (ID checked at the counter)
        </label>

        <button
          type="submit"
          disabled={loading}
          className="btn-press flex items-center gap-2 rounded-lg bg-teal px-5 py-2.5 text-sm font-medium text-white hover:bg-teal-dark focus-ring disabled:opacity-50"
        >
          {loading ? <Spinner size={15} /> : <UserCheck size={15} />}
          {loading ? 'Checking in…' : 'Check in'}
        </button>
      </form>

      {result && (
        <div className="panel p-5 mt-4 flex items-center justify-between animate-scale-in">
          <div className="flex items-center gap-3">
            <div className="relative flex h-10 w-10 items-center justify-center rounded-full bg-teal/10 text-teal">
              <span className="pulse-ring absolute inset-0 rounded-full text-teal" />
              <Ticket size={18} className="relative" />
            </div>
            <div>
              <p className="text-xs text-ink/50">Queued</p>
              <p className="text-sm text-ink">Sent to the doctor queue</p>
            </div>
          </div>
          <p className="font-mono text-2xl text-teal">{result.ticketNumber}</p>
        </div>
      )}
    </div>
  );
}
