import io
import numpy as np
from scipy.signal import butter, lfilter
from pydub import AudioSegment


def apply_voip_effect(audio_bytes: bytes) -> bytes:
    """MP3 오디오에 VoIP 저품질 효과 적용"""
    audio = AudioSegment.from_mp3(io.BytesIO(audio_bytes))
    samples = np.array(audio.get_array_of_samples(), dtype=np.float64)
    sample_rate = audio.frame_rate

    # 1. 로우패스 필터 (3.4kHz - 전화 대역폭 제한)
    nyquist = sample_rate / 2
    cutoff = 3400 / nyquist
    if cutoff < 1.0:
        b, a = butter(4, cutoff, btype='low')
        samples = lfilter(b, a, samples)

    # 2. 배경 노이즈 추가
    max_amp = np.max(np.abs(samples)) if np.max(np.abs(samples)) > 0 else 1
    noise = np.random.normal(0, max_amp * 0.015, len(samples))
    samples = samples + noise

    # 3. 에코 효과 (50ms)
    delay = int(0.05 * sample_rate)
    if delay < len(samples):
        echo = np.zeros_like(samples)
        echo[delay:] = samples[:-delay] * 0.2
        samples = samples + echo

    # 4. 패킷 손실 시뮬레이션 (1.5%)
    frame_size = int(0.02 * sample_rate)  # 20ms 프레임
    if frame_size > 0:
        num_frames = len(samples) // frame_size
        drop_count = max(1, int(num_frames * 0.015))
        if num_frames > drop_count:
            drop_indices = np.random.choice(num_frames, drop_count, replace=False)
            for idx in drop_indices:
                start = idx * frame_size
                end = min(start + frame_size, len(samples))
                samples[start:end] = 0

    # 클리핑 방지 + int16 변환
    samples = np.clip(samples, -32768, 32767).astype(np.int16)

    # AudioSegment로 재구성
    result = AudioSegment(
        samples.tobytes(),
        frame_rate=sample_rate,
        sample_width=2,
        channels=audio.channels,
    )

    # 저비트레이트 MP3로 출력
    buffer = io.BytesIO()
    result.export(buffer, format="mp3", bitrate="64k")
    return buffer.getvalue()
