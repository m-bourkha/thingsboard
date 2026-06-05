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
package org.thingsboard.server.common.data.role;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseData<RoleId>
        implements HasTenantId, HasName, HasVersion, ExportableEntity<RoleId> {

    private static final long serialVersionUID = 2886781786406022912L;

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "name")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Name of the role")
    private String name;

    @Schema(description = "Role type: GENERIC (tenant-wide) or GROUP (scoped to entity group)")
    private RoleType type;

    private transient JsonNode permissions;
    @JsonIgnore
    private byte[] permissionsBytes;

    @Getter @Setter
    private Long version;

    @Getter @Setter
    private RoleId externalId;

    public Role() {
        super();
    }

    public Role(RoleId id) {
        super(id);
    }

    public Role(Role role) {
        super(role);
        this.tenantId = role.getTenantId();
        this.name = role.getName();
        this.type = role.getType();
        setPermissions(role.getPermissions());
        this.version = role.getVersion();
        this.externalId = role.getExternalId();
    }

    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RoleType getType() {
        return type;
    }

    public void setType(RoleType type) {
        this.type = type;
    }

    public JsonNode getPermissions() {
        if (permissions != null) {
            return permissions;
        }
        if (permissionsBytes != null) {
            try {
                return mapper.readTree(new ByteArrayInputStream(permissionsBytes));
            } catch (IOException e) {
                log.warn("Can't deserialize permissions", e);
            }
        }
        return null;
    }

    public void setPermissions(JsonNode permissions) {
        this.permissions = permissions;
        if (permissions != null) {
            try {
                this.permissionsBytes = mapper.writeValueAsBytes(permissions);
            } catch (Exception e) {
                log.warn("Can't serialize permissions", e);
            }
        } else {
            this.permissionsBytes = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Role role = (Role) o;
        return Objects.equals(tenantId, role.tenantId)
                && Objects.equals(name, role.name)
                && type == role.type
                && Arrays.equals(permissionsBytes, role.permissionsBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tenantId, name, type, Arrays.hashCode(permissionsBytes));
    }
}
