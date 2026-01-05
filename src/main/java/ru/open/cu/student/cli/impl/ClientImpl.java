package ru.open.cu.student.cli.impl;

import ru.open.cu.student.cli.api.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientImpl implements Client {

    private final String host;
    private final int port;

    public ClientImpl(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void start() throws IOException {
        try (Socket socket = new Socket(host, port);
             BufferedReader serverReader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream())
             );
             PrintWriter serverWriter = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream()),
                     true
             );
             BufferedReader userInput = new BufferedReader(
                     new InputStreamReader(System.in)
             )
        ) {
            if (!readAndPrintResponse(serverReader)) {
                return;
            }

            while (true) {
                System.out.print("> ");
                String line = userInput.readLine();
                if (line == null) {
                    break;
                }

                String trimmed = line.trim();
                if ("exit".equalsIgnoreCase(trimmed) || "quit".equalsIgnoreCase(trimmed)) {
                    serverWriter.println(trimmed);
                    break;
                }

                serverWriter.println(line);
                if (!readAndPrintResponse(serverReader)) {
                    break;
                }
            }
        }
    }

    private boolean readAndPrintResponse(BufferedReader serverReader) throws IOException {
        String lineFromServer;
        while ((lineFromServer = serverReader.readLine()) != null) {
            if ("__END__".equals(lineFromServer)) {
                return true;
            }
            System.out.println(lineFromServer);
        }
        System.out.println("Соединение с сервером потеряно.");
        return false;
    }
}
