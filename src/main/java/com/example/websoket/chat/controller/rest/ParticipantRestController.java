package com.example.websoket.chat.controller.rest;

import com.example.websoket.chat.model.Participant;
import com.example.websoket.chat.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ParticipantRestController {

    private final ParticipantService participantService;

    @GetMapping("/participants/{chatId}")
    public ResponseEntity<Set<Participant>> getParticipant(@PathVariable UUID chatId){
        return ResponseEntity.ok(participantService.getParticipants(chatId));
    }
}
