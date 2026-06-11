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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.role.TbRoleService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

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
public class RoleController extends BaseController {

    private final TbRoleService tbRoleService;
    private final RoleService roleService;

    public static final String ROLE_ID = "roleId";
    public static final String ROLE_ID_PARAM_DESCRIPTION = "A string value representing the role id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";

    @ApiOperation(value = "Get Role (getRoleById)",
            notes = "Fetch the Role object based on the provided Role Id. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/role/{roleId}", method = RequestMethod.GET)
    @ResponseBody
    public Role getRoleById(
            @Parameter(description = ROLE_ID_PARAM_DESCRIPTION)
            @PathVariable(ROLE_ID) String strRoleId) throws ThingsboardException {
        checkParameter(ROLE_ID, strRoleId);
        RoleId roleId = new RoleId(toUUID(strRoleId));
        return checkRoleId(roleId, Operation.READ);
    }

    @ApiOperation(value = "Create or update Role (saveRole)",
            notes = "Creates or Updates the Role. When creating role, platform generates Role Id as " + UUID_WIKI_LINK +
                    "The newly created Role Id will be present in the response. " +
                    "Specify existing Role Id to update the role. " +
                    "Referencing non-existing Role Id will cause 'Not Found' error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/role", method = RequestMethod.POST)
    @ResponseBody
    public Role saveRole(
            @Parameter(description = "A JSON value representing the role.")
            @RequestBody Role role) throws Exception {
        role.setTenantId(getTenantId());
        checkEntity(role.getId(), role, Resource.ROLE);
        return tbRoleService.save(role, getCurrentUser());
    }

    @ApiOperation(value = "Delete Role (deleteRole)",
            notes = "Deletes the Role. Referencing non-existing Role Id will cause an error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/role/{roleId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRole(
            @Parameter(description = ROLE_ID_PARAM_DESCRIPTION)
            @PathVariable(ROLE_ID) String strRoleId) throws ThingsboardException {
        checkParameter(ROLE_ID, strRoleId);
        RoleId roleId = new RoleId(toUUID(strRoleId));
        Role role = checkRoleId(roleId, Operation.DELETE);
        tbRoleService.delete(role, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Roles (getRoles)",
            notes = "Returns a page of roles owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/roles")
    public PageData<Role> getRoles(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on the role name.")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(roleService.findRolesByTenantId(getTenantId(), pageLink));
    }

    private Role checkRoleId(RoleId roleId, Operation operation) throws ThingsboardException {
        return checkEntityId(roleId, roleService::findRoleById, operation);
    }

}
