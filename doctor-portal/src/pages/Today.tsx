import { useCallback, useEffect, useState } from 'react';
import { CalendarDays, CheckCircle2, PhoneCall, SkipForward, Stethoscope } from 'lucide-react';
import { getMyProfile } from '../api/practitioner';
import { searchMyAppointments } from '../api/appointments';
import { getPatient } from '../api/patients';
import { listActiveTickets, listQueues, callNext, startServing, completeTicket, skipTicket } from '../api/queue';
import { ApiError } from '../api/client';
import type { AppointmentResponse, PatientResponse, PractitionerResponse, QueueTicketResponse } from '../api/types';
import { StatusPill } from '../components/StatusPill';
import { Skeleton } from '../components/Skeleton';
import { EmptyState } from '../components/EmptyState';
import { Spinner } from '../components/Spinner';
import { useToast } from '../components/Toast';

const patientCache = new Map<string, PatientResponse>();
async function getCachedPatient(id: string): Promise<PatientResponse> {
  if (!patientCache.has(id)) patientCache.set(id, await getPatient(id));
  return patientCache.get(id)!;
}

export default function Today() {
  const [profile, setProfile] = useState<PractitionerResponse | null>(null);
  const [queueId, setQueueId] = useState<string | null>(null);
  const [tickets, setTickets] = useState<QueueTicketResponse[]>([]);
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [names, setNames] = useState<Map<string, string>>(new Map());
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const toast = useToast();

  const load = useCallback(async (currentProfile: PractitionerResponse) => {
    const clinicId = currentProfile.clinicIds[0];
    const queues = await listQueues(clinicId);
    const doctorQueue = queues.find((q) => q.type === 'DOCTOR') ?? null;
    setQueueId(doctorQueue?.id ?? null);

    // Simplification, documented: a ticket is treated as "mine" if it's explicitly assigned
    // to me, OR unassigned (common for walk-ins added without a specific doctor — see the
    // staff console's Queue.tsx walk-in form). In a genuinely multi-doctor clinic sharing one
    // DOCTOR queue, unassigned tickets would need an explicit routing step before landing
    // here; single-doctor-per-clinic (the common case for this demo data) makes that
    // simplification safe.
    const allTickets = doctorQueue ? await listActiveTickets(doctorQueue.id) : [];
    const myTickets = allTickets.filter((t) => t.practitionerId === currentProfile.id || t.practitionerId === null);
    setTickets(myTickets);

    const todayStart = new Date(); todayStart.setHours(0, 0, 0, 0);
    const todayEnd = new Date(); todayEnd.setHours(23, 59, 59, 999);
    const todaysAppointments = await searchMyAppointments(currentProfile.id, todayStart.toISOString(), todayEnd.toISOString());
    setAppointments(todaysAppointments.sort((a, b) => a.scheduledStart.localeCompare(b.scheduledStart)));

    const patientIds = new Set([...myTickets.map((t) => t.patientId), ...todaysAppointments.map((a) => a.patientId)]);
    const nameMap = new Map<string, string>();
    await Promise.all(Array.from(patientIds).map(async (id) => {
      try {
        const p = await getCachedPatient(id);
        nameMap.set(id, `${p.firstName} ${p.lastName}`);
      } catch { /* name unavailable — falls back to a placeholder below */ }
    }));
    setNames(nameMap);
  }, []);

  useEffect(() => {
    getMyProfile()
      .then(async (p) => { setProfile(p); await load(p); })
      .catch((err) => toast.show(err instanceof ApiError ? err.message : 'Could not load your profile.', 'error'))
      .finally(() => setLoading(false));
  }, [load]);

  async function run(action: () => Promise<unknown>, successMessage?: string) {
    if (!profile) return;
    setBusy(true);
    try {
      await action();
      await load(profile);
      if (successMessage) toast.show(successMessage);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Action failed.', 'error');
    } finally {
      setBusy(false);
    }
  }

  const nowServing = tickets.find((t) => t.status === 'SERVING');
  const waiting = tickets.filter((t) => t.status === 'WAITING' || t.status === 'CALLED');
  const upNext = waiting[0];

  if (loading) {
    return (
      <div className="animate-page space-y-4">
        <Skeleton className="h-32 rounded-card" />
        <Skeleton className="h-24 rounded-card" />
      </div>
    );
  }

  if (!profile) {
    return <EmptyState icon={Stethoscope} title="No practitioner record linked to this account" description="Ask your clinic admin to link your login to your doctor profile." />;
  }

  return (
    <div className="animate-page">
      <div className="mb-6">
        <p className="text-sm text-ink/50">{new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })}</p>
        <h1 className="font-display text-2xl text-ink">Good day, Dr. {profile.lastName}.</h1>
      </div>

      <div className="panel p-6 mb-4 relative overflow-hidden">
        {nowServing ? (
          <div className="animate-scale-in">
            <p className="text-xs uppercase tracking-widest text-teal mb-2">Now with you</p>
            <div className="flex items-center justify-between">
              <div>
                <p className="font-display text-2xl text-ink">{names.get(nowServing.patientId) ?? 'Patient'}</p>
                <p className="font-mono text-sm text-ink/40 mt-0.5">{nowServing.ticketNumber}</p>
              </div>
              <button
                onClick={() => run(() => completeTicket(nowServing.id), 'Visit completed.')}
                disabled={busy}
                className="btn-press flex items-center gap-2 rounded-full bg-teal px-5 py-2.5 text-sm font-semibold text-slate hover:bg-teal-light focus-ring disabled:opacity-50"
              >
                {busy ? <Spinner size={15} /> : <CheckCircle2 size={16} />}
                Complete visit
              </button>
            </div>
          </div>
        ) : upNext ? (
          <div className="animate-scale-in">
            <p className="text-xs uppercase tracking-widest text-ink/40 mb-2">Up next</p>
            <div className="flex items-center justify-between">
              <div className="relative">
                {upNext.status === 'WAITING' && <span className="pulse-ring absolute -inset-2 rounded-full text-teal" />}
                <p className="font-display text-2xl text-ink relative">{names.get(upNext.patientId) ?? 'Patient'}</p>
                <p className="font-mono text-sm text-ink/40 mt-0.5 relative">{upNext.ticketNumber}</p>
              </div>
              {upNext.status === 'WAITING' ? (
                <button
                  onClick={() => run(() => queueId && callNext(queueId), 'Patient called.')}
                  disabled={busy || !queueId}
                  className="btn-press flex items-center gap-2 rounded-full bg-teal px-5 py-2.5 text-sm font-semibold text-slate hover:bg-teal-light focus-ring disabled:opacity-50"
                >
                  {busy ? <Spinner size={15} /> : <PhoneCall size={16} />}
                  Call in
                </button>
              ) : (
                <div className="flex gap-2">
                  <button
                    onClick={() => run(() => skipTicket(upNext.id))}
                    disabled={busy}
                    className="btn-press flex items-center gap-1.5 rounded-full border border-slate-line px-3.5 py-2.5 text-xs font-medium text-amber hover:border-amber focus-ring"
                  >
                    <SkipForward size={14} /> Skip
                  </button>
                  <button
                    onClick={() => run(() => startServing(upNext.id), 'Consultation started.')}
                    disabled={busy}
                    className="btn-press flex items-center gap-2 rounded-full bg-teal px-5 py-2.5 text-sm font-semibold text-slate hover:bg-teal-light focus-ring disabled:opacity-50"
                  >
                    {busy ? <Spinner size={15} /> : <Stethoscope size={16} />}
                    Start visit
                  </button>
                </div>
              )}
            </div>
          </div>
        ) : (
          <EmptyState icon={Stethoscope} title="No one waiting" description="Your queue is clear for now." />
        )}
      </div>

      {waiting.length > 1 && (
        <p className="text-xs text-ink/40 mb-6 px-1">+{waiting.length - 1} more waiting</p>
      )}

      <div className="flex items-center gap-2 mb-3 mt-8">
        <CalendarDays size={15} className="text-ink/40" />
        <h2 className="text-xs font-medium text-ink/50 uppercase tracking-wide">Today's schedule</h2>
      </div>

      {appointments.length === 0 ? (
        <div className="panel"><EmptyState icon={CalendarDays} title="No appointments scheduled today" /></div>
      ) : (
        <div className="panel divide-y divide-slate-line">
          {appointments.map((a) => (
            <div key={a.id} className="px-4 py-3 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span className="font-mono text-xs text-ink/40 w-14">
                  {new Date(a.scheduledStart).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}
                </span>
                <span className="text-sm text-ink">{names.get(a.patientId) ?? 'Patient'}</span>
              </div>
              <StatusPill status={a.status} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
