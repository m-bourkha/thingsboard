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

import com.google.common.util.concurrent.FluentFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.role.RoleCacheEvictEvent;
import org.thingsboard.server.cache.role.RoleCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("RoleDaoService")
@Slf4j
public class RoleServiceImpl extends AbstractCachedEntityService<RoleCacheKey, Role, RoleCacheEvictEvent> implements RoleService {

    public static final String INCORRECT_ROLE_ID = "Incorrect roleId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String ROLE_UNIQUE_NAME_EX_MSG = "Role with such name already exists!";

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private DataValidator<Role> roleValidator;

    @TransactionalEventListener(classes = RoleCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(RoleCacheEvictEvent event) {
        List<RoleCacheKey> keys = new ArrayList<>(2);
        keys.add(new RoleCacheKey(event.tenantId(), event.newName()));
        if (StringUtils.isNotEmpty(event.oldName()) && !event.oldName().equals(event.newName())) {
            keys.add(new RoleCacheKey(event.tenantId(), event.oldName()));
        }
        cache.evict(keys);
    }

    @Override
    public Role findRoleById(TenantId tenantId, RoleId roleId) {
        log.trace("Executing findRoleById [{}]", roleId);
        Validator.validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        return roleDao.findById(tenantId, roleId.getId());
    }

    @Override
    public Role findRoleByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findRoleByTenantIdAndName [{}] [{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return cache.getAndPutInTransaction(new RoleCacheKey(tenantId, name),
                () -> roleDao.findRoleByTenantIdAndName(tenantId.getId(), name), true);
    }

    @Override
    @Transactional
    public Role saveRole(Role role) {
        return saveEntity(role, () -> doSaveRole(role));
    }

    private Role doSaveRole(Role role) {
        log.trace("Executing saveRole [{}]", role);
        Role oldRole = (role.getId() != null) ? roleDao.findById(role.getTenantId(), role.getId().getId()) : null;
        roleValidator.validate(role, Role::getTenantId);
        var evictEvent = new RoleCacheEvictEvent(role.getTenantId(), role.getName(), oldRole != null ? oldRole.getName() : null);
        try {
            Role savedRole = roleDao.saveAndFlush(role.getTenantId(), role);
            publishEvictEvent(evictEvent);
            eventPublisher.publishEvent(SaveEntityEvent.builder()
                    .tenantId(savedRole.getTenantId())
                    .entityId(savedRole.getId())
                    .entity(savedRole)
                    .oldEntity(oldRole)
                    .created(role.getId() == null)
                    .build());
            return savedRole;
        } catch (Exception e) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(e,
                    "tb_role_name_unq_key", ROLE_UNIQUE_NAME_EX_MSG,
                    "tb_role_external_id_unq_key", "Role with such external id already exists!");
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteRole(TenantId tenantId, RoleId roleId) {
        log.trace("Executing deleteRole [{}]", roleId);
        Validator.validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        deleteEntity(tenantId, roleId, false);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        RoleId roleId = (RoleId) id;
        Role role = findRoleById(tenantId, roleId);
        if (role == null) {
            if (force) {
                return;
            } else {
                throw new IncorrectParameterException("Unable to delete non-existent role.");
            }
        }
        roleDao.removeById(tenantId, roleId.getId());
        publishEvictEvent(new RoleCacheEvictEvent(role.getTenantId(), role.getName(), null));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(roleId).entity(role).build());
    }

    @Override
    public PageData<Role> findRolesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findRolesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return roleDao.findRolesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteRolesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteRolesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        rolesByTenantRemover.removeEntities(tenantId, tenantId);
    }

    private final PaginatedRemover<TenantId, Role> rolesByTenantRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Role> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return roleDao.findRolesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Role entity) {
                    deleteRole(tenantId, new RoleId(entity.getUuidId()));
                }
            };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findRoleById(tenantId, new RoleId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(roleDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return roleDao.countByTenantId(tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteRolesByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }

}
