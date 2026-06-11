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
package org.thingsboard.server.dao.role;

import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.UUID;

/**
 * The Interface RoleDao.
 */
public interface RoleDao extends Dao<Role>, TenantEntityDao<Role>, ExportableEntityDao<RoleId, Role> {

    /**
     * Save or update role object.
     *
     * @param tenantId the tenant id
     * @param role the role object
     * @return saved role object
     */
    Role save(TenantId tenantId, Role role);

    /**
     * Find roles by tenant id and page link.
     *
     * @param tenantId the tenant id
     * @param pageLink the page link
     * @return the page of role objects
     */
    PageData<Role> findRolesByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find role by tenantId and role name.
     *
     * @param tenantId the tenantId
     * @param name the role name
     * @return the role object
     */
    Role findRoleByTenantIdAndName(UUID tenantId, String name);

}
