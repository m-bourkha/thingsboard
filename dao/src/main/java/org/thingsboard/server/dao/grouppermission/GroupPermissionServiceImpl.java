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

import com.google.common.util.concurrent.FluentFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.grouppermission.GroupPermissionCacheEvictEvent;
import org.thingsboard.server.cache.grouppermission.GroupPermissionCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
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

@Service("GroupPermissionDaoService")
@Slf4j
public class GroupPermissionServiceImpl extends AbstractCachedEntityService<GroupPermissionCacheKey, GroupPermission, GroupPermissionCacheEvictEvent> implements GroupPermissionService {

    public static final String INCORRECT_GROUP_PERMISSION_ID = "Incorrect groupPermissionId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_USER_GROUP_ID = "Incorrect userGroupId ";
    public static final String INCORRECT_ENTITY_GROUP_ID = "Incorrect entityGroupId ";
    public static final String INCORRECT_ROLE_ID = "Incorrect roleId ";

    @Autowired
    private GroupPermissionDao groupPermissionDao;

    @Autowired
    private DataValidator<GroupPermission> groupPermissionValidator;

    @TransactionalEventListener(classes = GroupPermissionCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(GroupPermissionCacheEvictEvent event) {
        List<GroupPermissionCacheKey> keys = new ArrayList<>(2);
        if (event.newUserGroupId() != null) {
            keys.add(new GroupPermissionCacheKey(event.tenantId(), event.newUserGroupId(), event.newEntityGroupId()));
        }
        if (event.oldUserGroupId() != null
                && (!event.oldUserGroupId().equals(event.newUserGroupId())
                || (event.oldEntityGroupId() != null ? !event.oldEntityGroupId().equals(event.newEntityGroupId()) : event.newEntityGroupId() != null))) {
            keys.add(new GroupPermissionCacheKey(event.tenantId(), event.oldUserGroupId(), event.oldEntityGroupId()));
        }
        if (!keys.isEmpty()) {
            cache.evict(keys);
        }
    }

    @Override
    public GroupPermission findGroupPermissionById(TenantId tenantId, GroupPermissionId groupPermissionId) {
        log.trace("Executing findGroupPermissionById [{}]", groupPermissionId);
        Validator.validateId(groupPermissionId, id -> INCORRECT_GROUP_PERMISSION_ID + id);
        return groupPermissionDao.findById(tenantId, groupPermissionId.getId());
    }

    @Override
    @Transactional
    public GroupPermission saveGroupPermission(GroupPermission groupPermission) {
        return saveEntity(groupPermission, () -> doSaveGroupPermission(groupPermission));
    }

    private GroupPermission doSaveGroupPermission(GroupPermission groupPermission) {
        log.trace("Executing saveGroupPermission [{}]", groupPermission);
        GroupPermission oldGroupPermission = (groupPermission.getId() != null)
                ? groupPermissionDao.findById(groupPermission.getTenantId(), groupPermission.getId().getId()) : null;
        groupPermissionValidator.validate(groupPermission, GroupPermission::getTenantId);
        var evictEvent = new GroupPermissionCacheEvictEvent(groupPermission.getTenantId(),
                groupPermission.getUserGroupId(), oldGroupPermission != null ? oldGroupPermission.getUserGroupId() : null,
                groupPermission.getEntityGroupId(), oldGroupPermission != null ? oldGroupPermission.getEntityGroupId() : null);
        try {
            GroupPermission savedGroupPermission = groupPermissionDao.saveAndFlush(groupPermission.getTenantId(), groupPermission);
            publishEvictEvent(evictEvent);
            eventPublisher.publishEvent(SaveEntityEvent.builder()
                    .tenantId(savedGroupPermission.getTenantId())
                    .entityId(savedGroupPermission.getId())
                    .entity(savedGroupPermission)
                    .oldEntity(oldGroupPermission)
                    .created(groupPermission.getId() == null)
                    .build());
            return savedGroupPermission;
        } catch (Exception e) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(e,
                    "group_permission_external_id_unq_key", "Group permission with such external id already exists!");
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteGroupPermission(TenantId tenantId, GroupPermissionId groupPermissionId) {
        log.trace("Executing deleteGroupPermission [{}]", groupPermissionId);
        Validator.validateId(groupPermissionId, id -> INCORRECT_GROUP_PERMISSION_ID + id);
        deleteEntity(tenantId, groupPermissionId, false);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        GroupPermissionId groupPermissionId = (GroupPermissionId) id;
        GroupPermission groupPermission = findGroupPermissionById(tenantId, groupPermissionId);
        if (groupPermission == null) {
            if (force) {
                return;
            } else {
                throw new IncorrectParameterException("Unable to delete non-existent group permission.");
            }
        }
        groupPermissionDao.removeById(tenantId, groupPermissionId.getId());
        publishEvictEvent(new GroupPermissionCacheEvictEvent(tenantId,
                groupPermission.getUserGroupId(), null, groupPermission.getEntityGroupId(), null));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(groupPermissionId).entity(groupPermission).build());
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionsByTenantIdAndUserGroupId, tenantId [{}], userGroupId [{}]", tenantId, userGroupId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(userGroupId, id -> INCORRECT_USER_GROUP_ID + id);
        Validator.validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId(), pageLink);
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionsByTenantIdAndEntityGroupId, tenantId [{}], entityGroupId [{}]", tenantId, entityGroupId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        Validator.validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId(), pageLink);
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndRoleId(TenantId tenantId, RoleId roleId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionsByTenantIdAndRoleId, tenantId [{}], roleId [{}]", tenantId, roleId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        Validator.validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndRoleId(tenantId.getId(), roleId.getId(), pageLink);
    }

    @Override
    public void deleteGroupPermissionsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteGroupPermissionsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        groupPermissionsByTenantRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteGroupPermissionsByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId) {
        log.trace("Executing deleteGroupPermissionsByTenantIdAndUserGroupId, tenantId [{}], userGroupId [{}]", tenantId, userGroupId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(userGroupId, id -> INCORRECT_USER_GROUP_ID + id);
        groupPermissionsByUserGroupRemover.removeEntities(tenantId, userGroupId);
    }

    @Override
    public void deleteGroupPermissionsByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing deleteGroupPermissionsByTenantIdAndEntityGroupId, tenantId [{}], entityGroupId [{}]", tenantId, entityGroupId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        groupPermissionsByEntityGroupRemover.removeEntities(tenantId, entityGroupId);
    }

