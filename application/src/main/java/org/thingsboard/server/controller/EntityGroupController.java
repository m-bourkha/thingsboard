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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.group.EntityGroupQueryStrategyRegistry;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.group.TbEntityGroupService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class EntityGroupController extends BaseController {

    private final TbEntityGroupService tbEntityGroupService;
    private final EntityGroupService entityGroupService;
    private final EntityGroupQueryStrategyRegistry strategyRegistry;

    @ApiOperation(value = "Get Entity Group (getEntityGroupById)",
            notes = "Returns the entity group by id." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/entityGroup/{entityGroupId}")
    @ResponseBody
    public EntityGroup getEntityGroupById(
            @PathVariable("entityGroupId") String strEntityGroupId) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        return checkEntityId(entityGroupId, entityGroupService::findEntityGroupById, Operation.READ);
    }

    @ApiOperation(value = "Create or update Entity Group (saveEntityGroup)",
            notes = "Creates or updates the entity group." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/entityGroup")
    @ResponseBody
    public EntityGroup saveEntityGroup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Entity group to create or update.")
            @RequestBody EntityGroup entityGroup) throws Exception {
        entityGroup.setTenantId(getTenantId());
        if (entityGroup.getOwnerId() == null) {
            entityGroup.setOwnerId(Authority.CUSTOMER_USER.equals(getCurrentUser().getAuthority())
                    ? getCurrentUser().getCustomerId()
                    : getTenantId());
        }
        checkEntity(entityGroup.getId(), entityGroup, Resource.ENTITY_GROUP);
        return tbEntityGroupService.save(entityGroup, getCurrentUser());
    }

    @ApiOperation(value = "Delete Entity Group (deleteEntityGroup)",
            notes = "Deletes the entity group. The 'All' group cannot be deleted." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping("/entityGroup/{entityGroupId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteEntityGroup(
            @PathVariable("entityGroupId") String strEntityGroupId) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityId(entityGroupId, entityGroupService::findEntityGroupById, Operation.DELETE);
        tbEntityGroupService.delete(entityGroup, getCurrentUser());
    }

    @ApiOperation(value = "List Entity Groups by type (getEntityGroupsByType)",
            notes = "Returns all entity groups of the given type owned by the current user." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/entityGroups/{groupType}")
    @ResponseBody
    public PageData<EntityGroup> getEntityGroupsByType(
            @PathVariable("groupType") String strGroupType,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Text search") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        EntityType groupType = EntityType.valueOf(strGroupType.toUpperCase());
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        EntityId ownerId = Authority.CUSTOMER_USER.equals(getCurrentUser().getAuthority())
                ? getCurrentUser().getCustomerId()
                : getTenantId();
        return checkNotNull(entityGroupService.findEntityGroupsByOwnerAndType(getTenantId(), ownerId, groupType, pageLink));
    }

    @ApiOperation(value = "List Entity Groups by owner and type (getEntityGroupsByOwnerAndType)",
            notes = "Returns all entity groups of the given type for a specific owner." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/owner/{ownerType}/{ownerId}/entityGroups/{groupType}")
    @ResponseBody
    public PageData<EntityGroup> getEntityGroupsByOwnerAndType(
            @PathVariable("ownerType") String ownerType,
            @PathVariable("ownerId") String strOwnerId,
            @PathVariable("groupType") String strGroupType,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Text search") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        EntityType groupType = EntityType.valueOf(strGroupType.toUpperCase());
        EntityId ownerId = EntityIdFactory.getByTypeAndUuid(ownerType, toUUID(strOwnerId));
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(entityGroupService.findEntityGroupsByOwnerAndType(getTenantId(), ownerId, groupType, pageLink));
    }

    @ApiOperation(value = "Get entities in group (getEntitiesInGroup)",
            notes = "Returns a paged list of entities belonging to the given group." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/entityGroup/{entityGroupId}/entities")
    @ResponseBody
    public PageData<PageData<? extends UUIDBased>> getEntitiesInGroup(
            @PathVariable("entityGroupId") String strEntityGroupId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Text search") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityId(entityGroupId, entityGroupService::findEntityGroupById, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(strategyRegistry.findEntitiesInGroup(entityGroup, pageLink));
    }

    @ApiOperation(value = "Add entities to group (addEntitiesToGroup)",
            notes = "Adds a list of entities (by id) to the given entity group." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/entityGroup/{entityGroupId}/addEntities")
    @ResponseStatus(HttpStatus.OK)
    public void addEntitiesToGroup(
            @PathVariable("entityGroupId") String strEntityGroupId,
            @RequestBody List<EntityId> entityIds) throws Exception {
        checkParameter("entityGroupId", strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityId(entityGroupId, entityGroupService::findEntityGroupById, Operation.WRITE);
        tbEntityGroupService.addEntitiesToGroup(entityGroup, entityIds, getCurrentUser());
    }

    @ApiOperation(value = "Remove entities from group (removeEntitiesFromGroup)",
            notes = "Removes a list of entities (by id) from the given entity group." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/entityGroup/{entityGroupId}/removeEntities")
    @ResponseStatus(HttpStatus.OK)
    public void removeEntitiesFromGroup(
            @PathVariable("entityGroupId") String strEntityGroupId,
            @RequestBody List<EntityId> entityIds) throws Exception {
        checkParameter("entityGroupId", strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityId(entityGroupId, entityGroupService::findEntityGroupById, Operation.WRITE);
        tbEntityGroupService.removeEntitiesFromGroup(entityGroup, entityIds, getCurrentUser());
    }

    @ApiOperation(value = "Get groups for an entity (getEntityGroupsByEntity)",
            notes = "Returns all groups the given entity belongs to." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/entity/{entityType}/{entityId}/entityGroups")
    @ResponseBody
    public List<EntityGroup> getEntityGroupsByEntity(
            @PathVariable("entityType") String strEntityType,
            @PathVariable("entityId") String strEntityId) throws ThingsboardException {
        checkParameter("entityId", strEntityId);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(strEntityType, toUUID(strEntityId));
        return checkNotNull(entityGroupService.findEntityGroupsByEntityId(getTenantId(), entityId));
    }

    @ApiOperation(value = "Make group public (makeEntityGroupPublic)",
            notes = "Makes the entity group publicly readable." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/entityGroup/{entityGroupId}/makePublic")
    @ResponseBody
    public EntityGroup makeEntityGroupPublic(
            @PathVariable("entityGroupId") String strEntityGroupId) throws Exception {
        checkParameter("entityGroupId", strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityId(entityGroupId, entityGroupService::findEntityGroupById, Operation.WRITE);
        return tbEntityGroupService.makePublic(entityGroup, getCurrentUser());
    }

    @ApiOperation(value = "Make group private (makeEntityGroupPrivate)",
            notes = "Makes the entity group private." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/entityGroup/{entityGroupId}/makePrivate")
    @ResponseBody
    public EntityGroup makeEntityGroupPrivate(
            @PathVariable("entityGroupId") String strEntityGroupId) throws Exception {
        checkParameter("entityGroupId", strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityId(entityGroupId, entityGroupService::findEntityGroupById, Operation.WRITE);
        return tbEntityGroupService.makePrivate(entityGroup, getCurrentUser());
    }

    @ApiOperation(value = "Change group owner (changeEntityGroupOwner)",
            notes = "Transfers the entity group to a different owner." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/entityGroup/{entityGroupId}/changeOwner/{ownerType}/{ownerId}")
    @ResponseBody
    public EntityGroup changeEntityGroupOwner(
            @PathVariable("entityGroupId") String strEntityGroupId,
            @PathVariable("ownerType") String ownerType,
            @PathVariable("ownerId") String strOwnerId) throws Exception {
        checkParameter("entityGroupId", strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityId(entityGroupId, entityGroupService::findEntityGroupById, Operation.WRITE);
        EntityId newOwnerId = EntityIdFactory.getByTypeAndUuid(ownerType, toUUID(strOwnerId));
        return tbEntityGroupService.changeOwner(entityGroup, newOwnerId, getCurrentUser());
    }
}
