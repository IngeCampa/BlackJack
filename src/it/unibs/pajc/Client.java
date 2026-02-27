package it.unibs.pajc;

import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                System.out.println(serverMessage);
                if (serverMessage.toLowerCase().contains("digita")) {
                    String command = userInput.readLine();
                    out.println(command);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
