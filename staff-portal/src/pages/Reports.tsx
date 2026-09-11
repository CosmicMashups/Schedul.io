import { useEffect, useState } from 'react';
import { BarChart3, Clock3, Gauge } from 'lucide-react';
import { getReportSummary } from '../api/reporting';
import { ApiError } from '../api/client';
import type { ReportSummaryResponse } from '../api/types';
import { Field } from '../components/Field';
import { Skeleton } from '../components/Skeleton';
import { useToast } from '../components/Toast';
import { useCountUp } from '../hooks/useCountUp';

function daysAgo(n: number) {
  return new Date(Date.now() - n * 86400_000).toISOString().slice(0, 10);
}

export default function Reports() {
  const [from, setFrom] = useState(daysAgo(30));
  const [to, setTo] = useState(daysAgo(0));
  const [summary, setSummary] = useState<ReportSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const toast = useToast();

  async function load() {
    setLoading(true);
    try {
      setSummary(await getReportSummary({ from, to }));
    } catch (err) {
      toast.show(err instanceof ApiError ? err.message : 'Could not load report.', 'error');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  return (
    <div className="max-w-3xl animate-page">
      <div className="flex items-center gap-2 mb-6">
        <BarChart3 size={20} className="text-teal" />
        <h1 className="text-xl font-semibold text-ink">Reports</h1>
      </div>

      <form onSubmit={(e) => { e.preventDefault(); load(); }} className="panel p-4 flex items-end gap-3 mb-6">
        <div className="w-40"><Field label="From" type="date" value={from} onChange={setFrom} /></div>
        <div className="w-40"><Field label="To" type="date" value={to} onChange={setTo} /></div>
        <button type="submit" className="btn-press rounded-lg bg-teal px-4 py-2.5 text-sm text-white font-medium hover:bg-teal-dark focus-ring">
          Update
        </button>
      </form>

      {loading && !summary && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-20 rounded-card" />)}
        </div>
      )}

      {summary && (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
            <Metric label="Total appointments" value={summary.totalAppointments} />
            <Metric label="Confirmed" value={summary.confirmedCount} />
            <Metric label="Cancelled" value={summary.cancelledCount} />
            <Metric label="No-shows" value={summary.noShowCount} />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
            <RatePanel label="Confirmation rate" value={summary.confirmationRatePct} />
            <RatePanel label="Cancellation rate" value={summary.cancellationRatePct} />
            <RatePanel label="No-show rate" value={summary.noShowRatePct} />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="panel p-5 flex items-center gap-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-teal/10 text-teal shrink-0">
                <Clock3 size={18} />
              </div>
              <div>
                <p className="text-xs text-ink/50">Average booking lead time</p>
                <p className="text-lg font-semibold text-ink font-mono">
                  {summary.averageBookingLeadTimeHours != null ? `${summary.averageBookingLeadTimeHours.toFixed(1)}h` : '—'}
                </p>
              </div>
            </div>
            <div className="panel p-5 flex items-center gap-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-teal/10 text-teal shrink-0">
                <Gauge size={18} />
              </div>
              <div>
                <p className="text-xs text-ink/50">Average wait time (check-in → called)</p>
                <p className="text-lg font-semibold text-ink font-mono">
                  {summary.averageWaitTimeMinutes != null ? `${summary.averageWaitTimeMinutes.toFixed(0)} min` : '—'}
                </p>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  const displayValue = useCountUp(value);
  return (
    <div className="panel p-4">
      <p className="text-2xl font-semibold text-ink font-mono tabular-nums">{displayValue}</p>
      <p className="text-xs text-ink/50 mt-0.5">{label}</p>
    </div>
  );
}

function RatePanel({ label, value }: { label: string; value: number }) {
  return (
    <div className="panel p-4">
      <p className="text-xs text-ink/50 mb-2">{label}</p>
      <div className="flex items-center gap-2">
        <div className="flex-1 h-1.5 rounded-full bg-line overflow-hidden">
          <div className="h-full bg-teal rounded-full transition-all duration-500" style={{ width: `${Math.min(value, 100)}%` }} />
        </div>
        <span className="text-sm font-mono text-ink/70 w-12 text-right">{value.toFixed(1)}%</span>
      </div>
    </div>
  );
}
