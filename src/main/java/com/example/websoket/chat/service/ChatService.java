package com.example.websoket.chat.service;

import com.example.websoket.chat.controller.ws.ChatWsController;
import com.example.websoket.chat.model.Chat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.example.websoket.chat.controller.ws.ChatWsController.FETCH_CREATE_CHAT_EVENT;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SetOperations<String, Chat> setOperations;

    public static final String KEY = "holder:chat:chats";

    public void createChat(String chatName) {
        log.info(String.format("Chat \"%s\" created.", chatName));

        var chat = Chat.builder()
                .name(chatName)
                .build();

        setOperations.add(KEY, chat);

        messagingTemplate.convertAndSend(
                FETCH_CREATE_CHAT_EVENT,
                chat
        );
    }

    public void deleteChat(UUID chatId) {
        getChats().stream()
                .filter(chat -> Objects.equals(chatId, chat.getId()))
                .findAny()
                .ifPresent(chat -> {
                    log.info(String.format("Chat \"%s\" deleted.", chat.getName()));
                    setOperations.remove(KEY, chat);
                    messagingTemplate.convertAndSend(
                            ChatWsController.FETCH_DELETE_CHAT_EVENT,
                            chat
                    );
                });
    }

    public Set<Chat> getChats() {
        return new HashSet<>(Optional
                .ofNullable(setOperations.members(KEY))
                .orElseGet(HashSet::new));
    }

}
