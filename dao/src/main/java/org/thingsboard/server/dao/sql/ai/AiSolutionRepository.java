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
package org.thingsboard.server.dao.sql.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.ai.solution.AiSolutionStatus;
import org.thingsboard.server.dao.model.sql.AiSolutionEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiSolutionRepository extends JpaRepository<AiSolutionEntity, UUID> {

    Optional<AiSolutionEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<AiSolutionEntity> findByStatus(AiSolutionStatus status);

    @Query(
            value = """
                    SELECT *
                    FROM ai_solution s
                    WHERE s.tenant_id = :tenantId
                      AND (:textSearch IS NULL OR s.name ILIKE '%' || :textSearch || '%')
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM ai_solution s
                    WHERE s.tenant_id = :tenantId
                      AND (:textSearch IS NULL OR s.name ILIKE '%' || :textSearch || '%')
                    """,
            nativeQuery = true
    )
    Page<AiSolutionEntity> findByTenantId(@Param("tenantId") UUID tenantId, @Param("textSearch") String textSearch, Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM AiSolutionEntity s WHERE s.tenantId = :tenantId AND s.id = :id")
    int deleteByTenantIdAndId(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    @Transactional
    @Modifying
    @Query("DELETE FROM AiSolutionEntity s WHERE s.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

}
