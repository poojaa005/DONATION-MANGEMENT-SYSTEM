package com.ngo.donation_management.service;

import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class AccessScopeService {

    private final UserRepository userRepository;

    public AccessScopeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }

        return userRepository.findByEmail(authentication.getName());
    }

    public User requireCurrentUser() {
        return getCurrentUser().orElseThrow(() ->
                new AccessDeniedException("Authentication required"));
    }

    public boolean isAppAdmin(User user) {
        return user.getRole() == User.Role.admin;
    }

    public boolean isNgoAdmin(User user) {
        return user.getRole() == User.Role.ngo_admin;
    }

    public boolean isDonor(User user) {
        return user.getRole() == User.Role.donor;
    }

    public boolean isVolunteer(User user) {
        return user.getRole() == User.Role.volunteer;
    }

    public boolean isAdminLike(User user) {
        return isAppAdmin(user) || isNgoAdmin(user);
    }

    public Integer requireNgoId(User user) {
        if (user.getNgo() == null || user.getNgo().getNgoId() == null) {
            throw new AccessDeniedException(
                    "NGO admin account is not assigned to an NGO");
        }
        return user.getNgo().getNgoId();
    }

    public Optional<Integer> getScopedNgoId() {
        return getCurrentUser()
                .filter(this::isNgoAdmin)
                .map(this::requireNgoId);
    }

    public void ensureAppAdmin() {
        User currentUser = requireCurrentUser();
        if (!isAppAdmin(currentUser)) {
            throw new AccessDeniedException(
                    "Only the app admin can perform this action");
        }
    }

    public void ensureNgoAccess(Integer ngoId) {
        User currentUser = requireCurrentUser();

        if (isAppAdmin(currentUser)) {
            return;
        }

        if (!isNgoAdmin(currentUser)) {
            throw new AccessDeniedException("Access denied");
        }

        Integer currentNgoId = requireNgoId(currentUser);
        if (!Objects.equals(currentNgoId, ngoId)) {
            throw new AccessDeniedException(
                    "You can only manage your assigned NGO");
        }
    }
}
