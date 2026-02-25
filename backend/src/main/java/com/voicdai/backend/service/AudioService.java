package com.voicdai.backend.service;

public interface AudioService {

    byte[] textToSpeechStream(String text, String scenarioType, boolean voipEffect);

    byte[] textToSpeech(String text, String scenarioType, boolean voipEffect);
}
