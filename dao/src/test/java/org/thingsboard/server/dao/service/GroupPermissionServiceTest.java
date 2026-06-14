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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RolePermissions;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;

@DaoSqlTest
public class GroupPermissionServiceTest extends AbstractServiceTest {

    @Autowired
    GroupPermissionService groupPermissionService;
    @Autowired
    RoleService roleService;
    @Autowired
    EntityGroupService entityGroupService;

    Role groupRole;
    EntityGroup userGroup;
    EntityGroup entityGroup;

    @Before
    public void before() {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName("Test GROUP Role");
        role.setType(RoleType.GROUP);
        RolePermissions permissions = new RolePermissions();
        permissions.setOperations(List.of("ALL"));
        role.setPermissions(permissions);
        groupRole = roleService.saveRole(role);

        EntityGroup ug = new EntityGroup();
        ug.setTenantId(tenantId);
        ug.setOwnerId(tenantId);
        ug.setType(EntityType.USER);
        ug.setName("Test User Group");
        userGroup = entityGroupService.saveEntityGroup(ug);

        EntityGroup eg = new EntityGroup();
        eg.setTenantId(tenantId);
        eg.setOwnerId(tenantId);
        eg.setType(EntityType.DEVICE);
        eg.setName("Test Device Group");
        entityGroup = entityGroupService.saveEntityGroup(eg);
    }

    @After
    public void after() {
        groupPermissionService.deleteGroupPermissionsByTenantId(tenantId);
        entityGroupService.deleteEntityGroupsByTenantId(tenantId);
        roleService.deleteRolesByTenantId(tenantId);
    }

    private GroupPermission createGroupPermission(EntityGroupId userGroupId, RoleId roleId, EntityGroupId entityGroupId) {
        GroupPermission gp = new GroupPermission();
        gp.setTenantId(tenantId);
        gp.setUserGroupId(userGroupId);
        gp.setRoleId(roleId);
        gp.setEntityGroupId(entityGroupId);
        gp.setEntityGroupType(EntityType.DEVICE);
        gp.setPublicPermission(false);
        return gp;
    }

    @Test
    public void testSaveGroupPermission() {
        GroupPermission gp = createGroupPermission(userGroup.getId(), groupRole.getId(), entityGroup.getId());
        GroupPermission saved = groupPermissionService.saveGroupPermission(gp);

        Assert.assertNotNull(saved);
        Assert.assertNotNull(saved.getId());
        Assert.assertTrue(saved.getCreatedTime() > 0);
        Assert.assertEquals(tenantId, saved.getTenantId());
        Assert.assertEquals(userGroup.getId(), saved.getUserGroupId());
        Assert.assertEquals(groupRole.getId(), saved.getRoleId());
        Assert.assertEquals(entityGroup.getId(), saved.getEntityGroupId());
    }

    @Test
    public void testFindGroupPermissionById() {
        GroupPermission saved = groupPermissionService.saveGroupPermission(
                createGroupPermission(userGroup.getId(), groupRole.getId(), entityGroup.getId()));
        GroupPermission found = groupPermissionService.findGroupPermissionById(tenantId, saved.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(saved.getId(), found.getId());
    }

    @Test
    public void testDeleteGroupPermission() {
        GroupPermission saved = groupPermissionService.saveGroupPermission(
                createGroupPermission(userGroup.getId(), groupRole.getId(), entityGroup.getId()));
        groupPermissionService.deleteGroupPermission(tenantId, saved.getId());
        GroupPermission found = groupPermissionService.findGroupPermissionById(tenantId, saved.getId());
        Assert.assertNull(found);
    }

    @Test
    public void testFindByEntityGroupId() {
        groupPermissionService.saveGroupPermission(
                createGroupPermission(userGroup.getId(), groupRole.getId(), entityGroup.getId()));
        PageData<GroupPermission> page = groupPermissionService.findGroupPermissionsByTenantIdAndEntityGroupId(
                tenantId, entityGroup.getId(), new PageLink(10));
        Assert.assertEquals(1, page.getTotalElements());
    }

    @Test
    public void testFindByUserGroupId() {
        groupPermissionService.saveGroupPermission(
                createGroupPermission(userGroup.getId(), groupRole.getId(), entityGroup.getId()));
        PageData<GroupPermission> page = groupPermissionService.findGroupPermissionsByTenantIdAndUserGroupId(
                tenantId, userGroup.getId(), new PageLink(10));
        Assert.assertEquals(1, page.getTotalElements());
    }

    @Test
    public void testFindByRoleId() {
        groupPermissionService.saveGroupPermission(
                createGroupPermission(userGroup.getId(), groupRole.getId(), entityGroup.getId()));
        PageData<GroupPermission> page = groupPermissionService.findGroupPermissionsByTenantIdAndRoleId(
                tenantId, groupRole.getId(), new PageLink(10));
        Assert.assertEquals(1, page.getTotalElements());
    }

    @Test
    public void testDuplicatePermission() {
        groupPermissionService.saveGroupPermission(
                createGroupPermission(userGroup.getId(), groupRole.getId(), entityGroup.getId()));
        Assertions.assertThrows(DataValidationException.class, () ->
                groupPermissionService.saveGroupPermission(
                        createGroupPermission(userGroup.getId(), groupRole.getId(), entityGroup.getId())));
    }

    @Test
    public void testSaveWithNonUserEntityGroup() {
        EntityGroup deviceAsUserGroup = new EntityGroup();
        deviceAsUserGroup.setTenantId(tenantId);
        deviceAsUserGroup.setOwnerId(tenantId);
        deviceAsUserGroup.setType(EntityType.DEVICE);
        deviceAsUserGroup.setName("Not a User Group");
        EntityGroup savedDeviceGroup = entityGroupService.saveEntityGroup(deviceAsUserGroup);

        GroupPermission gp = new GroupPermission();
        gp.setTenantId(tenantId);
        gp.setUserGroupId(savedDeviceGroup.getId());
        gp.setRoleId(groupRole.getId());
        gp.setEntityGroupId(entityGroup.getId());
        gp.setEntityGroupType(EntityType.DEVICE);
        Assertions.assertThrows(DataValidationException.class, () -> groupPermissionService.saveGroupPermission(gp));
    }

    @Test
    public void testSaveGroupRoleWithoutEntityGroup() {
        GroupPermission gp = new GroupPermission();
        gp.setTenantId(tenantId);
        gp.setUserGroupId(userGroup.getId());
        gp.setRoleId(groupRole.getId());
        Assertions.assertThrows(DataValidationException.class, () -> groupPermissionService.saveGroupPermission(gp));
    }
}
