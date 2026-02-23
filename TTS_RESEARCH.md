# TTS 엔진 비교 연구

## 테스트 결과 요약

보이스피싱 시뮬레이션에 사용할 TTS 엔진을 비교 테스트했다.

### 테스트한 TTS 엔진

| TTS | 자연스러움 | 비용 | 한국어 | 비고 |
|-----|-----------|------|--------|------|
| edge-tts | 5/10 | 무료 | O | 발음 부자연스러움, 로봇 느낌 |
| OpenAI TTS (tts-1) | 6/10 | $15/1M자 | O | edge보다 낫지만 여전히 AI 느낌 |
| OpenAI TTS-HD | 6.5/10 | $30/1M자 | O | tts-1보다 약간 개선 |
| ElevenLabs (기본 음성) | 7/10 | 무료 10,000자/월 | O | 기본 제공 음성도 AI 느낌 |
| **ElevenLabs (음성 복제)** | **8.5/10** | **$5/월~** | **O** | **가장 자연스러움** |

### 결론: ElevenLabs 음성 복제 + VoIP 효과

- 기본 TTS들은 한국어에서 모두 AI 느낌이 남
- **ElevenLabs 음성 복제**(Instant Voice Clone)가 가장 자연스러움
- 여기에 **VoIP 효과를 입히면** TTS 티가 묻히고 전화 통화 느낌이 남
- 시뮬레이션 목적에 가장 적합

---

## ElevenLabs 음성 복제 (Voice Clone)

### 원리

```
사람 목소리 30초 녹음
    → ElevenLabs AI가 음성 특징 학습 (톤, 피치, 억양)
    → Voice ID 발급 (한 번만 하면 영구 사용)
    → 이후 아무 텍스트 → 그 사람 목소리로 음성 합성
```

### 복제된 Voice ID

| 버전 | 녹음 파일 | Voice ID | 비고 |
|------|----------|----------|------|
| v1 | my_voice.m4a | `tJ7UGo927OuQVp7IcAsa` | 첫 번째 녹음 |
| v2 | my_voice2.m4a | `REDACTED` | 두 번째 녹음 |

### 사용법 (Python)

```python
from elevenlabs import ElevenLabs

client = ElevenLabs(api_key="ELEVENLABS_API_KEY")

# 복제된 목소리로 TTS
audio = client.text_to_speech.convert(
    text="아무 텍스트나 입력하면 내 목소리로 나옴",
    voice_id="REDACTED",  # 복제된 Voice ID
    model_id="eleven_multilingual_v2",
    output_format="mp3_44100_128"
)

with open("output.mp3", "wb") as f:
    for chunk in audio:
        f.write(chunk)
```

### 비용

| 플랜 | 가격 | 글자 수 | 음성 복제 | 시뮬레이션 환산 |
|------|------|--------|----------|---------------|
| Free | $0 | 10,000자/월 | X | ~5회 |
| Starter | $5/월 | 30,000자/월 | O | ~15회 |
| Creator | $22/월 | 100,000자/월 | O | ~50회 |

- 1회 시뮬레이션 = 약 2,000자 (200자 × 10턴)
- Starter $5/월이면 개발 + 테스트 + 시연 충분

---

## VoIP 음질 시뮬레이션

### 목적

보이스피싱 전화는 해외 VoIP를 경유하므로 음질이 떨어진다.
TTS 음성에 **의도적으로 VoIP 저품질 효과**를 적용하여:
1. 보이스피싱 전화의 실제 음질을 재현
2. TTS의 부자연스러움을 가려줌 (일석이조)

### 적용 효과

| 효과 | 설명 | 파라미터 |
|------|------|---------|
| 로우패스 필터 | 3.4kHz 이상 고주파 차단 | cutoff=3400Hz, order=4 |
| 배경 노이즈 | 랜덤 가우시안 노이즈 추가 | amplitude=1.5% of max |
| 에코 | VoIP 특유의 에코 효과 | delay=50ms, gain=0.2 |
| 패킷 손실 | 랜덤 구간 무음 처리 | loss_rate=1.5%, frame=20ms |
| 비트레이트 저하 | MP3 저비트레이트 인코딩 | bitrate=32kbps |

