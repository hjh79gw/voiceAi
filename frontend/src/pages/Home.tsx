import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ScenarioInfo } from '../types';
import { getScenarios } from '../services/api';

export default function Home() {
  const [scenarios, setScenarios] = useState<ScenarioInfo[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    getScenarios().then(setScenarios).catch(console.error);
  }, []);

  const difficultyColor = (d: string) => {
    switch (d) {
      case 'EASY': return '#4CAF50';
      case 'MEDIUM': return '#FF9800';
      case 'HARD': return '#F44336';
      default: return '#999';
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: '#f5f5f5', padding: '40px 20px' }}>
      <div style={{ maxWidth: 600, margin: '0 auto' }}>
        <h1 style={{ textAlign: 'center', color: '#0046FF', marginBottom: 8 }}>
          보이스피싱 예방 시뮬레이션
        </h1>
        <p style={{ textAlign: 'center', color: '#666', marginBottom: 40 }}>
          AI와 실시간 음성 대화로 보이스피싱 판별력을 기르세요
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {scenarios.map(scenario => (
            <div
              key={scenario.id}
              onClick={() => navigate(`/simulation/${scenario.id}`)}
              style={{
                background: '#fff',
                borderRadius: 12,
                padding: 20,
                cursor: 'pointer',
                boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
                border: '2px solid transparent',
                transition: 'border-color 0.2s',
              }}
              onMouseEnter={e => (e.currentTarget.style.borderColor = '#0046FF')}
              onMouseLeave={e => (e.currentTarget.style.borderColor = 'transparent')}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h3 style={{ margin: 0 }}>{scenario.name}</h3>
                <span style={{
                  background: difficultyColor(scenario.difficulty),
                  color: '#fff',
                  padding: '4px 12px',
                  borderRadius: 20,
                  fontSize: 12,
                  fontWeight: 'bold',
                }}>
                  {scenario.difficulty}
                </span>
              </div>
              <p style={{ color: '#666', margin: '8px 0 0', fontSize: 14 }}>
                {scenario.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
