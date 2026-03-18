package it.unibs.pajc;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) {
        ServerModel gameRoom = new ServerModel();
       // ExecutorService executor = Executors.newFixedThreadPool(4);
        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("♠️ Server Blackjack MVC Avviato sulla porta 12345 ♠️");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandler(clientSocket, gameRoom));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}