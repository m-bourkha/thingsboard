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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * Request to generate a fresh architecture spec from a natural-language description.
 */
@Schema(description = "Request to generate an architecture specification from a natural-language prompt")
public record AiSolutionGenerateRequest(
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Id of the AI model to use for generation")
        UUID modelId,

        @NotBlank
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Natural-language description of the desired solution")
        String prompt
) implements Serializable {
}
