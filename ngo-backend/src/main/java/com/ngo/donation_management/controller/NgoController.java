package com.ngo.donation_management.controller;

// controller/NgoController.java

import com.ngo.donation_management.entity.Ngo;
import com.ngo.donation_management.service.NgoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ngos")
public class NgoController {

    @Autowired
    private NgoService ngoService;

    // CREATE
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Ngo> createNgo(@Valid @RequestBody Ngo ngo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ngoService.createNgo(ngo));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Ngo>> getAllNgos() {
        return ResponseEntity.ok(ngoService.getAllNgos());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getNgoById(@PathVariable Integer id) {
        return ngoService.getNgoById(id)
                .map(ngo -> ResponseEntity.ok((Object) ngo))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "NGO not found with id: " + id)));
    }

    // SEARCH BY NAME
    @GetMapping("/search/name")
    public ResponseEntity<List<Ngo>> searchByName(
            @RequestParam String name) {
        return ResponseEntity.ok(ngoService.searchByName(name));
    }

    // SEARCH BY CITY
    @GetMapping("/search/city")
    public ResponseEntity<List<Ngo>> searchByCity(
            @RequestParam String city) {
        return ResponseEntity.ok(ngoService.searchByCity(city));
    }

    // SEARCH BY STATE
    @GetMapping("/search/state")
    public ResponseEntity<List<Ngo>> searchByState(
            @RequestParam String state) {
        return ResponseEntity.ok(ngoService.searchByState(state));
    }

    // SEARCH NEARBY (Guest feature)
    @GetMapping("/search/nearby")
    public ResponseEntity<List<Ngo>> searchNearby(
            @RequestParam String location) {
        return ResponseEntity.ok(
                ngoService.searchNearby(location));
    }

    // UPDATE
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Ngo> updateNgo(
            @PathVariable Integer id, @Valid @RequestBody Ngo ngo) {
        return ResponseEntity.ok(ngoService.updateNgo(id, ngo));
    }

    // DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteNgo(@PathVariable Integer id) {
        ngoService.deleteNgo(id);
        return ResponseEntity.ok(Map.of("message",
                "NGO deleted successfully"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid NGO details",
                "fields", fieldErrors
        ));
    }
}
