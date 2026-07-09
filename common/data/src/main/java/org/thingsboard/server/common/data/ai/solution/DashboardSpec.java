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
package org.thingsboard.server.common.data.ai.solution;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * One role-oriented dashboard of the solution, as designed by the AI in the "Dashboard Design" step.
 * <p>
 * The AI decides <em>which</em> dashboards exist, who they are for and what they are used for.
 * It never produces widget JSON: the installer expands this spec into a real
 * {@code Dashboard} deterministically, so the result is always valid and openable.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "A single dashboard of the solution, designed by the AI Solution Creator")
public record DashboardSpec(
        @Schema(description = "Dashboard title", example = "Parking Administration")
        String name,

        @Schema(description = "Display name of the user (persona) this dashboard is built for",
                example = "Parking Platform Administrator")
        String assignedTo,

        @Schema(description = "Ownership scope of the dashboard", allowableValues = {"TENANT", "CUSTOMER"})
        String scope,

        @Schema(description = "Name of the customer this dashboard is assigned to, when scope is CUSTOMER")
        String customer,

        @Schema(description = "One-sentence summary shown under the dashboard title",
                example = "Tenant-level management dashboard for the Parking Platform Administrator.")
        String overview,

        @Schema(description = "Markdown description of the dashboard, grouped into '###' sections")
        String description,

        @Schema(description = "Concrete things the assigned user can do on this dashboard")
        List<String> useCases,

        @Schema(description = "Names of the device/asset profiles this dashboard visualizes")
        List<String> entityProfiles
) implements Serializable {

    public List<String> useCasesOrEmpty() {
        return useCases != null ? useCases : List.of();
    }

    public List<String> entityProfilesOrEmpty() {
        return entityProfiles != null ? entityProfiles : List.of();
    }

    public boolean isCustomerScoped() {
        return "CUSTOMER".equalsIgnoreCase(scope);
    }

}
