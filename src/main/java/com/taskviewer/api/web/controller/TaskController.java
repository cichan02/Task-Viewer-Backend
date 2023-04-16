package com.taskviewer.api.web.controller;

import com.taskviewer.api.model.Status;
import com.taskviewer.api.model.Task;
import com.taskviewer.api.model.TimeEstimate;
import com.taskviewer.api.model.User;
import com.taskviewer.api.postgres.PgTask;
import com.taskviewer.api.service.MailService;
import com.taskviewer.api.service.TaskService;
import com.taskviewer.api.service.UserService;
import com.taskviewer.api.web.rq.RqTask;
import com.taskviewer.api.web.rq.RqTaskSearchCriteria;
import com.taskviewer.api.web.rq.RqTaskUpdate;
import com.taskviewer.api.web.rq.RqTrackTime;
import com.taskviewer.api.web.rs.RsTask;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

  private final TaskService tasks;
  private final UserService users;
  private final MailService mails;

  @PreAuthorize("hasAuthority('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  public void create(@RequestBody final RqTask request) {
    this.tasks.add(
      PgTask.builder()
        .title(request.title())
        .username(request.username())
        .status(new Status.Simple(request.status(), request.priority()))
        .time(new TimeEstimate.InMinutes(request.due(), 0))
        .build()
    );
  }

  @PreAuthorize("hasAuthority('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/replicate/{id}")
  public void replicate(@PathVariable final Long id) {
    final Task task = this.tasks.byId(id);
    this.tasks.add(
      PgTask.builder()
        .title(task.title())
        .username(task.username())
        .status(task.status())
        .time(
          new TimeEstimate.InMinutes(
            task.time().due().plusDays(1L),
            0
          )
        )
        .build()
    );
  }

  @PreAuthorize("hasAuthority('ADMIN')")
  @PutMapping("/{id}")
  public RsTask update(
    @PathVariable final Long id,
    @RequestBody final RqTaskUpdate request) {
    return new RsTask(
      this.tasks.update(id, request)
    );
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
  @PatchMapping("/{id}")
  public RsTask track(@PathVariable final Long id,
                      @RequestBody final RqTrackTime request) {
    return new RsTask(this.tasks.track(id, request.minutes()));
  }

  @PreAuthorize("hasAuthority('ADMIN')")
  @DeleteMapping("/{id}")
  public void delete(@PathVariable final Long id) {
    this.tasks.delete(id);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
  @GetMapping
  public List<RsTask> byCriteria(@RequestBody final RqTaskSearchCriteria criteria) {
    return this.tasks.byCriteria(criteria)
      .stream()
      .map(RsTask::new)
      .toList();
  }

  @PreAuthorize("hasAuthority('ADMIN')")
  @PatchMapping("/close/{id}")
  public RsTask close(@PathVariable final Long id) {
    return new RsTask(
      this.tasks.update(id, "done")
    );
  }

  @PreAuthorize("hasAuthority('ADMIN')")
  @PostMapping("assign/{id}/{username}")
  public RsTask assign(@PathVariable final Long id,
                       @PathVariable final String username) {
    final User user = this.users.byUsername(username);
    final Task assigned = this.tasks.assign(id, user.id());
    this.mails.send(
      user,
      "Task assigned to you",
      "Task %s was assigned to you"
        .formatted(
          assigned.title()
        )
    );
    return new RsTask(assigned);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
  @GetMapping("/@{username}")
  public List<RsTask> assigned(@PathVariable final String username) {
    return this.tasks.byUsername(username)
      .stream()
      .map(RsTask::new)
      .toList();
  }

}
