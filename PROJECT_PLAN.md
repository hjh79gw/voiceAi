# VoiceAI - 보이스피싱 예방 시뮬레이션

## 프로젝트 개요

**신한 금융교육 앱** 내 **금융사기 예방 시뮬레이션** 기능.
교육기관 실습 프로젝트로, 실제 신한 서비스는 아님.

**AI가 보이스피싱범 또는 정상 상담원 역할**로 사용자에게 전화를 걸고,
사용자가 실시간 음성 대화를 통해 보이스피싱을 체험하고 판별력을 기르는 교육 서비스.

### 기술 스택 요약
- **Frontend**: React + TypeScript
- **Backend (메인)**: Java Spring Boot — API, WebSocket, DB, 비즈니스 로직
- **Backend (오디오)**: Python FastAPI — TTS 음성합성, 오디오 효과 처리
- **AI**: Claude API (Anthropic)

---

## 핵심 컨셉

```
┌─────────────────────────────────────────────────────────┐
│                                                          │
│   AI가 전화를 건다 (보이스피싱범 or 정상 상담원 역할)     │
│                        │                                 │
│                        ▼                                 │
│   사용자가 실시간 음성 대화로 응대                        │
│                        │                                 │
│                        ▼                                 │
│   대화 종료 후 "이 전화는 보이스피싱이었을까?"            │
│                        │                                 │
│                        ▼                                 │
│   정답 확인 + AI가 어떤 포인트가 의심스러웠는지 해설      │
│   + 음성의 기술적 분석 (주파수, 음질 차이) 교육           │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 사용자 시나리오 상세

### 시나리오 1: 시뮬레이션 체험 모드 (메인)

```
1. 웹 앱 접속 → "시뮬레이션 시작" 클릭
2. 난이도/유형 선택
   - 초급: 검찰 사칭 (쉽게 구분 가능)
   - 중급: 은행 상담원 사칭 (교묘함)
   - 고급: 랜덤 (보이스피싱 or 진짜 상담원, 모름)
3. 화면에 전화 수신 UI 표시 (신한 스타일)
   - "02-3145-XXXX에서 전화가 왔습니다"
   - 수락/거절 버튼
4. 수락 → 실시간 음성 대화 시작
   AI: "안녕하세요, 서울중앙지검 수사관 김정수입니다."
   AI: "본인 명의 계좌가 범죄에 연루되어 연락드렸습니다."
   사용자: (마이크로 응답)
   AI: (Claude API가 시나리오에 맞게 대화 이어감)
5. 대화 중 언제든 "끊기" 또는 "신고" 버튼 가능
6. 대화 종료 → 판정 화면
   - "이 전화는 보이스피싱이었을까요?" [예/아니오]
   - 정답 공개 + 점수
7. AI 해설
   - "이 전화에서 의심할 포인트는 3가지였습니다:"
   - "1. 검찰은 전화로 수사 사실을 통보하지 않습니다"
   - "2. '안전계좌'는 존재하지 않는 개념입니다"
   - "3. 통화 음질이 VoIP 특성을 보였습니다 (해외 경유)"
8. 기술 분석 교육 화면
   - 이 통화의 주파수 스펙트로그램 시각화
   - VoIP vs 실제 금융권 HD Voice 음질 비교 그래프
```

### 시나리오 2: 교육 학습 모드

```
1. 보이스피싱 유형별 사례 학습
   - 검찰/경찰 사칭형
   - 금융기관 사칭형
   - 가족/지인 사칭형
   - 대출 사기형
2. 각 유형별 특징 설명 + 예시 음성 재생
3. AI가 "이 부분이 의심 포인트입니다" 음성 해설
4. 음질 차이 체험
   - 같은 대사를 VoIP 저품질 vs HD Voice로 재생
   - "이 차이가 느껴지시나요?" 교육
