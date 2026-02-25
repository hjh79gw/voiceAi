export interface ScenarioInfo {
  id: number;
  name: string;
  type: string;
  difficulty: string;
  phishing: boolean;
  voipEffect: boolean;
  callerName: string;
  callerNumber: string;
  description: string;
}

export interface SimulationStartResponse {
  sessionId: string;
  callerName: string;
  callerNumber: string;
  scenarioType: string;
  difficulty: string;
}

export interface WsMessage {
  type: 'ai_text' | 'ai_audio' | 'turn_end' | 'call_ended';
  text?: string;
  audio?: string; // base64
  turn: number;
}

export interface JudgeResponse {
  correct: boolean;
  score: number;
  feedback: string;
}

export interface ConversationEntry {
  role: 'user' | 'ai';
  text: string;
}
