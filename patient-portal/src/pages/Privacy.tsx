import { ShieldCheck } from 'lucide-react';

export default function Privacy() {
  return (
    <div className="animate-page max-w-2xl">
      <div className="flex items-center gap-2 mb-1">
        <ShieldCheck size={20} className="text-teal" />
        <h1 className="font-display text-2xl text-ink">Privacy Policy</h1>
      </div>
      <p className="text-xs text-ink/40 mb-8">Last updated: placeholder — review before real-world use</p>

      <div className="rounded-card border border-line bg-mist/60 px-4 py-3 mb-8">
        <p className="text-xs text-ink/60 leading-relaxed">
          This is template legal content written for a demo project, not reviewed by counsel. Replace it
          with a policy drafted or approved by a qualified lawyer, and with your clinic's actual data
          handling practices, before using this in production.
        </p>
      </div>

      <div className="space-y-8 text-sm text-ink/70 leading-relaxed">
        <Section title="1. Information we collect">
          Account details (name, email, mobile number), the health-related information you provide
          during registration or booking (date of birth, sex, reason for visit), and usage data
          (appointments booked, pages visited) needed to operate the service.
        </Section>

        <Section title="2. How we use it">
          To match you with doctors and available slots, confirm and remind you about appointments,
          maintain your appointment history, and improve the reliability of the booking flow.
        </Section>

        <Section title="3. Who can see your information">
          The clinic staff and practitioners directly involved in scheduling or providing your care.
          We don't sell your information, and we don't share it with unrelated third parties.
        </Section>

        <Section title="4. Data retention">
          We keep your account and appointment records for as long as your account is active and as
          needed to meet the clinic's recordkeeping obligations, then delete or anonymize it.
        </Section>

        <Section title="5. Your rights">
          You can ask to see, correct, or delete the personal information we hold about you by
          contacting your clinic directly. Some records may need to be retained for a period required
          by law even after a deletion request.
        </Section>

        <Section title="6. Security">
          We use encryption in transit, role-based access controls, and audit logging to limit who can
          see your data and when. No system is perfectly secure, but we work to keep this one
          trustworthy.
        </Section>

        <Section title="7. Children's privacy">
          If a minor's appointment is booked through this service, it's expected to be done by a
          parent or guardian on the minor's behalf.
        </Section>

        <Section title="8. Changes to this policy">
          We may update this policy as the service evolves. Material changes will be communicated
          before they take effect.
        </Section>

        <Section title="9. Contact">
          Privacy questions or requests can be directed to your clinic's front desk or to the contact
          listed in your clinic's own privacy notice.
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
