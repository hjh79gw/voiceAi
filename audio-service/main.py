from __future__ import annotations

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse, Response
from pydantic import BaseModel
from dotenv import load_dotenv
from typing import Optional
import os

from tts_engine import tts_stream, tts_full
from audio_effects import apply_voip_effect

load_dotenv()

app = FastAPI(title="VoiceAI Audio Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:8080"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class TtsRequest(BaseModel):
    text: str
    voice_id: Optional[str] = None
    scenario_type: str = "normal"  # prosecution | bank_fake | bank_real_low | bank_real_high
    voip_effect: bool = False


class VoiceSettings(BaseModel):
    stability: float = 0.7
    similarity_boost: float = 0.85
    style: float = 0.2
    speed: float = 0.95


SCENARIO_VOICE_SETTINGS = {
    "prosecution": VoiceSettings(stability=0.6, similarity_boost=0.85, style=0.3, speed=0.9),
    "bank_fake": VoiceSettings(stability=0.5, similarity_boost=0.85, style=0.5, speed=1.0),
    "bank_real_low": VoiceSettings(stability=0.7, similarity_boost=0.85, style=0.2, speed=0.95),
    "bank_real_high": VoiceSettings(stability=0.7, similarity_boost=0.85, style=0.2, speed=0.95),
    "normal": VoiceSettings(),
}


@app.get("/api/health")
async def health():
    return {"status": "ok"}


@app.post("/api/tts/stream")
async def tts_stream_endpoint(request: TtsRequest):
    """스트리밍 TTS - 실시간 대화용"""
    voice_id = request.voice_id or os.getenv("ELEVENLABS_VOICE_ID")
    settings = SCENARIO_VOICE_SETTINGS.get(request.scenario_type, VoiceSettings())

    async def generate():
        async for chunk in tts_stream(
            text=request.text,
            voice_id=voice_id,
            settings=settings,
            voip_effect=request.voip_effect,
        ):
            yield chunk

    return StreamingResponse(generate(), media_type="audio/mpeg")


@app.post("/api/tts")
async def tts_full_endpoint(request: TtsRequest):
    """전체 TTS - 해설용"""
    voice_id = request.voice_id or os.getenv("ELEVENLABS_VOICE_ID")
    settings = SCENARIO_VOICE_SETTINGS.get(request.scenario_type, VoiceSettings())

    audio_bytes = await tts_full(
        text=request.text,
        voice_id=voice_id,
        settings=settings,
        voip_effect=request.voip_effect,
    )

    return Response(content=audio_bytes, media_type="audio/mpeg")


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", "8090"))
    uvicorn.run(app, host="0.0.0.0", port=port)
