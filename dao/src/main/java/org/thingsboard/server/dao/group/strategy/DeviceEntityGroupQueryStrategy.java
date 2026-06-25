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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupQueryStrategy;
import org.thingsboard.server.dao.sql.group.EntityGroupRepository;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

@Slf4j
@Component
public class DeviceEntityGroupQueryStrategy implements EntityGroupQueryStrategy<Device> {

    private static final String DEVICE_PROFILE_ID_FILTER = "deviceProfileId";

    @Autowired
    private EntityGroupRepository entityGroupRepository;

    @Autowired
    private DeviceService deviceService;

    @Override
    public EntityType supportedType() {
        return EntityType.DEVICE;
    }

    @Override
    public PageData<Device> findEntitiesInGroup(TenantId tenantId, EntityGroupId groupId, PageLink pageLink) {
        return findEntitiesInGroup(tenantId, groupId, pageLink, Collections.emptyMap());
    }

    @Override
    public PageData<Device> findEntitiesInGroup(TenantId tenantId, EntityGroupId groupId, PageLink pageLink,
                                                Map<String, String> filters) {
        List<UUID> deviceIds = entityGroupRepository.findEntityIdsByGroupIdAndType(groupId.getId(), EntityType.DEVICE.name());
        if (deviceIds.isEmpty()) {
            return PageData.emptyPageData();
        }

        List<Device> devices;
        try {
            devices = deviceService.findDevicesByTenantIdAndIdsAsync(tenantId,
                    deviceIds.stream().map(DeviceId::new).toList()).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to load devices for group " + groupId, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to load devices for group " + groupId, e);
        }

        List<Device> filtered = devices.stream()
                .filter(device -> matchesDeviceProfile(device, filters.get(DEVICE_PROFILE_ID_FILTER)))
                .filter(device -> matchesTextSearch(device, pageLink.getTextSearch()))
                .sorted(buildComparator(pageLink.getSortOrder()))
                .toList();

        return toPageData(filtered, pageLink);
    }

    private boolean matchesDeviceProfile(Device device, String deviceProfileId) {
        if (StringUtils.isEmpty(deviceProfileId)) {
            return true;
        }
        return device.getDeviceProfileId() != null
                && device.getDeviceProfileId().getId().toString().equals(deviceProfileId);
    }

    private boolean matchesTextSearch(Device device, String textSearch) {
        if (StringUtils.isBlank(textSearch)) {
            return true;
        }
        String search = textSearch.toLowerCase();
        return contains(device.getName(), search)
                || contains(device.getLabel(), search)
                || contains(device.getType(), search);
    }

    private boolean contains(String value, String lowerCaseSearch) {
        return value != null && value.toLowerCase().contains(lowerCaseSearch);
    }

    private Comparator<Device> buildComparator(SortOrder sortOrder) {
        String property = sortOrder != null ? sortOrder.getProperty() : "createdTime";
        Comparator<Device> comparator = switch (property) {
            case "name" -> Comparator.comparing(Device::getName, nullSafe());
            case "label" -> Comparator.comparing(Device::getLabel, nullSafe());
            case "type" -> Comparator.comparing(Device::getType, nullSafe());
            default -> Comparator.comparingLong(Device::getCreatedTime);
        };
        boolean descending = sortOrder == null || sortOrder.getDirection() == SortOrder.Direction.DESC;
        if (descending) {
            comparator = comparator.reversed();
        }
        // Stable tiebreaker on id, matching the grid's secondary sort.
        return comparator.thenComparing(device -> device.getId().getId());
    }

    private static Comparator<String> nullSafe() {
        return Comparator.nullsLast(Comparator.comparing(Function.identity(), String.CASE_INSENSITIVE_ORDER));
    }

    private PageData<Device> toPageData(List<Device> devices, PageLink pageLink) {
        int total = devices.size();
        int pageSize = pageLink.getPageSize();
        int from = pageLink.getPage() * pageSize;
        int to = Math.min(from + pageSize, total);
        List<Device> content = from >= total ? List.of() : devices.subList(from, to);
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 1;
        return new PageData<>(content, totalPages, total, to < total);
    }
}
