# VoiceAI MVP - 2일 완성 계획

## MVP 핵심 기능 (이것만 만든다)

```
사용자가 시뮬레이션 시작
→ 전화 수신 화면 (신한 스타일)
→ 수락하면 AI가 음성으로 말한다 (보이스피싱범/상담원 역할)
→ 사용자가 마이크로 대답한다
→ AI가 다시 음성으로 응답한다 (실시간 스트리밍 대화)
→ 대화 끝나면 "보이스피싱이었을까?" 판정
→ AI가 해설해준다
```

**음성 대화가 핵심이다. 스트리밍으로 체감 1.5~2초 지연.**

---

## TTS 엔진: ElevenLabs (음성 복제)

### 선정 이유

edge-tts, OpenAI TTS, ElevenLabs 기본 음성 비교 테스트 결과
→ **ElevenLabs 음성 복제**가 가장 자연스러움 (TTS_RESEARCH.md 참고)

### Voice ID

| 버전 | Voice ID | 비고 |
|------|----------|------|
| v1 | `tJ7UGo927OuQVp7IcAsa` | 첫 번째 녹음 |
| v2 | `REDACTED` | 두 번째 녹음 (채택) |

### 음성 세팅 (시나리오별)

```
보이스피싱 (검찰 사칭 — 강압적):
  stability: 0.6, similarity: 0.85, style: 0.3, speed: 0.9

보이스피싱 (은행 사칭 — 친절하지만 급한):
  stability: 0.5, similarity: 0.85, style: 0.5, speed: 1.0

정상 상담원 (차분):
  stability: 0.7, similarity: 0.85, style: 0.2, speed: 0.95
```

### 비용
- Starter $5/월 = 30,000자
- 시뮬레이션 1회 ~2,000자 → 약 15회 가능

---

## MVP 범위

### 포함 (Day 1~2)

| 기능 | 설명 |
|------|------|
| 실시간 음성 대화 (스트리밍) | STT → Claude 스트리밍 → ElevenLabs 스트리밍 → 즉시 재생 |
| 전화 UI | 수신 화면, 통화 화면, 실시간 자막 |
| 시나리오 3종 | 검찰 사칭, 은행 사칭, 정상 상담원 |
| VoIP 음질 효과 | 보이스피싱 시나리오에 저품질 음성 적용 |
| 판정 화면 | "보이스피싱이었을까?" + 정답 |
| AI 해설 | Claude가 왜 의심해야 했는지 설명 |

### 제외 (나중에)

퀴즈, 랭킹, 스펙트로그램, 음질비교 체험, 회원가입, DB 영구저장, Docker

---

## 아키텍처

```
┌─────────────────────┐
│   React (3000)      │
│                     │
│ - 전화 UI           │
│ - Web Speech API    │◄──── 사용자 마이크 (STT)
│   (마이크 → 텍스트) │
│ - Audio 스트리밍    │───── 사용자 스피커 (TTS 스트리밍 재생)
│   재생              │
│                     │
└────────┬────────────┘
         │ WebSocket (STOMP)
         ▼
┌─────────────────────┐         ┌──────────────────────┐
│ Spring Boot (8080)  │         │ Python FastAPI (8090)│
│                     │  HTTP   │                      │
│ - WebSocket 세션    │────────►│ - ElevenLabs TTS     │
│ - Claude API 호출   │◄────────│   (음성 복제 + 스트리밍)│
│   (스트리밍)        │         │ - VoIP 음질 효과     │
│ - 시나리오 관리     │         │                      │
│ - 인메모리 세션     │         └──────────────────────┘
└─────────────────────┘
```

---

## 스트리밍 음성 대화 파이프라인 (핵심)

### 기본 방식 (기다렸다 재생) — 3~4초

```
사용자 발화 → STT → Claude 전체 응답 대기 → TTS 전체 변환 대기 → 재생
```

### 스트리밍 방식 (채택) — 체감 1.5~2초

```
사용자: "네? 누구세요?" (마이크)
         │
         ▼ (~0.5초)
    Web Speech API → 텍스트 변환
         │
         ▼ (~0.1초)
    WebSocket → Spring Boot
         │
         ▼
    Claude API (스트리밍 모드)
    "본인" → "명의" → "계좌가" → "범죄에" → ... (토큰 단위로 흘러옴)
         │
         │  문장 단위로 끊어서 TTS 요청
         │  (마침표/물음표 기준으로 분리)
         │
         ▼ 첫 문장 완성 (~1초)
    "본인 명의 계좌가 범죄에 연루되었습니다."
         │
         ▼ (~0.5~1초)
    Python: ElevenLabs TTS 스트리밍 변환
         │
         ▼
    WebSocket → React: 오디오 청크 전송
         │
         ▼ 첫 음절 재생 시작 (체감 ~1.5초)
    스피커: "본인 명의 계좌가..."
         │
         │  동시에 다음 문장 Claude 생성 + TTS 변환 진행
         │
         ▼
    스피커: "...지금 바로 확인해야 합니다." (끊김 없이 이어짐)
```

