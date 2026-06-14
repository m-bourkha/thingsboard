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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RolePermissions;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class GroupPermissionControllerTest extends AbstractControllerTest {

    static final TypeReference<PageData<GroupPermissionInfo>> PAGE_DATA_GP_INFO_TYPE_REF = new TypeReference<>() {};

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("GP Test Tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("gp_test@thingsboard.org");
        tenantAdmin.setFirstName("GP");
        tenantAdmin.setLastName("Test");
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteTenant(savedTenant.getId());
    }

    private Role createGroupRole(String name) throws Exception {
        Role role = new Role();
        role.setName(name);
        role.setType(RoleType.GROUP);
        RolePermissions permissions = new RolePermissions();
        permissions.setOperations(List.of("ALL"));
        role.setPermissions(permissions);
        return doPost("/api/role", role, Role.class);
    }

    private EntityGroup createUserGroup(String name) throws Exception {
        EntityGroup eg = new EntityGroup();
        eg.setName(name);
        eg.setType(EntityType.USER);
        return doPost("/api/entityGroup", eg, EntityGroup.class);
    }

    private EntityGroup createDeviceGroup(String name) throws Exception {
        EntityGroup eg = new EntityGroup();
        eg.setName(name);
        eg.setType(EntityType.DEVICE);
        return doPost("/api/entityGroup", eg, EntityGroup.class);
    }

    private GroupPermission buildPermission(EntityGroupId userGroupId, RoleId roleId, EntityGroupId entityGroupId) {
        GroupPermission gp = new GroupPermission();
        gp.setUserGroupId(userGroupId);
        gp.setRoleId(roleId);
        gp.setEntityGroupId(entityGroupId);
        gp.setEntityGroupType(EntityType.DEVICE);
        gp.setPublicPermission(false);
        return gp;
    }

    @Test
    public void testSaveGroupPermission() throws Exception {
        Role role = createGroupRole("SaveTestRole");
        EntityGroup userGroup = createUserGroup("SaveTestUserGroup");
        EntityGroup entityGroup = createDeviceGroup("SaveTestEntityGroup");

        reset(tbClusterService, auditLogService);

        GroupPermission gp = buildPermission(userGroup.getId(), role.getId(), entityGroup.getId());
        GroupPermission saved = doPost("/api/groupPermission", gp, GroupPermission.class);

        testNotifyEntityAllOneTime(saved, saved.getId(), saved.getId(), saved.getTenantId(),
                new GroupPermissionId(GroupPermissionId.NULL_UUID), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        Assert.assertNotNull(saved);
        Assert.assertNotNull(saved.getId());
        Assert.assertTrue(saved.getCreatedTime() > 0);
    }

    @Test
    public void testFindGroupPermissionById() throws Exception {
        Role role = createGroupRole("FindTestRole");
        EntityGroup userGroup = createUserGroup("FindTestUserGroup");
        EntityGroup entityGroup = createDeviceGroup("FindTestEntityGroup");

        GroupPermission saved = doPost("/api/groupPermission",
                buildPermission(userGroup.getId(), role.getId(), entityGroup.getId()),
                GroupPermission.class);
        GroupPermission found = doGet("/api/groupPermission/" + saved.getId().getId(), GroupPermission.class);
        Assert.assertNotNull(found);
        Assert.assertEquals(saved.getId(), found.getId());
    }

    @Test
    public void testDeleteGroupPermission() throws Exception {
        Role role = createGroupRole("DeleteTestRole");
        EntityGroup userGroup = createUserGroup("DeleteTestUserGroup");
        EntityGroup entityGroup = createDeviceGroup("DeleteTestEntityGroup");

        GroupPermission saved = doPost("/api/groupPermission",
                buildPermission(userGroup.getId(), role.getId(), entityGroup.getId()),
                GroupPermission.class);

        doDelete("/api/groupPermission/" + saved.getId().getId()).andExpect(status().isOk());
        doGet("/api/groupPermission/" + saved.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testGetGroupPermissionsByEntityGroup() throws Exception {
        Role role = createGroupRole("EgListTestRole");
        EntityGroup userGroup = createUserGroup("EgListTestUserGroup");
        EntityGroup entityGroup = createDeviceGroup("EgListTestEntityGroup");

        doPost("/api/groupPermission", buildPermission(userGroup.getId(), role.getId(), entityGroup.getId()),
                GroupPermission.class);

        PageData<GroupPermissionInfo> page = doGetTypedWithPageLink(
                "/api/entityGroup/" + entityGroup.getId().getId() + "/groupPermissions?",
                PAGE_DATA_GP_INFO_TYPE_REF,
                new org.thingsboard.server.common.data.page.PageLink(10));

        Assert.assertEquals(1, page.getTotalElements());
        GroupPermissionInfo info = page.getData().get(0);
        Assert.assertEquals(role.getName(), info.getRoleName());
        Assert.assertEquals(userGroup.getName(), info.getUserGroupName());
    }

    @Test
    public void testSaveGroupPermissionWithInvalidUserGroup() throws Exception {
        Role role = createGroupRole("ValidationTestRole");
        EntityGroup deviceGroupAsUser = createDeviceGroup("NotAUserGroup");
        EntityGroup entityGroup = createDeviceGroup("ValidationEntityGroup");

        GroupPermission gp = buildPermission(deviceGroupAsUser.getId(), role.getId(), entityGroup.getId());
        doPost("/api/groupPermission", gp).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("USER")));
    }
}
