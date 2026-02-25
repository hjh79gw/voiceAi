package com.voicdai.backend.service;

import com.voicdai.backend.dto.SimulationStartResponse;

import java.util.List;

public interface SimulationService {

    List<SimulationServiceImpl.ScenarioInfo> getScenarios();

    SimulationStartResponse startSimulation(int scenarioId);

    SimulationServiceImpl.SimulationSession getSession(String sessionId);

    void addUserMessage(String sessionId, String text);

    void addAssistantMessage(String sessionId, String text);

    void endSession(String sessionId);
}