### 핵심: 문장 단위 파이프라이닝

```java
// Spring Boot: Claude 스트리밍 응답을 문장 단위로 분리
StringBuilder buffer = new StringBuilder();

claudeStreamingResponse.doOnNext(token -> {
    buffer.append(token);

    // 마침표, 물음표, 느낌표에서 문장 분리
    if (token.matches(".*[.?!]\\s*")) {
        String sentence = buffer.toString();
        buffer.setLength(0);

        // 즉시 TTS 변환 요청 (비동기)
        audioService.streamTTS(sentence, sessionId);
    }
}).subscribe();
```

```python
# Python: ElevenLabs 스트리밍 TTS
@app.post("/api/tts/stream")
async def tts_stream(request: TtsRequest):
    audio_stream = client.text_to_speech.convert(
        text=request.text,
        voice_id=VOICE_ID,
        model_id="eleven_multilingual_v2",
        output_format="mp3_44100_128",
        voice_settings=get_voice_settings(request.scenario_type),
        stream=True  # 스트리밍 모드
    )

    # 오디오 청크를 바로 반환
    return StreamingResponse(audio_stream, media_type="audio/mpeg")
```

### 지연시간 비교

```
기본 방식:                      스트리밍 방식:
STT        0.5초                STT        0.5초
Claude     2.0초 (전체 대기)    Claude     1.0초 (첫 문장만)
TTS        1.5초 (전체 대기)    TTS        0.5초 (첫 문장만)
전송+재생  0.1초                전송+재생  0.1초
─────────────────              ─────────────────
총: ~4초                        총: ~2초 (체감 1.5초)
```

---

## 음성 대화 플로우 (상세)

```
[1] 사용자가 "시뮬레이션 시작" 클릭
    │
    ▼
[2] React: 시나리오 선택 → Spring Boot에 세션 생성 요청
    POST /api/simulation/start {scenarioId: 1}
    → 응답: {sessionId: "abc123", callerName: "김정수", callerNumber: "02-3145-7890"}
    │
    ▼
[3] React: 전화 수신 UI 표시
    "02-3145-7890에서 전화가 왔습니다"
    [수락] [거절]
    │
    ▼ 수락 클릭
[4] React: WebSocket 연결 + AI 첫 발화 요청
    STOMP SEND /app/simulation/start {sessionId: "abc123"}
    │
    ▼
[5] Spring Boot: Claude API 스트리밍 호출
    → 첫 문장 완성 즉시 → Python TTS 스트리밍 요청
    → 오디오 청크를 WebSocket으로 React에 전송
    │
    ▼
[6] React: 오디오 청크 수신 → 즉시 재생 시작
    스피커: "여보세요. 서울중앙지검 수사관 김정수입니다."
    + 자막 실시간 표시
    │
    ▼
[7] AI 음성 재생 완료 → 사용자 마이크 자동 활성화
    사용자: "네? 누구세요?"
    Web Speech API → 텍스트 변환
    │
    ▼
[8] React → Spring Boot: WebSocket 전송
    {sessionId: "abc123", text: "네? 누구세요?"}
    │
    ▼
[9] 5~6 반복 (스트리밍: Claude → TTS → 즉시 재생)
    │
    ▼
[10] 대화 종료 (사용자 "끊기" 클릭 or 10턴 도달)
    │
    ▼
[11] React: 판정 화면
     "이 전화는 보이스피싱이었을까요?" [예] [아니오]
     │
     ▼
[12] Spring Boot: Claude API로 해설 생성
     → React에 해설 텍스트 + TTS 음성 전달
```

---

## Day 1: 파이프라인 구축

### 오전: 프로젝트 셋업 + 연결

```
1. Spring Boot 프로젝트 생성
   - spring-boot-starter-web
   - spring-boot-starter-websocket
   - spring-boot-starter-webflux (WebClient for Claude API 스트리밍)
   - lombok

2. React 프로젝트 생성
   - Vite + TypeScript
   - @stomp/stompjs (WebSocket)
   - axios

3. Python FastAPI 프로젝트 생성
   - fastapi, uvicorn
   - elevenlabs (TTS)
   - pydub, scipy, numpy (VoIP 효과)

4. WebSocket 연결 테스트
   - Spring STOMP 설정
   - React에서 메시지 송수신 확인
```