### 음질 비교 (보이스피싱 vs 정상)

```
정상 금융권 (HD Voice)          보이스피싱 (VoIP)
────────────────────           ──────────────────
대역폭: ~7kHz                  대역폭: ~3.4kHz
SNR: 35dB                      SNR: 12~15dB
패킷 손실: 없음                 패킷 손실: 1~3%
에코: 없음                      에코: 50ms
음성: 깨끗하고 선명              음성: 뭉개지고 울림
```

### Python 구현

```python
def apply_voip_effect(input_path, output_path):
    audio = AudioSegment.from_mp3(input_path)
    samples = np.array(audio.get_array_of_samples(), dtype=np.float64)
    sample_rate = audio.frame_rate

    # 1. 로우패스 필터 (3.4kHz)
    b, a = butter(4, 3400 / (sample_rate / 2), btype='low')
    samples = lfilter(b, a, samples)

    # 2. 배경 노이즈
    noise = np.random.normal(0, np.max(np.abs(samples)) * 0.015, len(samples))
    samples = samples + noise

    # 3. 에코 (50ms)
    delay = int(0.05 * sample_rate)
    echo = np.zeros_like(samples)
    echo[delay:] = samples[:-delay] * 0.2
    samples = samples + echo

    # 4. 패킷 손실 (1.5%)
    frame_size = int(0.02 * sample_rate)
    num_frames = len(samples) // frame_size
    drop_count = int(num_frames * 0.015)
    for idx in np.random.choice(num_frames, drop_count, replace=False):
        start = idx * frame_size
        samples[start:start + frame_size] = 0

    # 5. 저비트레이트 MP3 출력
    samples = np.clip(samples, -32768, 32767).astype(np.int16)
    result = AudioSegment(samples.tobytes(), frame_rate=sample_rate,
                          sample_width=2, channels=audio.channels)
    result.export(output_path, format="mp3", bitrate="32k")
```

---

## 생성된 샘플 파일 목록

### edge-tts (무료)
```
1_phishing_male_original.mp3      — 보이스피싱 남성 원본
2_phishing_male_voip.mp3          — 보이스피싱 남성 VoIP
3_normal_female_hd.mp3            — 정상 상담원 여성 HD
4_phishing_female_original.mp3    — 보이스피싱 여성 원본
5_phishing_female_voip.mp3        — 보이스피싱 여성 VoIP
```

### OpenAI TTS
```
openai_1_phishing_male.mp3        — 보이스피싱 남성 (tts-1)
openai_2_normal_female.mp3        — 정상 상담원 여성 (tts-1)
openai_3_phishing_male_hd.mp3     — 보이스피싱 남성 (tts-1-hd)
openai_4_phishing_male_voip.mp3   — 보이스피싱 남성 VoIP
openai_5_phishing_male_hd_voip.mp3 — 보이스피싱 HD VoIP
```

### ElevenLabs (기본 음성)
```
eleven_1_phishing_male.mp3        — 보이스피싱 남성 (Daniel)
eleven_2_normal_female.mp3        — 정상 상담원 여성 (Sarah)
```

### ElevenLabs (음성 복제) ★ 채택
```
v1_clone_phishing.mp3             — v1 보이스피싱 원본
v1_clone_phishing_voip.mp3        — v1 보이스피싱 VoIP
v1_clone_normal.mp3               — v1 정상 상담원 원본

v2_clone_phishing.mp3             — v2 보이스피싱 원본
v2_clone_phishing_voip.mp3        — v2 보이스피싱 VoIP
v2_clone_normal.mp3               — v2 정상 상담원 원본
v2_clone_normal_voip.mp3          — v2 정상 상담원 VoIP
```

---

## MVP 적용 방향

```
개발 시: OpenAI TTS (저렴, 빠름) + VoIP 효과
시연 시: ElevenLabs 음성 복제 + VoIP 효과 (자연스러움)

→ Python 오디오 서비스의 tts_engine.py만 교체하면 됨
→ Spring Boot, React 코드 변경 없음
```

### 환경변수

```
# .env
ELEVENLABS_API_KEY=your_key_here
ELEVENLABS_VOICE_ID=your_voice_id_here
OPENAI_API_KEY=your_key_here
```
