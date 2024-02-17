package com.yakushevso.data;

public class UserSession {
    private Account account;
    private int track;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public int getTrack() {
        return track;
    }

    public void setTrack(int track) {
        this.track = track;
    }

    public void update(UserSession userSession) {
        account = userSession.getAccount();
        track = userSession.getTrack();
    }
}
