package com.example.websoket.chat.controller.ws;

import com.example.websoket.chat.model.Chat;
import com.example.websoket.chat.model.Message;
import com.example.websoket.chat.service.ChatService;
import com.example.websoket.chat.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    public static final String CREATE_CHAT = "/topic/chats.create";

    public static final String FETCH_CREATE_CHAT_EVENT = "/topic/chats.create.event";
    public static final String FETCH_DELETE_CHAT_EVENT = "/topic/chats.delete.event";

    public static final String SEND_MESSAGE_TO_ALL = "/topic/chats.{chat_id}.messages.send";
    public static final String SEND_MESSAGE_TO_PARTICIPANT =
            "/topic/chats.{chat_id}.participants.{participant_id}.messages.send";

    public static final String FETCH_CHAT_MESSAGES = "/topic/chats.{chat_id}.messages";
    public static final String FETCH_PERSONAL_CHAT_MESSAGES =
            "/topic/chats.{chat_id}.participant.{participant_id}.messages";

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ParticipantService participantService;

    @MessageMapping(CREATE_CHAT)
    public void createChat(String chatName) {
        chatService.createChat(chatName);
    }

    @MessageMapping(SEND_MESSAGE_TO_ALL)
    public void sendMessageToAll(
            @DestinationVariable("chat_id") String chatId,
            String message,
            @Header String simpSessionId) {

        sendMessage(
                getFetchMessagesDestination(chatId),
                simpSessionId,
                message
        );
    }

    @MessageMapping(SEND_MESSAGE_TO_PARTICIPANT)
    public void sendMessageToParticipant(
            @DestinationVariable("chat_id") String chatId,
            @DestinationVariable("participant_id") String participantId,
            String message,
            @Header String simpSessionId) {

        sendMessage(
                getFetchPersonalMessagesDestination(chatId, participantId),
                simpSessionId,
                message
        );
    }

    @SubscribeMapping(FETCH_PERSONAL_CHAT_MESSAGES)
    public Message fetchPersonalMessages(
            @DestinationVariable("chat_id") UUID chatId,
            @DestinationVariable("participant_id") UUID participantId,
            @Header String simpSessionId) {

        participantService.handleJoinChat(simpSessionId, participantId, chatId);

        return null;
    }

    @SubscribeMapping(FETCH_CHAT_MESSAGES)
    public Message fetchMessages() {
        return null;
    }

    @SubscribeMapping(FETCH_CREATE_CHAT_EVENT)
    public Chat fetchCreateChatEvent() {
        return null;
    }

    @SubscribeMapping(FETCH_DELETE_CHAT_EVENT)
    public Chat fetchDeleteChatEvent() {
        return null;
    }

    private void sendMessage(String destination, String sessionId, String message) {
        messagingTemplate.convertAndSend(
                destination,
                Message.builder()
                        .from(sessionId)
                        .messageText(message)
                        .build()
        );
    }

    public static String getFetchMessagesDestination(String chatId) {
        return FETCH_CHAT_MESSAGES.replace("{chat_id}", chatId);
    }

    public static String getSendMessageToAll(String chatId) {
        return SEND_MESSAGE_TO_ALL.replace("{chat_id}", chatId);
    }

    public static String getFetchPersonalMessagesDestination(String chatId, String participantId) {
        return FETCH_PERSONAL_CHAT_MESSAGES
                .replace("{chat_id}", chatId)
                .replace("{participant_id}", participantId);
    }

}
