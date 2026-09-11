import { FormEvent, useEffect, useState } from 'react';
import { Clock3, ListOrdered, PhoneCall, PlusCircle, SkipForward, UserX2 } from 'lucide-react';
import { listClinics } from '../api/clinic';
import { searchPatients } from '../api/patients';
import {
  callNext, completeTicket, enqueueWalkIn, leaveWithoutBeingSeen, listActiveTickets, listQueues, skipTicket, startServing,
} from '../api/queue';
import { ApiError } from '../api/client';
import type { ClinicResponse, PatientResponse, QueueResponse, QueueTicketResponse } from '../api/types';
import { EmptyState } from '../components/EmptyState';
import { Skeleton } from '../components/Skeleton';
import { useToast } from '../components/Toast';
import { Spinner } from '../components/Spinner';

export default function Queue() {
  const [clinics, setClinics] = useState<ClinicResponse[]>([]);
  const [clinicId, setClinicId] = useState('');
  const [queue, setQueue] = useState<QueueResponse | null>(null);
  const [tickets, setTickets] = useState<QueueTicketResponse[]>([]);
  // Fixed: without a loading flag, "No queue yet for this clinic" briefly flashed on every
  // clinic switch, before that clinic's queue had a chance to load.
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [showWalkIn, setShowWalkIn] = useState(false);
  const toast = useToast();

  useEffect(() => {
    listClinics().then((cs) => {
      setClinics(cs);
      if (cs[0]) setClinicId(cs[0].id);
    }).catch(() => {});
  }, []);

  async function loadQueue(cId: string) {
    setLoading(true);
    try {
      const queues = await listQueues(cId);
      const doctorQueue = queues.find((q) => q.type === 'DOCTOR') ?? null;
      setQueue(doctorQueue);
      setTickets(doctorQueue ? await listActiveTickets(doctorQueue.id) : []);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not load the queue.', 'error');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { if (clinicId) loadQueue(clinicId); }, [clinicId]);

  const nowServing = tickets.find((t) => t.status === 'SERVING');
  const waiting = tickets.filter((t) => t.status === 'WAITING' || t.status === 'CALLED');

  async function run(action: () => Promise<unknown>, successMessage?: string) {
    setBusy(true);
    try {
      await action();
      if (clinicId) await loadQueue(clinicId);
      if (successMessage) toast.show(successMessage);
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Action failed.', 'error');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="animate-page">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <ListOrdered size={20} className="text-teal" />
          <h1 className="text-xl font-semibold text-ink">Doctor queue</h1>
        </div>
        <div className="flex items-center gap-2">
          <select
            value={clinicId}
            onChange={(e) => setClinicId(e.target.value)}
            className="rounded-lg border border-line bg-panel px-3 py-2 text-sm focus-ring"
          >
            {clinics.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <button
            onClick={() => setShowWalkIn((v) => !v)}
            className="btn-press flex items-center gap-1.5 rounded-lg border border-line px-3 py-2 text-sm font-medium hover:border-teal focus-ring"
          >
            <PlusCircle size={15} /> Walk-in
          </button>
        </div>
      </div>

      {showWalkIn && queue && (
        <WalkInForm queueId={queue.id} onDone={() => { setShowWalkIn(false); loadQueue(clinicId); toast.show('Walk-in added to the queue.'); }} />
      )}

      {/* Only show the full skeleton on the first load (or a clinic switch, which also clears
          `queue`) — action buttons (call next, complete, skip…) also trigger loadQueue to
          refresh, and swapping the whole panel out for a skeleton on every click would be a
          worse regression than the flash this fixes. Those already have their own `busy` state. */}
      {loading && !queue && (
        <div className="space-y-6">
          <Skeleton className="h-28 rounded-card" />
          <Skeleton className="h-40 rounded-card" />
        </div>
      )}

      {!loading && !queue && <div className="panel"><EmptyState icon={ListOrdered} title="No queue yet for this clinic" description="It's created automatically on first check-in." /></div>}

      {queue && (
        <>
          <div className="now-serving rounded-card p-6 mb-6 text-white flex items-center justify-between">
            <div>
              <p className="text-xs uppercase tracking-widest opacity-70 mb-1">Now serving</p>
              <p className="font-mono text-4xl font-semibold tabular-nums animate-count-pop">{nowServing?.ticketNumber ?? '—'}</p>
              {nowServing?.servingStartedAt && <ElapsedTime since={nowServing.servingStartedAt} />}
            </div>
            {nowServing ? (
              <button
                onClick={() => run(() => completeTicket(nowServing.id), 'Visit completed.')}
                disabled={busy}
                className="btn-press flex items-center gap-2 rounded-full bg-white/15 hover:bg-white/25 px-5 py-2.5 text-sm font-medium focus-ring disabled:opacity-50"
              >
                {busy && <Spinner size={14} />}
                Complete visit
              </button>
            ) : (
              <button
                onClick={() => run(() => callNext(queue.id), 'Next patient called.')}
                disabled={busy || waiting.length === 0}
                className="btn-press flex items-center gap-2 rounded-full bg-white/15 hover:bg-white/25 px-5 py-2.5 text-sm font-medium focus-ring disabled:opacity-50"
              >
                {busy ? <Spinner size={15} /> : <PhoneCall size={15} />} Call next
              </button>
            )}
          </div>

          <h2 className="text-xs font-medium text-ink/50 uppercase tracking-wide mb-3">Waiting ({waiting.length})</h2>
          {waiting.length === 0 ? (
            <div className="panel"><EmptyState icon={ListOrdered} title="Nobody waiting" /></div>
          ) : (
            <div className="panel divide-y divide-line/60">
              {waiting.map((t) => (
                <div key={t.id} className="flex items-center justify-between px-4 py-3">
                  <div className="flex items-center gap-3">
                    <span className="font-mono text-lg text-ink tabular-nums">{t.ticketNumber}</span>
                    {t.status === 'CALLED' && <span className="text-xs text-amber font-medium bg-amber/10 px-2 py-0.5 rounded-full">Called</span>}
                  </div>
                  <div className="flex gap-1.5">
                    {t.status === 'CALLED' && (
                      <TicketAction icon={PhoneCall} label="Start serving" tone="teal" onClick={() => run(() => startServing(t.id))} />
                    )}
                    <TicketAction icon={SkipForward} label="Skip" tone="amber" onClick={() => run(() => skipTicket(t.id))} />
                    <TicketAction icon={UserX2} label="Left" tone="coral" onClick={() => run(() => leaveWithoutBeingSeen(t.id))} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}

/** Live elapsed time since a ticket started being served — updates every 15s. Gives staff a
    real sense of how long the current consultation has run, instead of a static ticket number
    with no time context. */
function ElapsedTime({ since }: { since: string }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 15_000);
    return () => clearInterval(id);
  }, []);

  const minutes = Math.max(0, Math.floor((now - new Date(since).getTime()) / 60_000));
  return (
    <p className="flex items-center gap-1 text-xs opacity-70 mt-1">
      <Clock3 size={11} />
      <span className="tabular-nums">{minutes} min</span>
    </p>
  );
}

function TicketAction({
  icon: Icon, label, onClick, tone,
}: { icon: typeof PhoneCall; label: string; onClick: () => void; tone: 'teal' | 'coral' | 'amber' }) {
  const toneClass = { teal: 'text-teal hover:bg-teal/10', coral: 'text-coral hover:bg-coral/10', amber: 'text-amber hover:bg-amber/10' }[tone];
  return (
    <button onClick={onClick} className={`btn-press flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium focus-ring ${toneClass}`}>
      <Icon size={12} /> {label}
    </button>
  );
}

function WalkInForm({ queueId, onDone }: { queueId: string; onDone: () => void }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<PatientResponse[]>([]);
  const [selected, setSelected] = useState<PatientResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  async function search(e: FormEvent) {
    e.preventDefault();
    try {
      setResults(await searchPatients(query));
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Search failed.', 'error');
    }
  }

  async function addToQueue() {
    if (!selected) return;
    setLoading(true);
    try {
      await enqueueWalkIn(queueId, selected.id);
      onDone();
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not add walk-in.', 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="panel p-4 mb-6 animate-scale-in">
      <p className="text-sm font-medium text-ink mb-3">Add a walk-in patient</p>
      <form onSubmit={search} className="flex gap-2 mb-3">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by name, mobile, or email…"
          className="flex-1 rounded-lg border border-line bg-paper px-3.5 py-2 text-sm focus-ring"
        />
        <button type="submit" className="btn-press rounded-lg bg-teal px-4 py-2 text-sm text-white font-medium hover:bg-teal-dark focus-ring">
          Search
        </button>
      </form>

      {results.length > 0 && (
        <div className="border border-line rounded-lg divide-y divide-line/60 mb-3 max-h-40 overflow-y-auto">
          {results.map((p) => (
            <button
              key={p.id}
              onClick={() => setSelected(p)}
              className={`w-full text-left px-3 py-2 text-sm hover:bg-paper focus-ring ${selected?.id === p.id ? 'bg-teal/5 text-teal-dark font-medium' : ''}`}
            >
              {p.firstName} {p.lastName} <span className="text-ink/40 text-xs">{p.mobileNumber ?? p.email}</span>
            </button>
          ))}
        </div>
      )}

      {selected && (
        <button
          onClick={addToQueue}
          disabled={loading}
          className="btn-press rounded-lg bg-teal px-4 py-2 text-sm text-white font-medium hover:bg-teal-dark focus-ring disabled:opacity-50"
        >
          {loading ? 'Adding…' : `Add ${selected.firstName} to queue`}
        </button>
      )}
    </div>
  );
}
