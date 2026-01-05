package ru.open.cu.student.cli.impl;

import ru.open.cu.student.cli.api.Server;

import java.io.IOException;
import java.nio.file.Path;

public class ServerMain {

    private static final int DEFAULT_PORT = 5555;
    private static final Path DEFAULT_DATA_DIR = Path.of("dbdata");

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        Path dataDir = DEFAULT_DATA_DIR;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        if (args.length > 1) {
            dataDir = Path.of(args[1]);
        }

        Server server = new ServerImpl(port, dataDir);
        server.start();
    }
}
