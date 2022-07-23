package com.example.websoket.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chat implements Serializable {

    @Builder.Default
    private UUID id = UUID.randomUUID();
    private String name;
    private LocalDateTime createdAt = LocalDateTime.now();
}
