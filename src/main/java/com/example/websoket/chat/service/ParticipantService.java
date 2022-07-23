package com.example.websoket.chat.service;

import com.example.websoket.chat.controller.ws.ParticipantWsController;
import com.example.websoket.chat.model.Participant;
import com.example.websoket.chat.util.ParticipantKeyHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParticipantService {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SetOperations<String, Participant> setOperations;

    private static final Map<String, Participant> sessionIdToParticipantMap = new ConcurrentHashMap<>();

    public void handleJoinChat(String sessionId, UUID participantId, UUID chatId) {

        log.info(String.format("Participant \"%s\" join in chat \"%s\".", sessionId, chatId));

        Participant participant = Participant.builder()
                .sessionId(sessionId)
                .id(participantId)
                .chatId(chatId)
                .build();

        sessionIdToParticipantMap.put(participant.getSessionId(), participant);

        setOperations.add(ParticipantKeyHelper.makeKey(chatId), participant);

        messagingTemplate.convertAndSend(
                ParticipantWsController.getFetchParticipantJoinInChatDestination(chatId),
                participant
        );
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        handleLeaveChat(event);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        handleLeaveChat(event);
    }

    private void handleLeaveChat(AbstractSubProtocolEvent event) {
        var headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());

        Optional.ofNullable(headerAccessor.getSessionId())
                .map(sessionIdToParticipantMap::remove)
                .ifPresent(participant -> {
                    var chatId = participant.getChatId();

                    log.info(String.format(
                            "Participant \"%s\" leave from \"%s\" chat.",
                                    participant.getSessionId(),
                                    chatId
                            )
                    );

                    String key = ParticipantKeyHelper.makeKey(chatId);

                    setOperations.remove(key, participant);

                    Optional
                            .ofNullable(setOperations.size(key))
                            .filter(size -> size == 0L)
                            .ifPresent(size -> chatService.deleteChat(chatId));

                    messagingTemplate.convertAndSend(
                            key,
                            participant
                    );
                });
    }

    public Set<Participant> getParticipants(UUID chatId) {
        return new HashSet<>(Optional
                .ofNullable(setOperations.members(ParticipantKeyHelper.makeKey(chatId)))
                .orElseGet(HashSet::new));
    }

}
