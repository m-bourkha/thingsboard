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
package org.thingsboard.server.service.ai.solution;

import org.thingsboard.server.common.data.ai.solution.AiSolutionInstallResult;
import org.thingsboard.server.common.data.ai.solution.AiSolutionSpec;
import org.thingsboard.server.common.data.ai.solution.InstalledEntity;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

/**
 * Turns an {@link AiSolutionSpec} into real ThingsBoard entities (and back). Individual item
 * failures are reported in the {@link AiSolutionInstallResult} rather than aborting the whole run.
 */
public interface AiSolutionInstaller {

    /**
     * Create every entity described by the spec, in dependency order.
     *
     * @return the per-item status report plus the ids of everything created (for later uninstall).
     */
    Outcome install(SecurityUser user, AiSolutionSpec spec);

    /**
     * Delete the given previously-installed entities in reverse creation order.
     */
    AiSolutionInstallResult uninstall(SecurityUser user, List<InstalledEntity> installedEntities);

    /**
     * Result of an install run: the human-facing report and the machine-readable list of created
     * entities that the {@code AiSolution} persists so the solution can be uninstalled later.
     */
    record Outcome(AiSolutionInstallResult result, List<InstalledEntity> installedEntities) {
    }

}
