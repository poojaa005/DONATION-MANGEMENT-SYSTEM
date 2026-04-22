package com.ngo.donation_management.controller;

import com.ngo.donation_management.dto.LocationDTO;
import com.ngo.donation_management.dto.MapRouteDTO;
import com.ngo.donation_management.entity.PickupRequest;
import com.ngo.donation_management.entity.TaskAssignment;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.entity.Volunteer;
import com.ngo.donation_management.repository.PickupRequestRepository;
import com.ngo.donation_management.repository.VolunteerRepository;
import com.ngo.donation_management.service.AccessScopeService;
import com.ngo.donation_management.service.LocationService;
import com.ngo.donation_management.service.TaskAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Volunteer-specific task management:
 *  - Self-assign a pending pickup task
 *  - View assigned tasks with donor map location
 *  - Update task status (in_progress, completed)
 *  - Get navigation route to donor
 */
@RestController
@RequestMapping("/api/volunteer-tasks")
@PreAuthorize("hasRole('VOLUNTEER')")
public class VolunteerTaskController {

    @Autowired
    private TaskAssignmentService taskAssignmentService;

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private AccessScopeService accessScopeService;

    /**
     * POST /api/volunteer-tasks/accept
     * Volunteer self-assigns a pending pickup task.
     * Body: { "pickupId": 1, "volunteerId": 2 }
     */
    @PostMapping("/accept")
    public ResponseEntity<?> acceptTask(@RequestBody Map<String, Integer> body) {
        Integer pickupId = body.get("pickupId");
        Integer volunteerId = body.get("volunteerId");
        User currentUser = accessScopeService.requireCurrentUser();

        if (pickupId == null || volunteerId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Pickup and volunteer are required"));
        }

        PickupRequest pickup = pickupRequestRepository.findById(pickupId)
                .orElseThrow(() -> new RuntimeException("Pickup not found: " + pickupId));

        if (pickup.getPickupStatus() != PickupRequest.PickupStatus.pending) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Pickup is no longer available (status: "
                            + pickup.getPickupStatus() + ")"));
        }

        Volunteer volunteer = volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> new RuntimeException("Volunteer not found: " + volunteerId));

        if (volunteer.getUser() == null
                || !currentUser.getUserId().equals(volunteer.getUser().getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error",
                            "You can only accept tasks for your own volunteer account"));
        }

        // Create task assignment
        TaskAssignment task = new TaskAssignment();
        task.setPickupRequest(pickup);
        task.setVolunteer(volunteer);
        task.setTaskStatus(TaskAssignment.TaskStatus.pending);
        TaskAssignment savedTask = taskAssignmentService.createTask(task);

        // Update pickup status → assigned
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
    }

    /**
     * GET /api/volunteer-tasks/my-tasks/{volunteerId}
     * Get all tasks assigned to a volunteer.
     */
    @GetMapping("/my-tasks/{volunteerId}")
    public ResponseEntity<List<TaskAssignment>> getMyTasks(@PathVariable Integer volunteerId) {
        User currentUser = accessScopeService.requireCurrentUser();
        Volunteer volunteer = volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> new RuntimeException("Volunteer not found: " + volunteerId));

        if (volunteer.getUser() == null
                || !currentUser.getUserId().equals(volunteer.getUser().getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(taskAssignmentService.getByVolunteerId(volunteerId));
    }

    /**
     * PATCH /api/volunteer-tasks/{taskId}/status
     * Volunteer updates task status: pending → in_progress → completed.
     * Body: { "status": "in_progress" }
     */
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskAssignment> updateTaskStatus(
            @PathVariable Integer taskId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        TaskAssignment updated = taskAssignmentService.updateTaskStatus(taskId, status);

        return ResponseEntity.ok(updated);
    }

    /**
     * GET /api/volunteer-tasks/{taskId}/map
     * Get donor location for a specific task (for map display).
     */
    @GetMapping("/{taskId}/map")
    public ResponseEntity<?> getTaskMapLocation(@PathVariable Integer taskId) {
        TaskAssignment task = taskAssignmentService.getTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        Integer pickupId = task.getPickupRequest().getPickupId();
        try {
            LocationDTO loc = locationService.getAllPendingPickupLocations()
                    .stream()
                    .filter(l -> l.getPickupId().equals(pickupId))
                    .findFirst()
                    .orElseGet(() -> {
                        // Also check assigned locations
                        return locationService.getLocationsForVolunteer(
                                task.getVolunteer().getVolunteerId())
                                .stream()
                                .filter(l -> l.getPickupId().equals(pickupId))
                                .findFirst()
                                .orElse(null);
                    });
            if (loc == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No location set for this pickup."));
            }
            return ResponseEntity.ok(loc);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Location not available: " + e.getMessage()));
        }
    }

    /**
     * GET /api/volunteer-tasks/{taskId}/navigate?fromLat=&fromLng=
     * Get Google Maps navigation URL from volunteer's current GPS to donor location.
     */
    @GetMapping("/{taskId}/navigate")
    public ResponseEntity<?> navigateToTask(
            @PathVariable Integer taskId,
            @RequestParam Double fromLat,
            @RequestParam Double fromLng) {
        TaskAssignment task = taskAssignmentService.getTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        Integer pickupId = task.getPickupRequest().getPickupId();

        try {
            var donorLoc = locationService.getLocationByPickupId(pickupId);
            MapRouteDTO route = locationService.getNavigationRoute(
                    fromLat, fromLng, donorLoc.getLatitude(), donorLoc.getLongitude());
            return ResponseEntity.ok(route);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Donor location not set for this pickup."));
        }
    }

    /**
     * GET /api/volunteer-tasks/available-pickups
     * All pending pickups with location data — shown to volunteers before they accept.
     */
    @GetMapping("/available-pickups")
    public ResponseEntity<List<LocationDTO>> getAvailablePickups() {
        return ResponseEntity.ok(locationService.getAllPendingPickupLocations());
    }
}
