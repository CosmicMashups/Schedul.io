import { ShieldCheck } from 'lucide-react';

export default function Privacy() {
  return (
    <div className="animate-page max-w-2xl">
      <div className="flex items-center gap-2 mb-1">
        <ShieldCheck size={20} className="text-teal" />
        <h1 className="text-xl font-semibold text-ink">Privacy Policy</h1>
      </div>
      <p className="text-xs text-ink/40 mb-8">Last updated: placeholder — review before real-world use</p>

      <div className="panel px-4 py-3 mb-8">
        <p className="text-xs text-ink/60 leading-relaxed">
          This is template legal content written for a demo project, not reviewed by counsel. Replace it
          with a policy drafted or approved by a qualified lawyer, and with your clinic's actual data
          handling practices, before using this in production.
        </p>
      </div>

      <div className="space-y-8 text-sm text-ink/70 leading-relaxed">
        <Section title="1. Information we collect">
          Staff account details, and — as part of normal clinic operations — the patient, appointment,
          and queue information staff enter or view while using the console.
        </Section>

        <Section title="2. How we use it">
          To operate scheduling, check-in, queueing, and reporting features for your clinic, and to
          maintain an audit trail of changes made through the console.
        </Section>

        <Section title="3. Who can see this information">
          Other authorized staff and practitioners at the same clinic, scoped by role. We don't sell
          this information, and we don't share it with unrelated third parties.
        </Section>

        <Section title="4. Data retention">
          Records are kept for as long as the clinic's account is active and as needed to meet
          recordkeeping obligations, then deleted or anonymized.
        </Section>

        <Section title="5. Your rights">
          Staff can ask their clinic administrator to correct or remove their own account information.
          Patient-record requests follow the clinic's own patient-facing privacy process.
        </Section>

        <Section title="6. Security">
          Access is role-scoped, actions are logged, and data is encrypted in transit. No system is
          perfectly secure, but we work to keep this one trustworthy.
        </Section>

        <Section title="7. Changes to this policy">
          We may update this policy as the service evolves. Material changes will be communicated
          before they take effect.
        </Section>

        <Section title="8. Contact">
          Privacy questions can be directed to your clinic administrator.
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
