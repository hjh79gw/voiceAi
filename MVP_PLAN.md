# VoiceAI MVP - 2일 완성 계획

## MVP 핵심 기능 (이것만 만든다)

```
사용자가 시뮬레이션 시작
→ 전화 수신 화면 (신한 스타일)
→ 수락하면 AI가 음성으로 말한다 (보이스피싱범/상담원 역할)
→ 사용자가 마이크로 대답한다
→ AI가 다시 음성으로 응답한다 (실시간 대화)
→ 대화 끝나면 "보이스피싱이었을까?" 판정
→ AI가 해설해준다
```

**음성 대화가 핵심이다. 이것만 되면 MVP 성공.**

---

## MVP 범위

### 포함 (Day 1~2)

| 기능 | 설명 |
|------|------|
| 실시간 음성 대화 | 마이크 STT → Claude API → TTS → 스피커 재생 |
| 전화 UI | 수신 화면, 통화 화면, 실시간 자막 |
| 시나리오 3종 | 검찰 사칭, 은행 사칭, 정상 상담원 |
| VoIP 음질 효과 | 보이스피싱 시나리오에 저품질 음성 적용 |
| 판정 화면 | "보이스피싱이었을까?" + 정답 |
| AI 해설 | Claude가 왜 의심해야 했는지 설명 |

### 제외 (나중에)

퀴즈, 랭킹, 스펙트로그램, 음질비교 체험, 회원가입, DB 영구저장, Docker

---

## 아키텍처 (심플)

```
┌─────────────────────┐
│   React (3000)      │
│                     │
│ - 전화 UI           │
│ - Web Speech API    │◄──── 사용자 마이크 (STT)
│   (마이크 → 텍스트) │
│ - Audio 재생        │───── 사용자 스피커 (TTS 재생)
│                     │
└────────┬────────────┘
         │ WebSocket (STOMP)
         ▼
┌─────────────────────┐         ┌──────────────────────┐
│ Spring Boot (8080)  │         │ Python FastAPI (8090)│
│                     │  HTTP   │                      │
│ - WebSocket 세션    │────────►│ - edge-tts (TTS)     │
│ - Claude API 호출   │◄────────│ - VoIP 음질 효과     │
│ - 시나리오 관리     │         │                      │
│ - 인메모리 세션     │         └──────────────────────┘
└─────────────────────┘
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
[5] Spring Boot: Claude API에 첫 발화 요청
    system: "검찰 사칭 시나리오 프롬프트..."
    → Claude 응답: "안녕하세요, 서울중앙지검 수사관 김정수입니다."
    │
    ▼
[6] Spring Boot → Python: TTS 변환 요청
    POST http://localhost:8090/api/tts
    {text: "안녕하세요, 서울중앙지검...", voice: "male", voip_effect: true}
    → 응답: MP3 바이너리
    │
    ▼
[7] Spring Boot → React: WebSocket으로 전송
    {type: "ai_speech", text: "안녕하세요...", audio: "base64_mp3..."}
    │
    ▼
[8] React: AI 음성 재생 + 자막 표시
    스피커: "안녕하세요, 서울중앙지검 수사관 김정수입니다."
    │
    ▼
[9] React: 사용자 마이크 활성화 (Web Speech API)
    사용자: "네? 누구세요?"
    STT → 텍스트 변환
    │
    ▼
[10] React → Spring Boot: WebSocket으로 전송
     STOMP SEND /app/simulation/speech
     {sessionId: "abc123", text: "네? 누구세요?"}
     │
     ▼
[11] 5~7 반복 (Claude 응답 → TTS → 재생)
     │
     ▼
[12] 대화 종료 (사용자가 "끊기" 클릭 or 10턴 도달)
     │
     ▼
[13] React: 판정 화면
     "이 전화는 보이스피싱이었을까요?" [예] [아니오]
     │
     ▼
[14] Spring Boot: Claude API로 해설 생성
     → React에 해설 텍스트 + TTS 음성 전달
```

---

## Day 1: 파이프라인 구축

### 오전: 프로젝트 셋업 + 연결

