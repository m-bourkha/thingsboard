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

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.ai.solution.AiSolutionSpec;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * Turns natural-language input into an {@link AiSolutionSpec} using the tenant's configured AI model.
 * Both operations return the full spec so the UI can re-render the whole overview after each turn.
 */
public interface AiSolutionGenerationService {

    /**
     * Generate a fresh architecture spec from a natural-language description.
     */
    ListenableFuture<AiSolutionSpec> generate(TenantId tenantId, AiModelId modelId, String prompt);

    /**
     * Refine an existing spec according to a chat message and return the complete updated spec.
     */
    ListenableFuture<AiSolutionSpec> refine(TenantId tenantId, AiModelId modelId, AiSolutionSpec currentSpec, String message);

}
