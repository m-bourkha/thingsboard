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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.group.EntityGroupQueryStrategy;
import org.thingsboard.server.dao.sql.device.DeviceRepository;

@Component
public class DeviceEntityGroupQueryStrategy implements EntityGroupQueryStrategy<Device> {

    @Autowired
    private DeviceRepository deviceRepository;

    @Override
    public EntityType supportedType() {
        return EntityType.DEVICE;
    }

    @Override
    public PageData<Device> findEntitiesInGroup(TenantId tenantId, EntityGroupId groupId, PageLink pageLink) {
        return DaoUtil.toPageData(deviceRepository.findByEntityGroupId(
                groupId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }
}
