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
package org.thingsboard.server.service.entitiy.role;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbRoleService extends AbstractTbEntityService implements TbRoleService {

    private final RoleService roleService;

    @Override
    public Role save(Role role, SecurityUser user) throws Exception {
        ActionType actionType = role.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = role.getTenantId();
        try {
            Role savedRole = checkNotNull(roleService.saveRole(role));
            autoCommit(user, savedRole.getId());
            logEntityActionService.logEntityAction(tenantId, savedRole.getId(), savedRole, null, actionType, user);
            return savedRole;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ROLE), role, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(Role role, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = role.getTenantId();
        RoleId roleId = role.getId();
        try {
            roleService.deleteRole(tenantId, roleId);
            logEntityActionService.logEntityAction(tenantId, roleId, role, null, actionType, user, roleId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ROLE), actionType, user, e, roleId.toString());
            throw e;
        }
    }
}
