package com.ngo.donation_management.service;

// service/UrgentNeedsService.java

import com.ngo.donation_management.entity.UrgentNeeds;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.repository.UrgentNeedsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UrgentNeedsService {

    @Autowired
    private UrgentNeedsRepository urgentNeedsRepository;

    @Autowired
    @Lazy
    private NotificationService notificationService;

    /**
     * Creates an urgent need and immediately notifies all active volunteers
     * via both Email and SMS.
     */
    public UrgentNeeds createUrgentNeed(UrgentNeeds urgentNeed) {
        User currentUser = accessScopeService.requireCurrentUser();
        Integer ngoId = urgentNeed.getNgo() != null
                ? urgentNeed.getNgo().getNgoId()
                : null;

        if (ngoId == null) {
            throw new IllegalArgumentException("Urgent need NGO is required");
        }

        accessScopeService.ensureNgoAccess(ngoId);
        urgentNeed.setAdmin(currentUser);
        UrgentNeeds saved = urgentNeedsRepository.save(urgentNeed);
        // Fire-and-forget async notifications
        notificationService.notifyVolunteersUrgentNeed(saved);
        notificationService.notifyDonorsUrgentNeed(saved);
        notificationService.notifyUrgentNeedCreated(saved);
        return saved;
    }

    public List<UrgentNeeds> getAllUrgentNeeds() {
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isPresent()) {
            return urgentNeedsRepository.findByNgo_NgoId(scopedNgoId.get());
        }
        return urgentNeedsRepository.findAll();
    }

    public Optional<UrgentNeeds> getUrgentNeedById(Integer id) {
        return urgentNeedsRepository.findById(id)
                .filter(this::canAccessUrgentNeed);
    }

    public List<UrgentNeeds> getByStatus(String status) {
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isPresent()) {
            return urgentNeedsRepository.findByUrgentStatusAndNgo_NgoId(
                    UrgentNeeds.UrgentStatus.valueOf(
                            status.toLowerCase()),
                    scopedNgoId.get());
        }

        return urgentNeedsRepository.findByUrgentStatus(
                UrgentNeeds.UrgentStatus.valueOf(
                        status.toLowerCase()));
    }

    public List<UrgentNeeds> searchByTitle(String title) {
        return filterByScope(urgentNeedsRepository
                .findByTitleContainingIgnoreCase(title));
    }

    public List<UrgentNeeds> getByAdminId(Integer adminId) {
        return filterByScope(urgentNeedsRepository.findByAdmin_UserId(adminId));
    }

    public UrgentNeeds updateUrgentNeed(Integer id,
                                        UrgentNeeds updatedNeed) {
        return urgentNeedsRepository.findById(id).map(need -> {
            if (need.getNgo() != null) {
                accessScopeService.ensureNgoAccess(need.getNgo().getNgoId());
            }

            Integer ngoId = updatedNeed.getNgo() != null
                    ? updatedNeed.getNgo().getNgoId()
                    : null;
            if (ngoId == null) {
                throw new IllegalArgumentException("Urgent need NGO is required");
            }

            accessScopeService.ensureNgoAccess(ngoId);
            need.setAdmin(accessScopeService.requireCurrentUser());
            need.setNgo(updatedNeed.getNgo());
            need.setTitle(updatedNeed.getTitle());
            need.setMessage(updatedNeed.getMessage());
            need.setStartTime(updatedNeed.getStartTime());
            need.setEndTime(updatedNeed.getEndTime());
            need.setUrgentStatus(updatedNeed.getUrgentStatus());
            return urgentNeedsRepository.save(need);
        }).orElseThrow(() -> new RuntimeException(
                "Urgent need not found with id: " + id));
    }

    public void deleteUrgentNeed(Integer id) {
        UrgentNeeds need = urgentNeedsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Urgent need not found with id: " + id));
        if (need.getNgo() != null) {
            accessScopeService.ensureNgoAccess(need.getNgo().getNgoId());
        }
        urgentNeedsRepository.delete(need);
    }

    @Autowired
    private AccessScopeService accessScopeService;

    private List<UrgentNeeds> filterByScope(List<UrgentNeeds> urgentNeeds) {
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isEmpty()) {
            return urgentNeeds;
        }

        Integer ngoId = scopedNgoId.get();
        return urgentNeeds.stream()
                .filter(need -> need.getNgo() != null
                        && ngoId.equals(need.getNgo().getNgoId()))
                .collect(Collectors.toList());
    }

    private boolean canAccessUrgentNeed(UrgentNeeds urgentNeed) {
        try {
            if (urgentNeed != null && urgentNeed.getNgo() != null) {
                accessScopeService.ensureNgoAccess(urgentNeed.getNgo().getNgoId());
            } else if (accessScopeService.getScopedNgoId().isPresent()) {
                return false;
            }
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
