package com.taskviewer.api.web.rq;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public record RqTaskUpdate(
  String title,
  String about,
  String username,
  String status,
  Integer priority,
  LocalDateTime estimate,
  LocalDateTime tracked
  ) {

  @Contract(value = " -> new", pure = true)
  public static @NotNull RqTaskUpdate.TaskUpdateSqlBuilder taskUpdateSqlBuilder() {
    return new RqTaskUpdate.TaskUpdateSqlBuilder();
  }

  public static class TaskUpdateSqlBuilder {
    private final StringBuilder sql = new StringBuilder("UPDATE task set updated = now()");

    public TaskUpdateSqlBuilder withTitle(String title) {
      if (title != null) {
        sql.append(", title = '")
          .append(title)
          .append("'");
      }
      return this;
    }

    public TaskUpdateSqlBuilder withAbout(String about) {
      if (about != null) {
        sql.append(", about = '")
          .append(about)
          .append("'");
      }
      return this;
    }

    public TaskUpdateSqlBuilder withAssignee(String username) {
      if (username != null) {
        sql.append(", assigne = (select l.id from login l where l.username = '")
          .append(username)
          .append("')");
      }
      return this;
    }

    public TaskUpdateSqlBuilder withStatus(String status) {
      if (status != null) {
        sql.append(", status = '")
          .append(status)
          .append("'");
      }
      return this;
    }

    public TaskUpdateSqlBuilder withPriority(Integer priority) {
      if (priority != null) {
        sql.append(", priority = ")
          .append(priority);
      }
      return this;
    }

    public TaskUpdateSqlBuilder withEstimate(LocalDateTime estimate) {
      if (estimate != null) {
        sql.append(", estimate = ")
          .append(Timestamp.valueOf(estimate));
      }
      return this;
    }

    public TaskUpdateSqlBuilder withTracked(LocalDateTime tracked) {
      if (tracked != null) {
        sql.append(", tracked = ")
          .append(Timestamp.valueOf(tracked));
      }
      return this;
    }

    public String build(Long id) {
      sql.append(" where task.id = ")
        .append(id);
      return sql.toString();

    }
  }
}
