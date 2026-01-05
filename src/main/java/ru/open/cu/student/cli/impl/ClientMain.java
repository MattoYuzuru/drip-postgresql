package ru.open.cu.student.cli.impl;

import ru.open.cu.student.cli.api.Client;

import java.io.IOException;

public class ClientMain {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5555;

    public static void main(String[] args) throws IOException {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        Client client = new ClientImpl(host, port);
        client.start();
    }
}