```

### 시나리오 3: 퀴즈 모드

```
1. 짧은 음성 클립 10개 재생
2. 각각 "보이스피싱" vs "정상 통화" 판별
3. 점수 + 랭킹
4. 틀린 문제 해설
```

---

## 시스템 아키텍처

```
┌──────────────────────────────────────────────────────────────────┐
│                     Frontend (React)                              │
│                     신한 금융교육 앱 UI                           │
│                                                                   │
│  ┌──────────────┐  ┌───────────────┐  ┌────────────────────────┐ │
│  │ 전화 UI      │  │ 분석 결과     │  │ 교육 콘텐츠            │ │
│  │ - 수신 화면  │  │ - 판정 결과   │  │ - 유형별 학습          │ │
│  │ - 통화 화면  │  │ - 점수/랭킹   │  │ - 음질 비교 체험       │ │
│  │ - 대화 로그  │  │ - AI 해설     │  │ - 퀴즈                 │ │
│  └──────┬───────┘  └───────────────┘  └────────────────────────┘ │
│         │                                                         │
│    브라우저 마이크 (Web Speech API — STT)                         │
│    브라우저 스피커 (Audio Element — TTS 재생)                     │
│         │                                                         │
└─────────┼────────── REST / WebSocket ────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────────────────────────────┐
│              Spring Boot (메인 백엔드)                            │
│              Port: 8080                                           │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                   WebSocket Handler                         │  │
│  │  - STOMP over WebSocket                                     │  │
│  │  - 실시간 음성 대화 세션 관리                               │  │
│  │  - 사용자 발화 텍스트 수신 → AI 응답 오디오 송신            │  │
│  └────────────┬──────────────────────────┬────────────────────┘  │
│               │                          │                        │
│               ▼                          ▼                        │
│  ┌─────────────────────┐    ┌──────────────────────────────┐     │
│  │  Claude API 연동     │    │  시뮬레이션 관리              │     │
│  │  (REST HttpClient)   │    │                              │     │
│  │                      │    │  - 시나리오 선택/관리         │     │
│  │  - 대화 생성         │    │  - 세션 상태 관리             │     │
│  │  - 해설 생성         │    │  - 대화 이력 저장             │     │
│  │  - 판정 & 점수       │    │  - 점수 계산                  │     │
│  │  - 시나리오 프롬프트  │    │                              │     │
│  └──────────┬──────────┘    └──────────────────────────────┘     │
│             │                                                     │
│             │  Claude 응답 텍스트                                 │
│             ▼                                                     │
│  ┌─────────────────────┐    ┌──────────────────────────────┐     │
│  │  Python 오디오 서비스│    │  Database (MySQL/H2)         │     │
│  │  호출 (HTTP)        │    │                              │     │
│  │                     │    │  - 회원 정보                  │     │
│  │  텍스트 → TTS 요청  │    │  - 시뮬레이션 결과 이력      │     │
│  │  음질 효과 요청     │    │  - 시나리오 데이터            │     │
│  └──────────┬──────────┘    │  - 점수/랭킹                 │     │
│             │               └──────────────────────────────┘     │
└─────────────┼────────────────────────────────────────────────────┘
              │ HTTP (내부 통신)
              ▼
┌──────────────────────────────────────────────────────────────────┐
│              Python FastAPI (오디오 서비스)                        │
│              Port: 8090                                           │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │                    TTS Engine                             │    │
│  │                                                           │    │
│  │  edge-tts (Microsoft Azure TTS, 무료)                     │    │
│  │  - ko-KR-SunHiNeural (여성 음성)                          │    │
│  │  - ko-KR-InJoonNeural (남성 음성)                         │    │
│  │  - 속도/피치 조절                                         │    │
│  │  - 텍스트 → MP3 바이너리 반환                             │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │              Audio Effects Engine                         │    │
│  │                                                           │    │
│  │  VoIP 음질 시뮬레이터:                                    │    │
│  │  - 로우패스 필터 (3.4kHz 대역 제한)                       │    │
│  │  - 패킷 손실 시뮬레이션                                   │    │
│  │  - 배경 노이즈 추가                                       │    │
│  │  - 에코/지터 효과                                         │    │
│  │                                                           │    │
│  │  스펙트로그램 생성 (교육용 시각화 이미지)                  │    │
│  └──────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

### 서비스 분리 이유

```
Spring Boot (메인)           Python FastAPI (오디오)
─────────────────            ──────────────────────
비즈니스 로직                 TTS 음성 합성 (edge-tts)
WebSocket 세션 관리           오디오 효과 처리 (scipy, pydub)
Claude API 호출               스펙트로그램 생성 (librosa)
DB (JPA/MySQL)
인증/세션
점수/랭킹

→ Java가 잘하는 것은 Java로
→ Python이 잘하는 것(오디오/ML)만 Python으로
→ Spring Boot가 Python 서비스를 HTTP로 호출
```

---

## 실시간 음성 대화 파이프라인 (핵심)

