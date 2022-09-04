package com.example.problem;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class FfmpegStreamSocketHandler implements WebSocketHandler {
    private final FfmpegStreamRunner ffmpegStreamRunner;

    public FfmpegStreamSocketHandler(FfmpegStreamRunner ffmpegStreamRunner) {
        this.ffmpegStreamRunner = ffmpegStreamRunner;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.send(ffmpegStreamRunner.getFfmpegStream().map(dataBuffer -> new WebSocketMessage(WebSocketMessage.Type.BINARY, dataBuffer)));
    }

    @Override
    public List<String> getSubProtocols() {
        return List.of("mpeg-ts");
    }
}
