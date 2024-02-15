package com.yakushevso.data;

import java.util.List;

public class Settings {
    private List<Account> accounts;
    private String driver_path;
    private String folder_path;
    private String json_path;
    private String data_path;
    private String site_link;

    public Settings(List<Account> accounts, String chromedriver_path,
                    String folder_path, String json_path, String data_path, String site_link) {
        this.accounts = accounts;
        this.driver_path = chromedriver_path;
        this.folder_path = folder_path;
        this.json_path = json_path;
        this.data_path = data_path;
        this.site_link = site_link;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(String login, String password, int id) {
        this.accounts.add(new Account(login, password, id));
    }

    public String getChromedriver_path() {
        return driver_path;
    }

    public void setChromedriver_path(String chromedriver_path) {
        this.driver_path = chromedriver_path;
    }

    public String getFolder_path() {
        return folder_path;
    }

    public void setFolder_path(String folder_path) {
        this.folder_path = folder_path;
    }

    public String getJson_path() {
        return json_path;
    }

    public void setJson_path(String json_path) {
        this.json_path = json_path;
    }

    public String getData_path() {
        return data_path;
    }

    public void setData_path(String data_path) {
        this.data_path = data_path;
    }

    public String getSite_link() {
        return site_link;
    }

    public void setSite_link(String site_link) {
        this.site_link = site_link;
    }
}
