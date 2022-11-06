/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationSeverity;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService {

    private final NotificationRequestDao notificationRequestDao;
    private final NotificationDao notificationDao;

    private final NotificationRequestValidator notificationRequestValidator = new NotificationRequestValidator();

    @Override
    public NotificationRequest saveNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest) {
        if (StringUtils.isBlank(notificationRequest.getNotificationReason())) {
            notificationRequest.setNotificationReason(NotificationRequest.GENERAL_NOTIFICATION_REASON);
        }
        if (notificationRequest.getNotificationSeverity() == null) {
            notificationRequest.setNotificationSeverity(NotificationSeverity.NORMAL);
        }
        notificationRequestValidator.validate(notificationRequest, NotificationRequest::getTenantId);
        return notificationRequestDao.save(tenantId, notificationRequest);
    }

    @Override
    public NotificationRequest findNotificationRequestById(TenantId tenantId, NotificationRequestId id) {
        return notificationRequestDao.findById(tenantId, id.getId());
    }

    @Override
    public PageData<NotificationRequest> findNotificationRequestsByTenantId(TenantId tenantId, PageLink pageLink) {
        return notificationRequestDao.findByTenantIdAndPageLink(tenantId, pageLink);
    }

    @Override
    public List<NotificationRequest> findNotificationRequestsByRuleIdAndAlarmId(TenantId tenantId, NotificationRuleId ruleId, AlarmId alarmId) {
        return notificationRequestDao.findByRuleIdAndAlarmId(tenantId, ruleId, alarmId);
    }

    // ON DELETE CASCADE is used: notifications for request are deleted as well
    @Override
    public void deleteNotificationRequestById(TenantId tenantId, NotificationRequestId id) {
        notificationRequestDao.removeById(tenantId, id.getId());
    }

    @Override
    public Notification saveNotification(TenantId tenantId, Notification notification) {
        return notificationDao.save(tenantId, notification);
    }

    @Override
    public Notification findNotificationById(TenantId tenantId, NotificationId notificationId) {
        return notificationDao.findById(tenantId, notificationId.getId());
    }

    @Override
    public boolean markNotificationAsRead(TenantId tenantId, UserId userId, NotificationId notificationId) {
        return notificationDao.updateStatusByIdAndUserId(tenantId, userId, notificationId, NotificationStatus.READ);
    }

    @Override
    public PageData<Notification> findNotificationsByUserIdAndReadStatus(TenantId tenantId, UserId userId, boolean unreadOnly, PageLink pageLink) {
        if (unreadOnly) {
            return notificationDao.findUnreadByUserIdAndPageLink(tenantId, userId, pageLink);
        } else {
            return notificationDao.findByUserIdAndPageLink(tenantId, userId, pageLink);
        }
    }

    @Override
    public PageData<Notification> findLatestUnreadNotificationsByUserId(TenantId tenantId, UserId userId, int limit) {
        SortOrder sortOrder = new SortOrder(EntityKeyMapping.CREATED_TIME, SortOrder.Direction.DESC);
        PageLink pageLink = new PageLink(limit, 0, null, sortOrder);
        return findNotificationsByUserIdAndReadStatus(tenantId, userId, true, pageLink);
    }

    @Override
    public int countUnreadNotificationsByUserId(TenantId tenantId, UserId userId) {
        return notificationDao.countUnreadByUserId(tenantId, userId);
    }

    @Override
    public int updateNotificationsInfosByRequestId(TenantId tenantId, NotificationRequestId notificationRequestId, NotificationInfo notificationInfo) {
        return notificationDao.updateInfosByRequestId(tenantId, notificationRequestId, notificationInfo);
    }

    private static class NotificationRequestValidator extends DataValidator<NotificationRequest> {

        @Override
        protected void validateDataImpl(TenantId tenantId, NotificationRequest notificationRequest) {
        }

    }

}
