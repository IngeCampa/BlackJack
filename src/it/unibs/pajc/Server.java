package it.unibs.pajc;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) {
        ServerModel gameRoom = new ServerModel();
       // ExecutorService executor = Executors.newFixedThreadPool(4);
       // in questo modo possono entrare più di 4 giocatori(4 giocano e gli altri attendono))
        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server avviato sulla porta 12345");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandler(clientSocket, gameRoom));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}