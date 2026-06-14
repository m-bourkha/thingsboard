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
package org.thingsboard.server.dao.grouppermission;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.Optional;
import java.util.UUID;

/**
 * The Interface GroupPermissionDao.
 */
public interface GroupPermissionDao extends Dao<GroupPermission>, TenantEntityDao<GroupPermission> {

    /**
     * Save or update group permission object.
     *
     * @param tenantId the tenant id
     * @param groupPermission the group permission object
     * @return saved group permission object
     */
    GroupPermission save(TenantId tenantId, GroupPermission groupPermission);

    /**
     * Find group permissions by tenant id and page link.
     *
     * @param tenantId the tenant id
     * @param pageLink the page link
     * @return the page of group permission objects
     */
    PageData<GroupPermission> findGroupPermissionsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find group permissions by tenant id, user group id and page link.
     *
     * @param tenantId the tenant id
     * @param userGroupId the user group id
     * @param pageLink the page link
     * @return the page of group permission objects
     */
    PageData<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupId(UUID tenantId, UUID userGroupId, PageLink pageLink);

    /**
     * Find group permissions by tenant id, entity group id and page link.
     *
     * @param tenantId the tenant id
     * @param entityGroupId the entity group id
     * @param pageLink the page link
     * @return the page of group permission objects
     */
    PageData<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupId(UUID tenantId, UUID entityGroupId, PageLink pageLink);

    /**
     * Find group permissions by tenant id, role id and page link.
     *
     * @param tenantId the tenant id
     * @param roleId the role id
     * @param pageLink the page link
     * @return the page of group permission objects
     */
    PageData<GroupPermission> findGroupPermissionsByTenantIdAndRoleId(UUID tenantId, UUID roleId, PageLink pageLink);

    /**
     * Find a group permission by its unique tenant + user group + role + entity group combination.
     *
     * @param tenantId the tenant id
     * @param userGroupId the user group id
     * @param roleId the role id
     * @param entityGroupId the entity group id (nullable)
     * @return the group permission object
     */
    Optional<GroupPermission> findGroupPermissionByTenantIdAndUserGroupIdAndRoleIdAndEntityGroupId(UUID tenantId, UUID userGroupId, UUID roleId, UUID entityGroupId);

}
