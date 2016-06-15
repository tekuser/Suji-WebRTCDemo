# webRtcDemo
This is a simple example of a chat service using Websockets. I use jetty for serving static files over http and Netty to handle webRTC communication.

This example is based on the khs-stockticker:

https://keyholesoftware.com/2015/03/16/netty-a-different-kind-of-websocket-server/
https://github.com/jwboardman/khs-stockticker


Technologies Used:
------------------
Jetty for the Http server
Netty for the WebSocket server
GSON for JSON data
Maven for the build tool


Instruction
------------------
1) Open a console and execute 'mvn jetty:run' to start the http server on the port 9080

2) In a new console, execute 'mvn test' to compile and run the Netty websocket server on the port 9090

3) Open a web browser and go to the url http://localhost:9080

Note: Jetty and Netty should be run on the same server because the websocket url is build using Jetty's hostname. This is hardcoded in the client javascript (webRtcDemo.js).
In a same manner, the port of the Netty server should also be 9090 because it is also hardcoded in the client javascript (webRtcDemo.js).


 




