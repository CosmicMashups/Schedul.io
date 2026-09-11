import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, CalendarClock, UserCheck, ListOrdered, Users, Stethoscope,
  ClipboardList, CalendarRange, BarChart3, LogOut, LayoutGrid,
} from 'lucide-react';
import { clearSession } from '../api/client';

// Brand mark: a calendar with a pulse cross — replaces the placeholder Building2-as-logo.
function BrandMark({ size = 16 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="3" y="5" width="18" height="16" rx="4" stroke="currentColor" strokeWidth="2" />
      <path d="M8 3v4M16 3v4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M12 10.5v7M8.5 14h7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

const LINKS = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/calendar', label: 'Calendar', icon: LayoutGrid },
  { to: '/appointments', label: 'Appointments', icon: CalendarClock },
  { to: '/check-in', label: 'Check-in', icon: UserCheck },
  { to: '/queue', label: 'Queue', icon: ListOrdered },
  { to: '/patients', label: 'Patients', icon: Users },
  { to: '/practitioners', label: 'Doctors', icon: Stethoscope },
  { to: '/services', label: 'Services', icon: ClipboardList },
  { to: '/schedules', label: 'Schedules', icon: CalendarRange },
  { to: '/reports', label: 'Reports', icon: BarChart3 },
];

export function Sidebar() {
  const navigate = useNavigate();
  return (
    <aside className="w-56 shrink-0 border-r border-line bg-panel h-screen sticky top-0 flex flex-col">
      <div className="px-5 py-5 border-b border-line flex items-center gap-2.5">
        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-teal text-white">
          <BrandMark size={16} />
        </span>
        <div>
          <p className="text-[10px] font-mono uppercase tracking-widest text-teal leading-none mb-1">Staff console</p>
          <p className="font-medium text-ink text-sm leading-none">Schedul.io</p>
        </div>
      </div>
      <nav className="flex-1 py-3">
        {LINKS.map((link) => {
          const Icon = link.icon;
          return (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `flex items-center gap-2.5 px-5 py-2 text-sm font-medium focus-ring transition-colors ${
                  isActive ? 'text-teal bg-teal/5 border-r-2 border-teal' : 'text-ink/60 hover:text-ink hover:bg-paper'
                }`
              }
            >
              <Icon size={16} strokeWidth={1.9} className="icon-nudge" />
              {link.label}
            </NavLink>
          );
        })}
      </nav>
      <button
        onClick={() => { clearSession(); navigate('/login'); }}
        className="mx-5 mb-5 flex items-center gap-1.5 text-left text-xs text-ink/40 hover:text-coral focus-ring"
      >
        <LogOut size={13} /> Sign out
      </button>
    </aside>
  );
}
