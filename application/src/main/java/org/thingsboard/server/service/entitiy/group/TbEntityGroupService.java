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
package org.thingsboard.server.service.entitiy.group;


import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.entitiy.SimpleTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

public interface TbEntityGroupService extends SimpleTbEntityService<EntityGroup> {

    EntityGroup makePublic(EntityGroup entityGroup, SecurityUser user) throws Exception;

    EntityGroup makePrivate(EntityGroup entityGroup, SecurityUser user) throws Exception;

    EntityGroup changeOwner(EntityGroup entityGroup, EntityId newOwnerId, SecurityUser user) throws Exception;

    void addEntitiesToGroup(EntityGroup entityGroup, List<EntityId> entityIds, SecurityUser user) throws Exception;

    void removeEntitiesFromGroup(EntityGroup entityGroup, List<EntityId> entityIds, SecurityUser user) throws Exception;
}