```
1. Spring Boot 프로젝트 생성
   - spring-boot-starter-web
   - spring-boot-starter-websocket
   - spring-boot-starter-webflux (WebClient for Claude API)
   - lombok

2. React 프로젝트 생성
   - Vite + TypeScript
   - @stomp/stompjs (WebSocket)
   - axios

3. Python FastAPI 프로젝트 생성
   - fastapi, uvicorn
   - edge-tts
   - pydub, scipy, numpy

4. WebSocket 연결 테스트
   - Spring STOMP 설정
   - React에서 메시지 송수신 확인
```

### 오후: 핵심 파이프라인 완성

```
5. Claude API 연동 (Spring Boot)
   - ClaudeService.java
   - WebClient로 POST /v1/messages
   - 시나리오 프롬프트로 대화 테스트

6. TTS 연동 (Python)
   - edge-tts로 텍스트 → MP3
   - FastAPI 엔드포인트: POST /api/tts
   - Spring Boot에서 HTTP 호출 테스트

7. STT 연동 (React)
   - Web Speech API (SpeechRecognition)
   - useSpeechRecognition 훅
   - 마이크 → 텍스트 변환 테스트

8. 전체 파이프라인 연결
   - 마이크 → STT → WebSocket → Claude → TTS → 재생
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

10. 시나리오 3종 프롬프트 작성
    - 검찰 사칭 (보이스피싱)
    - 은행 사칭 (보이스피싱)
    - 정상 상담원 (정상)

11. VoIP 음질 효과 (Python)
    - 로우패스 필터 3.4kHz
    - 기본 노이즈 추가
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
│       │   └── useSpeechRecognition.ts ← 마이크 STT
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
│       │   │   ├── SimulationController.java     ← REST (세션 생성, 시나리오 목록)
│       │   │   └── SimulationWsController.java   ← WebSocket (음성 대화)
│       │   │
│       │   ├── service/
│       │   │   ├── ClaudeService.java            ← Claude API
│       │   │   ├── AudioService.java             ← Python TTS 호출
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
    ├── main.py                  ← FastAPI (2개 엔드포인트)
    ├── tts_engine.py            ← edge-tts 래퍼
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
← {userAnswer: true}  // 사용자가 "보이스피싱이다"라고 판정
→ {correct: true, score: 85, feedback: "AI 해설 텍스트...", feedbackAudio: "base64..."}
```

### Spring Boot WebSocket (STOMP)

```
# 클라이언트 → 서버
SEND /app/simulation/start     {sessionId: "abc123"}
SEND /app/simulation/speech    {sessionId: "abc123", text: "네? 누구세요?"}
SEND /app/simulation/end       {sessionId: "abc123"}

# 서버 → 클라이언트
SUBSCRIBE /user/queue/simulation
← {type:"ai_speech", text:"안녕하세요...", audio:"base64_mp3", turn:1}
← {type:"ai_speech", text:"본인 확인을...", audio:"base64_mp3", turn:2}
← {type:"call_ended", totalTurns:8}
```

### Python FastAPI

```
POST /api/tts
← {text:"안녕하세요 서울중앙지검...", voice:"male", voip_effect:true, speed:1.1}
→ Content-Type: audio/mpeg (MP3 바이너리)

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

spring:
  cors:
    allowed-origins: http://localhost:3000
```

### .env (Python)

```
HOST=0.0.0.0
PORT=8090
```

### 환경변수 체크리스트

```
[ ] CLAUDE_API_KEY  — Anthropic 콘솔에서 발급
[ ] Java 17+       — java -version
[ ] Node.js 18+    — node -v
[ ] Python 3.11+   — python --version
[ ] ffmpeg 설치    — pydub가 사용 (choco install ffmpeg)
```

---

## 시연 시나리오 (2분)

```
1. 웹 앱 접속 → 시나리오 선택 화면 보여줌 (10초)
2. "검찰 사칭" 선택 → 전화 수신 화면 등장 (5초)
3. 수락 → AI가 음성으로 말함 (10초)
   "안녕하세요, 서울중앙지검 수사관 김정수입니다."
4. 마이크로 대답 → AI가 다시 응답 (3~4턴, 60초)
5. "끊기" 클릭 → 판정 화면 (10초)
   "이 전화는 보이스피싱이었을까요?" → [예] 클릭
6. AI 해설 화면 (20초)
   "정답입니다! 이 전화에서 의심할 포인트는..."
   + AI 음성으로 해설 재생
```
