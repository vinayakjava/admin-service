package com.pws.admin.controller;

import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;

public class MyWebSocketClient implements WebSocketHandler {

    private final WebSocketSession session;

    public MyWebSocketClient(String url) throws Exception {
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.doHandshake(this, new WebSocketHttpHeaders(), URI.create(url)).get();
        this.session = session;
    }

    public void send(String message) throws Exception {
        session.sendMessage(new TextMessage(message));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Connected to WebSocket server");
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        System.out.println("Received message from WebSocket server: " + message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("Error occurred in WebSocket transport");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        System.out.println("Disconnected from WebSocket server");
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}