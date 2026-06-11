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
import org.mockito.Mockito;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RoleControllerTest extends AbstractControllerTest {

    static final TypeReference<PageData<Role>> PAGE_DATA_ROLE_TYPE_REFERENCE = new TypeReference<>() {
    };

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteTenant(savedTenant.getId());
    }

    private Role createRole(String name, RoleType type) {
        Role role = new Role();
        role.setName(name);
        role.setType(type);
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));
        return role;
    }

    @Test
    public void testSaveRole() throws Exception {
        Role role = createRole("My role", RoleType.GENERIC);

        Mockito.reset(tbClusterService, auditLogService);

        Role savedRole = doPost("/api/role", role, Role.class);

        testNotifyEntityAllOneTime(savedRole, savedRole.getId(), savedRole.getId(), savedRole.getTenantId(),
                new RoleId(RoleId.NULL_UUID), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        Assert.assertNotNull(savedRole);
        Assert.assertNotNull(savedRole.getId());
        Assert.assertTrue(savedRole.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedRole.getTenantId());
        Assert.assertEquals(role.getName(), savedRole.getName());
        Assert.assertEquals(RoleType.GENERIC, savedRole.getType());

        savedRole.setName("My new role");
        doPost("/api/role", savedRole, Role.class);

        Role foundRole = doGet("/api/role/" + savedRole.getId().getId(), Role.class);
        Assert.assertEquals(foundRole.getName(), savedRole.getName());
    }

    @Test
    public void testFindRoleById() throws Exception {
        Role savedRole = doPost("/api/role", createRole("My role", RoleType.GROUP), Role.class);
        Role foundRole = doGet("/api/role/" + savedRole.getId().getId(), Role.class);
        Assert.assertNotNull(foundRole);
        Assert.assertEquals(savedRole, foundRole);
    }

    @Test
    public void testDeleteRole() throws Exception {
        Role savedRole = doPost("/api/role", createRole("My role", RoleType.GENERIC), Role.class);

        doDelete("/api/role/" + savedRole.getId().getId())
                .andExpect(status().isOk());

        doGet("/api/role/" + savedRole.getId().getId())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveRoleWithEmptyName() throws Exception {
        Role role = new Role();
        role.setType(RoleType.GENERIC);
        doPost("/api/role", role)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Role name " + msgErrorShouldBeSpecified)));
    }

    @Test
    public void testFindRoles() throws Exception {
        List<Role> roles = new ArrayList<>();
        int cntEntity = 56;
        for (int i = 0; i < cntEntity; i++) {
            Role role = createRole("Role" + i, i % 2 == 0 ? RoleType.GENERIC : RoleType.GROUP);
            roles.add(doPost("/api/role", role, Role.class));
        }

        List<Role> loadedRoles = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Role> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/roles?", PAGE_DATA_ROLE_TYPE_REFERENCE, pageLink);
            loadedRoles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(loadedRoles).containsExactlyInAnyOrderElementsOf(roles);
    }

}
