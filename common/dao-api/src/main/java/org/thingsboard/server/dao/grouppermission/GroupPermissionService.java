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

import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.entity.EntityDaoService;

public interface GroupPermissionService extends EntityDaoService {

    GroupPermission findGroupPermissionById(TenantId tenantId, GroupPermissionId groupPermissionId);

    GroupPermission saveGroupPermission(GroupPermission groupPermission);

    void deleteGroupPermission(TenantId tenantId, GroupPermissionId groupPermissionId);

    PageData<GroupPermission> findGroupPermissionsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionsByTenantIdAndRoleId(TenantId tenantId, RoleId roleId, PageLink pageLink);

    void deleteGroupPermissionsByTenantId(TenantId tenantId);

    void deleteGroupPermissionsByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId);

    void deleteGroupPermissionsByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId);

    void deleteGroupPermissionsByTenantIdAndRoleId(TenantId tenantId, RoleId roleId);

}
