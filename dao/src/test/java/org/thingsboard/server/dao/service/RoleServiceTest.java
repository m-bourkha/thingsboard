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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DaoSqlTest
public class RoleServiceTest extends AbstractServiceTest {

    @Autowired
    RoleService roleService;

    static final int TIMEOUT = 30;

    ListeningExecutorService executor;

    @Before
    public void before() {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
    }

    @After
    public void after() {
        executor.shutdownNow();
        roleService.deleteRolesByTenantId(tenantId);
    }

    private Role createRole(String name, RoleType type) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName(name);
        role.setType(type);
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));
        return role;
    }

    @Test
    public void testSaveRole() {
        Role role = createRole("My role", RoleType.GENERIC);
        Role savedRole = roleService.saveRole(role);

        Assert.assertNotNull(savedRole);
        Assert.assertNotNull(savedRole.getId());
        Assert.assertTrue(savedRole.getCreatedTime() > 0);
        Assert.assertEquals(role.getTenantId(), savedRole.getTenantId());
        Assert.assertEquals(role.getName(), savedRole.getName());
        Assert.assertEquals(RoleType.GENERIC, savedRole.getType());

        savedRole.setName("My new role");
        roleService.saveRole(savedRole);
        Role foundRole = roleService.findRoleById(tenantId, savedRole.getId());
        Assert.assertEquals(foundRole.getName(), savedRole.getName());
    }

    @Test
    public void testFindRoleById() {
        Role savedRole = roleService.saveRole(createRole("My role", RoleType.GROUP));
        Role foundRole = roleService.findRoleById(tenantId, savedRole.getId());
        Assert.assertNotNull(foundRole);
        Assert.assertEquals(savedRole, foundRole);
    }

    @Test
    public void testFindRoleByTenantIdAndName() {
        Role savedRole = roleService.saveRole(createRole("My role", RoleType.GENERIC));
        Role foundRole = roleService.findRoleByTenantIdAndName(tenantId, "My role");
        Assert.assertNotNull(foundRole);
        Assert.assertEquals(savedRole.getId(), foundRole.getId());
    }

    @Test
    public void testSaveRoleWithEmptyName() {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setType(RoleType.GENERIC);
        Assertions.assertThrows(DataValidationException.class, () -> roleService.saveRole(role));
    }

    @Test
    public void testSaveRoleWithEmptyType() {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName("My role");
        Assertions.assertThrows(DataValidationException.class, () -> roleService.saveRole(role));
    }

    @Test
    public void testSaveRoleWithEmptyTenant() {
        Role role = new Role();
        role.setName("My role");
        role.setType(RoleType.GENERIC);
        Assertions.assertThrows(DataValidationException.class, () -> roleService.saveRole(role));
    }

    @Test
    public void testSaveRoleWithInvalidTenant() {
        Role role = createRole("My role", RoleType.GENERIC);
        role.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> roleService.saveRole(role));
    }

    @Test
    public void testSaveRoleWithExistingName() {
        roleService.saveRole(createRole("My role", RoleType.GENERIC));
        assertThatThrownBy(() -> roleService.saveRole(createRole("My role", RoleType.GENERIC)))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Role with such name already exists!");
    }

    @Test
    public void testDeleteRole() {
        Role savedRole = roleService.saveRole(createRole("My role", RoleType.GENERIC));
        roleService.deleteRole(tenantId, savedRole.getId());
        Role foundRole = roleService.findRoleById(tenantId, savedRole.getId());
        Assert.assertNull(foundRole);
    }

    @Test
    public void testFindRolesByTenantId() throws Exception {
        List<ListenableFuture<Role>> futures = new ArrayList<>(135);
        for (int i = 0; i < 135; i++) {
            Role role = createRole("Role" + i, i % 2 == 0 ? RoleType.GENERIC : RoleType.GROUP);
            futures.add(executor.submit(() -> roleService.saveRole(role)));
        }
        List<Role> roles = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Role> loadedRoles = new ArrayList<>(135);
        PageLink pageLink = new PageLink(23);
        PageData<Role> pageData;
        do {
            pageData = roleService.findRolesByTenantId(tenantId, pageLink);
            loadedRoles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(roles).containsExactlyInAnyOrderElementsOf(loadedRoles);

        roleService.deleteRolesByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = roleService.findRolesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

}
