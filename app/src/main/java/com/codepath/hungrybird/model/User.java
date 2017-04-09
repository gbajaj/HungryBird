package com.codepath.hungrybird.model;

import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

/**
 * Created by ajasuja on 4/8/17.
 */
public class User {
    public final ParseUser parseUser;

    public User(ParseUser parseUser) {
        //no-op
        this.parseUser = parseUser;
    }

    public boolean isChef() {
        return parseUser.getBoolean("isChef");

    }

    public String getObjectId() {
        return parseUser.getObjectId();
    }

    public void setChef(boolean chef) {
        parseUser.put("isChef", chef);
    }

    public ParseFile getProfileImage() {
        return parseUser.getParseFile("profileImage");
    }

    public void setProfileImage(ParseFile profileImage) {
        parseUser.put("profileImage", profileImage);

    }

    public final void saveInBackground(SaveCallback callback) {
        parseUser.saveInBackground(callback);

    }

    public void setUsername(String username) {
        parseUser.setUsername(username);
    }

    public String getUsername() {
        return parseUser.getUsername();
    }

}
