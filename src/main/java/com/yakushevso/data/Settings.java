package com.yakushevso.data;

import java.util.List;

public class Settings {
    private final List<Account> accounts;
    private String driverPath;
    private String folderPath;

    public Settings(List<Account> accounts, String driverPath,
                    String folderPath) {
        this.accounts = accounts;
        this.driverPath = driverPath;
        this.folderPath = folderPath;
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
}
