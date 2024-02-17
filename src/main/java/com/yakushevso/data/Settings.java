package com.yakushevso.data;

import java.util.List;

public class Settings {
    private final List<Account> accounts;
    private String driverPath;
    private String folderPath;
    private String jsonPath;
    private String dataPath;
    private String siteLink;

    public Settings(List<Account> accounts, String driverPath,
                    String folderPath, String jsonPath, String dataPath, String siteLink) {
        this.accounts = accounts;
        this.driverPath = driverPath;
        this.folderPath = folderPath;
        this.jsonPath = jsonPath;
        this.dataPath = dataPath;
        this.siteLink = siteLink;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void addAccount(Account account) {
        accounts.add(account);
    }

    public void delAccount(int index) {
        accounts.remove(index);
    }

    public String getChromedriverPath() {
        return driverPath;
    }

    public void setChromedriverPath(String chromedriver_path) {
        this.driverPath = chromedriver_path;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getSiteLink() {
        return siteLink;
    }

    public void setSiteLink(String siteLink) {
        this.siteLink = siteLink;
    }
}
