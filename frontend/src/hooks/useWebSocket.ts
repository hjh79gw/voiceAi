import { useRef, useCallback, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { WsMessage } from '../types';

export function useWebSocket() {
  const clientRef = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);

  const connect = useCallback((sessionId: string, onMessage: (msg: WsMessage) => void) => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/simulation/${sessionId}`, (message) => {
          const data: WsMessage = JSON.parse(message.body);
          onMessage(data);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      },
    });

    client.activate();
    clientRef.current = client;
  }, []);

  const send = useCallback((destination: string, body: object) => {
    if (clientRef.current?.connected) {
      clientRef.current.publish({
        destination: `/app${destination}`,
        body: JSON.stringify(body),
      });
    }
  }, []);

  const disconnect = useCallback(() => {
    clientRef.current?.deactivate();
    setConnected(false);
  }, []);

  return { connect, send, disconnect, connected };
}
