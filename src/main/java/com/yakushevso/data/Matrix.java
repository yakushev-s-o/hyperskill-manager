package com.yakushevso.data;

public record Matrix(String name_row, String name_columns, boolean check) {

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
