export function Field({
  label, value, onChange, type = 'text', placeholder, mono = false, required = true,
}: {
  label: string; value: string; onChange: (v: string) => void; type?: string;
  placeholder?: string; mono?: boolean; required?: boolean;
}) {
  return (
    <label className="block">
      <span className="block text-xs font-medium text-ink/60 mb-1.5">{label}</span>
      <input
        type={type}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        required={required}
        className={`w-full rounded-lg border border-slate-line bg-slate px-3.5 py-2.5 text-sm text-ink placeholder:text-ink/30 focus-ring ${mono ? 'font-mono text-xs' : ''}`}
      />
    </label>
  );
}
