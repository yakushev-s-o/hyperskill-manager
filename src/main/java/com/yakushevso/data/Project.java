package com.yakushevso.data;

import java.util.List;

public record Project(int id, boolean completed, String url, String title,
                      List<String> stagesIds) {
}
