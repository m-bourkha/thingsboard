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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.grouppermission.TbGroupPermissionService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class GroupPermissionController extends BaseController {

    private final TbGroupPermissionService tbGroupPermissionService;
    private final GroupPermissionService groupPermissionService;
    private final RoleService roleService;
    private final EntityGroupService entityGroupService;
    private final EntityService entityService;

    public static final String GROUP_PERMISSION_ID = "groupPermissionId";
    private static final String GROUP_PERMISSION_ID_PARAM = "A string value representing the group permission id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";

    @ApiOperation(value = "Get Group Permission (getGroupPermissionById)",
            notes = "Fetch the Group Permission object based on the provided Id. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/groupPermission/{groupPermissionId}", method = RequestMethod.GET)
    @ResponseBody
    public GroupPermission getGroupPermissionById(
            @Parameter(description = GROUP_PERMISSION_ID_PARAM)
            @PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId) throws ThingsboardException {
        checkParameter(GROUP_PERMISSION_ID, strGroupPermissionId);
        GroupPermissionId groupPermissionId = new GroupPermissionId(toUUID(strGroupPermissionId));
        return checkGroupPermissionId(groupPermissionId, Operation.READ);
    }

    @ApiOperation(value = "Create or update Group Permission (saveGroupPermission)",
            notes = "Creates or Updates the Group Permission. When creating, platform generates Id as " + UUID_WIKI_LINK +
                    "The newly created Id will be present in the response. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/groupPermission", method = RequestMethod.POST)
    @ResponseBody
    public GroupPermission saveGroupPermission(
            @Parameter(description = "A JSON value representing the group permission.")
            @RequestBody GroupPermission groupPermission) throws Exception {
        groupPermission.setTenantId(getTenantId());
        if (groupPermission.getEntityGroupId() != null && groupPermission.getEntityGroupType() == null) {
            EntityGroup entityGroup = entityGroupService.findEntityGroupById(getTenantId(), groupPermission.getEntityGroupId());
            if (entityGroup != null) {
                groupPermission.setEntityGroupType(entityGroup.getType());
            }
        }
        checkEntity(groupPermission.getId(), groupPermission, Resource.GROUP_PERMISSION);
        return tbGroupPermissionService.save(groupPermission, getCurrentUser());
    }

    @ApiOperation(value = "Delete Group Permission (deleteGroupPermission)",
            notes = "Deletes the Group Permission. Referencing non-existing Id will cause an error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/groupPermission/{groupPermissionId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteGroupPermission(
            @Parameter(description = GROUP_PERMISSION_ID_PARAM)
            @PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId) throws ThingsboardException {
        checkParameter(GROUP_PERMISSION_ID, strGroupPermissionId);
        GroupPermissionId groupPermissionId = new GroupPermissionId(toUUID(strGroupPermissionId));
        GroupPermission groupPermission = checkGroupPermissionId(groupPermissionId, Operation.DELETE);
        tbGroupPermissionService.delete(groupPermission, getCurrentUser());
    }

    @ApiOperation(value = "Get Group Permissions by User Group (getGroupPermissionsByUserGroupId)",
            notes = "Returns a page of group permissions for the specified user group. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/userGroup/{userGroupId}/groupPermissions", method = RequestMethod.GET)
    @ResponseBody
    public PageData<GroupPermissionInfo> getGroupPermissionsByUserGroupId(
            @Parameter(description = "A string value representing the user group id.")
            @PathVariable("userGroupId") String strUserGroupId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on role name.") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime"})) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"})) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("userGroupId", strUserGroupId);
        EntityGroupId userGroupId = new EntityGroupId(toUUID(strUserGroupId));
        checkEntityId(userGroupId, entityGroupService::findEntityGroupById, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        PageData<GroupPermission> permissionsPage = groupPermissionService.findGroupPermissionsByTenantIdAndUserGroupId(getTenantId(), userGroupId, pageLink);
        return toGroupPermissionInfoPage(getTenantId(), permissionsPage);
    }

    @ApiOperation(value = "Get Group Permissions by Entity Group (getGroupPermissionsByEntityGroupId)",
            notes = "Returns a page of group permissions for the specified entity group. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/groupPermissions", method = RequestMethod.GET)
    @ResponseBody
    public PageData<GroupPermissionInfo> getGroupPermissionsByEntityGroupId(
            @Parameter(description = "A string value representing the entity group id.")
            @PathVariable("entityGroupId") String strEntityGroupId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on role name.") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime"})) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"})) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        checkEntityId(entityGroupId, entityGroupService::findEntityGroupById, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        PageData<GroupPermission> permissionsPage = groupPermissionService.findGroupPermissionsByTenantIdAndEntityGroupId(getTenantId(), entityGroupId, pageLink);
        return toGroupPermissionInfoPage(getTenantId(), permissionsPage);
    }

    @ApiOperation(value = "Get Group Permissions by Role (getGroupPermissionsByRoleId)",
            notes = "Returns a page of group permissions for the specified role. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/role/{roleId}/groupPermissions", method = RequestMethod.GET)
    @ResponseBody
    public PageData<GroupPermissionInfo> getGroupPermissionsByRoleId(
            @Parameter(description = "A string value representing the role id.")
            @PathVariable("roleId") String strRoleId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on role name.") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime"})) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"})) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("roleId", strRoleId);
        RoleId roleId = new RoleId(toUUID(strRoleId));
        checkEntityId(roleId, roleService::findRoleById, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        PageData<GroupPermission> permissionsPage = groupPermissionService.findGroupPermissionsByTenantIdAndRoleId(getTenantId(), roleId, pageLink);
        return toGroupPermissionInfoPage(getTenantId(), permissionsPage);
    }

    private GroupPermission checkGroupPermissionId(GroupPermissionId groupPermissionId, Operation operation) throws ThingsboardException {
        return checkEntityId(groupPermissionId, groupPermissionService::findGroupPermissionById, operation);
    }

    private PageData<GroupPermissionInfo> toGroupPermissionInfoPage(TenantId tenantId, PageData<GroupPermission> permissionsPage) {
        List<GroupPermissionInfo> infoList = new ArrayList<>(permissionsPage.getData().size());
        for (GroupPermission perm : permissionsPage.getData()) {
            infoList.add(toGroupPermissionInfo(tenantId, perm));
        }
        return new PageData<>(infoList, permissionsPage.getTotalPages(), permissionsPage.getTotalElements(), permissionsPage.hasNext());
    }

    private GroupPermissionInfo toGroupPermissionInfo(TenantId tenantId, GroupPermission perm) {
        GroupPermissionInfo info = new GroupPermissionInfo(perm);
        if (perm.getRoleId() != null) {
            Role role = roleService.findRoleById(tenantId, perm.getRoleId());
            if (role != null) {
                info.setRoleName(role.getName());
                info.setRoleType(role.getType());
            }
        }
        if (perm.getUserGroupId() != null) {
            EntityGroup userGroup = entityGroupService.findEntityGroupById(tenantId, perm.getUserGroupId());
            if (userGroup != null) {
                info.setUserGroupName(userGroup.getName());
                if (userGroup.getOwnerId() != null) {
                    info.setUserGroupOwnerId(userGroup.getOwnerId());
                    entityService.fetchEntityName(tenantId, userGroup.getOwnerId())
                            .ifPresent(info::setUserGroupOwnerName);
                }
            }
        }
        return info;
    }
}
