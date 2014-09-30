package com.company;

/**
 * Created by kevin on 2014-09-29.
 */

import java.util.*;

public class User {

    private int id;
    private ArrayList<Integer> followers;

    public User(int id) {
        this.id = id;
        this.followers = new ArrayList<Integer>();
    }

    public ArrayList<Integer> getFollowers() {
        return this.followers;
    }

    public void addFollower(Integer follower) {
        followers.add(follower);
    }

    public void unFollower(Integer follower) {
        followers.remove(follower);
    }
}
