import axios from 'axios';
import type { ScenarioInfo, SimulationStartResponse, JudgeResponse } from '../types';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
});

export const getScenarios = () =>
  api.get<ScenarioInfo[]>('/scenarios').then(res => res.data);

export const startSimulation = (scenarioId: number) =>
  api.post<SimulationStartResponse>('/simulation/start', { scenarioId }).then(res => res.data);

export const judgeSimulation = (sessionId: string, userAnswer: boolean) =>
  api.post<JudgeResponse>(`/simulation/${sessionId}/judge`, { userAnswer }).then(res => res.data);
