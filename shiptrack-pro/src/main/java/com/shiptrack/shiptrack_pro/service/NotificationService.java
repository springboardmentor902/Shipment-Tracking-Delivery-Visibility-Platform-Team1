package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.NotificationRequest;
import com.shiptrack.shiptrack_pro.dto.NotificationResponse;
import com.shiptrack.shiptrack_pro.entity.NotificationType;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;

import java.util.List;

public interface NotificationService {
    NotificationResponse send(NotificationType type, User user, Shipment shipment);

    List<NotificationResponse> getForUser(String requesterEmail);

    NotificationResponse markAsRead(Long notificationId, String requesterEmail);

    NotificationResponse create(NotificationRequest request, String requesterEmail);
}
