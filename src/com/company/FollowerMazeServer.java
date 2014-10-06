package com.company;

/**
 * Created by kevin on 2014-09-29.
 */

import java.net.*;
import java.io.*;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FollowerMazeServer{

    private int sourcePort;
    private int userPort;
    private HashMap<Integer, User> userGraph;
    private ConcurrentLinkedQueue<String> messagesToBeProcessedByWorker;
    private Queue<String> bufferedMessages;
    private int lastValidMessage;
    private Set<Integer> connectedUsers;
    private HashMap<Integer, BufferedWriter> userClientWriterMap;
    private final int TIMEOUT = 1000;

    public FollowerMazeServer(int sourcePort, int userPort) {
        this.sourcePort = sourcePort;
        this.userPort = userPort;
        this.connectedUsers = new HashSet<Integer>();
        this.userGraph = new HashMap<Integer, User>();
        this.messagesToBeProcessedByWorker = new ConcurrentLinkedQueue<String>();
        this.bufferedMessages = new PriorityQueue<String>(100,
                new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        int messageId1 = Integer.parseInt(o1.substring(0, o1.indexOf("|")));
                        int messageId2 = Integer.parseInt(o2.substring(0, o2.indexOf("|")));
                        if (messageId1 > messageId2) {
                            return 1;
                        } else if (messageId1 < messageId2) {
                            return -1;
                        }
                        return 0;
                    }
                });
        this.userClientWriterMap = new HashMap<Integer, BufferedWriter>();
        this.lastValidMessage = 1;
    }

    private void setupUserConnections() {
        //setting up the user connections
        try {
            ServerSocket userClientServerSocket = new ServerSocket(this.userPort);
            userClientServerSocket.setSoTimeout(TIMEOUT);

            while (true) {
                Socket userClientSocket = userClientServerSocket.accept();
                BufferedReader userClientIn = new BufferedReader(
                        new InputStreamReader(userClientSocket.getInputStream()));
                int userClientNum = Integer.parseInt(userClientIn.readLine().trim());
                BufferedWriter userClientOut = new BufferedWriter(
                        new OutputStreamWriter(userClientSocket.getOutputStream()));
                this.userClientWriterMap.put(userClientNum, userClientOut);
                userGraph.put(userClientNum, new User(userClientNum));
                connectedUsers.add(userClientNum);
            }
        } catch (SocketTimeoutException s) {
        } catch (IOException e) {
            System.err.println("IOException for user socket");
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    public void start() {
        try {
            ServerSocket eventSourceServerSocket = new ServerSocket(this.sourcePort);
            Socket eventSourceSocket = eventSourceServerSocket.accept();
            BufferedReader eventSourceIn = new BufferedReader(
                    new InputStreamReader(eventSourceSocket.getInputStream()));

            setupUserConnections();

            //Starts the worker, that pulls stuff off the queue and writes them.
            FollowerMazeRunnable worker = new FollowerMazeRunnable(this.messagesToBeProcessedByWorker,
                    this.userClientWriterMap);
            Thread workerThread = new Thread(worker);
            workerThread.start();

            //Processes the input messages and adds them to the queue.
            String input;
            while ((input = eventSourceIn.readLine()) != null) {
                processRawMessage(input);
            }

            worker.stopRunning();
            eventSourceIn.close();
            for (BufferedWriter w: this.userClientWriterMap.values()) {
                w.close();
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    private void addToMessagesToBeProcessed(int showUser, String message) {
        if (!connectedUsers.contains(showUser)) {
            return;
        }
        String toBeProcessedMessage = Integer.toString(showUser) + "|" + message;
        this.messagesToBeProcessedByWorker.add(toBeProcessedMessage);
    }

    private void processRawMessage (String message) {
        bufferedMessages.add(message);
        processBufferedMessages();
    }

    private void processBufferedMessages() {
        String currentMessage = bufferedMessages.peek();
        int messageId = Integer.parseInt(currentMessage.substring(0, currentMessage.indexOf("|")));
        while (messageId == lastValidMessage) {
            processCurrentMessage(bufferedMessages.remove());
            currentMessage = bufferedMessages.peek();
            if (currentMessage != null) {
                messageId = Integer.parseInt(currentMessage.substring(0, currentMessage.indexOf("|")));
            } else {
                messageId = 0;
            }
            lastValidMessage++;
        }
    }

    private void processCurrentMessage(String message) {
        String strippedMessage = message.substring(message.indexOf("|") + 1);
        String messageType = strippedMessage.substring(0,1);
        if (messageType.equals("F")) {
            followMessage(message);
        } else if (messageType.equals("U")) {
            unfollowMessage(message);
        } else if (messageType.equals("B")) {
            broadcastMessage(message);
        } else if (messageType.equals("P")) {
            privateMessage(message);
        } else if (messageType.equals("S")) {
            statusUpdateMessage(message);
        }
    }

    private void followMessage(String message) {
        String parseMessage[] = message.split("\\|");
        int fromUserId = Integer.parseInt(parseMessage[2]);
        int toUserId = Integer.parseInt(parseMessage[3]);
        if (!userGraph.containsKey(fromUserId)) {
            userGraph.put(fromUserId, new User(fromUserId));
        }
        if (!userGraph.containsKey(toUserId)) {
            userGraph.put(toUserId, new User(toUserId));
        }
        userGraph.get(toUserId).addFollower(fromUserId);
        addToMessagesToBeProcessed(toUserId, message);
    }

    private void unfollowMessage(String message) {
        String parseMessage[] = message.split("\\|");
        int fromUserId = Integer.parseInt(parseMessage[2]);
        int toUserId = Integer.parseInt(parseMessage[3]);
        if (!userGraph.containsKey(fromUserId)) {
            userGraph.put(fromUserId, new User(fromUserId));
        }
        if (!userGraph.containsKey(toUserId)) {
            userGraph.put(toUserId, new User(toUserId));
        }
        userGraph.get(toUserId).unFollower(fromUserId);
    }

    private void broadcastMessage(String message) {
        for (int userId : connectedUsers) {
            addToMessagesToBeProcessed(userId, message);
        }
    }

    private void privateMessage(String message) {
        String parseMessage[] = message.split("\\|");
        String toUserId = parseMessage[3];
        addToMessagesToBeProcessed(Integer.parseInt(toUserId), message);
    }

    private void statusUpdateMessage(String message) {
        String parseMessage[] = message.split("\\|");
        int fromUserId = Integer.parseInt(parseMessage[2]);
        if (!userGraph.containsKey(fromUserId)) {
            userGraph.put(fromUserId, new User(fromUserId));
        }
        ArrayList<Integer> followers = userGraph.get(fromUserId).getFollowers();
        for (int follower : followers) {
            addToMessagesToBeProcessed(follower, message);
        }
    }
}