### 오후: 핵심 파이프라인 완성

```
5. Claude API 스트리밍 연동 (Spring Boot)
   - ClaudeService.java
   - WebClient SSE 스트리밍으로 POST /v1/messages
   - stream: true 옵션
   - 문장 단위 분리 로직 (마침표/물음표 기준)

6. ElevenLabs TTS 스트리밍 연동 (Python)
   - tts_engine.py: ElevenLabs 음성 복제 TTS
   - 스트리밍 응답 (StreamingResponse)
   - VoIP 효과 적용 옵션
   - FastAPI 엔드포인트: POST /api/tts/stream

7. STT 연동 (React)
   - Web Speech API (SpeechRecognition)
   - useSpeechRecognition 훅
   - 마이크 → 텍스트 변환 테스트

8. 전체 스트리밍 파이프라인 연결
   - 마이크 → STT → WebSocket → Claude 스트리밍
   - → 문장 단위 TTS 스트리밍 → WebSocket → 즉시 재생
   - 콘솔에서 동작 확인 (UI 없이)
```

---

## Day 2: UI + 시나리오 + 완성

### 오전: 전화 UI + 시나리오

```
9. 전화 UI 구현
   - IncomingCall: 전화 수신 화면 (수락/거절)
   - PhoneCallUI: 통화 화면 (자막, 끊기/신고 버튼)
   - 신한 블루 (#0046FF) 테마 적용
   - 오디오 스트리밍 재생 처리

10. 시나리오 3종 프롬프트 작성
    - 검찰 사칭 (보이스피싱) — 강압적, 권위적
    - 은행 사칭 (보이스피싱) — 친절하지만 급한
    - 정상 상담원 (정상) — 차분하고 친절

11. VoIP 음질 효과 (Python)
    - 로우패스 필터 3.4kHz
    - 노이즈 + 에코 + 패킷 손실
    - 보이스피싱 시나리오에만 적용
```

### 오후: 판정 + 해설 + 마무리

```
12. 판정 화면
    - "보이스피싱이었을까?" UI
    - 정답 공개 + 맞춤/틀림 표시

13. AI 해설
    - Claude API로 대화 분석 + 해설 생성
    - 해설 텍스트 표시 + TTS 음성 재생

14. 시뮬레이션 선택 화면
    - 시나리오 카드 3개 (난이도 표시)
    - 또는 랜덤 선택

15. 최종 테스트 & 시연 준비
    - 전체 플로우 테스트
    - UI 미세 조정
    - 시연 시나리오 리허설
```

---

## 파일 구조 (MVP 최소한)

```
voiceai/
├── PROJECT_PLAN.md          ← 전체 계획
├── MVP_PLAN.md              ← 이 파일
├── TTS_RESEARCH.md          ← TTS 비교 연구 결과
│
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx                    ← 라우터 (3페이지)
│       │
│       ├── pages/
│       │   ├── Home.tsx               ← 시나리오 선택
│       │   ├── SimulationCall.tsx     ← 전화 시뮬레이션 (핵심)
│       │   └── SimulationResult.tsx   ← 판정 & 해설
│       │
│       ├── components/
│       │   ├── IncomingCall.tsx        ← 전화 수신 화면
│       │   ├── PhoneCallUI.tsx        ← 통화 화면 + 자막
│       │   └── FeedbackCard.tsx       ← AI 해설 카드
│       │
│       ├── hooks/
│       │   ├── useWebSocket.ts        ← STOMP 연결
│       │   ├── useSpeechRecognition.ts ← 마이크 STT
│       │   └── useAudioStream.ts      ← 오디오 스트리밍 재생
│       │
│       └── types/
│           └── index.ts
│
├── backend/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/com/voiceai/
│       │   ├── VoiceAiApplication.java
│       │   │
│       │   ├── config/
│       │   │   ├── WebSocketConfig.java
│       │   │   └── CorsConfig.java
│       │   │
│       │   ├── controller/
│       │   │   ├── SimulationController.java     ← REST
│       │   │   └── SimulationWsController.java   ← WebSocket
│       │   │
│       │   ├── service/
│       │   │   ├── ClaudeService.java            ← Claude API 스트리밍
│       │   │   ├── AudioService.java             ← Python TTS 스트리밍 호출
│       │   │   └── SimulationService.java        ← 세션/시나리오 관리
│       │   │
│       │   └── dto/
│       │       ├── SpeechMessage.java
│       │       ├── SimulationResponse.java
│       │       └── TtsRequest.java
│       │
│       └── resources/
│           ├── application.yml
│           └── prompts/
│               ├── prosecution.txt
│               ├── bank_fake.txt
│               └── bank_real.txt
│
└── audio-service/
    ├── requirements.txt
    ├── main.py                  ← FastAPI 서버
    ├── tts_engine.py            ← ElevenLabs TTS 스트리밍
    └── audio_effects.py         ← VoIP 음질 효과
```

