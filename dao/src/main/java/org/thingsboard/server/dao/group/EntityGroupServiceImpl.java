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
package org.thingsboard.server.dao.group;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.group.EntityGroupCacheEvictEvent;
import org.thingsboard.server.cache.group.EntityGroupCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("EntityGroupDaoService")
@Slf4j
public class EntityGroupServiceImpl extends AbstractCachedEntityService<EntityGroupCacheKey, EntityGroup, EntityGroupCacheEvictEvent>
        implements EntityGroupService {

    private static final String INCORRECT_ENTITY_GROUP_ID = "Incorrect entityGroupId ";
    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private EntityGroupDao entityGroupDao;

    @Autowired
    private DataValidator<EntityGroup> entityGroupValidator;

    @Autowired
    private EntityCountService countService;

    @Autowired
    @Lazy
    private GroupPermissionService groupPermissionService;

    @TransactionalEventListener(classes = EntityGroupCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(EntityGroupCacheEvictEvent event) {
        List<EntityGroupCacheKey> keys = new ArrayList<>(2);
        keys.add(new EntityGroupCacheKey(event.tenantId(), event.ownerId(), event.entityType(), event.newName()));
        if (StringUtils.isNotEmpty(event.oldName()) && !event.oldName().equals(event.newName())) {
            keys.add(new EntityGroupCacheKey(event.tenantId(), event.ownerId(), event.entityType(), event.oldName()));
        }
        cache.evict(keys);
    }

    @Override
    public EntityGroup findEntityGroupById(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupById [{}]", entityGroupId);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        return entityGroupDao.findById(tenantId, entityGroupId.getId());
    }

    @Override
    @Transactional
    public EntityGroup saveEntityGroup(EntityGroup entityGroup) {
        log.trace("Executing saveEntityGroup [{}]", entityGroup);
        EntityGroup oldGroup = entityGroup.getId() != null
                ? entityGroupDao.findById(entityGroup.getTenantId(), entityGroup.getId().getId())
                : null;
        entityGroupValidator.validate(entityGroup, EntityGroup::getTenantId);
        var evictEvent = new EntityGroupCacheEvictEvent(
                entityGroup.getTenantId(), entityGroup.getOwnerId(), entityGroup.getType(),
                entityGroup.getName(), oldGroup != null ? oldGroup.getName() : null);
        try {
            EntityGroup savedGroup = entityGroupDao.save(entityGroup.getTenantId(), entityGroup);
            if (entityGroup.getId() == null) {
                countService.publishCountEntityEvictEvent(savedGroup.getTenantId(), EntityType.ENTITY_GROUP);
            }
            publishEvictEvent(evictEvent);
            eventPublisher.publishEvent(SaveEntityEvent.builder()
                    .tenantId(savedGroup.getTenantId())
                    .entityId(savedGroup.getId())
                    .entity(savedGroup)
                    .oldEntity(oldGroup)
                    .created(entityGroup.getId() == null)
                    .build());
            return savedGroup;
        } catch (Exception e) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(e,
                    "entity_group_name_unq_key", "Entity group with such name already exists!",
                    "entity_group_external_id_unq_key", "Entity group with such external id already exists!");
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing deleteEntityGroup [{}]", entityGroupId);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        deleteEntity(tenantId, entityGroupId, false);
    }

    @Transactional
    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        EntityGroupId entityGroupId = (EntityGroupId) id;
        EntityGroup group = findEntityGroupById(tenantId, entityGroupId);
        if (group == null) {
            if (force) {
                return;
            }
            throw new IncorrectParameterException("Unable to delete non-existent entity group.");
        }
        if (group.isGroupAll()) {
            throw new DataValidationException("The 'All' group cannot be deleted!");
        }
        groupPermissionService.deleteGroupPermissionsByTenantIdAndEntityGroupId(tenantId, entityGroupId);
        if (group.getType() == EntityType.USER) {
            groupPermissionService.deleteGroupPermissionsByTenantIdAndUserGroupId(tenantId, entityGroupId);
        }
        entityGroupDao.removeById(tenantId, entityGroupId.getId());
        publishEvictEvent(new EntityGroupCacheEvictEvent(tenantId, group.getOwnerId(), group.getType(), group.getName(), null));
        countService.publishCountEntityEvictEvent(tenantId, EntityType.ENTITY_GROUP);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(entityGroupId).build());
    }

    @Override
    public PageData<EntityGroup> findEntityGroupsByOwnerAndType(TenantId tenantId, EntityId ownerId, EntityType entityType, PageLink pageLink) {
        log.trace("Executing findEntityGroupsByOwnerAndType tenantId={}, ownerId={}, type={}", tenantId, ownerId, entityType);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return entityGroupDao.findByOwnerAndType(tenantId.getId(), ownerId.getId(), entityType.name(), pageLink);
    }

    @Override
    public List<EntityGroup> findEntityGroupsByEntityId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing findEntityGroupsByEntityId entityId={}", entityId);
        return entityGroupDao.findGroupsByEntityId(tenantId.getId(), entityId.getId(), entityId.getEntityType().name());
    }

    @Override
    @Transactional
    public EntityGroup findOrCreateAllGroup(TenantId tenantId, EntityId ownerId, EntityType entityType) {
        EntityGroup existing = entityGroupDao.findAllGroupByOwnerAndType(ownerId.getId(), entityType.name());
        if (existing != null) {
            return existing;
        }
        EntityGroup allGroup = new EntityGroup();
        allGroup.setTenantId(tenantId);
        allGroup.setOwnerId(ownerId);
        allGroup.setType(entityType);
        allGroup.setName("All " + entityType.getNormalName() + "s");
        allGroup.setGroupAll(true);
        try {
            return saveEntityGroup(allGroup);
        } catch (DataIntegrityViolationException e) {
            EntityGroup race = entityGroupDao.findAllGroupByOwnerAndType(ownerId.getId(), entityType.name());
            if (race != null) {
                return race;
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public void addEntityToGroup(EntityGroupId groupId, EntityId entityId) {
        entityGroupDao.addEntityToGroup(groupId.getId(), entityId.getId(),
                entityId.getEntityType().name(), System.currentTimeMillis());
    }

    @Override
    @Transactional
    public void removeEntityFromGroup(EntityGroupId groupId, EntityId entityId) {
        entityGroupDao.removeEntityFromGroup(groupId.getId(), entityId.getId());
    }

    @Override
    @Transactional
    public void removeEntityFromAllGroups(TenantId tenantId, EntityId entityId) {
        entityGroupDao.removeEntityFromAllGroups(tenantId.getId(), entityId.getId(),
                entityId.getEntityType().name());
    }

    @Override
    public void deleteEntityGroupsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEntityGroupsByTenantId tenantId={}", tenantId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        entityGroupsByTenantRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public boolean isEntityInGroup(EntityGroupId groupId, EntityId entityId) {
        return entityGroupDao.isEntityInGroup(groupId.getId(), entityId.getId());
    }

    @Override
    @Transactional
    public EntityGroup makePublic(TenantId tenantId, EntityGroupId entityGroupId) {
        EntityGroup group = findEntityGroupById(tenantId, entityGroupId);
        if (group == null) {
            throw new IncorrectParameterException("Entity group not found.");
        }
        group.setPublicGroup(true);
        return saveEntityGroup(group);
    }

    @Override
    @Transactional
    public EntityGroup makePrivate(TenantId tenantId, EntityGroupId entityGroupId) {
        EntityGroup group = findEntityGroupById(tenantId, entityGroupId);
        if (group == null) {
            throw new IncorrectParameterException("Entity group not found.");
        }
        group.setPublicGroup(false);
        return saveEntityGroup(group);
    }

    @Override
    @Transactional
    public EntityGroup changeOwner(TenantId tenantId, EntityGroupId entityGroupId, EntityId newOwnerId) {
        EntityGroup group = findEntityGroupById(tenantId, entityGroupId);
        if (group == null) {
            throw new IncorrectParameterException("Entity group not found.");
        }
        if (group.isGroupAll()) {
            throw new DataValidationException("Owner of the 'All' group cannot be changed!");
        }
        group.setOwnerId(newOwnerId);
        return saveEntityGroup(group);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteEntityGroupsByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findEntityGroupById(tenantId, new EntityGroupId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(Futures.immediateFuture(findEntity(tenantId, entityId)));
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return 0L;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_GROUP;
    }

    private final PaginatedRemover<TenantId, EntityGroup> entityGroupsByTenantRemover =
            new PaginatedRemover<>() {
                @Override
                protected PageData<EntityGroup> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return entityGroupDao.findByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, EntityGroup entity) {
                    if (!entity.isGroupAll()) {
                        deleteEntityGroup(tenantId, entity.getId());
                    } else {
                        entityGroupDao.removeById(tenantId, entity.getId().getId());
                    }
                }
            };
}
