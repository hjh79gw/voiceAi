package com.voicdai.backend.service;

import com.voicdai.backend.dto.SimulationStartResponse;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SimulationServiceImpl implements SimulationService {

    private final Map<String, SimulationSession> sessions = new ConcurrentHashMap<>();

    private static final List<ScenarioInfo> SCENARIOS = List.of(
            new ScenarioInfo(1, "검찰 사칭", "prosecution", "EASY", true, true,
                    "김정수", "02-3145-7890",
                    "검찰 사칭 보이스피싱 — 강압적, 저품질"),
            new ScenarioInfo(2, "은행 사칭", "bank_fake", "HARD", true, false,
                    "이수현", "1599-8000",
                    "은행 사칭 보이스피싱 — 친절하지만 급한, 고품질"),
            new ScenarioInfo(3, "정상 은행 콜센터", "bank_real_low", "HARD", false, true,
                    "박지영", "1599-8000",
                    "정상 은행 콜센터 — ARS 거친 저품질"),
            new ScenarioInfo(4, "정상 은행 상담원", "bank_real_high", "EASY", false, false,
                    "김은지", "1599-8000",
                    "정상 은행 상담원 — 차분, 고품질")
    );

    @Override
    public List<ScenarioInfo> getScenarios() {
        return SCENARIOS;
    }

    @Override
    public SimulationStartResponse startSimulation(int scenarioId) {
        ScenarioInfo scenario = SCENARIOS.stream()
                .filter(s -> s.getId() == scenarioId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("시나리오를 찾을 수 없습니다: " + scenarioId));

        String sessionId = UUID.randomUUID().toString().substring(0, 8);

        SimulationSession session = new SimulationSession();
        session.setSessionId(sessionId);
        session.setScenario(scenario);
        session.setConversationHistory(new ArrayList<>());
        session.setTurnCount(0);

        sessions.put(sessionId, session);

        return SimulationStartResponse.builder()
                .sessionId(sessionId)
                .callerName(scenario.getCallerName())
                .callerNumber(scenario.getCallerNumber())
                .scenarioType(scenario.getType())
                .difficulty(scenario.getDifficulty())
                .build();
    }

    @Override
    public SimulationSession getSession(String sessionId) {
        SimulationSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId);
        }
        return session;
    }

    @Override
    public void addUserMessage(String sessionId, String text) {
        SimulationSession session = getSession(sessionId);
        session.getConversationHistory().add(Map.of("role", "user", "content", text));
        session.setTurnCount(session.getTurnCount() + 1);
    }

    @Override
    public void addAssistantMessage(String sessionId, String text) {
        SimulationSession session = getSession(sessionId);
        session.getConversationHistory().add(Map.of("role", "assistant", "content", text));
    }

    @Override
    public void endSession(String sessionId) {
        sessions.remove(sessionId);
    }

    @Data
    public static class SimulationSession {
        private String sessionId;
        private ScenarioInfo scenario;
        private List<Map<String, String>> conversationHistory;
        private int turnCount;
    }

    @Data
    public static class ScenarioInfo {
        private final int id;
        private final String name;
        private final String type;
        private final String difficulty;
        private final boolean phishing;
        private final boolean voipEffect;
        private final String callerName;
        private final String callerNumber;
        private final String description;

        public ScenarioInfo(int id, String name, String type, String difficulty,
                            boolean phishing, boolean voipEffect,
                            String callerName, String callerNumber, String description) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.difficulty = difficulty;
            this.phishing = phishing;
            this.voipEffect = voipEffect;
            this.callerName = callerName;
            this.callerNumber = callerNumber;
            this.description = description;
        }
    }
}
