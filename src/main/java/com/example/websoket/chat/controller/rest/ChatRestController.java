package com.example.websoket.chat.controller.rest;

import com.example.websoket.chat.model.Chat;
import com.example.websoket.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @GetMapping("/chats")
    public ResponseEntity<Set<Chat>> getChats(){
        return ResponseEntity.ok(chatService.getChats());
    }
}