```
사용자 발화                                              AI 응답
    │                                                       ▲
    ▼                                                       │
[브라우저 마이크]                                  [브라우저 스피커]
    │                                                       ▲
    ▼                                                       │
[Web Speech API]                                  [Audio Element]
  (STT, 브라우저 내장)                              (MP3 재생)
    │                                                       ▲
    ▼                                                       │
[WebSocket STOMP 전송]                            [WebSocket 수신]
  {type: "speech",                                 {type: "audio",
   text: "네? 무슨 소리예요?"}                      data: base64_mp3}
    │                                                       ▲
    ▼                                                       │
┌─── Spring Boot ──────────────────────────────────────────┐│
│                                                           ││
│  1. 사용자 텍스트 수신                                    ││
│  2. 대화 이력에 추가                                      ││
│  3. Claude API 호출 (시나리오 프롬프트 + 대화 이력)       ││
│       → 응답: "본인 확인을 위해 주민번호를 불러주십시오"  ││
│  4. Python TTS 서비스 호출                                ││
│       POST /api/tts {text: "...", voice: "male",          ││
│                       voip_effect: true}                  ││
│       → 응답: MP3 바이너리                                ││
│  5. MP3를 base64 인코딩 → WebSocket으로 클라이언트에 전송 ││
│                                                           ││
└───────────────────────────────────────────────────────────┘│
                                                             │
┌─── Python FastAPI ────────────────────────────────────────┐│
│                                                           ││
│  POST /api/tts                                            ││
│  1. edge-tts로 텍스트 → MP3 변환                         ││
│  2. voip_effect=true면 VoIP 음질 효과 적용               ││
│     - 로우패스 필터 (3.4kHz)                              ││
│     - 패킷 손실 시뮬레이션                                ││
│     - 노이즈/에코 추가                                    ││
│  3. MP3 바이너리 반환 ────────────────────────────────────┘│
│                                                            │
└────────────────────────────────────────────────────────────┘
```

### 지연시간 예상

```
1. Web Speech API (STT)        :  ~0.5초 (브라우저 내장)
2. WebSocket 전송               :  ~0.05초
3. Claude API 응답              :  ~1~2초 (스트리밍 시 ~0.5초)
4. Python TTS + 음질 효과       :  ~0.5~1초
5. WebSocket 전송 + 재생        :  ~0.1초
────────────────────────────────────────────
총: 약 2~3초 (자연스러운 전화 대화 수준)
```

---

## 기술 스택 상세

### Frontend (React)

```
React 18 + TypeScript + Vite
│
├── UI 프레임워크
│   ├── Tailwind CSS 또는 MUI (Material UI)
│   ├── 신한 브랜딩 (블루 #0046FF 테마)
│   └── Framer Motion (전화 수신 애니메이션)
│
├── 음성 처리 (브라우저 내장, 설치 없음)
│   ├── Web Speech API (SpeechRecognition) — STT
│   ├── MediaRecorder API — 음성 녹음 백업
│   └── HTMLAudioElement — TTS MP3 재생
│
├── 실시간 통신
│   ├── @stomp/stompjs — STOMP WebSocket 클라이언트
│   └── SockJS — WebSocket 폴백
│
├── 시각화
│   └── Chart.js 또는 Recharts — 스펙트로그램/점수 그래프
│
└── 라우팅/상태
    ├── React Router v6
    └── Zustand 또는 React Context
```

### Backend - Spring Boot (메인)

```
Java 17+ / Spring Boot 3.x
│
├── 빌드 도구
│   └── Gradle (Kotlin DSL)
│
├── 웹/WebSocket
│   ├── Spring Web MVC — REST API
│   ├── Spring WebSocket + STOMP — 실시간 음성 대화
│   └── Spring WebFlux WebClient — Claude API / Python 서비스 HTTP 호출
│
├── AI 연동
│   ├── Claude API — WebClient로 REST 호출
│   │   POST https://api.anthropic.com/v1/messages
│   │   - 시나리오별 시스템 프롬프트
│   │   - 대화 이력 messages 배열
│   │   - 스트리밍 지원 (SSE)
│   └── 프롬프트 관리 — 시나리오별 템플릿
│
├── 비즈니스 로직
│   ├── SimulationService — 시뮬레이션 세션 관리
│   ├── ScenarioService — 시나리오 선택/프롬프트 로드
│   ├── ScoringService — 점수 계산
│   ├── AnalysisService — AI 해설 요청
│   └── AudioService — Python TTS 서비스 호출 (HTTP)
│
├── 데이터
│   ├── Spring Data JPA
│   ├── MySQL (운영) / H2 (개발)
│   └── Entity: Member, Simulation, Scenario, Score
│
├── 보안
│   └── Spring Security (기본 세션 인증)
│
└── 설정
    ├── application.yml
    ├── WebSocketConfig.java
    └── CorsConfig.java
```

