package com.voicdai.backend.controller;

import com.voicdai.backend.dto.SpeechMessage;
import com.voicdai.backend.dto.WsMessage;
import com.voicdai.backend.service.AudioService;
import com.voicdai.backend.service.ClaudeService;
import com.voicdai.backend.service.SimulationService;
import com.voicdai.backend.service.SimulationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SimulationWsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ClaudeService claudeService;
    private final AudioService audioService;
    private final SimulationService simulationService;
    private final ExecutorService ttsExecutor = Executors.newFixedThreadPool(4);

    /**
     * AI 첫 발화 시작
     */
    @MessageMapping("/simulation/start")
    public void startConversation(SpeechMessage message) {
        var session = simulationService.getSession(message.getSessionId());
        String systemPrompt = getSystemPrompt(session.getScenario());

        // 첫 발화 시 대화 이력이 비어있으면 초기 메시지 추가
        if (session.getConversationHistory().isEmpty()) {
            simulationService.addUserMessage(message.getSessionId(), "여보세요?");
        }

        StringBuilder sentenceBuffer = new StringBuilder();
        StringBuilder fullResponse = new StringBuilder();
        int turn = session.getTurnCount() + 1;

        claudeService.streamChat(systemPrompt, session.getConversationHistory(),
                token -> {
                    sentenceBuffer.append(token);
                    fullResponse.append(token);

                    sendToSession(message.getSessionId(), WsMessage.builder()
                            .type("ai_text")
                            .text(token)
                            .turn(turn)
                            .build());

                    String current = sentenceBuffer.toString();
                    if (current.matches(".*[.?!。？！]\\s*$")) {
                        sendTtsAudio(message.getSessionId(), current.trim(),
                                session.getScenario(), turn);
                        sentenceBuffer.setLength(0);
                    }
                },
                () -> {
                    String remaining = sentenceBuffer.toString().trim();
                    if (!remaining.isEmpty()) {
                        sendTtsAudio(message.getSessionId(), remaining,
                                session.getScenario(), turn);
                    }

                    simulationService.addAssistantMessage(message.getSessionId(),
                            fullResponse.toString());

                    sendToSession(message.getSessionId(), WsMessage.builder()
                            .type("turn_end")
                            .turn(turn)
                            .build());
                }
        );
    }

    /**
     * 사용자 발화 수신 → AI 응답
     */
    @MessageMapping("/simulation/speech")
    public void handleSpeech(SpeechMessage message) {
        log.info("사용자 발화: {}", message.getText());

        var session = simulationService.getSession(message.getSessionId());
        simulationService.addUserMessage(message.getSessionId(), message.getText());

        if (session.getTurnCount() >= 10) {
            sendToSession(message.getSessionId(), WsMessage.builder()
                    .type("call_ended")
                    .turn(session.getTurnCount())
                    .build());
            return;
        }

        startConversation(message);
    }

    /**
     * 대화 종료
     */
    @MessageMapping("/simulation/end")
    public void endConversation(SpeechMessage message) {
        sendToSession(message.getSessionId(), WsMessage.builder()
                .type("call_ended")
                .turn(simulationService.getSession(message.getSessionId()).getTurnCount())
                .build());
    }

    private void sendTtsAudio(String sessionId, String text,
                              SimulationServiceImpl.ScenarioInfo scenario, int turn) {
        ttsExecutor.submit(() -> {
            try {
                byte[] audio = audioService.textToSpeechStream(
                        text, scenario.getType(), scenario.isVoipEffect());

                String base64Audio = Base64.getEncoder().encodeToString(audio);
                sendToSession(sessionId, WsMessage.builder()
                        .type("ai_audio")
                        .audio(base64Audio)
                        .turn(turn)
                        .build());
            } catch (Exception e) {
                log.error("TTS 변환 실패: {}", e.getMessage());
            }
        });
    }

    private void sendToSession(String sessionId, WsMessage message) {
        messagingTemplate.convertAndSend("/topic/simulation/" + sessionId, message);
    }

    private String getSystemPrompt(SimulationServiceImpl.ScenarioInfo scenario) {
        String common = "당신은 금융사기 예방 교육 시뮬레이션 AI입니다.\n" +
                "규칙:\n" +
                "- 할당된 역할을 끝까지 유지하세요\n" +
                "- 자연스러운 전화 대화처럼 짧은 문장으로 말하세요 (1~2문장)\n" +
                "- 절대 교육용 시뮬레이션이라는 사실을 먼저 밝히지 마세요\n" +
                "- 한국어로만 대답하세요\n\n";

        return switch (scenario.getType()) {
            case "prosecution" -> common +
                    "역할: 서울중앙지검 수사관 " + scenario.getCallerName() + "\n" +
                    "목표: 사용자의 개인정보(주민번호)와 계좌이체를 유도\n" +
                    "특징: 강압적, 혼내듯이, 시간 압박\n" +
                    "대화 흐름: 신분 밝힘 → 계좌 범죄 연루 공포 → 주민번호 요구 → 안전계좌 이체 유도 → 체포 압박";
            case "bank_fake" -> common +
                    "역할: 신한은행 고객센터 상담원 " + scenario.getCallerName() + "\n" +
                    "목표: 카드 정보/OTP 번호 탈취\n" +
                    "특징: 친절하지만 급한, 전문 용어 사용\n" +
                    "대화 흐름: 카드 이상 거래 감지 알림 → 카드번호 뒷자리 확인 → OTP 번호 요구 → 앱 본인인증 유도";
            case "bank_real_low" -> common +
                    "역할: 진짜 신한은행 콜센터 상담원 " + scenario.getCallerName() + "\n" +
                    "목표: 정상적인 카드 만료 안내 (보이스피싱 아님)\n" +
                    "특징: 차분, 사무적\n" +
                    "대화 흐름: 카드 유효기간 만료 안내 → 새 카드 발송 제안 → 주소 변경은 앱에서 직접 하라고 안내\n" +
                    "중요: 절대 개인정보(주민번호, 카드번호, OTP)를 요구하지 마세요";
            case "bank_real_high" -> common +
                    "역할: 진짜 신한은행 상담원 " + scenario.getCallerName() + "\n" +
                    "목표: 정상적인 적금 만기 안내 (보이스피싱 아님)\n" +
                    "특징: 차분, 친절\n" +
                    "대화 흐름: 적금 만기 안내 → 자동 재예치 안내 → 변경은 앱이나 지점 방문 안내\n" +
                    "중요: 절대 개인정보(주민번호, 카드번호, OTP)를 요구하지 마세요";
            default -> common + "역할: 일반 상담원\n";
        };
    }
}
