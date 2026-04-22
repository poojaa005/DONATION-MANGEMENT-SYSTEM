package com.ngo.donation_management.service;

import com.ngo.donation_management.dto.UserNotificationDTO;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.entity.UserNotification;
import com.ngo.donation_management.repository.UserNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserNotificationService {

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private AccessScopeService accessScopeService;

    @Transactional
    public UserNotification createNotification(User user,
                                               String title,
                                               String message,
                                               UserNotification.NotificationType type,
                                               String actionUrl,
                                               String eventKey) {
        if (eventKey != null && !eventKey.isBlank()) {
            return userNotificationRepository
                    .findByUser_UserIdAndEventKey(user.getUserId(), eventKey)
                    .orElseGet(() -> saveNotification(user, title, message, type, actionUrl, eventKey));
        }

        return saveNotification(user, title, message, type, actionUrl, null);
    }

    public boolean exists(User user, String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            return false;
        }

        return userNotificationRepository
                .findByUser_UserIdAndEventKey(user.getUserId(), eventKey)
                .isPresent();
    }

    public List<UserNotificationDTO> getCurrentUserNotifications() {
        User currentUser = accessScopeService.requireCurrentUser();
        return userNotificationRepository
                .findByUser_UserIdOrderByCreatedAtDesc(currentUser.getUserId())
                .stream()
                .map(UserNotificationDTO::from)
                .toList();
    }

    public long getCurrentUserUnreadCount() {
        User currentUser = accessScopeService.requireCurrentUser();
        return userNotificationRepository.countByUser_UserIdAndIsReadFalse(
                currentUser.getUserId());
    }

    @Transactional
    public UserNotificationDTO markAsRead(Integer notificationId) {
        User currentUser = accessScopeService.requireCurrentUser();
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException(
                        "Notification not found with id: " + notificationId));

        if (!notification.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new IllegalArgumentException("You cannot update this notification");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = userNotificationRepository.save(notification);
        }

        return UserNotificationDTO.from(notification);
    }

    @Transactional
    public void markAllAsRead() {
        User currentUser = accessScopeService.requireCurrentUser();
        List<UserNotification> notifications =
                userNotificationRepository.findByUser_UserIdOrderByCreatedAtDesc(
                        currentUser.getUserId());

        LocalDateTime readAt = LocalDateTime.now();
        notifications.stream()
                .filter(notification -> !notification.isRead())
                .forEach(notification -> {
                    notification.setRead(true);
                    notification.setReadAt(readAt);
                });

        userNotificationRepository.saveAll(notifications);
    }

    private UserNotification saveNotification(User user,
                                              String title,
                                              String message,
                                              UserNotification.NotificationType type,
                                              String actionUrl,
                                              String eventKey) {
        UserNotification notification = new UserNotification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setActionUrl(actionUrl);
        notification.setEventKey(eventKey);
        return userNotificationRepository.save(notification);
    }
}