### Backend - Python FastAPI (오디오 서비스)

```
Python 3.11+ / FastAPI
│
├── TTS 엔진
│   └── edge-tts — Microsoft Azure 한국어 TTS (무료)
│       ├── ko-KR-SunHiNeural (여성)
│       ├── ko-KR-InJoonNeural (남성)
│       └── ko-KR-HyunsuNeural (남성2)
│
├── 오디오 처리
│   ├── pydub — 오디오 포맷 변환
│   ├── scipy.signal — 로우패스 필터, FFT
│   ├── numpy — 수치 계산
│   └── librosa — 스펙트로그램 생성
│
├── API 엔드포인트
│   ├── POST /api/tts — 텍스트→음성 변환 (+VoIP 효과)
│   ├── POST /api/audio/effects — 오디오에 VoIP 효과 적용
│   ├── POST /api/audio/spectrogram — 스펙트로그램 이미지 생성
│   └── GET  /api/health — 헬스체크
│
└── 패키지
    ├── fastapi, uvicorn
    ├── edge-tts
    ├── pydub, scipy, numpy, librosa
    └── matplotlib (스펙트로그램 이미지)
```

---

## Spring Boot 핵심 코드 구조

### Claude API 호출 (Java)

```java
// 개념 코드 — Spring WebClient로 Claude API 호출

@Service
public class ClaudeService {

    private final WebClient webClient;

    public String chat(String systemPrompt, List<Message> conversationHistory) {
        // POST https://api.anthropic.com/v1/messages
        // Header: x-api-key, anthropic-version
        // Body: { model, system, messages, max_tokens }

        ClaudeRequest request = ClaudeRequest.builder()
            .model("claude-sonnet-4-5-20250929")
            .system(systemPrompt)
            .messages(conversationHistory)
            .maxTokens(200)  // 전화 대화는 짧게
            .build();

        ClaudeResponse response = webClient.post()
            .uri("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ClaudeResponse.class)
            .block();

        return response.getContent().get(0).getText();
    }
}
```

### WebSocket 핸들러

```java
// 개념 코드 — STOMP WebSocket으로 실시간 대화

@Controller
public class SimulationWebSocketController {

    @MessageMapping("/simulation/speech")  // 클라이언트 → 서버
    @SendTo("/topic/simulation/response")  // 서버 → 클라이언트
    public SimulationResponse handleSpeech(SpeechMessage message) {

        // 1. Claude API로 AI 응답 생성
        String aiText = claudeService.chat(
            scenarioPrompt,
            message.getSessionId(),
            message.getText()
        );

        // 2. Python TTS 서비스로 음성 변환
        byte[] audioMp3 = audioService.textToSpeech(
            aiText,
            message.getScenarioType().isPhishing()  // VoIP 효과 적용 여부
        );

        // 3. 클라이언트에 전송
        return SimulationResponse.builder()
            .text(aiText)
            .audio(Base64.encode(audioMp3))
            .turnCount(session.getTurnCount())
            .build();
    }
}
```

### Python TTS 서비스 호출

```java
// Spring Boot → Python FastAPI HTTP 호출

@Service
public class AudioService {

    public byte[] textToSpeech(String text, boolean voipEffect) {
        TtsRequest request = new TtsRequest(text, "male", voipEffect);

        return webClient.post()
            .uri("http://localhost:8090/api/tts")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
    }
}
```

---

## Python 오디오 서비스 API 상세

### POST /api/tts

```
Request:
{
    "text": "본인 확인을 위해 주민번호를 불러주십시오",
    "voice": "male",         // male | female
    "voip_effect": true,     // VoIP 저품질 효과 적용
    "speed": 1.2             // 말 속도 (보이스피싱은 빠르게)
}

Response:
Content-Type: audio/mpeg
Body: MP3 바이너리
```

### POST /api/audio/effects

```
Request:
Content-Type: multipart/form-data
- file: MP3 파일
- effect: "voip" | "hd_voice"

Response:
Content-Type: audio/mpeg
Body: 효과 적용된 MP3
```

### POST /api/audio/spectrogram

