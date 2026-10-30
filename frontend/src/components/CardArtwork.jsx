export default function CardArtwork() {
  return (
    <div className="artwork" aria-hidden="true">
      <div className="artwork-orbit artwork-orbit-one" />
      <div className="artwork-orbit artwork-orbit-two" />
      <div className="floating-dot dot-one" />
      <div className="floating-dot dot-two" />
      <div className="floating-dot dot-three" />
      <div className="payment-card">
        <div className="payment-card-top">
          <span className="card-chip" />
          <span className="card-contactless">)))</span>
        </div>
        <div className="card-number">•••• &nbsp;•••• &nbsp;•••• &nbsp;2486</div>
        <div className="payment-card-bottom">
          <span>YOUR EVERYDAY, ELEVATED</span>
          <span className="card-logo">p.</span>
        </div>
        <div className="card-shine" />
      </div>
      <div className="artwork-caption">
        <span className="caption-dot" />
        <span>Money moves better together</span>
      </div>
    </div>
  );
}
