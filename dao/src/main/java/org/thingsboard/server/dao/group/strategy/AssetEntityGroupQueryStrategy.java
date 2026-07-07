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
package org.thingsboard.server.dao.group.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.group.EntityGroupQueryStrategy;
import org.thingsboard.server.dao.sql.group.EntityGroupRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

@Slf4j
@Component
public class AssetEntityGroupQueryStrategy implements EntityGroupQueryStrategy<Asset> {

    @Autowired
    private EntityGroupRepository entityGroupRepository;

    @Autowired
    private AssetService assetService;

    @Override
    public EntityType supportedType() {
        return EntityType.ASSET;
    }

    @Override
    public PageData<Asset> findEntitiesInGroup(TenantId tenantId, EntityGroupId groupId, PageLink pageLink) {
        List<UUID> assetIds = entityGroupRepository.findEntityIdsByGroupIdAndType(groupId.getId(), EntityType.ASSET.name());
        if (assetIds.isEmpty()) {
            return PageData.emptyPageData();
        }

        List<Asset> assets;
        try {
            assets = assetService.findAssetsByTenantIdAndIdsAsync(tenantId,
                    assetIds.stream().map(AssetId::new).toList()).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to load assets for group " + groupId, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to load assets for group " + groupId, e);
        }

        List<Asset> filtered = assets.stream()
                .filter(asset -> matchesTextSearch(asset, pageLink.getTextSearch()))
                .sorted(buildComparator(pageLink.getSortOrder()))
                .toList();

        return toPageData(filtered, pageLink);
    }

    private boolean matchesTextSearch(Asset asset, String textSearch) {
        if (StringUtils.isBlank(textSearch)) {
            return true;
        }
        String search = textSearch.toLowerCase();
        return contains(asset.getName(), search)
                || contains(asset.getLabel(), search)
                || contains(asset.getType(), search);
    }

    private boolean contains(String value, String lowerCaseSearch) {
        return value != null && value.toLowerCase().contains(lowerCaseSearch);
    }

    private Comparator<Asset> buildComparator(SortOrder sortOrder) {
        String property = sortOrder != null ? sortOrder.getProperty() : "createdTime";
        Comparator<Asset> comparator = switch (property) {
            case "name" -> Comparator.comparing(Asset::getName, nullSafe());
            case "label" -> Comparator.comparing(Asset::getLabel, nullSafe());
            case "type" -> Comparator.comparing(Asset::getType, nullSafe());
            default -> Comparator.comparingLong(Asset::getCreatedTime);
        };
        boolean descending = sortOrder == null || sortOrder.getDirection() == SortOrder.Direction.DESC;
        if (descending) {
            comparator = comparator.reversed();
        }
        // Stable tiebreaker on id, matching the grid's secondary sort.
        return comparator.thenComparing(asset -> asset.getId().getId());
    }

    private static Comparator<String> nullSafe() {
        return Comparator.nullsLast(Comparator.comparing(Function.identity(), String.CASE_INSENSITIVE_ORDER));
    }

    private PageData<Asset> toPageData(List<Asset> assets, PageLink pageLink) {
        int total = assets.size();
        int pageSize = pageLink.getPageSize();
        int from = pageLink.getPage() * pageSize;
        int to = Math.min(from + pageSize, total);
        List<Asset> content = from >= total ? List.of() : assets.subList(from, to);
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 1;
        return new PageData<>(content, totalPages, total, to < total);
    }
}
