import { createContext, useCallback, useContext, useState, ReactNode } from 'react';
import { CheckCircle2, XCircle, X } from 'lucide-react';

interface ToastItem {
  id: number;
  message: string;
  tone: 'success' | 'error';
  leaving: boolean;
}

interface ToastContextValue {
  show: (message: string, tone?: 'success' | 'error') => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}

const AUTO_DISMISS_MS = 4500;
const EXIT_ANIMATION_MS = 200;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismiss = useCallback((id: number) => {
    // Two-phase removal: mark "leaving" to trigger the exit animation, then actually remove
    // once it's had time to play — a toast that just vanishes mid-frame reads as a glitch.
    setToasts((prev) => prev.map((t) => (t.id === id ? { ...t, leaving: true } : t)));
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), EXIT_ANIMATION_MS);
  }, []);

  const show = useCallback((message: string, tone: 'success' | 'error' = 'success') => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, message, tone, leaving: false }]);
    setTimeout(() => dismiss(id), AUTO_DISMISS_MS);
  }, [dismiss]);

  return (
    <ToastContext.Provider value={{ show }}>
      {children}
      <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-sm">
        {toasts.map((t) => (
          <div
            key={t.id}
            role="status"
            className={`relative overflow-hidden flex items-start gap-2.5 rounded-card px-4 py-3 shadow-lg text-sm text-white transition-all duration-200 ${
              t.leaving ? 'opacity-0 translate-x-2 scale-95' : 'animate-toast-in'
            } ${t.tone === 'success' ? 'bg-teal-dark' : 'bg-coral'}`}
          >
            {t.tone === 'success' ? <CheckCircle2 size={18} className="shrink-0 mt-0.5" /> : <XCircle size={18} className="shrink-0 mt-0.5" />}
            <span className="flex-1">{t.message}</span>
            <button onClick={() => dismiss(t.id)} className="opacity-70 hover:opacity-100 focus-ring rounded">
              <X size={16} />
            </button>
            {!t.leaving && <span className="toast-progress absolute bottom-0 left-0 h-0.5 w-full bg-white/40" />}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
