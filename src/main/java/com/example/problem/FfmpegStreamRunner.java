package com.example.problem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class FfmpegStreamRunner implements DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FfmpegStreamRunner.class);
    private static final int dataBufferCapacity = 1024;
    private final String ffmpegCommand;

    private Process ffmpegProcess = null;

    private final Sinks.Many<DataBuffer> sink = Sinks.many().multicast().directAllOrNothing();
    private Disposable inputStreamSubscription = null;
    private Disposable errorStreamSubscription = null;

    private DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory(false, dataBufferCapacity);

    public FfmpegStreamRunner(@Value("${ffmpeg-command}") String ffmpegCommand) {
        this.ffmpegCommand = ffmpegCommand;
        startProcess();
        startSubscription();
    }

    private void startProcess() {
        String[] command = new String[] {
                ffmpegCommand,
                "-hide_banner",
                "-y",
                "-loglevel", "error",
                "-rtsp_transport", "tcp",
                "-i", "rtsp://rtsp.stream/pattern",
                "-f", "mpegts",
                "-codec:v", "mpeg1video",
                "-r", "30",
                "-vf", "scale=1024:768",
                "-"
        };

        LOGGER.info("ffmpegProcess running cmd: " + Arrays.toString(command));
        try {
            this.ffmpegProcess = new ProcessBuilder(command).start();
        } catch (IOException e) {
            LOGGER.error("Failed to start FFMPEG", e);
            throw new RuntimeException("Failed to start FFMPEG", e);
        }
    }

    private void startSubscription() {
        Flux<DataBuffer> inputStreamFlux = DataBufferUtils.readInputStream(
                ffmpegProcess::getInputStream,
                dataBufferFactory,
                dataBufferCapacity
        );
        this.inputStreamSubscription = inputStreamFlux
                .subscribeOn(Schedulers.newBoundedElastic(1, 1, "ffmpeg-input-listen"))
                .subscribe(sink::tryEmitNext);

        Flux<DataBuffer> errorStreamFlux = DataBufferUtils.readInputStream(
                ffmpegProcess::getErrorStream,
                dataBufferFactory,
                dataBufferCapacity
        );
        this.errorStreamSubscription = errorStreamFlux
                .map(dataBuffer -> dataBuffer.toString(StandardCharsets.UTF_8))
                .subscribeOn(Schedulers.newBoundedElastic(1, 1, "ffmpeg-error-listen"))
                .subscribe(LOGGER::error);

    }

    public Flux<DataBuffer> getFfmpegStream() {
        if(ffmpegProcess == null) {
            LOGGER.error("ffmpegProcess not started");
            return Flux.empty();
        }
        return sink.asFlux();
    }

    @Override
    public void destroy() throws Exception {
        if(this.inputStreamSubscription != null) {
            this.inputStreamSubscription.dispose();
            this.inputStreamSubscription = null;
        }

        if(this.errorStreamSubscription != null) {
            this.errorStreamSubscription.dispose();
            this.errorStreamSubscription = null;
        }

        if(this.ffmpegProcess != null) {
            this.ffmpegProcess.destroy();
            try {
                this.ffmpegProcess.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException("ffmpegProcess destroy interrupted", e);
            }
            this.ffmpegProcess = null;
        }
    }
}
