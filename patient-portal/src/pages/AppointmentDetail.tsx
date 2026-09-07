import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { cancelAppointment, getAppointment, getAvailability, holdSlot, rescheduleAppointment } from '../api/booking';
import { ApiError } from '../api/client';
import type { AppointmentResponse, SlotResponse } from '../api/types';
import { StatusPill } from '../components/StatusPill';
import { SlotPicker } from '../components/SlotPicker';
import { SelectField } from '../components/Field';
import { useToast } from '../components/Toast';
import { CalendarClock, XCircle } from 'lucide-react';

const CANCEL_REASONS = [
  { value: 'PATIENT_REQUEST', label: "Can't make it" },
  { value: 'NO_LONGER_NEEDED', label: 'No longer needed' },
  { value: 'DUPLICATE', label: 'Booked by mistake / duplicate' },
  { value: 'OTHER', label: 'Other' },
];

const CANCELLABLE = new Set(['REQUESTED', 'PENDING_CONFIRMATION', 'CONFIRMED']);
const RESCHEDULABLE = new Set(['CONFIRMED']);

export default function AppointmentDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [appointment, setAppointment] = useState<AppointmentResponse | null>(null);
  const [mode, setMode] = useState<'view' | 'cancel' | 'reschedule'>('view');
  const [cancelReason, setCancelReason] = useState('');
  const [rescheduleSlots, setRescheduleSlots] = useState<SlotResponse[]>([]);
  const [selectedSlot, setSelectedSlot] = useState<SlotResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const toast = useToast();

  function load() {
    if (id) getAppointment(id).then(setAppointment).catch((err) => toast.show(err instanceof ApiError ? err.message : 'Not found.', 'error'));
  }
  useEffect(load, [id]);

  async function startReschedule() {
    if (!appointment) return;
    setMode('reschedule');
    const from = new Date().toISOString().slice(0, 10);
    const to = new Date(Date.now() + 14 * 86400_000).toISOString().slice(0, 10);
    const res = await getAvailability({ practitionerId: appointment.practitionerId, clinicId: appointment.clinicId, from, to });
    setRescheduleSlots(res.slots);
  }

  async function onConfirmCancel() {
    if (!id || !cancelReason) return;
    setBusy(true);
    try {
      await cancelAppointment(id, cancelReason);
      load();
      setMode('view');
      toast.show('Appointment cancelled.');
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not cancel this appointment.', 'error');
    } finally {
      setBusy(false);
    }
  }

  async function onSelectRescheduleSlot(slot: SlotResponse) {
    setSelectedSlot(slot);
    try {
      await holdSlot(slot.slotId);
    } catch {
      toast.show('That slot was just taken — please pick another.', 'error');
      setSelectedSlot(null);
    }
  }

  async function onConfirmReschedule() {
    if (!id || !selectedSlot) return;
    setBusy(true);
    try {
      const replacement = await rescheduleAppointment(id, selectedSlot.slotId);
      navigate(`/appointments/${replacement.id}/confirmed`);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not reschedule this appointment.', 'error');
    } finally {
      setBusy(false);
    }
  }

  if (!appointment) return <p className="text-sm text-ink/50">Loading…</p>;

  const start = new Date(appointment.scheduledStart);

  return (
    <div className="max-w-md animate-page">
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="font-display text-2xl text-ink mb-1">
            {start.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })}
          </h1>
          <p className="text-ink/60">{start.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}</p>
        </div>
        <StatusPill status={appointment.status} />
      </div>

      {appointment.reason && <p className="text-sm text-ink/60 mb-6 pb-6 border-b border-line">{appointment.reason}</p>}

      {mode === 'view' && (
        <div className="flex gap-3">
          {RESCHEDULABLE.has(appointment.status) && (
            <button onClick={startReschedule} className="btn-press flex items-center gap-1.5 rounded-full border border-line px-5 py-2.5 text-sm font-medium hover:border-teal focus-ring">
              <CalendarClock size={15} /> Reschedule
            </button>
          )}
          {CANCELLABLE.has(appointment.status) && (
            <button onClick={() => setMode('cancel')} className="btn-press flex items-center gap-1.5 rounded-full border border-line px-5 py-2.5 text-sm font-medium text-coral hover:border-coral focus-ring">
              <XCircle size={15} /> Cancel
            </button>
          )}
        </div>
      )}

      {mode === 'cancel' && (
        <div className="space-y-4">
          <SelectField label="Why are you cancelling?" value={cancelReason} onChange={setCancelReason} options={CANCEL_REASONS} />
          <div className="flex gap-3">
            <button
              onClick={onConfirmCancel}
              disabled={busy || !cancelReason}
              className="btn-press rounded-full bg-coral px-5 py-2.5 text-sm font-medium text-white hover:opacity-90 focus-ring disabled:opacity-50"
            >
              {busy ? 'Cancelling…' : 'Confirm cancellation'}
            </button>
            <button onClick={() => setMode('view')} className="rounded-full px-5 py-2.5 text-sm text-ink/60 hover:text-ink focus-ring">
              Never mind
            </button>
          </div>
        </div>
      )}

      {mode === 'reschedule' && (
        <div className="space-y-4">
          <SlotPicker slots={rescheduleSlots} selectedSlotId={selectedSlot?.slotId ?? null} onSelect={onSelectRescheduleSlot} />
          {selectedSlot && (
            <button
              onClick={onConfirmReschedule}
              disabled={busy}
              className="btn-press rounded-full bg-teal px-5 py-2.5 text-sm font-medium text-white hover:bg-teal-dark focus-ring disabled:opacity-50"
            >
              {busy ? 'Rescheduling…' : 'Confirm new time'}
            </button>
          )}
          <button onClick={() => setMode('view')} className="block text-sm text-ink/60 hover:text-ink focus-ring">
            Never mind
          </button>
        </div>
      )}

    </div>
  );
}
