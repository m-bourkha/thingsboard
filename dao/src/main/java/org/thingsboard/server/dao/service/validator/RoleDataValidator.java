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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

@Component
public class RoleDataValidator extends DataValidator<Role> {

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private TenantService tenantService;

    @Override
    protected void validateCreate(TenantId tenantId, Role role) {
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.ROLE);
    }

    @Override
    protected Role validateUpdate(TenantId tenantId, Role role) {
        Role old = roleDao.findById(role.getTenantId(), role.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing role!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Role role) {
        validateString("Role name", role.getName());
        if (role.getType() == null) {
            throw new DataValidationException("Role type should be specified!");
        }
        if (role.getTenantId() == null) {
            throw new DataValidationException("Role should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(role.getTenantId())) {
                throw new DataValidationException("Role is referencing to non-existent tenant!");
            }
        }
    }
}
