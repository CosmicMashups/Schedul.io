import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  CalendarClock, CheckCircle2, Clock, LayoutGrid, ListOrdered, TrendingDown, TrendingUp, Users, XCircle,
} from 'lucide-react';
import { getReportSummary } from '../api/reporting';
import { ApiError } from '../api/client';
import type { ReportSummaryResponse } from '../api/types';
import { Skeleton } from '../components/Skeleton';
import { useToast } from '../components/Toast';
import { useCountUp } from '../hooks/useCountUp';

export default function Dashboard() {
  const [summary, setSummary] = useState<ReportSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const toast = useToast();

  useEffect(() => {
    const today = new Date().toISOString().slice(0, 10);
    getReportSummary({ from: today, to: today })
      .then(setSummary)
      .catch((err) => toast.show(err instanceof ApiError ? err.message : 'Could not load today\u2019s summary.', 'error'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="animate-page">
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-ink">Today</h1>
        <p className="text-sm text-ink/50">{new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })}</p>
      </div>

      {loading ? (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-20 rounded-card" />)}
        </div>
      ) : summary ? (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          <StatCard icon={CalendarClock} label="Appointments" value={summary.totalAppointments} tone="teal" />
          <StatCard icon={CheckCircle2} label="Completed" value={summary.completedCount} tone="teal" />
          <StatCard icon={XCircle} label="Cancelled" value={summary.cancelledCount} tone="coral" />
          <StatCard icon={Clock} label="No-shows" value={summary.noShowCount} tone="amber" />
        </div>
      ) : null}

      {summary && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-6">
          <RateCard label="Confirmation rate" value={summary.confirmationRatePct} good />
          <RateCard label="Cancellation rate" value={summary.cancellationRatePct} good={false} />
          <RateCard label="No-show rate" value={summary.noShowRatePct} good={false} />
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <QuickLink to="/queue" icon={ListOrdered} title="Doctor queue" desc="Call next, serve, and complete visits." />
        <QuickLink to="/check-in" icon={Users} title="Check-in" desc="Bring a confirmed patient into the queue." />
        <QuickLink to="/calendar" icon={LayoutGrid} title="Calendar" desc="Week and month views across doctors." />
      </div>
    </div>
  );
}

function StatCard({ icon: Icon, label, value, tone }: { icon: typeof CalendarClock; label: string; value: number; tone: 'teal' | 'coral' | 'amber' }) {
  const toneClasses = {
    teal: 'bg-teal/10 text-teal',
    coral: 'bg-coral/10 text-coral',
    amber: 'bg-amber/10 text-amber',
  }[tone];
  const displayValue = useCountUp(value);
  return (
    <div className="panel p-4 card-interactive">
      <div className={`mb-3 flex h-8 w-8 items-center justify-center rounded-lg ${toneClasses}`}>
        <Icon size={16} strokeWidth={1.9} />
      </div>
      <p className="text-2xl font-semibold text-ink font-mono tabular-nums">{displayValue}</p>
      <p className="text-xs text-ink/50 mt-0.5">{label}</p>
    </div>
  );
}

function RateCard({ label, value, good }: { label: string; value: number; good: boolean }) {
  const isPositive = good ? value >= 70 : value <= 15;
  return (
    <div className="panel p-4 flex items-center justify-between">
      <div>
        <p className="text-xs text-ink/50 mb-1">{label}</p>
        <p className="text-lg font-semibold text-ink font-mono">{value.toFixed(1)}%</p>
      </div>
      {isPositive ? <TrendingUp size={18} className="text-teal" /> : <TrendingDown size={18} className="text-amber" />}
    </div>
  );
}

function QuickLink({ to, icon: Icon, title, desc }: { to: string; icon: typeof ListOrdered; title: string; desc: string }) {
  return (
    <Link to={to} className="panel card-interactive p-5 flex items-start gap-4 focus-ring">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-teal/10 text-teal">
        <Icon size={18} />
      </div>
      <div>
        <p className="font-medium text-ink">{title}</p>
        <p className="text-sm text-ink/50 mt-0.5">{desc}</p>
      </div>
    </Link>
  );
}
