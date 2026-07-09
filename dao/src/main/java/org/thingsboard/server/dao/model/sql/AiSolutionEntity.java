/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.proxy.HibernateProxy;
import org.thingsboard.server.common.data.ai.solution.AiSolution;
import org.thingsboard.server.common.data.ai.solution.AiSolutionSpec;
import org.thingsboard.server.common.data.ai.solution.AiSolutionStatus;
import org.thingsboard.server.common.data.ai.solution.InstalledEntity;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.ModelConstants;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@Entity
@Table(name = ModelConstants.AI_SOLUTION_TABLE_NAME)
public class AiSolutionEntity implements Serializable {

    @Id
    @Column(name = ModelConstants.ID_PROPERTY, columnDefinition = "UUID")
    private UUID id;

    @Column(name = ModelConstants.CREATED_TIME_PROPERTY, nullable = false)
    private long createdTime;

    @Column(name = ModelConstants.AI_SOLUTION_TENANT_ID_COLUMN_NAME, nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(name = ModelConstants.VERSION_COLUMN)
    private Long version;

    @Column(name = ModelConstants.AI_SOLUTION_NAME_COLUMN_NAME, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AI_SOLUTION_STATUS_COLUMN_NAME, nullable = false)
    private AiSolutionStatus status;

    @Column(name = ModelConstants.AI_SOLUTION_ORIGINAL_PROMPT_COLUMN_NAME)
    private String originalPrompt;

    @Type(JsonBinaryType.class)
    @Column(name = ModelConstants.AI_SOLUTION_SPEC_COLUMN_NAME, columnDefinition = "JSONB")
    private AiSolutionSpec spec;

    @Type(JsonBinaryType.class)
    @Column(name = ModelConstants.AI_SOLUTION_INSTALLED_ENTITIES_COLUMN_NAME, columnDefinition = "JSONB")
    private List<InstalledEntity> installedEntities;

    public AiSolutionEntity() {
    }

    public AiSolutionEntity(AiSolution solution) {
        this.id = solution.getId();
        this.createdTime = solution.getCreatedTime();
        this.tenantId = solution.getTenantId() != null ? solution.getTenantId().getId() : null;
        this.version = solution.getVersion();
        this.name = solution.getName();
        this.status = solution.getStatus();
        this.originalPrompt = solution.getOriginalPrompt();
        this.spec = solution.getSpec();
        this.installedEntities = solution.getInstalledEntities();
    }

    public AiSolution toData() {
        AiSolution solution = new AiSolution();
        solution.setId(id);
        solution.setCreatedTime(createdTime);
        solution.setTenantId(tenantId != null ? TenantId.fromUUID(tenantId) : null);
        solution.setVersion(version);
        solution.setName(name);
        solution.setStatus(status);
        solution.setOriginalPrompt(originalPrompt);
        solution.setSpec(spec);
        solution.setInstalledEntities(installedEntities);
        return solution;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AiSolutionEntity that = (AiSolutionEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

}
