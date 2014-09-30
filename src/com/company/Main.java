package com.company;

public class Main {

    public static void main(String[] args) {

        FollowerMazeServer server = new FollowerMazeServer(9090, 9099);
        server.start();
    }
}
