
# File Server and Computation Service Implementation


## 1. Project Overview

This project implements a multi-threaded file server and computation service using Java.

The server listens on port 5000 and accepts multiple client connections using Java Socket programming and ExecutorService.

The application supports file uploads, deletion, renaming, and automatic file synchronization between the client and server.

It also provides synchronous and asynchronous remote procedure calls (RPCs) for mathematical addition and sorting operations.

## 2. Project Structure

FileServerComputation/

- Server.java
- Client.java
- README.md
- server_storage/
- client_storage/

## 3. Requirements

- Java Development Kit (JDK 11 or later)
- Command Prompt, PowerShell, or Linux Terminal
- Basic knowledge of Java networking

## 4. Installation

1. Install the Java JDK.
2. Create the project folder.
3. Save Server.java and Client.java inside the folder.
4. Create the server_storage and client_storage folders.
5. Open a terminal in the project directory.

Check Java installation:

java -version

Check Java compiler:

javac -version

## 5. Compilation

Compile both Java files:

javac Server.java Client.java

## 6. Running the Application

First, start the server:

java Server

The server will listen on port 5000.

Open another terminal window and start the client:

java Client

The client will connect to the server and display the main menu.

## 7. Available Features

### File Operations

- Upload files to server storage.
- Delete files from server storage.
- Rename existing server files.
- List files stored on the server.

### Automatic Synchronization

The client monitors the client_storage directory using Java WatchService.

When a new file is created or an existing file is modified, the client automatically uploads it to the server.

### Synchronous Computation

The server immediately processes requests for:

- Addition of two numbers.
- Sorting a list of numbers.

### Asynchronous Computation

The server submits computation tasks to a separate ExecutorService.

The client receives a unique request ID and can retrieve the result later.

Supported operations:

- Asynchronous addition.
- Asynchronous sorting.
- Checking pending computation results.

## 8. Technologies Used

- Java
- TCP Sockets
- ExecutorService
- ConcurrentHashMap
- WatchService API
- File I/O
- Multithreading
- Remote Procedure Calls (RPC)

