package com.example.websoket.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    private String from;

    private String messageText;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
