package com.yakushevso.data;

public class Account {
    private final String login;
    private String password;
    private final int id;

    public Account(String login, String password, int id) {
        this.login = login;
        this.password = password;
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getId() {
        return id;
    }
}
