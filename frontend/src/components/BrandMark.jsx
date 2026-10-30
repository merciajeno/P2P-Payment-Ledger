export default function BrandMark({ href, onClick }) {
  return (
    <a className="brand" href={href} onClick={onClick} aria-label="P2P Ledger home">
      <span className="brand-mark" aria-hidden="true">
        <span />
        <span />
      </span>
      <span>p2p<span className="brand-light">ledger</span></span>
    </a>
  );
}
