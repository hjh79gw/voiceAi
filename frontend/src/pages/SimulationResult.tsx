import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { judgeSimulation } from '../services/api';
import type { JudgeResponse } from '../types';

export default function SimulationResult() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();

  const [judged, setJudged] = useState(false);
  const [result, setResult] = useState<JudgeResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const handleJudge = async (userAnswer: boolean) => {
    if (!sessionId) return;
    setLoading(true);
    try {
      const res = await judgeSimulation(sessionId, userAnswer);
      setResult(res);
      setJudged(true);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  if (!judged) {
    return (
      <div style={{
        minHeight: '100vh', background: '#1a1a2e', display: 'flex',
        alignItems: 'center', justifyContent: 'center',
      }}>
        <div style={{ textAlign: 'center', color: '#fff' }}>
          <h1 style={{ fontSize: 28, marginBottom: 16 }}>
            이 전화는 보이스피싱이었을까요?
          </h1>
          <p style={{ color: '#aaa', marginBottom: 40 }}>
            방금 받은 전화를 판별해보세요
          </p>
          <div style={{ display: 'flex', gap: 20, justifyContent: 'center' }}>
            <button
              onClick={() => handleJudge(true)}
              disabled={loading}
              style={{
                padding: '16px 48px', fontSize: 18, borderRadius: 12,
                background: '#F44336', color: '#fff', border: 'none',
                cursor: 'pointer', fontWeight: 'bold',
              }}
            >
              보이스피싱!
            </button>
            <button
              onClick={() => handleJudge(false)}
              disabled={loading}
              style={{
                padding: '16px 48px', fontSize: 18, borderRadius: 12,
                background: '#4CAF50', color: '#fff', border: 'none',
                cursor: 'pointer', fontWeight: 'bold',
              }}
            >
              정상 통화
            </button>
          </div>
          {loading && <p style={{ marginTop: 20, color: '#aaa' }}>AI가 해설을 준비 중...</p>}
        </div>
      </div>
    );
  }

  return (
    <div style={{
      minHeight: '100vh', background: '#1a1a2e', display: 'flex',
      alignItems: 'center', justifyContent: 'center', padding: 20,
    }}>
      <div style={{ maxWidth: 500, width: '100%', textAlign: 'center', color: '#fff' }}>
        <div style={{
          fontSize: 60, marginBottom: 16,
        }}>
          {result?.correct ? 'O' : 'X'}
        </div>
        <h2 style={{
          color: result?.correct ? '#4CAF50' : '#F44336',
          marginBottom: 8,
        }}>
          {result?.correct ? '정답입니다!' : '틀렸습니다!'}
        </h2>
        <p style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 24 }}>
          점수: {result?.score}점
        </p>

        <div style={{
          background: 'rgba(255,255,255,0.1)', borderRadius: 12,
          padding: 24, textAlign: 'left', marginBottom: 32,
          lineHeight: 1.8,
        }}>
          <h3 style={{ marginTop: 0 }}>AI 해설</h3>
          <p style={{ color: '#ccc' }}>{result?.feedback}</p>
        </div>

        <button
          onClick={() => navigate('/')}
          style={{
            padding: '14px 40px', fontSize: 16, borderRadius: 12,
            background: '#0046FF', color: '#fff', border: 'none',
            cursor: 'pointer', fontWeight: 'bold',
          }}
        >
          다시 도전하기
        </button>
      </div>
    </div>
  );
}
