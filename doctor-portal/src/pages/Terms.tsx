import { FileText } from 'lucide-react';

export default function Terms() {
  return (
    <div className="animate-page max-w-2xl">
      <div className="flex items-center gap-2 mb-1">
        <FileText size={20} className="text-teal" />
        <h1 className="text-xl font-semibold text-ink">Terms of Service</h1>
      </div>
      <p className="text-xs text-ink/40 mb-8">Last updated: placeholder — review before real-world use</p>

      <div className="panel px-4 py-3 mb-8">
        <p className="text-xs text-ink/60 leading-relaxed">
          This is template legal content written for a demo project, not reviewed by counsel. Replace it
          with terms drafted or approved by a qualified lawyer before using this in production.
        </p>
      </div>

      <div className="space-y-8 text-sm text-ink/70 leading-relaxed">
        <Section title="1. Acceptance of these terms">
          By using the Schedul.io doctor portal under your clinic's account, you agree to these Terms
          of Service and to the <a href="/privacy" className="text-teal hover:text-teal-light underline underline-offset-2">Privacy Policy</a>.
        </Section>

        <Section title="2. What the service is">
          The doctor portal shows your own schedule and queue — who's waiting, who's being served,
          and today's appointments — scoped to the practitioner record linked to your account. It is
          a scheduling tool, not a source of clinical or legal advice.
        </Section>

        <Section title="3. Authorized use only">
          Access is limited to the practitioner account your clinic has linked to your login. Don't
          share your login credentials.
        </Section>

        <Section title="4. Data handling responsibilities">
          Information you view here about patients is provided so you can deliver care; it's expected
          to be handled per your clinic's own confidentiality policies, in addition to this platform's
          {' '}<a href="/privacy" className="text-teal hover:text-teal-light underline underline-offset-2">Privacy Policy</a>.
        </Section>

        <Section title="5. Disclaimers">
          The service is provided "as is." We don't guarantee it will be uninterrupted or error-free.
        </Section>

        <Section title="6. Changes to these terms">
          We may update these terms from time to time. Material changes will be communicated before
          they take effect.
        </Section>

        <Section title="7. Contact">
          Questions about these terms can be directed to your clinic administrator.
        </Section>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h2 className="font-medium text-ink mb-2">{title}</h2>
      <p>{children}</p>
    </div>
  );
}
