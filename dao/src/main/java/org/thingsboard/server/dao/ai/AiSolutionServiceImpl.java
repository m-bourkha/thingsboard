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

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ai.solution.AiSolution;
import org.thingsboard.server.common.data.ai.solution.AiSolutionStatus;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.model.sql.AiSolutionEntity;
import org.thingsboard.server.dao.sql.ai.AiSolutionRepository;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiSolutionServiceImpl implements AiSolutionService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdTime", "name", "status");
    private static final Map<String, String> COLUMN_MAP = Map.of("createdTime", "created_time");

    private final AiSolutionRepository repository;

    @Override
    public AiSolution save(AiSolution solution) {
        validate(solution);
        if (solution.getStatus() == null) {
            solution.setStatus(AiSolutionStatus.DRAFT);
        }
        if (solution.getId() == null) {
            solution.setId(UUID.randomUUID());
            solution.setCreatedTime(System.currentTimeMillis());
            solution.setVersion(1L);
        } else {
            solution.setVersion(solution.getVersion() == null ? 1L : solution.getVersion() + 1);
        }
        return repository.save(new AiSolutionEntity(solution)).toData();
    }

    @Override
    public AiSolution findById(TenantId tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId.getId(), id)
                .map(AiSolutionEntity::toData)
                .orElse(null);
    }

    @Override
    public PageData<AiSolution> findByTenantId(TenantId tenantId, PageLink pageLink) {
        var page = repository.findByTenantId(
                tenantId.getId(),
                StringUtils.defaultIfEmpty(pageLink.getTextSearch(), null),
                toPageRequest(pageLink));
        List<AiSolution> data = page.getContent().stream().map(AiSolutionEntity::toData).toList();
        return new PageData<>(data, page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @Override
    public boolean deleteById(TenantId tenantId, UUID id) {
        return repository.deleteByTenantIdAndId(tenantId.getId(), id) > 0;
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        repository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public List<AiSolution> findAllByStatus(AiSolutionStatus status) {
        return repository.findByStatus(status).stream().map(AiSolutionEntity::toData).toList();
    }

    private static void validate(AiSolution solution) {
        if (solution.getTenantId() == null) {
            throw new DataValidationException("AI solution should be assigned to a tenant!");
        }
        if (StringUtils.isBlank(solution.getName())) {
            throw new DataValidationException("AI solution name should be specified!");
        }
    }

    private static PageRequest toPageRequest(PageLink pageLink) {
        SortOrder sortOrder = pageLink.getSortOrder();
        Sort sort;
        if (sortOrder == null || !ALLOWED_SORT_PROPERTIES.contains(sortOrder.getProperty())) {
            sort = Sort.by(Sort.Direction.DESC, "created_time");
        } else {
            String column = COLUMN_MAP.getOrDefault(sortOrder.getProperty(), sortOrder.getProperty());
            sort = Sort.by(Sort.Direction.fromString(sortOrder.getDirection().name()), column);
        }
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), sort);
    }

}
