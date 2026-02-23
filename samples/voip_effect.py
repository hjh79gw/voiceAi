"""
VoIP 음질 시뮬레이션
- TTS 음성에 VoIP 저품질 효과를 적용한다
- 보이스피싱 전화의 실제 음질을 재현
"""
import numpy as np
from scipy.signal import butter, lfilter
from pydub import AudioSegment


def apply_voip_effect(input_path, output_path):
    """MP3 파일에 VoIP 저품질 효과 적용"""
    audio = AudioSegment.from_mp3(input_path)
    samples = np.array(audio.get_array_of_samples(), dtype=np.float64)
    sample_rate = audio.frame_rate

    # 1. 로우패스 필터 (3.4kHz 대역 제한)
    nyquist = sample_rate / 2
    cutoff = 3400 / nyquist
    if cutoff < 1.0:
        b, a = butter(4, cutoff, btype='low')
        samples = lfilter(b, a, samples)

    # 2. 배경 노이즈 추가
    noise = np.random.normal(0, np.max(np.abs(samples)) * 0.015, len(samples))
    samples = samples + noise

    # 3. 에코 추가 (50ms 딜레이)
    delay = int(0.05 * sample_rate)
    echo = np.zeros_like(samples)
    echo[delay:] = samples[:-delay] * 0.2
    samples = samples + echo

    # 4. 패킷 손실 시뮬레이션 (1.5% 랜덤 프레임 드롭)
    frame_size = int(0.02 * sample_rate)  # 20ms 프레임
    num_frames = len(samples) // frame_size
    drop_count = int(num_frames * 0.015)
    drop_indices = np.random.choice(num_frames, drop_count, replace=False)
    for idx in drop_indices:
        start = idx * frame_size
        end = min(start + frame_size, len(samples))
        samples[start:end] = 0

    # 클리핑 방지 + 저비트레이트 MP3 출력
    samples = np.clip(samples, -32768, 32767).astype(np.int16)
    result = AudioSegment(
        samples.tobytes(),
        frame_rate=sample_rate,
        sample_width=2,
        channels=audio.channels
    )
    result.export(output_path, format="mp3", bitrate="32k")


if __name__ == "__main__":
    import sys
    if len(sys.argv) < 3:
        print("사용법: python voip_effect.py <입력.mp3> <출력.mp3>")
        sys.exit(1)

    apply_voip_effect(sys.argv[1], sys.argv[2])
    print(f"VoIP 효과 적용 완료: {sys.argv[2]}")
