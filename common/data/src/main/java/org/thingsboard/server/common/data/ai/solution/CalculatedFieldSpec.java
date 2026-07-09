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
 * A calculated field to attach to a device/asset profile. Kept deliberately simple: a SIMPLE
 * single-expression field over a set of telemetry argument keys, producing one output key.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CalculatedFieldSpec(
        @Schema(description = "Calculated field name", example = "Parking Lot occupied spaces calculation")
        String name,

        @Schema(description = "Name of the device or asset profile this field is attached to")
        String entityProfile,

        @Schema(description = "Telemetry keys used as arguments in the expression")
        List<String> arguments,

        @Schema(description = "Expression evaluated over the arguments (e.g. 'occupied ? 1 : 0')")
        String expression,

        @Schema(description = "Output telemetry key name", example = "occupiedSpaces")
        String output
) implements Serializable {

    public List<String> argumentsOrEmpty() {
        return arguments != null ? arguments : List.of();
    }

}
