package com.github.mxab;

import java.util.UUID;
import java.util.stream.Stream;

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TaskController {

	private static final Logger LOG = LoggerFactory
			.getLogger(TaskController.class);
	@Autowired
	ProjectConnection connection;

	@Autowired
	SimpMessagingTemplate messagingTemplate;

	@ResponseBody
	@RequestMapping(value = "/tasks", method = RequestMethod.GET)
	public UITask[] tasks() {

		DomainObjectSet<? extends GradleTask> tasks = connection.getModel(
				GradleProject.class).getTasks();

		Stream<UITask> map = tasks.stream().map(UITask::new);

		UITask[] array = map.toArray(UITask[]::new);
		return array;
	}

	public static class UITask {
		private String displayName;
		private String description;
		private String name;
		private String path;

		public UITask(GradleTask task) {
			displayName = task.getDisplayName();
			description = task.getDescription();
			name = task.getName();
			path = task.getPath();
		}

		public String getDescription() {
			return description;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getName() {
			return name;
		}

		public String getPath() {
			return path;
		}
	}

	public static class LaunchedTask {
		private String id;

		public LaunchedTask(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

	}

	@ResponseBody
	@RequestMapping(value = "/tasks/{task}", method = RequestMethod.POST)
	public LaunchedTask tasks(@PathVariable("task") String task) {

		UUID uuid = UUID.randomUUID();
		BuildLauncher launcher = connection.newBuild().forTasks(task);
		launcher.addProgressListener(new ProgressListener() {

			@Override
			public void statusChanged(ProgressEvent event) {

				messagingTemplate.convertAndSend("/topic/tasks/progress",
						new Progress(uuid.toString(), event.getDescription()));
				LOG.info("Progress: {}", event.getDescription());
			}
		});

		launcher.run(new ResultHandler<Void>() {

			@Override
			public void onComplete(Void result) {
				LOG.info("Complete: {}", result);

				messagingTemplate.convertAndSend("/topic/tasks/progress",
						new Progress(uuid.toString(), "COMPLETE"));
			}

			@Override
			public void onFailure(GradleConnectionException failure) {
				LOG.info("Failure: {}", failure);
				messagingTemplate.convertAndSend("/topic/tasks/progress",
						new Progress(uuid.toString(), "FAILURE"));

			}
		});
		return new LaunchedTask(uuid.toString());
	}

	public static class Progress {
		private String description;
		private String taskId;

		public Progress(String taskId, String description) {
			this.taskId = taskId;
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getTaskId() {
			return taskId;
		}

		public void setTaskId(String taskId) {
			this.taskId = taskId;
		}
	}
}
