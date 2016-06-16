package com.suji.webRtcDemo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;
import com.suji.webRtcDemo.data.ClientRequest;
import com.suji.webRtcDemo.data.ServerEvent;
import com.suji.webRtcDemo.data.ServerResponse;



public class WsMessageProcessor {
    
    private final Gson jsonSerializer = new Gson();
    
    private final ConcurrentHashMap<String, AtomicReference<Channel> > userMap = new ConcurrentHashMap<String, AtomicReference<Channel>>();
    
    public WsMessageProcessor() {
    }
    
    public void processMessage(final ChannelHandlerContext ctx, final String jsonData) {
    	
    	System.out.println("INFO - "+jsonData);
    	
        ClientRequest req = jsonSerializer.fromJson(jsonData, ClientRequest.class);
        
        if (req.getCommand() == null)
            sendResponse(ctx.channel(), "OP_ERROR", "", "No command provided");
        else if (req.getCommand().equalsIgnoreCase("connect"))
            processConnect(ctx, req);
        else if (req.getCommand().equalsIgnoreCase("sendMessage"))
            processSendMessage(ctx, req);
        else if (req.getCommand().equalsIgnoreCase("listUsers"))
            processListUsers(ctx, req);
        else
            sendResponse(ctx.channel(), "OP_ERROR", req.getCommand(), "invalid command: "+req.getCommand());
    }
    
    private void processConnect(final ChannelHandlerContext ctx, final ClientRequest req) {
        final String userId = req.getFromUserId();
        if (userId == null || userId.isEmpty()) {
            sendResponse(ctx.channel(), "OP_ERROR", req.getCommand(), "No user provided");
            return;
        }
        
        AtomicReference<Channel> oldChannel = userMap.putIfAbsent(userId, new AtomicReference<Channel>(ctx.channel()));
        
        if (oldChannel == null) {
            ctx.channel().closeFuture().addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    userMap.remove(userId);
                    broadcastDisconnectedUser(userId);
                }
            });
            sendResponse(ctx.channel(), "OP_SUCCESS", req.getCommand(), "user "+userId+" connected");
            broadcastConnectedUser(userId);
        }
        else
            sendResponse(ctx.channel(), "OP_ERROR", req.getCommand(), "user "+req.getFromUserId()+" already exist");
    }
    
    private void processSendMessage(final ChannelHandlerContext ctx, final ClientRequest req) {
        if (req.getToUserId() == null || req.getToUserId().isEmpty()) {
            sendResponse(ctx.channel(), "OP_ERROR", req.getCommand(), "No recipient user provided");
            return;
        }
        if (req.getMessage() == null || req.getMessage().isEmpty()) {
            sendResponse(ctx.channel(), "OP_ERROR", req.getCommand(), "No message to deliver");
            return;
        }
        
        final AtomicReference<Channel> rec = userMap.get(req.getToUserId());
        if (rec == null)
            sendResponse(ctx.channel(), "OP_ERROR", req.getCommand(), "no user "+req.getToUserId()+" found");
        else {
            ServerEvent serverEvent = new ServerEvent("MessageReceived", req.getFromUserId(), req.getMessage());
            
            sendData(rec.get(), serverEvent).addListener(new ChannelFutureListener() {
                public void operationComplete(final ChannelFuture future) {
                    if (future.isCancelled())
                        sendResponse(ctx.channel(), "OP_ERROR", req.getCommand(), "sendMessage operation was cancelled");
                    else if (!future.isSuccess()) {
                        sendResponse(ctx.channel(), "OP_ERROR", req.getCommand(), "sendMessage operation error: "+future.cause().getMessage());
                        rec.get().close();
                    }
                    else
                        sendResponse(ctx.channel(), "OP_SUCCESS", req.getCommand(), "message sent to "+req.getToUserId());
                }
            });
        }
    }
    
     private void processListUsers(final ChannelHandlerContext ctx, ClientRequest req) {
    	 String thisUserId = req.getFromUserId();
    	 StringBuffer listUsers = new StringBuffer();
         listUsers.append('[');
         for (String user : userMap.keySet()){
        	if (user.equals(thisUserId))
        		 continue;
            if (listUsers.length() != 1)
                listUsers.append(',');
            listUsers.append('\"').append(user).append('\"');
         }
         listUsers.append(']');
         sendResponse(ctx.channel(), "OP_SUCCESS", req.getCommand(), listUsers.toString());
     }
    
    private void broadcastDisconnectedUser(final String userid) {
        final ServerEvent evt = new ServerEvent("DisconnectedUser", userid, null);
        broadcastServerEvent(evt, userid);
    }
    
    private void broadcastConnectedUser(final String userid) {
        final ServerEvent evt = new ServerEvent("ConnectedUser", userid, null);
        broadcastServerEvent(evt, userid);
    }
    
    private void broadcastServerEvent(final ServerEvent evt, final String exceptUser) {
        for (Map.Entry<String, AtomicReference<Channel>> entry : userMap.entrySet()){
            final String currUser = entry.getKey();
            if (!currUser.equals(exceptUser)) {
                sendData(entry.getValue().get() , evt).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        }
    }
    
    private ChannelFuture sendData(final Channel channel, Object data) {
        return channel.writeAndFlush(new TextWebSocketFrame(jsonSerializer.toJson(data)));
    }
    
    private ChannelFuture sendResponse(final Channel channel, String statusType, String cmd, String message) {
        ServerResponse resp = new ServerResponse(statusType, cmd, message);
        return sendData(channel, resp);
    }

}

