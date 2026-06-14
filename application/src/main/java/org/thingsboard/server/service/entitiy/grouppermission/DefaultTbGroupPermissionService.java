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
package org.thingsboard.server.service.entitiy.grouppermission;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbGroupPermissionService extends AbstractTbEntityService implements TbGroupPermissionService {

    private final GroupPermissionService groupPermissionService;

    @Override
    public GroupPermission save(GroupPermission groupPermission, SecurityUser user) throws Exception {
        ActionType actionType = groupPermission.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = groupPermission.getTenantId();
        try {
            GroupPermission saved = checkNotNull(groupPermissionService.saveGroupPermission(groupPermission));
            autoCommit(user, saved.getId());
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, null, actionType, user);
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.GROUP_PERMISSION), groupPermission, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(GroupPermission groupPermission, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = groupPermission.getTenantId();
        GroupPermissionId groupPermissionId = groupPermission.getId();
        try {
            groupPermissionService.deleteGroupPermission(tenantId, groupPermissionId);
            logEntityActionService.logEntityAction(tenantId, groupPermissionId, groupPermission, null, actionType, user, groupPermissionId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.GROUP_PERMISSION), actionType, user, e, groupPermissionId.toString());
            throw e;
        }
    }
}