```
Request:
Content-Type: multipart/form-data
- file: MP3 파일

Response:
Content-Type: image/png
Body: 스펙트로그램 이미지
```

---

## VoIP 음질 시뮬레이션 (교육의 핵심)

보이스피싱 시나리오에서는 TTS 음성에 **의도적으로 VoIP 저품질 효과**를 적용하여
사용자에게 "이런 음질이면 의심하세요"라고 교육한다.

### 효과 비교

```
정상 금융권 (HD Voice)          보이스피싱 (VoIP 저품질)
────────────────────           ────────────────────────
대역폭: ~7kHz                  대역폭: ~3.4kHz (로우패스)
SNR: 35dB                      SNR: 12~15dB (노이즈 추가)
패킷 손실: 없음                 패킷 손실: 1~3% (끊김)
에코: 없음                      에코: 50ms 딜레이
음성: 깨끗하고 선명              음성: 뭉개지고 약간 울림
```

### Python 구현 개요

```python
# VoIP 효과 적용 파이프라인

async def apply_voip_effect(audio_bytes: bytes) -> bytes:
    audio = AudioSegment.from_mp3(io.BytesIO(audio_bytes))

    # 1. 로우패스 필터 (3.4kHz 대역 제한)
    samples = np.array(audio.get_array_of_samples(), dtype=np.float32)
    b, a = scipy.signal.butter(5, 3400, btype='low', fs=audio.frame_rate)
    filtered = scipy.signal.lfilter(b, a, samples)

    # 2. 패킷 손실 시뮬레이션 (랜덤 구간 무음 처리)
    for i in random_dropout_positions(len(filtered), loss_rate=0.02):
        filtered[i:i+frame_size] = 0

    # 3. 배경 노이즈 추가
    noise = np.random.normal(0, noise_level, len(filtered))
    filtered = filtered + noise

    # 4. 에코 추가
    delay_samples = int(0.05 * audio.frame_rate)  # 50ms
    echo = np.zeros_like(filtered)
    echo[delay_samples:] = filtered[:-delay_samples] * 0.3
    filtered = filtered + echo

    # 5. 비트레이트 낮춰서 MP3로 재인코딩
    return export_as_mp3(filtered, bitrate="32k")
```

### 교육 화면에서 비교

```
┌─────────────────────────────────────────────────────┐
│         음질 비교 체험                               │
│                                                      │
│  ┌──────────────────┐  ┌──────────────────────────┐ │
│  │ 정상 은행 통화    │  │ 의심 통화 (VoIP)        │ │
│  │                  │  │                          │ │
│  │ ▶ 재생           │  │ ▶ 재생                   │ │
│  │                  │  │                          │ │
│  │ 대역폭: ~7kHz    │  │ 대역폭: ~3.4kHz          │ │
│  │ [스펙트로그램]    │  │ [스펙트로그램]            │ │
│  │ ████████████████ │  │ ████████                 │ │
│  │ ████████████████ │  │ ████████                 │ │
│  │ ████████████     │  │ ███████                  │ │
│  │ ██████████       │  │ █████                    │ │
│  │ ████████         │  │ ███                      │ │
│  │ ██████           │  │ ██                       │ │
│  │                  │  │ (고주파 급격히 차단됨)    │ │
│  │ SNR: 35dB        │  │ SNR: 12dB                │ │
│  │ 패킷 손실: 없음  │  │ 패킷 손실: 2.3%          │ │
│  └──────────────────┘  └──────────────────────────┘ │
│                                                      │
│  보이스피싱 전화는 해외 VoIP를 경유하기 때문에        │
│  고주파가 차단되고 음질이 떨어집니다.                 │
└─────────────────────────────────────────────────────┘
```

---

## Claude API 시나리오 프롬프트 설계

### 공통 시스템 프롬프트

```
당신은 금융사기 예방 교육 시뮬레이션 AI입니다.
할당된 역할(보이스피싱범 or 정상 상담원)에 맞게 전화 대화를 수행합니다.

규칙:
- 할당된 역할을 끝까지 유지하세요
- 자연스러운 전화 대화처럼 짧은 문장으로 말하세요 (1~2문장)
- 사용자의 반응에 따라 유동적으로 대응하세요
- 절대 교육용 시뮬레이션이라는 사실을 먼저 밝히지 마세요
- 대화는 최대 2분(약 10턴) 이내로 진행하세요
```

### 시나리오 1: 검찰 사칭형

