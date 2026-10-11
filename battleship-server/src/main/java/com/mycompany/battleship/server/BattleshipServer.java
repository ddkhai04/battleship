package com.mycompany.battleship.server;

import com.mycompany.battleship.server.game.GameManager;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class BattleshipServer {

    public static ConcurrentHashMap<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();
    public static GameManager gameManager = new GameManager();

    public static void broadcastLobbyList() {
        for (ClientHandler client : onlineUsers.values()) {
            client.handleListPlayers();
        }
    }

    public static void main(String[] args) {
        int port = 2209;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping server and shutting down GameManager...");
            gameManager.shutdown();
        }));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Lobby & Game Server is starting and listening on port " + port + "...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New connection from: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            System.err.println("Server startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}