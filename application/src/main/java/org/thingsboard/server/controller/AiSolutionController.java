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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.ai.solution.AiSolution;
import org.thingsboard.server.common.data.ai.solution.AiSolutionDashboardRequest;
import org.thingsboard.server.common.data.ai.solution.AiSolutionGenerateRequest;
import org.thingsboard.server.common.data.ai.solution.AiSolutionInstallResult;
import org.thingsboard.server.common.data.ai.solution.AiSolutionRefineRequest;
import org.thingsboard.server.common.data.ai.solution.AiSolutionSpec;
import org.thingsboard.server.common.data.ai.solution.AiSolutionStatus;
import org.thingsboard.server.common.data.ai.solution.DashboardSpec;
import org.thingsboard.server.common.data.ai.solution.InstalledEntity;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.ai.AiSolutionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.solution.AiSolutionGenerationService;
import org.thingsboard.server.service.ai.solution.AiSolutionInstaller;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/ai/solution")
public class AiSolutionController extends BaseController {

    private final AiSolutionGenerationService aiSolutionGenerationService;
    private final AiSolutionInstaller aiSolutionInstaller;
    private final AiSolutionService aiSolutionService;

    // --- Architecture generation --------------------------------------------------------------

    @ApiOperation(
            value = "Generate solution architecture (generateArchitecture)",
            notes = "Generates an architecture specification from a natural-language description using the " +
                    "specified AI model. Returns the full specification as JSON." + TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/generate")
    public DeferredResult<AiSolutionSpec> generateArchitecture(@RequestBody @Valid AiSolutionGenerateRequest request) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI_MODEL, Operation.READ);
        ListenableFuture<AiSolutionSpec> future = aiSolutionGenerationService.generate(
                user.getTenantId(), new AiModelId(request.modelId()), request.prompt());
        return wrapFuture(future);
    }

    @ApiOperation(
            value = "Refine solution architecture (refineArchitecture)",
            notes = "Refines an existing architecture specification via a natural-language instruction and " +
                    "returns the complete updated specification." + TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/refine")
    public DeferredResult<AiSolutionSpec> refineArchitecture(@RequestBody @Valid AiSolutionRefineRequest request) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI_MODEL, Operation.READ);
        ListenableFuture<AiSolutionSpec> future = aiSolutionGenerationService.refine(
                user.getTenantId(), new AiModelId(request.modelId()), request.currentSpec(), request.message());
        return wrapFuture(future);
    }

    // --- Dashboard design ---------------------------------------------------------------------

    @ApiOperation(
            value = "Design solution dashboards (generateDashboards)",
            notes = "Designs one dashboard per persona described by an already-generated architecture " +
                    "specification. Returns the dashboard specifications; the actual widget layout is derived " +
                    "deterministically when the solution is installed." + TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/dashboards/generate")
    public DeferredResult<List<DashboardSpec>> generateDashboards(@RequestBody @Valid AiSolutionDashboardRequest request) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI_MODEL, Operation.READ);
        ListenableFuture<List<DashboardSpec>> future = aiSolutionGenerationService.generateDashboards(
                user.getTenantId(), new AiModelId(request.modelId()), request.spec());
        return wrapFuture(future);
    }

    // --- Solution persistence -----------------------------------------------------------------

    @ApiOperation(
            value = "Save AI solution (saveAiSolution)",
            notes = "Creates or updates a saved AI solution session (name, prompt and current spec)." + TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping
    public AiSolution saveAiSolution(@RequestBody AiSolution solution) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        solution.setTenantId(user.getTenantId());
        if (solution.getId() != null) {
            // ensure the caller owns the existing record
            checkNotNull(aiSolutionService.findById(user.getTenantId(), solution.getId()));
        }
        if (solution.getStatus() == null) {
            solution.setStatus(AiSolutionStatus.DRAFT);
        }
        return aiSolutionService.save(solution);
    }

    @ApiOperation(value = "Get AI solution by id (getAiSolutionById)", notes = TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/{solutionId}")
    public AiSolution getAiSolutionById(
            @Parameter(description = "AI solution id", required = true) @PathVariable UUID solutionId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return checkNotNull(aiSolutionService.findById(user.getTenantId(), solutionId));
    }

    @ApiOperation(value = "Get AI solutions (getAiSolutions)", notes = PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping
    public PageData<AiSolution> getAiSolutions(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'startsWith' filter based on the solution name") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "status"})) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"})) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return aiSolutionService.findByTenantId(user.getTenantId(), pageLink);
    }

    @ApiOperation(
            value = "Delete AI solution (deleteAiSolution)",
            notes = "Deletes the saved AI solution session. If the solution is still installed, every entity it " +
                    "created (profiles, devices, assets, calculated fields, alarm rules, roles, entity groups, " +
                    "users, permissions and dashboards) is deleted first. If any of those deletions fail, the " +
                    "solution is kept — marked partially installed with the surviving entities — so they can " +
                    "still be cleaned up rather than being orphaned." + TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/{solutionId}")
    public boolean deleteAiSolution(
            @Parameter(description = "AI solution id", required = true) @PathVariable UUID solutionId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        AiSolution solution = checkNotNull(aiSolutionService.findById(user.getTenantId(), solutionId));
        List<InstalledEntity> installed =
                solution.getInstalledEntities() != null ? solution.getInstalledEntities() : List.of();
        if (!installed.isEmpty()) {
            AiSolutionInstaller.Outcome outcome = aiSolutionInstaller.uninstall(user, installed);
            if (!outcome.result().success()) {
                // Deleting the record now would strand the entities we could not remove: keep it, pointing
                // at the survivors, so the tenant can retry the delete once the blocker is resolved.
                solution.setInstalledEntities(outcome.installedEntities());
                solution.setStatus(AiSolutionStatus.PARTIALLY_INSTALLED);
                aiSolutionService.save(solution);
                throw new ThingsboardException("Failed to delete " + outcome.installedEntities().size() +
                        " entity(-ies) created by this solution; the solution was not deleted",
                        ThingsboardErrorCode.GENERAL);
            }
        }
        return aiSolutionService.deleteById(user.getTenantId(), solutionId);
    }

    // --- Install / uninstall ------------------------------------------------------------------

    @ApiOperation(
            value = "Install AI solution (installAiSolution)",
            notes = "Creates every entity described by the solution's specification (profiles, devices, assets, " +
                    "calculated fields, alarms, roles, entity groups, users, permissions and dashboards) and returns " +
                    "a per-item status report." + TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/{solutionId}/install")
    public AiSolutionInstallResult installAiSolution(
            @Parameter(description = "AI solution id", required = true) @PathVariable UUID solutionId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        AiSolution solution = checkNotNull(aiSolutionService.findById(user.getTenantId(), solutionId));
        checkNotNull(solution.getSpec());
        AiSolutionInstaller.Outcome outcome = aiSolutionInstaller.install(user, solution.getSpec());
        solution.setInstalledEntities(outcome.installedEntities());
        solution.setStatus(outcome.result().success() ? AiSolutionStatus.INSTALLED : AiSolutionStatus.PARTIALLY_INSTALLED);
        aiSolutionService.save(solution);
        return outcome.result();
    }

    @ApiOperation(
            value = "Uninstall AI solution (uninstallAiSolution)",
            notes = "Deletes the entities previously created by installing this solution and returns a per-item " +
                    "status report." + TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/{solutionId}/uninstall")
    public AiSolutionInstallResult uninstallAiSolution(
            @Parameter(description = "AI solution id", required = true) @PathVariable UUID solutionId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        AiSolution solution = checkNotNull(aiSolutionService.findById(user.getTenantId(), solutionId));
        List<InstalledEntity> installed =
                solution.getInstalledEntities() != null ? solution.getInstalledEntities() : List.of();
        AiSolutionInstaller.Outcome outcome = aiSolutionInstaller.uninstall(user, installed);
        // Whatever survived a failed delete is still owned by the solution, so it stays uninstallable.
        solution.setInstalledEntities(outcome.installedEntities());
        solution.setStatus(outcome.result().success() ? AiSolutionStatus.UNINSTALLED : AiSolutionStatus.PARTIALLY_INSTALLED);
        aiSolutionService.save(solution);
        return outcome.result();
    }

}