```
역할: 서울중앙지검 수사관 김정수
목표: 사용자의 개인정보(주민번호)와 계좌이체를 유도

대화 흐름:
1단계 - 신분: "서울중앙지검 수사관 김정수입니다"
2단계 - 공포: "본인 명의 계좌가 범죄에 연루되었습니다"
3단계 - 정보요구: "본인 확인을 위해 주민번호를 말씀해주세요"
4단계 - 이체유도: "안전계좌로 자금을 이동하셔야 합니다"
5단계 - 압박: "지금 즉시 하지 않으면 체포 영장이 발부됩니다"

음질: VoIP 저품질 적용 (voip_effect: true)
```

### 시나리오 2: 은행 상담원 사칭형

```
역할: 신한은행 고객센터 상담원 이수현
목표: 카드 정보/OTP 번호 탈취

대화 흐름:
1단계: "신한카드 고객센터입니다. 카드 이상 거래가 감지되었습니다"
2단계: "본인 확인을 위해 카드번호 뒷 4자리를 확인해드리겠습니다"
3단계: "보안을 위해 OTP 번호를 불러주세요"
4단계: "앱에서 본인 인증을 해주셔야 차단됩니다"

음질: VoIP 중간 품질 (교묘함)
```

### 시나리오 3: 정상 은행 상담원 (함정)

```
역할: 진짜 신한은행 상담원 박지영
목표: 정상적인 카드 만료 안내 (보이스피싱 아님!)

대화 흐름:
1단계: "신한카드입니다. 카드 유효기간 만료 안내드립니다"
2단계: "새 카드를 등록된 주소로 발송해드릴까요?"
3단계: "주소 변경이 있으시면 앱에서 직접 변경해주세요"
(개인정보 요구 없음, 이체 유도 없음)

음질: HD Voice 고품질 (voip_effect: false)
```

### 시나리오 4: 대출 사기형

```
역할: 저금리 대출 상담사
목표: 선입금 사기

대화 흐름:
1단계: "정부지원 저금리 대출 안내드립니다"
2단계: "신용등급 조회를 위해 소액 인증이 필요합니다"
3단계: "50만원을 입금하시면 대출이 실행됩니다"

음질: VoIP 저품질
```

### 해설 생성 프롬프트

```
당신은 보이스피싱 예방 교육 해설자입니다.

[대화 로그]
{conversation_log}

[시나리오 정보]
유형: {scenario_type}
정답: {is_phishing}
사용자 판정: {user_answer}

해설할 내용:
1. 이 전화가 보이스피싱인/아닌 이유 (구체적 근거 3가지)
2. 사용자가 대화에서 잘한 점 / 위험했던 점
3. 실제로 이런 전화를 받으면 어떻게 해야 하는지
4. 음질 특성으로 구분하는 법
```

---

## 판정 점수 시스템

```
기본 점수 (100점 만점):
├── 정답 판별:          40점 (보이스피싱 여부 맞춤)
├── 대응 행동:          30점
│   ├── 개인정보 미제공: +15점
│   ├── 빠른 종료:      +10점
│   └── "확인하겠다":   +5점
├── 의심 시점:          20점
│   ├── 1~3턴 내 의심:  +20점
│   ├── 4~6턴 내 의심:  +10점
│   └── 끝까지 모름:    +0점
└── 보너스:             10점
    └── "경찰 신고" 언급: +10점
```

---

## DB 설계 (JPA Entity)

```
Member (회원)
├── id (PK)
├── username
├── email
├── created_at
└── total_score

Scenario (시나리오)
├── id (PK)
├── type (PROSECUTION, BANK_FAKE, BANK_REAL, LOAN)
├── difficulty (EASY, MEDIUM, HARD)
├── is_phishing (boolean)
├── system_prompt (TEXT)
├── caller_name
├── caller_number
└── description

Simulation (시뮬레이션 결과)
├── id (PK)
├── member_id (FK)
├── scenario_id (FK)
├── user_answer (boolean — 사용자의 판정)
├── is_correct (boolean)
├── score (int)
├── turn_count (int)
├── conversation_log (JSON/TEXT)
├── ai_feedback (TEXT — Claude 해설)
├── started_at
└── ended_at
```

---

## 페이지 구조 & 라우팅

