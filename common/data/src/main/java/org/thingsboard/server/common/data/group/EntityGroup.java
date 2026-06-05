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
package org.thingsboard.server.common.data.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@EqualsAndHashCode(callSuper = true)
public class EntityGroup extends BaseDataWithAdditionalInfo<EntityGroupId>
        implements HasTenantId, HasName, HasVersion, ExportableEntity<EntityGroupId> {

    private static final long serialVersionUID = -3530642684550958285L;

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;

    @Schema(description = "JSON object with the owner entity Id (Tenant or Customer)")
    private EntityId ownerId;

    @Schema(description = "Entity type grouped by this group (DEVICE, ASSET, CUSTOMER)")
    private EntityType type;

    @NoXss
    @Length(fieldName = "name")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Name of the entity group")
    private String name;

    @Schema(description = "True if this is the system-managed 'All' group for the owner and entity type")
    private boolean groupAll;

    @Schema(description = "True if this group is publicly shared")
    private boolean publicGroup;

    @Schema(description = "Group display and behaviour configuration")
    private EntityGroupConfiguration configuration;

    @Getter @Setter
    private Long version;

    @Getter @Setter
    private EntityGroupId externalId;

    public EntityGroup() {
        super();
    }

    public EntityGroup(EntityGroupId id) {
        super(id);
    }

    public EntityGroup(EntityGroup entityGroup) {
        super(entityGroup);
        this.tenantId = entityGroup.getTenantId();
        this.ownerId = entityGroup.getOwnerId();
        this.type = entityGroup.getType();
        this.name = entityGroup.getName();
        this.groupAll = entityGroup.isGroupAll();
        this.publicGroup = entityGroup.isPublicGroup();
        this.configuration = entityGroup.getConfiguration();
        this.version = entityGroup.getVersion();
        this.externalId = entityGroup.getExternalId();
    }

    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    public EntityId getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(EntityId ownerId) {
        this.ownerId = ownerId;
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isGroupAll() {
        return groupAll;
    }

    public void setGroupAll(boolean groupAll) {
        this.groupAll = groupAll;
    }

    public boolean isPublicGroup() {
        return publicGroup;
    }

    public void setPublicGroup(boolean publicGroup) {
        this.publicGroup = publicGroup;
    }

    public EntityGroupConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(EntityGroupConfiguration configuration) {
        this.configuration = configuration;
    }
}
