interface Props {
  callerName: string;
  callerNumber: string;
  onAccept: () => void;
  onReject: () => void;
}

export default function IncomingCall({ callerName, callerNumber, onAccept, onReject }: Props) {
  return (
    <div style={{
      minHeight: '100vh', background: 'linear-gradient(180deg, #0046FF 0%, #001a66 100%)',
      display: 'flex', flexDirection: 'column', alignItems: 'center',
      justifyContent: 'center', color: '#fff',
    }}>
      <p style={{ fontSize: 14, opacity: 0.8, marginBottom: 8 }}>전화가 왔습니다</p>
      <h1 style={{ fontSize: 32, marginBottom: 4 }}>{callerName}</h1>
      <p style={{ fontSize: 18, opacity: 0.7, marginBottom: 60 }}>{callerNumber}</p>

      <div style={{ display: 'flex', gap: 60 }}>
        <button
          onClick={onReject}
          style={{
            width: 70, height: 70, borderRadius: '50%',
            background: '#F44336', border: 'none', cursor: 'pointer',
            color: '#fff', fontSize: 24,
          }}
        >
          X
        </button>
        <button
          onClick={onAccept}
          style={{
            width: 70, height: 70, borderRadius: '50%',
            background: '#4CAF50', border: 'none', cursor: 'pointer',
            color: '#fff', fontSize: 24,
          }}
        >
          O
        </button>
      </div>

      <div style={{ marginTop: 40, display: 'flex', gap: 40 }}>
        <span style={{ fontSize: 14, opacity: 0.6 }}>거절</span>
        <span style={{ fontSize: 14, opacity: 0.6 }}>수락</span>
      </div>
    </div>
  );
}
