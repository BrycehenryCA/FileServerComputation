
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class Client {

    private static final String SERVER = "localhost";
    private static final int PORT = 5000;
    private static final String LOCAL_DIR = "client_storage";

    private static DataInputStream input;
    private static DataOutputStream output;
    private static Socket socket;

    public static void main(String[] args) {

        try {
            Files.createDirectories(Paths.get(LOCAL_DIR));

            socket = new Socket(SERVER, PORT);

            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            System.out.println("Connected to File Server.");

            Thread syncThread = new Thread(Client::monitorFolder);
            syncThread.setDaemon(true);
            syncThread.start();

            Scanner scanner = new Scanner(System.in);

            while (true) {

                System.out.println("\n===== FILE SERVER CLIENT =====");
                System.out.println("1. Upload File");
                System.out.println("2. Delete File");
                System.out.println("3. Rename File");
                System.out.println("4. Add Numbers (Synchronous)");
                System.out.println("5. Sort Numbers (Synchronous)");
                System.out.println("6. Add Numbers (Asynchronous)");
                System.out.println("7. Sort Numbers (Asynchronous)");
                System.out.println("8. Retrieve Async Result");
                System.out.println("9. List Server Files");
                System.out.println("0. Exit");
                System.out.print("Choose an option: ");

                String choice = scanner.nextLine();

                try {
                    switch (choice) {

                        case "1":
                            System.out.print("Enter local filename: ");
                            uploadFile(scanner.nextLine());
                            break;

                        case "2":
                            System.out.print("Enter filename: ");
                            deleteFile(scanner.nextLine());
                            break;

                        case "3":
                            System.out.print("Old filename: ");
                            String oldName = scanner.nextLine();

                            System.out.print("New filename: ");
                            String newName = scanner.nextLine();

                            renameFile(oldName, newName);
                            break;

                        case "4":
                            System.out.print("First number: ");
                            double a = Double.parseDouble(scanner.nextLine());

                            System.out.print("Second number: ");
                            double b = Double.parseDouble(scanner.nextLine());

                            output.writeUTF("ADD");
                            output.writeDouble(a);
                            output.writeDouble(b);
                            output.flush();

                            System.out.println(input.readUTF());
                            break;

                        case "5":
                            System.out.print("How many numbers? ");
                            int count = Integer.parseInt(scanner.nextLine());

                            if (count < 0 || count > 10000) {
                                System.out.println("Invalid number count.");
                                break;
                            }

                            output.writeUTF("SORT");
                            output.writeInt(count);

                            for (int i = 0; i < count; i++) {
                                System.out.print("Number " + (i + 1) + ": ");
                                output.writeDouble(
                                        Double.parseDouble(scanner.nextLine()));
                            }

                            output.flush();
                            System.out.println(input.readUTF());
                            break;

                        case "6":
                            System.out.print("First number: ");
                            double x = Double.parseDouble(scanner.nextLine());

                            System.out.print("Second number: ");
                            double y = Double.parseDouble(scanner.nextLine());

                            output.writeUTF("ASYNC_ADD");
                            output.writeDouble(x);
                            output.writeDouble(y);
                            output.flush();

                            System.out.println("Request submitted: "
                                    + input.readUTF());
                            break;

                        case "7":
                            System.out.print("How many numbers? ");
                            int n = Integer.parseInt(scanner.nextLine());

                            if (n < 0 || n > 10000) {
                                System.out.println("Invalid number count.");
                                break;
                            }

                            output.writeUTF("ASYNC_SORT");
                            output.writeInt(n);

                            for (int i = 0; i < n; i++) {
                                System.out.print("Number " + (i + 1) + ": ");
                                output.writeDouble(
                                        Double.parseDouble(scanner.nextLine()));
                            }

                            output.flush();

                            System.out.println("Request submitted: "
                                    + input.readUTF());
                            break;

                        case "8":
                            System.out.print("Enter request ID: ");
                            String requestId = scanner.nextLine();

                            output.writeUTF("GET_RESULT");
                            output.writeUTF(requestId);
                            output.flush();

                            System.out.println(input.readUTF());
                            break;

                        case "9":
                            output.writeUTF("LIST");
                            output.flush();

                            System.out.println(input.readUTF());
                            break;

                        case "0":
                            output.writeUTF("EXIT");
                            output.flush();

                            socket.close();
                            System.out.println("Disconnected.");
                            return;

                        default:
                            System.out.println("Invalid option.");
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Please enter valid numeric values.");
                } catch (IOException e) {
                    System.out.println("Communication error: " + e.getMessage());
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Unable to connect to server: " + e.getMessage());
        }
    }

    // Upload a file from local storage
    private static synchronized void uploadFile(String fileName)
            throws IOException {

        Path file = Paths.get(LOCAL_DIR, fileName).normalize();

        if (!file.startsWith(Paths.get(LOCAL_DIR).normalize())
                || !Files.isRegularFile(file)) {
            System.out.println("File not found or invalid filename.");
            return;
        }

        output.writeUTF("UPLOAD");
        output.writeUTF(file.getFileName().toString());
        output.writeLong(Files.size(file));

        try (InputStream fileInput = Files.newInputStream(file)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fileInput.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }

        output.flush();

        System.out.println(input.readUTF());
    }

    // Delete a server file
    private static synchronized void deleteFile(String fileName)
            throws IOException {

        output.writeUTF("DELETE");
        output.writeUTF(fileName);
        output.flush();

        System.out.println(input.readUTF());
    }

    // Rename a server file
    private static synchronized void renameFile(
            String oldName, String newName) throws IOException {

        output.writeUTF("RENAME");
        output.writeUTF(oldName);
        output.writeUTF(newName);
        output.flush();

        System.out.println(input.readUTF());
    }

    // Automatically monitor local folder changes
    private static void monitorFolder() {

        Path folder = Paths.get(LOCAL_DIR);

        try (WatchService watcher =
                     FileSystems.getDefault().newWatchService()) {

            folder.register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );

            Map<String, Long> lastUpload = new HashMap<>();

            System.out.println("Automatic synchronization enabled.");

            while (!Thread.currentThread().isInterrupted()) {

                WatchKey key = watcher.take();

                for (WatchEvent<?> event : key.pollEvents()) {

                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Path changedFile = (Path) event.context();
                    Path fullPath = folder.resolve(changedFile);

                    if (!Files.isRegularFile(fullPath)) {
                        continue;
                    }

                    try {
                        Thread.sleep(500);

                        long modified = Files.getLastModifiedTime(fullPath)
                                .toMillis();

                        String name = changedFile.toString();

                        if (lastUpload.getOrDefault(name, -1L) != modified) {

                            synchronized (Client.class) {
                                uploadFile(name);
                            }

                            lastUpload.put(name, modified);
                        }

                    } catch (IOException e) {
                        System.out.println("Sync error: " + e.getMessage());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                if (!key.reset()) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Folder monitoring error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
