package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.TasksViewDetail;
import cz.tacr.elza.service.TaskService;

@RestController
@RequestMapping("/api/v1")
public class TaskController implements TasksApi {

	@Autowired
	private TaskService taskService;

	// GET /tasks/my
	public ResponseEntity<List<TasksViewDetail>> tasksGetMyTasks() {
		return ResponseEntity.ok(taskService.getMyTasks());
	}
}
