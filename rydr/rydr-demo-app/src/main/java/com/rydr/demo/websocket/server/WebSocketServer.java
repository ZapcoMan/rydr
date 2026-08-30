package com.rydr.demo.websocket.server;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
/**
 *
 * @author oi
 * @date 2019-01-27 08:49:19
 */
@ServerEndpoint("/websocket/{sid}")
@Component
public class WebSocketServer {

	// Jackson mapper used to build/parse WebSocket payloads
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	// Static variable to track the current number of online connections. Should be designed as thread-safe.
	private static int onlineCount = 0;
	// Thread-safe Set from the concurrent package, used to store the WebSocket object for each client.
	private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<WebSocketServer>();

	// The connection session with a specific client, used to send data to the client
	private Session session;

	// Receives sid
	private String sid = "";

	/**
	 * Method called when connection is successfully established
	 */
	@OnOpen
	public void onOpen(Session session, @PathParam("sid") String sid) {
		this.session = session;
		webSocketSet.add(this); // Add to the set
		addOnlineCount(); // Increment online count by 1
		System.out.println("New window started listening: " + sid + ", current online count: " + getOnlineCount());
		this.sid = sid;
		try {
			ObjectNode j = OBJECT_MAPPER.createObjectNode();
			j.put("to", sid);
			j.put("data", "{}");
			sendMessage(j.toString());
		} catch (IOException e) {
			System.out.println("WebSocket IO exception");
		}
	}

	/**
	 * Method called when connection is closed
	 */
	@OnClose
	public void onClose() {
		webSocketSet.remove(this); // Remove from the set
		subOnlineCount(); // Decrement online count by 1
		System.out.println("A connection was closed! Current online count: " + getOnlineCount());
	}

	/**
	 * Method called when a message is received from the client
	 *
	 * @param message Message sent from the client
	 */
	@OnMessage
	public void onMessage(String message, Session session) {
		System.out.println("Received message from window " + sid + ": " + message);
		// Broadcast message
		for (WebSocketServer item : webSocketSet) {
			try {
				JsonNode m = OBJECT_MAPPER.readTree(message);
				String sid = m.path("to").asText();
				if (item.sid.equals(sid)) {
					item.sendMessage(message);
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
		}
	}

	/**
	 *
	 * @param session
	 * @param error
	 */
	@OnError
	public void onError(Session session, Throwable error) {
		System.out.println("An error occurred");
		error.printStackTrace();
	}

	/**
	 * Server-initiated push
	 */
	public void sendMessage(String message) throws IOException {
		this.session.getBasicRemote().sendText(message);
	}

	/**
	 * Broadcast custom message
	 */
	public static void sendInfo(String message, @PathParam("sid") String sid) throws IOException {
		System.out.println("Pushing message to window " + sid + ", content: " + message);
		int count = 0;
		for (WebSocketServer item : webSocketSet) {
			System.out.println("Send count: "+count);
			try {
				// Here you can set to push only to this specific sid; if null, push to all
				if (sid == null) {
					item.sendMessage(message);
				} else if (item.sid.equals(sid)) {
					item.sendMessage(message);
				}
			} catch (IOException e) {
				continue;
			}
		}
	}

	public static synchronized int getOnlineCount() {
		return onlineCount;
	}

	public static synchronized void addOnlineCount() {
		WebSocketServer.onlineCount++;
	}

	public static synchronized void subOnlineCount() {
		WebSocketServer.onlineCount--;
	}
}
