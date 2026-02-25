import os
import io
from elevenlabs import ElevenLabs
from audio_effects import apply_voip_effect

client = None


def get_client():
    global client
    if client is None:
        api_key = os.getenv("ELEVENLABS_API_KEY")
        if not api_key:
            raise ValueError("ELEVENLABS_API_KEY 환경변수가 설정되지 않았습니다")
        client = ElevenLabs(api_key=api_key)
    return client


async def tts_stream(text: str, voice_id: str, settings, voip_effect: bool = False):
    """ElevenLabs 스트리밍 TTS - 청크 단위로 yield"""
    c = get_client()

    audio_stream = c.text_to_speech.convert(
        text=text,
        voice_id=voice_id,
        model_id="eleven_multilingual_v2",
        output_format="mp3_44100_128",
        voice_settings={
            "stability": settings.stability,
            "similarity_boost": settings.similarity_boost,
            "style": settings.style,
            "speed": settings.speed,
        },
    )

    if voip_effect:
        # VoIP 효과 적용 시: 전체 오디오를 모은 후 효과 적용
        audio_bytes = b""
        for chunk in audio_stream:
            audio_bytes += chunk

        processed = apply_voip_effect(audio_bytes)
        # 청크 단위로 yield
        chunk_size = 4096
        for i in range(0, len(processed), chunk_size):
            yield processed[i:i + chunk_size]
    else:
        # VoIP 효과 없으면 바로 스트리밍
        for chunk in audio_stream:
            yield chunk


async def tts_full(text: str, voice_id: str, settings, voip_effect: bool = False) -> bytes:
    """ElevenLabs 전체 TTS - 전체 오디오를 bytes로 반환"""
    c = get_client()

    audio_stream = c.text_to_speech.convert(
        text=text,
        voice_id=voice_id,
        model_id="eleven_multilingual_v2",
        output_format="mp3_44100_128",
        voice_settings={
            "stability": settings.stability,
            "similarity_boost": settings.similarity_boost,
            "style": settings.style,
            "speed": settings.speed,
        },
    )

    audio_bytes = b""
    for chunk in audio_stream:
        audio_bytes += chunk

    if voip_effect:
        audio_bytes = apply_voip_effect(audio_bytes)

    return audio_bytes
