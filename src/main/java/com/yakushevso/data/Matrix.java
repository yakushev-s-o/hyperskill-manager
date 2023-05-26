package com.yakushevso.data;

public class Matrix {
    private final String name_row;
    private final String name_columns;
    private final boolean check;

    public Matrix(String name_row, String name_columns, boolean check) {
        this.name_row = name_row;
        this.name_columns = name_columns;
        this.check = check;
    }

    public String getName_row() {
        return name_row;
    }

    public String getName_columns() {
        return name_columns;
    }

    public boolean isCheck() {
        return check;
    }
}
