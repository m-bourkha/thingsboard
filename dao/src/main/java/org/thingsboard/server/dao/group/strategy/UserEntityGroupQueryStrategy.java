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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.group.EntityGroupQueryStrategy;
import org.thingsboard.server.dao.sql.user.UserRepository;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserEntityGroupQueryStrategy implements EntityGroupQueryStrategy<User> {

    @Autowired
    private UserRepository userRepository;

    public static final Map<String, String> entityGroupUserColumnMap = new HashMap<>();
    static {
        entityGroupUserColumnMap.put("createdTime", "created_time");
        entityGroupUserColumnMap.put("firstName", "first_name");
        entityGroupUserColumnMap.put("lastName", "last_name");
        entityGroupUserColumnMap.put("email", "email");
    }

    @Override
    public EntityType supportedType() {
        return EntityType.USER;
    }

    @Override
    public PageData<User> findEntitiesInGroup(TenantId tenantId, EntityGroupId groupId, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findByEntityGroupId(
                groupId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink, entityGroupUserColumnMap)));
    }
}
