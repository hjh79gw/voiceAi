import { useRef, useCallback } from 'react';

export function useAudioStream() {
  const audioQueueRef = useRef<string[]>([]);
  const isPlayingRef = useRef(false);

  const playNext = useCallback(() => {
    if (audioQueueRef.current.length === 0) {
      isPlayingRef.current = false;
      return;
    }

    isPlayingRef.current = true;
    const base64Audio = audioQueueRef.current.shift()!;
    const audio = new Audio(`data:audio/mpeg;base64,${base64Audio}`);

    audio.onended = () => playNext();
    audio.onerror = () => playNext();
    audio.play().catch(() => playNext());
  }, []);

  const addToQueue = useCallback((base64Audio: string) => {
    audioQueueRef.current.push(base64Audio);
    if (!isPlayingRef.current) {
      playNext();
    }
  }, [playNext]);

  const clearQueue = useCallback(() => {
    audioQueueRef.current = [];
    isPlayingRef.current = false;
  }, []);

  return { addToQueue, clearQueue };
}