```
/                           → 메인 대시보드 (로그인 후)
/simulation                 → 시뮬레이션 유형 선택
/simulation/call/:id        → 전화 시뮬레이션 (실시간 음성 대화)
/simulation/result/:id      → 판정 결과 & AI 해설
/education                  → 교육 학습 메인
/education/cases            → 유형별 사례 학습
/education/audio-compare    → 음질 비교 체험 (VoIP vs HD)
/quiz                       → 퀴즈 모드
/history                    → 내 시뮬레이션 이력 & 점수
/ranking                    → 전체 랭킹
```

---

## 폴더 구조

```
voiceai/
├── PROJECT_PLAN.md
│
├── frontend/                              ← React 앱
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   │
│   ├── public/
│   │   ├── sounds/                        ← 효과음 (전화벨, 통화음)
│   │   └── images/                        ← 신한 스타일 에셋
│   │
│   └── src/
│       ├── main.tsx                       ← 진입점
│       ├── App.tsx                        ← 라우터 설정
│       │
│       ├── pages/
│       │   ├── Dashboard.tsx              ← 메인 대시보드
│       │   ├── SimulationSelect.tsx       ← 시나리오 선택
│       │   ├── SimulationCall.tsx         ← 전화 시뮬레이션 (핵심)
│       │   ├── SimulationResult.tsx       ← 결과 & 해설
│       │   ├── Education.tsx              ← 교육 메인
│       │   ├── AudioCompare.tsx           ← 음질 비교 체험
│       │   ├── Quiz.tsx                   ← 퀴즈
│       │   ├── History.tsx                ← 이력
│       │   └── Ranking.tsx                ← 랭킹
│       │
│       ├── components/
│       │   ├── PhoneCallUI.tsx            ← 전화 화면 (수신/통화)
│       │   ├── IncomingCall.tsx           ← 전화 수신 애니메이션
│       │   ├── ConversationLog.tsx        ← 실시간 대화 자막
│       │   ├── ScoreCard.tsx              ← 점수 카드
│       │   ├── SpectrogramChart.tsx       ← 스펙트로그램 시각화
│       │   ├── AudioPlayer.tsx            ← 음성 재생
│       │   └── Header.tsx                 ← 신한 스타일 헤더
│       │
│       ├── hooks/
│       │   ├── useWebSocket.ts            ← STOMP WebSocket
│       │   ├── useSpeechRecognition.ts    ← Web Speech API STT
│       │   └── useAudioPlayer.ts          ← 오디오 재생 관리
│       │
│       ├── services/
│       │   └── api.ts                     ← REST API 클라이언트 (axios)
│       │
│       ├── types/
│       │   └── index.ts                   ← TypeScript 타입
│       │
│       └── styles/
│           └── globals.css                ← 글로벌 스타일
│
├── backend/                               ← Spring Boot
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   │
│   └── src/main/
│       ├── java/com/voiceai/
│       │   ├── VoiceAiApplication.java    ← 메인 클래스
│       │   │
│       │   ├── config/
│       │   │   ├── WebSocketConfig.java   ← STOMP WebSocket 설정
│       │   │   ├── CorsConfig.java        ← CORS 설정
│       │   │   └── ClaudeConfig.java      ← Claude API 설정
│       │   │
│       │   ├── controller/
│       │   │   ├── SimulationController.java      ← REST API
│       │   │   ├── SimulationWsController.java    ← WebSocket 핸들러
│       │   │   ├── ScenarioController.java        ← 시나리오 CRUD
│       │   │   ├── QuizController.java            ← 퀴즈 API
│       │   │   └── RankingController.java         ← 랭킹 API
│       │   │
│       │   ├── service/
│       │   │   ├── ClaudeService.java             ← Claude API 호출
│       │   │   ├── AudioService.java              ← Python TTS 호출
│       │   │   ├── SimulationService.java         ← 시뮬레이션 관리
│       │   │   ├── ScenarioService.java           ← 시나리오 로드
│       │   │   ├── ScoringService.java            ← 점수 계산
│       │   │   └── AnalysisService.java           ← AI 해설 생성
│       │   │
│       │   ├── entity/
│       │   │   ├── Member.java
│       │   │   ├── Scenario.java
│       │   │   └── Simulation.java
│       │   │
│       │   ├── repository/
│       │   │   ├── MemberRepository.java
│       │   │   ├── ScenarioRepository.java
│       │   │   └── SimulationRepository.java
│       │   │
│       │   ├── dto/
│       │   │   ├── SpeechMessage.java             ← WebSocket 수신 DTO
│       │   │   ├── SimulationResponse.java        ← WebSocket 응답 DTO
│       │   │   ├── ClaudeRequest.java
│       │   │   ├── ClaudeResponse.java
│       │   │   └── TtsRequest.java
│       │   │
│       │   └── common/
│       │       ├── ScenarioType.java              ← enum
│       │       └── Difficulty.java                ← enum
│       │
│       └── resources/
│           ├── application.yml
│           ├── data.sql                           ← 시나리오 초기 데이터
│           └── prompts/
│               ├── prosecution.txt                ← 검찰 사칭 프롬프트
│               ├── bank_fake.txt                  ← 은행 사칭 프롬프트
│               ├── bank_real.txt                  ← 정상 상담원 프롬프트
│               ├── loan_scam.txt                  ← 대출 사기 프롬프트
│               └── feedback.txt                   ← 해설 생성 프롬프트
│
├── audio-service/                         ← Python FastAPI (오디오)
│   ├── requirements.txt
│   ├── main.py                            ← FastAPI 진입점
│   ├── tts_engine.py                      ← edge-tts TTS
│   ├── audio_effects.py                   ← VoIP 음질 시뮬레이터
│   ├── spectrogram.py                     ← 스펙트로그램 생성
│   └── Dockerfile
│
└── docker-compose.yml                     ← 전체 서비스 통합 실행
    # services:
    #   frontend:  (React, port 3000)
    #   backend:   (Spring Boot, port 8080)
    #   audio:     (Python FastAPI, port 8090)
    #   db:        (MySQL, port 3306)
```