    @Override
    public void deleteGroupPermissionsByTenantIdAndRoleId(TenantId tenantId, RoleId roleId) {
        log.trace("Executing deleteGroupPermissionsByTenantIdAndRoleId, tenantId [{}], roleId [{}]", tenantId, roleId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        groupPermissionsByRoleRemover.removeEntities(tenantId, roleId);
    }

    private final PaginatedRemover<TenantId, GroupPermission> groupPermissionsByTenantRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<GroupPermission> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return groupPermissionDao.findGroupPermissionsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, GroupPermission entity) {
                    deleteGroupPermission(tenantId, new GroupPermissionId(entity.getUuidId()));
                }
            };

    private final PaginatedRemover<EntityGroupId, GroupPermission> groupPermissionsByUserGroupRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<GroupPermission> findEntities(TenantId tenantId, EntityGroupId id, PageLink pageLink) {
                    return groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, GroupPermission entity) {
                    deleteGroupPermission(tenantId, new GroupPermissionId(entity.getUuidId()));
                }
            };

    private final PaginatedRemover<EntityGroupId, GroupPermission> groupPermissionsByEntityGroupRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<GroupPermission> findEntities(TenantId tenantId, EntityGroupId id, PageLink pageLink) {
                    return groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupId(tenantId.getId(), id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, GroupPermission entity) {
                    deleteGroupPermission(tenantId, new GroupPermissionId(entity.getUuidId()));
                }
            };

    private final PaginatedRemover<RoleId, GroupPermission> groupPermissionsByRoleRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<GroupPermission> findEntities(TenantId tenantId, RoleId id, PageLink pageLink) {
                    return groupPermissionDao.findGroupPermissionsByTenantIdAndRoleId(tenantId.getId(), id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, GroupPermission entity) {
                    deleteGroupPermission(tenantId, new GroupPermissionId(entity.getUuidId()));
                }
            };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findGroupPermissionById(tenantId, new GroupPermissionId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(groupPermissionDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return groupPermissionDao.countByTenantId(tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteGroupPermissionsByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.GROUP_PERMISSION;
    }

}
