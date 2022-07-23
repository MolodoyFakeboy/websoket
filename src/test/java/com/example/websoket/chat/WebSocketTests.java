package com.example.websoket.chat;

import com.example.websoket.chat.config.WebSocketConfig;
import com.example.websoket.chat.controller.rest.ChatRestController;
import com.example.websoket.chat.model.Chat;
import com.example.websoket.chat.model.Message;
import com.example.websoket.chat.service.ChatService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.example.websoket.chat.controller.ws.ChatWsController.CREATE_CHAT;
import static com.example.websoket.chat.controller.ws.ChatWsController.FETCH_CHAT_MESSAGES;
import static com.example.websoket.chat.controller.ws.ChatWsController.FETCH_CREATE_CHAT_EVENT;
import static com.example.websoket.chat.controller.ws.ChatWsController.SEND_MESSAGE_TO_ALL;
import static com.example.websoket.chat.controller.ws.ChatWsController.getFetchMessagesDestination;
import static com.example.websoket.chat.controller.ws.ChatWsController.getSendMessageToAll;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@AutoConfigureMockMvc
class WebSocketTests {

	@Value("${local.server.port}")
	private int port;
	private static WebClient client;
	private final ObjectMapper mapper;
	private final MockMvc mockMvc;
	private final SetOperations<String, Chat> setOperations;

	@BeforeAll
	public void setup() throws Exception {

		var runStopFrameHandler = new RunStopFrameHandler(new CompletableFuture<>());

		String wsUrl = "ws://127.0.0.1:" + port + WebSocketConfig.REGISTRY;

		var stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));

		stompClient.setMessageConverter(new MappingJackson2MessageConverter());

		StompSession stompSession = stompClient
				.connect(wsUrl, new StompSessionHandlerAdapter() {})
				.get(1, TimeUnit.SECONDS);

		client = WebClient.builder()
				.stompClient(stompClient)
				.stompSession(stompSession)
				.handler(runStopFrameHandler)
				.build();
	}

	@BeforeEach
	public void clear(){
		 Objects.requireNonNull(setOperations.members(ChatService.KEY))
				 .forEach(chat -> setOperations.remove(ChatService.KEY,chat));
	}

	@AfterAll
	public void tearDown() {

		if (client.getStompSession().isConnected()) {
			client.getStompSession().disconnect();
			client.getStompClient().stop();
		}
	}

	@SneakyThrows
	@Test
	void should_PassSuccessfully_When_CreateChat() {
		var stompSession = client.getStompSession();
		var runStopFrameHandler = client.getHandler();
		stompSession.subscribe(FETCH_CREATE_CHAT_EVENT, runStopFrameHandler);

		stompSession.send(CREATE_CHAT, "Real chat");

		byte[] result = (byte[]) runStopFrameHandler.getFuture().get();
		var chat = mapper.readValue(result, Chat.class);

		var mvcResponse = mockMvc
				.perform(get("/chats"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse();

		var chatFromResponse = mapper.readValue(mvcResponse.getContentAsString(),
				new TypeReference<Set<Chat>>() {
		});

		var firstChat = chatFromResponse.stream().findFirst().orElse(null);
		var firstChatId = firstChat.getId();

		stompSession.subscribe(getFetchMessagesDestination(firstChatId.toString()),runStopFrameHandler);

		var textMessage = "Message";
		stompSession.send(getSendMessageToAll(firstChatId.toString()),textMessage);

		byte[] messageResult = (byte[]) runStopFrameHandler.getFuture().get();

		var message = mapper.readValue(messageResult, Message.class);

		Assertions.assertEquals(chat.getId(),firstChatId);
		Assertions.assertEquals(textMessage,message.getMessageText());
	}

	private List<Transport> createTransportClient() {

		List<Transport> transports = new ArrayList<>(1);

		transports.add(new WebSocketTransport(new StandardWebSocketClient()));

		return transports;
	}

	@Data
	@AllArgsConstructor
	private static class RunStopFrameHandler implements StompFrameHandler {

		private CompletableFuture<Object> future;

		@Override
		public @NonNull Type getPayloadType(StompHeaders stompHeaders) {
			log.info(stompHeaders.toString());
			return byte[].class;
		}

		@Override
		public void handleFrame(@NonNull StompHeaders stompHeaders, Object o) {
			future.complete(o);
			future = new CompletableFuture<>();
		}
	}

	@Data
	@Builder
	private static class WebClient {

		private WebSocketStompClient stompClient;
		private StompSession stompSession;
		private String sessionToken;
		private RunStopFrameHandler handler;
	}
}