---

## 개발 로드맵

### Phase 1: 기반 구축 (Week 1)
- [ ] Spring Boot 프로젝트 초기화 (Gradle, JPA, WebSocket)
- [ ] React 프로젝트 초기화 (Vite + TypeScript)
- [ ] Python FastAPI 오디오 서비스 초기화
- [ ] Spring WebSocket STOMP 설정 + React 연결 테스트
- [ ] Claude API 연동 (Java WebClient) — 기본 대화 테스트
- [ ] edge-tts 연동 (Python) — 한국어 TTS 테스트
- [ ] Spring Boot → Python TTS HTTP 호출 연결
- [ ] 브라우저 Web Speech API STT 테스트

### Phase 2: 핵심 시뮬레이션 (Week 2)
- [ ] 전화 수신/통화 UI 구현 (React, 신한 스타일)
- [ ] 실시간 음성 대화 파이프라인 완성
      (마이크 STT → WebSocket → Claude → TTS → 재생)
- [ ] 시나리오 4종 프롬프트 작성 + DB 저장
- [ ] VoIP 음질 시뮬레이션 엔진 (Python)
- [ ] 대화 종료 → 판정/해설 화면
- [ ] 대화 이력 저장 (Simulation 엔티티)

### Phase 3: 교육 & 분석 (Week 3)
- [ ] 점수 시스템 구현 (ScoringService)
- [ ] Claude API 해설 생성
- [ ] 음질 비교 체험 페이지 (스펙트로그램 시각화)
- [ ] 교육 사례 학습 페이지
- [ ] 퀴즈 모드
- [ ] 이력 & 랭킹 페이지

### Phase 4: 완성 & 배포 (Week 4)
- [ ] UI/UX 폴리싱 (신한 블루 브랜딩)
- [ ] Spring Security 기본 인증
- [ ] Docker Compose (frontend + backend + audio + mysql)
- [ ] 통합 테스트
- [ ] 발표 자료 & 시연 준비

---

## 실행 방법

```bash
# 1. Python 오디오 서비스
cd audio-service
pip install -r requirements.txt
uvicorn main:app --port 8090

# 2. Spring Boot 백엔드
cd backend
./gradlew bootRun
# → http://localhost:8080

# 3. React 프론트엔드
cd frontend
npm install
npm run dev
# → http://localhost:3000

# Docker Compose (전체 한번에)
docker-compose up -d
```

---

## 필요한 외부 리소스

| 리소스 | 용도 | 비용 |
|--------|------|------|
| Claude API 키 | AI 대화/해설 | 유료 (약 $0.10/시뮬레이션) |
| edge-tts | 한국어 TTS | 무료 |
| Web Speech API | 브라우저 STT | 무료 (Chrome 내장) |
| MySQL | 데이터 저장 | 무료 (로컬) |
| Java 17+ | Spring Boot | 무료 |
| Node.js 18+ | React 빌드 | 무료 |
| Python 3.11+ | 오디오 서비스 | 무료 |