---

## API 명세 (MVP)

### Spring Boot REST API

```
GET  /api/scenarios
→ [{id:1, name:"검찰 사칭", difficulty:"EASY", description:"..."},
   {id:2, name:"은행 사칭", difficulty:"MEDIUM", description:"..."},
   {id:3, name:"정상 상담원", difficulty:"HARD", description:"..."}]

POST /api/simulation/start
← {scenarioId: 1}
→ {sessionId:"abc123", callerName:"김정수", callerNumber:"02-3145-7890"}

POST /api/simulation/{sessionId}/judge
← {userAnswer: true}
→ {correct: true, score: 85, feedback: "AI 해설...", feedbackAudio: "base64..."}
```

### Spring Boot WebSocket (STOMP)

```
# 클라이언트 → 서버
SEND /app/simulation/start     {sessionId: "abc123"}
SEND /app/simulation/speech    {sessionId: "abc123", text: "네? 누구세요?"}
SEND /app/simulation/end       {sessionId: "abc123"}

# 서버 → 클라이언트 (스트리밍)
SUBSCRIBE /user/queue/simulation
← {type:"ai_text", text:"본인 명의", turn:1}          ← 자막용 (텍스트 청크)
← {type:"ai_audio", audio:"base64_chunk", turn:1}     ← 음성 (오디오 청크)
← {type:"ai_text", text:"계좌가 범죄에", turn:1}
← {type:"ai_audio", audio:"base64_chunk", turn:1}
← {type:"turn_end", turn:1}                           ← 턴 종료 → 마이크 활성화
← {type:"call_ended", totalTurns:8}
```

### Python FastAPI

```
POST /api/tts/stream
← {text:"본인 명의 계좌가...", voice_id:"REDACTED",
   scenario_type:"prosecution", voip_effect:true}
→ Content-Type: audio/mpeg (스트리밍 MP3)

POST /api/tts
← {text:"해설 텍스트...", voice_id:"...", voip_effect:false}
→ Content-Type: audio/mpeg (전체 MP3, 해설용)

GET /api/health
→ {status: "ok"}
```

---

## 핵심 설정 파일

### application.yml (Spring Boot)

```yaml
server:
  port: 8080

claude:
  api-key: ${CLAUDE_API_KEY}
  model: claude-sonnet-4-5-20250929
  max-tokens: 150

audio-service:
  url: http://localhost:8090

elevenlabs:
  voice-id: ${ELEVENLABS_VOICE_ID}

spring:
  cors:
    allowed-origins: http://localhost:3000
```

### .env (Python audio-service)

```
HOST=0.0.0.0
PORT=8090
ELEVENLABS_API_KEY=your_key_here
ELEVENLABS_VOICE_ID=your_voice_id_here
```

### 환경변수 체크리스트

```
[ ] CLAUDE_API_KEY       — Anthropic 콘솔에서 발급
[ ] ELEVENLABS_API_KEY   — ElevenLabs 대시보드
[ ] ELEVENLABS_VOICE_ID  — 음성 복제 후 발급된 ID
[ ] Java 17+             — java -version
[ ] Node.js 18+          — node -v
[ ] Python 3.11+         — python --version
[ ] ffmpeg 설치          — pydub가 사용 (choco install ffmpeg)
```

---

## 시연 시나리오 (2분)

```
1. 웹 앱 접속 → 시나리오 선택 화면 보여줌 (10초)
2. "검찰 사칭" 선택 → 전화 수신 화면 등장 (5초)
3. 수락 → AI가 음성으로 말함 (~1.5초 후 바로 재생) (10초)
   "여보세요. 서울중앙지검 수사관 김정수입니다."
4. 마이크로 대답 → AI가 스트리밍으로 즉시 응답 (3~4턴, 60초)
5. "끊기" 클릭 → 판정 화면 (10초)
   "이 전화는 보이스피싱이었을까요?" → [예] 클릭
6. AI 해설 화면 (20초)
   "정답입니다! 이 전화에서 의심할 포인트는..."
   + AI 음성으로 해설 재생
```
