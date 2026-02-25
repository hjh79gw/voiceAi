import type { ConversationEntry } from '../types';

interface Props {
  callerName: string;
  callerNumber: string;
  conversation: ConversationEntry[];
  currentAiText: string;
  isAiSpeaking: boolean;
  isListening: boolean;
  transcript: string;
  onHangUp: () => void;
}

export default function PhoneCallUI({
  callerName, callerNumber, conversation, currentAiText,
  isAiSpeaking, isListening, transcript, onHangUp,
}: Props) {
  return (
    <div style={{
      minHeight: '100vh', background: '#1a1a2e',
      display: 'flex', flexDirection: 'column', color: '#fff',
    }}>
      {/* 상단 정보 */}
      <div style={{ textAlign: 'center', padding: '30px 20px 10px' }}>
        <p style={{ fontSize: 14, opacity: 0.6, margin: 0 }}>통화 중</p>
        <h2 style={{ margin: '4px 0' }}>{callerName}</h2>
        <p style={{ fontSize: 14, opacity: 0.5, margin: 0 }}>{callerNumber}</p>
      </div>

      {/* 대화 로그 */}
      <div style={{
        flex: 1, overflowY: 'auto', padding: '20px',
        display: 'flex', flexDirection: 'column', gap: 12,
      }}>
        {conversation.map((entry, i) => (
          <div key={i} style={{
            alignSelf: entry.role === 'user' ? 'flex-end' : 'flex-start',
            maxWidth: '80%',
          }}>
            <div style={{
              background: entry.role === 'user' ? '#0046FF' : 'rgba(255,255,255,0.1)',
              borderRadius: 16, padding: '10px 16px', fontSize: 14, lineHeight: 1.6,
            }}>
              {entry.text}
            </div>
          </div>
        ))}

        {/* AI 현재 말하는 중 */}
        {currentAiText && (
          <div style={{ alignSelf: 'flex-start', maxWidth: '80%' }}>
            <div style={{
              background: 'rgba(255,255,255,0.1)',
              borderRadius: 16, padding: '10px 16px', fontSize: 14, lineHeight: 1.6,
            }}>
              {currentAiText}
              <span style={{ opacity: 0.5 }}> ...</span>
            </div>
          </div>
        )}

        {/* 사용자 말하는 중 */}
        {isListening && transcript && (
          <div style={{ alignSelf: 'flex-end', maxWidth: '80%' }}>
            <div style={{
              background: 'rgba(0,70,255,0.5)',
              borderRadius: 16, padding: '10px 16px', fontSize: 14,
              fontStyle: 'italic',
            }}>
              {transcript}...
            </div>
          </div>
        )}
      </div>

      {/* 상태 표시 */}
      <div style={{ textAlign: 'center', padding: '10px' }}>
        {isAiSpeaking && (
          <p style={{ color: '#4CAF50', fontSize: 14 }}>상대방이 말하고 있습니다...</p>
        )}
        {isListening && !isAiSpeaking && (
          <p style={{ color: '#0046FF', fontSize: 14 }}>통화 중...</p>
        )}
      </div>

      {/* 하단 버튼 */}
      <div style={{
        display: 'flex', justifyContent: 'center',
        padding: '20px', paddingBottom: 40,
      }}>
        <button
          onClick={onHangUp}
          style={{
            width: 64, height: 64, borderRadius: '50%',
            background: '#F44336', border: 'none', cursor: 'pointer',
            color: '#fff', fontSize: 20, fontWeight: 'bold',
          }}
        >
          END
        </button>
      </div>
    </div>
  );
}
