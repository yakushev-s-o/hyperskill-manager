package com.yakushevso.data;

public record Account(String login, String password, int id) {

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public int getId() {
        return id;
    }
}
