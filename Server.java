
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {

    private static final int PORT = 5000;
    private static final String STORAGE_DIR = "server_storage";

    private static final ExecutorService clientPool =
            Executors.newFixedThreadPool(10);

    private static final ExecutorService computationPool =
            Executors.newFixedThreadPool(5);

    private static final Map<String, Future<String>> asyncResults =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {

        try {
            Files.createDirectories(Paths.get(STORAGE_DIR));

            ServerSocket serverSocket = new ServerSocket(PORT);

            System.out.println("File Server started on port " + PORT);
            System.out.println("Waiting for client connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                System.out.println("Client connected: "
                        + clientSocket.getInetAddress());

                clientPool.submit(() -> handleClient(clientSocket));
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {

        try (
            Socket client = socket;
            DataInputStream input =
                    new DataInputStream(client.getInputStream());
            DataOutputStream output =
                    new DataOutputStream(client.getOutputStream())
        ) {

            while (true) {

                String command;

                try {
                    command = input.readUTF();
                } catch (EOFException e) {
                    break;
                }

                switch (command) {

                    case "UPLOAD":
                        uploadFile(input, output);
                        break;

                    case "DELETE":
                        deleteFile(input, output);
                        break;

                    case "RENAME":
                        renameFile(input, output);
                        break;

                    case "ADD":
                        addNumbers(input, output);
                        break;

                    case "SORT":
                        sortNumbers(input, output);
                        break;

                    case "ASYNC_ADD":
                        asyncAdd(input, output);
                        break;

                    case "ASYNC_SORT":
                        asyncSort(input, output);
                        break;

                    case "GET_RESULT":
                        getAsyncResult(input, output);
                        break;

                    case "LIST":
                        listFiles(output);
                        break;

                    case "EXIT":
                        return;

                    default:
                        output.writeUTF("ERROR: Unknown command.");
                        output.flush();
                }
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }

    // Upload a file to server storage
    private static void uploadFile(
            DataInputStream input,
            DataOutputStream output) throws IOException {

        String fileName = Paths.get(input.readUTF())
                .getFileName().toString();

        long fileSize = input.readLong();

        if (fileSize < 0 || fileSize > 100_000_000) {
            output.writeUTF("ERROR: Invalid file size.");
            output.flush();
            return;
        }

        Path destination = Paths.get(STORAGE_DIR)
                .resolve(fileName).normalize();

        if (!destination.startsWith(Paths.get(STORAGE_DIR).toAbsolutePath()
                .normalize()) &&
                !destination.startsWith(Paths.get(STORAGE_DIR).normalize())) {
            output.writeUTF("ERROR: Invalid filename.");
            output.flush();
            return;
        }

        Path temporary = Files.createTempFile(
                Paths.get(STORAGE_DIR), "upload-", ".tmp");

        boolean completed = false;

        try (OutputStream fileOutput =
                     Files.newOutputStream(temporary)) {

            byte[] buffer = new byte[8192];
            long remaining = fileSize;

            while (remaining > 0) {
                int bytesRead = input.read(
                        buffer, 0, (int) Math.min(buffer.length, remaining));

                if (bytesRead == -1) {
                    throw new EOFException("Incomplete file transfer.");
                }

                fileOutput.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }

            Files.move(temporary, destination,
                    StandardCopyOption.REPLACE_EXISTING);

            completed = true;

            output.writeUTF("SUCCESS: File uploaded successfully.");

        } finally {
            if (!completed) {
                Files.deleteIfExists(temporary);
            }
        }

        output.flush();
    }

    // Delete a file
    private static void deleteFile(
            DataInputStream input,
            DataOutputStream output) throws IOException {

        String fileName = Paths.get(input.readUTF())
                .getFileName().toString();

        Path file = Paths.get(STORAGE_DIR, fileName);

        if (Files.deleteIfExists(file)) {
            output.writeUTF("SUCCESS: File deleted.");
        } else {
            output.writeUTF("ERROR: File not found.");
        }

        output.flush();
    }

    // Rename a file
    private static void renameFile(
            DataInputStream input,
            DataOutputStream output) throws IOException {

        String oldName = Paths.get(input.readUTF())
                .getFileName().toString();

        String newName = Paths.get(input.readUTF())
                .getFileName().toString();

        Path oldFile = Paths.get(STORAGE_DIR, oldName);
        Path newFile = Paths.get(STORAGE_DIR, newName);

        if (!Files.exists(oldFile)) {
            output.writeUTF("ERROR: Original file not found.");
        } else if (Files.exists(newFile)) {
            output.writeUTF("ERROR: New filename already exists.");
        } else {
            Files.move(oldFile, newFile);
            output.writeUTF("SUCCESS: File renamed.");
        }

        output.flush();
    }

    // Synchronous addition
    private static void addNumbers(
            DataInputStream input,
            DataOutputStream output) throws IOException {

        double a = input.readDouble();
        double b = input.readDouble();

        double result = a + b;

        output.writeUTF("Addition result: " + result);
        output.flush();
    }

    // Synchronous sorting
    private static void sortNumbers(
            DataInputStream input,
            DataOutputStream output) throws IOException {

        int count = input.readInt();

        if (count < 0 || count > 10000) {
            output.writeUTF("ERROR: Invalid number count.");
            output.flush();
            return;
        }

        double[] numbers = new double[count];

        for (int i = 0; i < count; i++) {
            numbers[i] = input.readDouble();
        }

        Arrays.sort(numbers);

        output.writeUTF("Sorted numbers: "
                + Arrays.toString(numbers));

        output.flush();
    }

    // Asynchronous addition
    private static void asyncAdd(
            DataInputStream input,
            DataOutputStream output) throws IOException {

        String requestId = UUID.randomUUID().toString();

        double a = input.readDouble();
        double b = input.readDouble();

        Future<String> future = computationPool.submit(() -> {
            Thread.sleep(3000);
            return "Addition result: " + (a + b);
        });

        asyncResults.put(requestId, future);

        output.writeUTF("ASYNC_REQUEST:" + requestId);
        output.flush();
    }

    // Asynchronous sorting
    private static void asyncSort(
            DataInputStream input,
            DataOutputStream output) throws IOException {

        String requestId = UUID.randomUUID().toString();

        int count = input.readInt();

        if (count < 0 || count > 10000) {
            output.writeUTF("ERROR: Invalid number count.");
            output.flush();
            return;
        }

        double[] numbers = new double[count];

        for (int i = 0; i < count; i++) {
            numbers[i] = input.readDouble();
        }

        Future<String> future = computationPool.submit(() -> {
            Thread.sleep(3000);
            Arrays.sort(numbers);
            return "Sorted numbers: " + Arrays.toString(numbers);
        });

        asyncResults.put(requestId, future);

        output.writeUTF("ASYNC_REQUEST:" + requestId);
        output.flush();
    }

    // Retrieve asynchronous computation result
    private static void getAsyncResult(
            DataInputStream input,
            DataOutputStream output) throws IOException {

        String requestId = input.readUTF();

        Future<String> future = asyncResults.get(requestId);

        if (future == null) {
            output.writeUTF("ERROR: Request not found.");
        } else if (!future.isDone()) {
            output.writeUTF("PENDING: Computation is still running.");
        } else {
            try {
                output.writeUTF("RESULT: " + future.get());
                asyncResults.remove(requestId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                output.writeUTF("ERROR: Computation interrupted.");
            } catch (ExecutionException e) {
                output.writeUTF("ERROR: Computation failed.");
            }
        }

        output.flush();
    }

    // List server files
    private static void listFiles(
            DataOutputStream output) throws IOException {

        File folder = new File(STORAGE_DIR);
        File[] files = folder.listFiles();

        StringBuilder result = new StringBuilder("Server files:\n");

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    result.append(file.getName())
                            .append(" (")
                            .append(file.length())
                            .append(" bytes)\n");
                }
            }
        }

        output.writeUTF(result.toString());
        output.flush();
    }
}
