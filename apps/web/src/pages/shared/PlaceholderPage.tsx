export function PlaceholderPage({ title, description }: { title: string; description: string }) {
  return (
    <section className="page-section">
      <p className="eyebrow">Platform Core</p>
      <h1>{title}</h1>
      <p className="lead">{description}</p>
      <p className="muted">This functionality belongs to a later slice.</p>
    </section>
  )
}
