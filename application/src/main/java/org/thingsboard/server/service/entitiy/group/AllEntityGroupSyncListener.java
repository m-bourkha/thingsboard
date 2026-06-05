/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy.group;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupTypes;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.group.EntityGroupService;

import java.lang.reflect.Method;

@Component
@RequiredArgsConstructor
@Slf4j
public class AllEntityGroupSyncListener {

    private final EntityGroupService entityGroupService;

    @TransactionalEventListener(fallbackExecution = true)
    public void onSave(SaveEntityEvent<?> event) {
        EntityId entityId = event.getEntityId();
        EntityType entityType = entityId.getEntityType();
        if (!EntityGroupTypes.GROUPABLE.contains(entityType)) {
            return;
        }
        Object entity = event.getEntity();
        Object oldEntity = event.getOldEntity();
        if (entity == null) {
            return;
        }
        TenantId tenantId = getTenantId(entity);
        if (tenantId == null) {
            return;
        }
        EntityId ownerId = resolveOwner(entity, tenantId);
        EntityGroup allGroup = entityGroupService.findOrCreateAllGroup(tenantId, ownerId, entityType);
        entityGroupService.addEntityToGroup(allGroup.getId(), entityId);

        if (oldEntity != null) {
            EntityId oldOwnerId = resolveOwner(oldEntity, tenantId);
            if (!oldOwnerId.equals(ownerId)) {
                EntityGroup oldAllGroup = entityGroupService.findOrCreateAllGroup(tenantId, oldOwnerId, entityType);
                entityGroupService.removeEntityFromGroup(oldAllGroup.getId(), entityId);
            }
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onDelete(DeleteEntityEvent event) {
        EntityId entityId = event.getEntityId();
        EntityType entityType = entityId.getEntityType();
        if (!EntityGroupTypes.GROUPABLE.contains(entityType)) {
            return;
        }
        TenantId tenantId = event.getTenantId();
        entityGroupService.removeEntityFromAllGroups(tenantId, entityId);
    }

    private TenantId getTenantId(Object entity) {
        if (entity instanceof HasTenantId hasTenantId) {
            return hasTenantId.getTenantId();
        }
        return null;
    }

    private EntityId resolveOwner(Object entity, TenantId tenantId) {
        try {
            Method getOwnerId = entity.getClass().getMethod("getOwnerId");
            EntityId ownerId = (EntityId) getOwnerId.invoke(entity);
            if (ownerId != null) {
                return ownerId;
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.warn("Failed to resolve owner for {}: {}", entity.getClass().getSimpleName(), e.getMessage());
        }
        if (entity instanceof Customer customer) {
            return customer.getTenantId();
        }
        return tenantId;
    }
}
