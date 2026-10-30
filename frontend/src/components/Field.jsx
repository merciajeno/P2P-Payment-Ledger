export default function Field({ label, hint, ...inputProps }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      <input {...inputProps} />
      {hint && <span className="field-hint">{hint}</span>}
    </label>
  );
}
