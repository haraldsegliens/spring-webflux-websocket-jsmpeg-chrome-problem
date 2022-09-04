# spring-webflux-websocket-jsmpeg-chrome-problem
Minimal reproducible example of jsmpeg chrome vs spring webflux websocket server  

## About the problem

This application implements Spring webflux websocket server that forwards ffmpeg stream output to websocket protocol. 
This application allows multiple viewers to see the stream from their web browser by using jsmpeg javascript library.

This solution works as expected in Firefox web browser, but in Chrome web browser the stream fails no load. Chrome browser cannot get data from the websocket connection.

![image](https://user-images.githubusercontent.com/9857685/188263000-a1ce8383-73b2-4851-bec8-3cbe76623f12.png)

## How to reproduce the problem

1. Install FFMpeg: https://ffmpeg.org/download.html
2. Update ffmpeg-command variable in src/main/resources/application.yaml so it references the ffmpeg program in your system
3. Run Spring application. The application by default creates a following endpoint: ws://localhost:8080/stream
4. Open view_stream.html in Chrome browser
