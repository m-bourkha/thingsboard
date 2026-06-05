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
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupTypes;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
public class EntityGroupDataValidator extends DataValidator<EntityGroup> {

    @Autowired
    private EntityGroupDao entityGroupDao;

    @Autowired
    private TenantService tenantService;

    @Override
    protected void validateCreate(TenantId tenantId, EntityGroup entityGroup) {
    }

    @Override
    protected EntityGroup validateUpdate(TenantId tenantId, EntityGroup entityGroup) {
        EntityGroup old = entityGroupDao.findById(tenantId, entityGroup.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non-existing entity group!");
        }
        if (old.isGroupAll() && !old.getName().equals(entityGroup.getName())) {
            throw new DataValidationException("Name of the 'All' group cannot be changed!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, EntityGroup entityGroup) {
        validateString("Entity group name", entityGroup.getName());
        if (entityGroup.getType() == null) {
            throw new DataValidationException("Entity group type must be specified!");
        }
        if (!EntityGroupTypes.GROUPABLE.contains(entityGroup.getType())) {
            throw new DataValidationException("Entity type '" + entityGroup.getType() + "' is not groupable!");
        }
        if (entityGroup.getTenantId() == null) {
            throw new DataValidationException("Entity group must be assigned to a tenant!");
        }
        if (!tenantService.tenantExists(entityGroup.getTenantId())) {
            throw new DataValidationException("Entity group references a non-existent tenant!");
        }
        if (entityGroup.getOwnerId() == null) {
            throw new DataValidationException("Entity group must have an owner!");
        }
    }
}
