package com.example.websoket.chat.util;

import java.util.UUID;

public class ParticipantKeyHelper {

    private ParticipantKeyHelper(){

    }

    private static final String KEY = "participant:chat:chats:{chat_id}:participants";

    public static String makeKey(UUID chatId) {
        return KEY.replace("{chat_id}", chatId.toString());
    }
}
