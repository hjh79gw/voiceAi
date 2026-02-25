import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { startSimulation } from '../services/api';
import { useWebSocket } from '../hooks/useWebSocket';
import { useSpeechRecognition } from '../hooks/useSpeechRecognition';
import { useAudioStream } from '../hooks/useAudioStream';
import type { SimulationStartResponse, ConversationEntry, WsMessage } from '../types';
import IncomingCall from '../components/IncomingCall';
import PhoneCallUI from '../components/PhoneCallUI';

type Phase = 'incoming' | 'call' | 'ended';

export default function SimulationCall() {
  const { scenarioId } = useParams<{ scenarioId: string }>();
  const navigate = useNavigate();

  const [phase, setPhase] = useState<Phase>('incoming');
  const [session, setSession] = useState<SimulationStartResponse | null>(null);
  const [conversation, setConversation] = useState<ConversationEntry[]>([]);
  const [aiText, setAiText] = useState('');
  const [isAiSpeaking, setIsAiSpeaking] = useState(false);

  const { connect, send, disconnect } = useWebSocket();
  const { startListening, stopListening, isListening, transcript } = useSpeechRecognition();
  const { addToQueue, clearQueue } = useAudioStream();

  const sessionRef = useRef<SimulationStartResponse | null>(null);

  const handleWsMessage = (msg: WsMessage) => {
    switch (msg.type) {
      case 'ai_text':
        setAiText(prev => prev + (msg.text || ''));
        setIsAiSpeaking(true);
        break;
      case 'ai_audio':
        if (msg.audio) addToQueue(msg.audio);
        break;
      case 'turn_end':
        setConversation(prev => [...prev, { role: 'ai', text: aiTextRef.current }]);
        aiTextRef.current = '';
        setAiText('');
        setIsAiSpeaking(false);
        break;
      case 'call_ended':
        setPhase('ended');
        stopListening();
        disconnect();
        if (sessionRef.current) {
          navigate(`/result/${sessionRef.current.sessionId}`);
        }
        break;
    }
  };

  const aiTextRef = useRef('');
  useEffect(() => {
    aiTextRef.current = aiText;
  }, [aiText]);

  const handleAccept = async () => {
    const res = await startSimulation(Number(scenarioId));
    setSession(res);
    sessionRef.current = res;
    setPhase('call');

    // 수화 버튼 클릭(유저 제스처) 컨텍스트에서 음성인식 시작
    startListening(handleUserSpeech);

    connect(res.sessionId, handleWsMessage);

    // 약간의 딜레이 후 AI 첫 발화 시작
    setTimeout(() => {
      send('/simulation/start', { sessionId: res.sessionId });
    }, 1000);
  };

  const handleReject = () => {
    navigate('/');
  };

  const handleUserSpeech = (text: string) => {
    if (!sessionRef.current || !text.trim()) return;
    setConversation(prev => [...prev, { role: 'user', text }]);
    send('/simulation/speech', {
      sessionId: sessionRef.current.sessionId,
      text,
    });
  };

  const handleHangUp = () => {
    if (sessionRef.current) {
      send('/simulation/end', { sessionId: sessionRef.current.sessionId });
    }
  };

  if (phase === 'incoming') {
    return (
      <IncomingCall
        callerName={`시나리오 ${scenarioId}`}
        callerNumber="02-3145-7890"
        onAccept={handleAccept}
        onReject={handleReject}
      />
    );
  }

  return (
    <PhoneCallUI
      callerName={session?.callerName || ''}
      callerNumber={session?.callerNumber || ''}
      conversation={conversation}
      currentAiText={aiText}
      isAiSpeaking={isAiSpeaking}
      isListening={isListening}
      transcript={transcript}
      onHangUp={handleHangUp}
    />
  );
}
