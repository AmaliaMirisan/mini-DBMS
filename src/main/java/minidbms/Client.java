package minidbms;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 8080;

        try (Socket socket = new Socket(hostname, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server");

            while (true) {
                System.out.print("Enter command: ");
                String command = scanner.nextLine();

                // Send command to server
                out.println(command);

                // Receive response from server
                String response = in.readLine();
                System.out.println(response);

                if ("exit".equalsIgnoreCase(command)) {
                    System.out.println("Goodbye!");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
