package com.company;

/**
 * Created by kevin on 2014-09-29.
 */

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FollowerMazeRunnable implements Runnable{
    private HashMap<Integer, BufferedWriter> userWriterMap;
    private ConcurrentLinkedQueue<String> messageQueue;
    private volatile boolean stop;

    public FollowerMazeRunnable(ConcurrentLinkedQueue<String> messageQueue, HashMap<Integer,
            BufferedWriter> userWriterMap) {
        this.messageQueue = messageQueue;
        this.userWriterMap = userWriterMap;
        this.stop = false;
    }

    public void stopRunning() {
        stop = true;
    }

    public void run() {
        try {
            while (!this.stop) {
                String instructionMessage = messageQueue.poll();
                if (instructionMessage != null) {
                    String parsedMessage[] = instructionMessage.split("\\|");
                    int showMessageToUserId = Integer.parseInt(parsedMessage[0]);
                    String message = instructionMessage.substring(instructionMessage.indexOf("|") + 1);
                    userWriterMap.get(showMessageToUserId).write(message);
                    userWriterMap.get(showMessageToUserId).write("\n");
                    userWriterMap.get(showMessageToUserId).flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Can't write to the user clients");
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }
}
