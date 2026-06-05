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
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultTbEntityGroupService extends AbstractTbEntityService implements TbEntityGroupService {

    private final EntityGroupService entityGroupService;

    @Override
    public EntityGroup save(EntityGroup entityGroup, SecurityUser user) throws Exception {
        ActionType actionType = entityGroup.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = entityGroup.getTenantId();
        try {
            EntityGroup saved = checkNotNull(entityGroupService.saveEntityGroup(entityGroup));
            autoCommit(user, saved.getId());
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, null, actionType, user);
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_GROUP), entityGroup, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(EntityGroup entityGroup, User user) {
        TenantId tenantId = entityGroup.getTenantId();
        EntityGroupId entityGroupId = entityGroup.getId();
        try {
            entityGroupService.deleteEntityGroup(tenantId, entityGroupId);
            logEntityActionService.logEntityAction(tenantId, entityGroupId, entityGroup, ActionType.DELETED, user, entityGroupId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_GROUP), ActionType.DELETED, user, e, entityGroupId.toString());
            throw e;
        }
    }

    @Override
    public EntityGroup makePublic(EntityGroup entityGroup, SecurityUser user) throws Exception {
        TenantId tenantId = entityGroup.getTenantId();
        try {
            EntityGroup updated = checkNotNull(entityGroupService.makePublic(tenantId, entityGroup.getId()));
            logEntityActionService.logEntityAction(tenantId, updated.getId(), updated, null, ActionType.UPDATED, user);
            return updated;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, entityGroup.getId(), entityGroup, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    public EntityGroup makePrivate(EntityGroup entityGroup, SecurityUser user) throws Exception {
        TenantId tenantId = entityGroup.getTenantId();
        try {
            EntityGroup updated = checkNotNull(entityGroupService.makePrivate(tenantId, entityGroup.getId()));
            logEntityActionService.logEntityAction(tenantId, updated.getId(), updated, null, ActionType.UPDATED, user);
            return updated;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, entityGroup.getId(), entityGroup, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    public EntityGroup changeOwner(EntityGroup entityGroup, EntityId newOwnerId, SecurityUser user) throws Exception {
        TenantId tenantId = entityGroup.getTenantId();
        try {
            EntityGroup updated = checkNotNull(entityGroupService.changeOwner(tenantId, entityGroup.getId(), newOwnerId));
            logEntityActionService.logEntityAction(tenantId, updated.getId(), updated, null, ActionType.UPDATED, user);
            return updated;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, entityGroup.getId(), entityGroup, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    public void addEntitiesToGroup(EntityGroup entityGroup, List<EntityId> entityIds, SecurityUser user) throws Exception {
        TenantId tenantId = entityGroup.getTenantId();
        for (EntityId entityId : entityIds) {
            entityGroupService.addEntityToGroup(entityGroup.getId(), entityId);
        }
        logEntityActionService.logEntityAction(tenantId, entityGroup.getId(), entityGroup, null, ActionType.UPDATED, user);
    }

    @Override
    public void removeEntitiesFromGroup(EntityGroup entityGroup, List<EntityId> entityIds, SecurityUser user) throws Exception {
        TenantId tenantId = entityGroup.getTenantId();
        for (EntityId entityId : entityIds) {
            entityGroupService.removeEntityFromGroup(entityGroup.getId(), entityId);
        }
        logEntityActionService.logEntityAction(tenantId, entityGroup.getId(), entityGroup, null, ActionType.UPDATED, user);
    }
}
