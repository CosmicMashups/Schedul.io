import { useNavigate } from 'react-router-dom';
import { LogOut, Stethoscope } from 'lucide-react';
import { clearSession } from '../api/client';

export function Header() {
  const navigate = useNavigate();
  return (
    <header className="border-b border-slate-line">
      <div className="mx-auto max-w-2xl px-5 py-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="flex h-7 w-7 items-center justify-center rounded-full bg-teal/15 text-teal">
            <Stethoscope size={14} />
          </span>
          <p className="text-xs font-mono uppercase tracking-widest text-teal">Doctor Portal</p>
        </div>
        <button
          onClick={() => { clearSession(); navigate('/login'); }}
          className="flex items-center gap-1.5 text-xs text-ink/40 hover:text-coral focus-ring transition-colors"
        >
          <LogOut size={13} /> Sign out
        </button>
      </div>
    </header>
  );
}
