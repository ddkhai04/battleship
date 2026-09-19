package com.mycompany.battleship.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class BattleshipServer {
    
    // Store connected Clients (Key is username) to manage online list and send invites
    public static ConcurrentHashMap<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = 2209; // Network port for the Server
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Lobby Server is starting and listening on port " + port + "...");
            
            // Infinite loop to continuously accept new players
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New connection from: " + socket.getInetAddress());
                
                // Assign the accepted connection to an independent Thread
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            System.err.println("Server startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}