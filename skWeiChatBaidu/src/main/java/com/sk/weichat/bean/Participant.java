package com.sk.weichat.bean;

public class Participant {
    private boolean raisedHand;
    private boolean pinned;
    private String avatarID;
    private String role;
    private String avatarURL;
    private String loadableAvatarUrl;
    private String name;
    private String id;
    private String email;
    private boolean local;
    private boolean dominantSpeaker;

    public boolean isRaisedHand() {
        return raisedHand;
    }

    public void setRaisedHand(boolean raisedHand) {
        this.raisedHand = raisedHand;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public String getAvatarID() {
        return avatarID;
    }

    public void setAvatarID(String avatarID) {
        this.avatarID = avatarID;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    public String getLoadableAvatarUrl() {
        return loadableAvatarUrl;
    }

    public void setLoadableAvatarUrl(String loadableAvatarUrl) {
        this.loadableAvatarUrl = loadableAvatarUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public boolean isDominantSpeaker() {
        return dominantSpeaker;
    }

    public void setDominantSpeaker(boolean dominantSpeaker) {
        this.dominantSpeaker = dominantSpeaker;
    }

    @Override
    public String toString() {
        return
                "Participant{" +
                        "raisedHand = '" + raisedHand + '\'' +
                        ",pinned = '" + pinned + '\'' +
                        ",avatarID = '" + avatarID + '\'' +
                        ",role = '" + role + '\'' +
                        ",avatarURL = '" + avatarURL + '\'' +
                        ",loadableAvatarUrl = '" + loadableAvatarUrl + '\'' +
                        ",name = '" + name + '\'' +
                        ",id = '" + id + '\'' +
                        ",email = '" + email + '\'' +
                        ",local = '" + local + '\'' +
                        ",dominantSpeaker = '" + dominantSpeaker + '\'' +
                        "}";
    }
}
