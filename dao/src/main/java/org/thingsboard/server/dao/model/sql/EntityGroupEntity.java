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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupConfiguration;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "entity_group")
public final class EntityGroupEntity extends BaseVersionedEntity<EntityGroup> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "owner_type")
    private String ownerType;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "name")
    private String name;

    @Column(name = "group_all")
    private boolean groupAll;

    @Column(name = "public_group")
    private boolean publicGroup;

    @Convert(converter = JsonConverter.class)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private JsonNode configuration;

    @Convert(converter = JsonConverter.class)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    @Column(name = "external_id")
    private UUID externalId;

    public EntityGroupEntity() {
        super();
    }

    public EntityGroupEntity(EntityGroup entityGroup) {
        super(entityGroup);
        this.tenantId = entityGroup.getTenantId().getId();
        if (entityGroup.getOwnerId() != null) {
            this.ownerType = entityGroup.getOwnerId().getEntityType().name();
            this.ownerId = entityGroup.getOwnerId().getId();
        }
        this.entityType = entityGroup.getType() != null ? entityGroup.getType().name() : null;
        this.name = entityGroup.getName();
        this.groupAll = entityGroup.isGroupAll();
        this.publicGroup = entityGroup.isPublicGroup();
        this.configuration = toJson(entityGroup.getConfiguration());
        this.additionalInfo = entityGroup.getAdditionalInfo();
        if (entityGroup.getExternalId() != null) {
            this.externalId = entityGroup.getExternalId().getId();
        }
    }

    @Override
    public EntityGroup toData() {
        EntityGroup entityGroup = new EntityGroup(new EntityGroupId(this.getUuid()));
        entityGroup.setCreatedTime(createdTime);
        entityGroup.setVersion(version);
        entityGroup.setTenantId(TenantId.fromUUID(tenantId));
        if (ownerType != null && ownerId != null) {
            entityGroup.setOwnerId(EntityIdFactory.getByTypeAndUuid(ownerType, ownerId));
        }
        if (entityType != null) {
            entityGroup.setType(EntityType.valueOf(entityType));
        }
        entityGroup.setName(name);
        entityGroup.setGroupAll(groupAll);
        entityGroup.setPublicGroup(publicGroup);
        if (configuration != null) {
            entityGroup.setConfiguration(fromJson(configuration, EntityGroupConfiguration.class));
        }
        entityGroup.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            entityGroup.setExternalId(new EntityGroupId(externalId));
        }
        return entityGroup;
    }
}
