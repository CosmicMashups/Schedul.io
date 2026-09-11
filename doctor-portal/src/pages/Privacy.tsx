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
          Your practitioner account details, and — as part of normal clinic operations — the
          appointment and queue information for the patients under your care.
        </Section>

        <Section title="2. How we use it">
          To show your own schedule and queue, and to keep an audit trail of care-related actions
          (calling a patient, completing a visit) taken through the portal.
        </Section>

        <Section title="3. Who can see this information">
          Authorized staff at your clinic, scoped by role. We don't sell this information, and we
          don't share it with unrelated third parties.
        </Section>

        <Section title="4. Data retention">
          Records are kept for as long as the clinic's account is active and as needed to meet
          recordkeeping obligations, then deleted or anonymized.
        </Section>

        <Section title="5. Your rights">
          You can ask your clinic administrator to correct or remove your own account information.
        </Section>

        <Section title="6. Security">
          Access is role-scoped, actions are logged, and data is encrypted in transit.
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
