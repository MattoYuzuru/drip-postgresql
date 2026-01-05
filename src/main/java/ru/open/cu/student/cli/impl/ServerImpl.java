package ru.open.cu.student.cli.impl;

import ru.open.cu.student.cli.api.Engine;
import ru.open.cu.student.cli.api.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

public class ServerImpl implements Server {

    private final int port;
    private final Path baseDir;
    private final Engine engine;

    public ServerImpl(int port, Path baseDir) {
        this.port = port;
        this.baseDir = baseDir;
        this.engine = new EngineImpl(baseDir);
    }

    @Override
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port + ", data dir=" + baseDir.toAbsolutePath());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress());

                BackendWorkerImpl backendWorker = new BackendWorkerImpl(clientSocket, engine);
                Thread thread = new Thread(
                        backendWorker,
                        "backend-" + clientSocket.getRemoteSocketAddress()
                );
                thread.start();
            }
        }
    }
}
