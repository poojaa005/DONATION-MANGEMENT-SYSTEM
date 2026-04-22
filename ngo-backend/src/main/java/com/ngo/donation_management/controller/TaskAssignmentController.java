package com.ngo.donation_management.controller;

// controller/TaskAssignmentController.java
import com.ngo.donation_management.entity.TaskAssignment;
import com.ngo.donation_management.service.TaskAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskAssignmentController {

    @Autowired
    private TaskAssignmentService taskAssignmentService;

    // CREATE (Assign Task)
    @PostMapping
    public ResponseEntity<?> createTask(
            @RequestBody TaskAssignment task) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("error",
                        "Manual task assignment is disabled. Volunteers must accept approved pickups themselves."));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<TaskAssignment>> getAllTasks() {
        return ResponseEntity.ok(
                taskAssignmentService.getAllTasks());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(
            @PathVariable Integer id) {
        return taskAssignmentService.getTaskById(id)
                .map(t -> ResponseEntity.ok((Object) t))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Task not found with id: " + id)));
    }

    // VIEW SCHEDULE (By Volunteer ID)
    @GetMapping("/volunteer/{volunteerId}")
    public ResponseEntity<List<TaskAssignment>> getByVolunteerId(
            @PathVariable Integer volunteerId) {
        return ResponseEntity.ok(
                taskAssignmentService
                        .getByVolunteerId(volunteerId));
    }

    // SEARCH BY STATUS
    @GetMapping("/search/status")
    public ResponseEntity<List<TaskAssignment>> getByStatus(
            @RequestParam String status) {
        return ResponseEntity.ok(
                taskAssignmentService.getByStatus(status));
    }

    // SEARCH BY PICKUP ID
    @GetMapping("/search/pickup/{pickupId}")
    public ResponseEntity<List<TaskAssignment>> getByPickupId(
            @PathVariable Integer pickupId) {
        return ResponseEntity.ok(
                taskAssignmentService.getByPickupId(pickupId));
    }

    // UPDATE TASK STATUS (Volunteer feature)
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskAssignment> updateTaskStatus(
            @PathVariable Integer id,
            @RequestParam String status) {
        return ResponseEntity.ok(
                taskAssignmentService
                        .updateTaskStatus(id, status));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<TaskAssignment> updateTask(
            @PathVariable Integer id,
            @RequestBody TaskAssignment task) {
        return ResponseEntity.ok(
                taskAssignmentService.updateTask(id, task));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(
            @PathVariable Integer id) {
        taskAssignmentService.deleteTask(id);
        return ResponseEntity.ok(Map.of("message",
                "Task deleted successfully"));
    }
}
