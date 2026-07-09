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
package org.thingsboard.server.dao.ai;

import org.thingsboard.server.common.data.ai.solution.AiSolution;
import org.thingsboard.server.common.data.ai.solution.AiSolutionStatus;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for persisted {@link AiSolution} sessions. Tenant-scoped; not an {@code EntityType}, so it
 * uses a self-contained persistence layer rather than the shared entity DAO framework.
 */
public interface AiSolutionService {

    AiSolution save(AiSolution solution);

    AiSolution findById(TenantId tenantId, UUID id);

    PageData<AiSolution> findByTenantId(TenantId tenantId, PageLink pageLink);

    boolean deleteById(TenantId tenantId, UUID id);

    void deleteByTenantId(TenantId tenantId);

    List<AiSolution> findAllByStatus(AiSolutionStatus status);

}
