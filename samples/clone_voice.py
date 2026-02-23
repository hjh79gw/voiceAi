"""
ElevenLabs 음성 복제 (Instant Voice Clone)
- 30초 녹음 파일로 목소리를 복제하고
- 복제된 목소리로 보이스피싱/정상 상담원 대사를 생성한다
"""
import os
from elevenlabs import ElevenLabs

# 환경변수에서 API 키 로드
API_KEY = os.getenv("ELEVENLABS_API_KEY")
client = ElevenLabs(api_key=API_KEY)

OUTPUT_DIR = "C:/Users/Administrator/voiceai/samples"

PHISHING_TEXT = "안녕하세요, 서울중앙지방검찰청 수사관 김정수입니다. 본인 명의 계좌가 대포통장으로 사용되어 범죄에 연루된 것으로 확인되었습니다. 본인 확인을 위해 주민등록번호를 말씀해 주시기 바랍니다."

NORMAL_TEXT = "안녕하세요, 신한카드 고객센터입니다. 고객님의 카드 유효기간 만료가 다가와 안내드립니다. 새 카드를 등록된 주소로 발송해드릴까요?"


def clone_voice(name, voice_file):
    """녹음 파일로 음성 복제 → Voice ID 반환"""
    voice = client.voices.ivc.create(
        name=name,
        files=[open(voice_file, "rb")],
    )
    print(f"  복제 완료! Voice ID: {voice.voice_id}")
    return voice.voice_id


def generate_tts(text, voice_id, output_path):
    """복제된 음성으로 TTS 생성"""
    audio = client.text_to_speech.convert(
        text=text,
        voice_id=voice_id,
        model_id="eleven_multilingual_v2",
        output_format="mp3_44100_128"
    )
    with open(output_path, "wb") as f:
        for chunk in audio:
            f.write(chunk)


if __name__ == "__main__":
    # 사용법:
    # 1. 환경변수 설정: set ELEVENLABS_API_KEY=sk_xxxxx
    # 2. 녹음 파일 준비: my_voice.m4a (30초 이상)
    # 3. 실행: python clone_voice.py

    VOICE_FILE = f"{OUTPUT_DIR}/my_voice.m4a"

    print("=== ElevenLabs 음성 복제 ===\n")

    print("[1/3] 음성 복제 중...")
    voice_id = clone_voice("MyVoice", VOICE_FILE)

    print("[2/3] 보이스피싱 대사 생성...")
    generate_tts(PHISHING_TEXT, voice_id, f"{OUTPUT_DIR}/clone_phishing.mp3")
    print("  완료!")

    print("[3/3] 정상 상담원 대사 생성...")
    generate_tts(NORMAL_TEXT, voice_id, f"{OUTPUT_DIR}/clone_normal.mp3")
    print("  완료!")

    print(f"\nVoice ID: {voice_id}")
    print("이 ID를 .env에 저장하면 다시 복제할 필요 없음")
