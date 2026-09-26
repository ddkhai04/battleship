package com.mycompany.battleship.server;

import com.mycompany.battleship.common.model.User;
import com.mycompany.battleship.common.model.UserDTO;
import com.mycompany.battleship.server.dao.UserDAO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.mindrot.jbcrypt.BCrypt;

public class ClientHandler implements Runnable {
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String loggedInUsername = null;
    private User currentUser = null;
    
    private ClientHandler opponent = null; 

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Ready to communicate with a new Client!");
            
            String request;
            while ((request = in.readLine()) != null) {
                if (request.startsWith("LOGIN|") || request.startsWith("REGISTER|")) {
                    String[] tempParts = request.split("\\|");
                    if (tempParts.length >= 2) {
                        System.out.println("Received command: " + tempParts[0] + " from account: " + tempParts[1]);
                    }
                } else {
                    System.out.println("Received command: " + request);
                }
                
                String[] parts = request.split("\\|");
                String command = parts[0];

                switch (command) {
                    case "LOGIN":
                        if (parts.length >= 3) handleLogin(parts[1], parts[2]);
                        break;
                    case "REGISTER":
                        if (parts.length >= 3) handleRegister(parts[1], parts[2]);
                        break;
                    case "LIST_PLAYERS":
                        handleListPlayers();
                        break;
                    case "INVITE":
                        if (parts.length >= 2) handleInvite(parts[1]);
                        break;
                    case "CANCEL_INVITE":
                        if (parts.length >= 2) handleCancelInvite(parts[1]);
                        break;
                    case "ACCEPT":
                    case "INVITE_ACCEPT":   
                        if (parts.length >= 2) handleAccept(parts[1]);
                        break;
                    case "REJECT":
                    case "INVITE_REJECT":   
                        if (parts.length >= 2) handleReject(parts[1]);
                        break;
                    case "SURRENDER":
                        handleSurrender();
                        break;
                    default:
                        System.out.println("Unknown command: " + command);
                }
            }
        } catch (IOException e) {
            System.out.println("Client unexpectedly disconnected: " + e.getMessage());
        } finally {
            if (loggedInUsername != null) {
                BattleshipServer.onlineUsers.remove(loggedInUsername);
                
                if (currentUser != null) {
                    new UserDAO().updateStatus(currentUser.getId(), "OFFLINE");
                }
                
                if (this.opponent != null) {
                    System.out.println("Rescuing " + this.opponent.loggedInUsername + " because the opponent disconnected.");
                    
                    this.opponent.sendMessage("OPPONENT_QUIT|Opponent disconnected. The match is canceled.");
                                     
                    if (this.opponent.currentUser != null) {
                        new UserDAO().updateStatus(this.opponent.currentUser.getId(), "ONLINE");
                        this.opponent.currentUser.setStatus("ONLINE");
                    }
                    
                    this.opponent.opponent = null;
                    this.opponent = null;
                }
                
                System.out.println(loggedInUsername + " has left. Remaining online users: " + BattleshipServer.onlineUsers.size());
                
                for (ClientHandler client : BattleshipServer.onlineUsers.values()) {
                    client.handleListPlayers();
                }
            }
        }
    }

    private void handleLogin(String username, String password) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.checkLogin(username);
        
        if (user != null && BCrypt.checkpw(password, user.getPassword())) {
            if (BattleshipServer.onlineUsers.containsKey(username)) {
                out.println("LOGIN_FAIL|This account is already logged in elsewhere.");
                System.out.println(username + " login failed due to duplicate session.");
                return;
            }
            
            this.loggedInUsername = username;
            this.currentUser = user;
            
            BattleshipServer.onlineUsers.put(username, this);
            
            userDAO.updateStatus(user.getId(), "ONLINE");
            this.currentUser.setStatus("ONLINE");
            
            out.println("LOGIN_OK|" + username + "|" + user.getScore());
            System.out.println(username + " logged in successfully!");
            
            for (ClientHandler client : BattleshipServer.onlineUsers.values()) {
                client.handleListPlayers();
            }
            
        } else {
            out.println("LOGIN_FAIL|Invalid username or password.");
        }
    }

    private void handleRegister(String username, String password) {
        UserDAO userDAO = new UserDAO();
        
        if (userDAO.checkUsernameExist(username)) {
            out.println("REGISTER_FAIL|Username already exists.");
            return;
        }
        
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(hashedPassword);
        newUser.setNickname(username);
        
        if (userDAO.registerUser(newUser)) {
            out.println("REGISTER_OK");
            System.out.println("Account registered successfully: " + username);
        } else {
            out.println("REGISTER_FAIL|Database system error.");
        }
    }

    private void handleListPlayers() {
        UserDAO userDAO = new UserDAO();
        java.util.List<UserDTO> lobbyUsers = userDAO.getLobbyUsers(); 
        
        if (lobbyUsers.isEmpty()) {
            out.println("PLAYER_LIST|");
            return;
        }

        StringBuilder sb = new StringBuilder("PLAYER_LIST|");
        for (int i = 0; i < lobbyUsers.size(); i++) {
            UserDTO u = lobbyUsers.get(i);
            
            String realStatus = u.getStatus();
            
            if (!BattleshipServer.onlineUsers.containsKey(u.getNickname())) {
                realStatus = "OFFLINE";
            }
            
            sb.append(u.getNickname()).append(",")
              .append(realStatus).append(",")
              .append(u.getScore());
              
            if (i < lobbyUsers.size() - 1) {
                sb.append(";");
            }
        }
        
        out.println(sb.toString());
    }

    private void handleInvite(String targetUser) {
        if ("IN_GAME".equals(this.currentUser.getStatus())) {
            out.println("INVITE_FAIL|You are currently in a game, cannot send invites.");
            return;
        }

        ClientHandler targetHandler = BattleshipServer.onlineUsers.get(targetUser);
        
        if (targetHandler != null && targetHandler.currentUser != null) {
            if ("IN_GAME".equals(targetHandler.currentUser.getStatus())) {
                out.println("INVITE_FAIL|Player " + targetUser + " is busy in another match.");
                return;
            }
            
            targetHandler.sendMessage("INVITE_FROM|" + this.loggedInUsername);
            System.out.println(this.loggedInUsername + " sent an invite to " + targetUser);
        } else {
            out.println("INVITE_FAIL|Player " + targetUser + " is not online.");
        }
    }

    private void handleCancelInvite(String targetUser) {
        ClientHandler targetHandler = BattleshipServer.onlineUsers.get(targetUser);
        if (targetHandler != null) {
            targetHandler.sendMessage("INVITE_CANCELLED|" + this.loggedInUsername);
            System.out.println(this.loggedInUsername + " đã hủy lời mời tới " + targetUser);
        }
    }

    private void handleAccept(String challengerUsername) {
        if ("IN_GAME".equals(this.currentUser.getStatus())) {
            out.println("ERROR|You are in another match, cannot join this one.");
            return;
        }

        ClientHandler challengerHandler = BattleshipServer.onlineUsers.get(challengerUsername);
        
        if (challengerHandler != null && challengerHandler.currentUser != null && this.currentUser != null) {
            
            if ("IN_GAME".equals(challengerHandler.currentUser.getStatus())) {
                out.println("ERROR|The challenger has joined another match.");
                return;
            }
            
            UserDAO userDAO = new UserDAO();
            
            userDAO.updateStatus(this.currentUser.getId(), "IN_GAME");
            userDAO.updateStatus(challengerHandler.currentUser.getId(), "IN_GAME");
            
            this.currentUser.setStatus("IN_GAME");
            challengerHandler.currentUser.setStatus("IN_GAME");
            
            this.opponent = challengerHandler;
            challengerHandler.opponent = this;
            
            String roomId = "ROOM_" + System.currentTimeMillis();
            
            challengerHandler.sendMessage("MATCH_START|" + roomId + "|" + this.loggedInUsername);
            out.println("MATCH_START|" + roomId + "|" + challengerUsername);
            
            for (ClientHandler client : BattleshipServer.onlineUsers.values()) {
                client.handleListPlayers();
            }
            
            System.out.println("Match started between: " + challengerUsername + " and " + this.loggedInUsername);
        } else {
            out.println("ERROR|The challenger has left or is invalid.");
        }
    }

    private void handleReject(String challengerUsername) {
        ClientHandler challengerHandler = BattleshipServer.onlineUsers.get(challengerUsername);
        
        if (challengerHandler != null) {
            challengerHandler.sendMessage("REJECT_FROM|" + this.loggedInUsername);
            System.out.println(this.loggedInUsername + " rejected the invite from " + challengerUsername);
        }
    }
    
    private void handleSurrender() {
        if (this.opponent != null) {
            System.out.println(this.loggedInUsername + " has surrendered. Opponent " + this.opponent.loggedInUsername + " wins!");

            this.sendMessage("MATCH_END|LOSE|SURRENDER");
            this.opponent.sendMessage("MATCH_END|WIN|SURRENDER");

            UserDAO userDAO = new UserDAO();
            if (this.currentUser != null) {
                userDAO.updateStatus(this.currentUser.getId(), "ONLINE");
                this.currentUser.setStatus("ONLINE");
            }
            if (this.opponent.currentUser != null) {
                userDAO.updateStatus(this.opponent.currentUser.getId(), "ONLINE");
                this.opponent.currentUser.setStatus("ONLINE");
            }

            this.opponent.opponent = null;
            this.opponent = null;

            for (ClientHandler client : BattleshipServer.onlineUsers.values()) {
                client.handleListPlayers();
            }
        }
    }
    
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}