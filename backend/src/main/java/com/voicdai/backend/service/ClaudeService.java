package com.voicdai.backend.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface ClaudeService {

    void streamChat(String systemPrompt,
                    List<Map<String, String>> conversationHistory,
                    Consumer<String> onToken,
                    Runnable onComplete);

    String chat(String systemPrompt, List<Map<String, String>> messages);
}
