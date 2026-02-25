package com.voicdai.backend.controller;

import com.voicdai.backend.dto.*;
import com.voicdai.backend.service.ClaudeService;
import com.voicdai.backend.service.SimulationService;
import com.voicdai.backend.service.SimulationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;
    private final ClaudeService claudeService;

    @GetMapping("/scenarios")
    public List<SimulationServiceImpl.ScenarioInfo> getScenarios() {
        return simulationService.getScenarios();
    }

    @PostMapping("/simulation/start")
    public SimulationStartResponse startSimulation(@RequestBody SimulationStartRequest request) {
        return simulationService.startSimulation(request.getScenarioId());
    }

    @PostMapping("/simulation/{sessionId}/judge")
    public JudgeResponse judge(@PathVariable String sessionId,
                               @RequestBody JudgeRequest request) {
        var session = simulationService.getSession(sessionId);
        boolean isPhishing = session.getScenario().isPhishing();
        boolean correct = request.isUserAnswer() == isPhishing;

        String feedbackPrompt = buildFeedbackPrompt(session, request.isUserAnswer());
        String feedback = claudeService.chat(feedbackPrompt,
                List.of(Map.of("role", "user", "content", "해설해주세요")));

        int score = correct ? 80 : 40;

        return JudgeResponse.builder()
                .correct(correct)
                .score(score)
                .feedback(feedback)
                .build();
    }

    private String buildFeedbackPrompt(SimulationServiceImpl.SimulationSession session,
                                       boolean userAnswer) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 보이스피싱 예방 교육 해설자입니다.\n\n");
        sb.append("[대화 로그]\n");
        for (var msg : session.getConversationHistory()) {
            sb.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
        }
        sb.append("\n[시나리오 정보]\n");
        sb.append("유형: ").append(session.getScenario().getName()).append("\n");
        sb.append("정답: ").append(session.getScenario().isPhishing() ? "보이스피싱" : "정상 통화").append("\n");
        sb.append("사용자 판정: ").append(userAnswer ? "보이스피싱" : "정상 통화").append("\n\n");
        sb.append("해설할 내용:\n");
        sb.append("1. 이 전화가 보이스피싱인/아닌 이유 (대화 내용 중심, 구체적 근거 3가지)\n");
        sb.append("2. 사용자가 잘한 점 / 위험했던 점\n");
        sb.append("3. 실제로 이런 전화를 받으면 어떻게 해야 하는지\n");
        sb.append("4. 음질만으로 판단하면 안 되는 이유\n");
        sb.append("\n간결하게 3~5문장으로 답해주세요.");
        return sb.toString();
    }
}
