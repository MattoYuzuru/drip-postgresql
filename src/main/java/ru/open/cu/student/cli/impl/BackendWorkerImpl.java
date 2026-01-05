package ru.open.cu.student.cli.impl;

import ru.open.cu.student.cli.api.BackendWorker;
import ru.open.cu.student.cli.api.Engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class BackendWorkerImpl implements BackendWorker {

    private final Socket socket;
    private final Engine engine;

    public BackendWorkerImpl(Socket socket, Engine engine) {
        this.socket = socket;
        this.engine = engine;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream()),
                     true
             )
        ) {
            out.println("Welcome to SQL engine.");
            out.println("__END__");

            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();

                if ("exit".equalsIgnoreCase(trimmed) || "quit".equalsIgnoreCase(trimmed)) {
                    break;
                }

                if (trimmed.isEmpty()) {
                    out.println("__END__");
                    continue;
                }

                System.out.println("[client " + socket.getRemoteSocketAddress() + "] " + trimmed);

                String result = engine.executeSql(trimmed);
                if (result != null && !result.isEmpty()) {
                    String[] lines = result.split("\\R");
                    for (String resultLine : lines) {
                        out.println(resultLine);
                    }
                }
                out.println("__END__");
            }
        } catch (IOException e) {
            System.err.println("BackendWorker error for client " + socket.getRemoteSocketAddress());
            e.printStackTrace(System.err);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
