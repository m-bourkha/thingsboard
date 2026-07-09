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

/**
 * A single telemetry / attribute key on an entity profile. Numeric TIMESERIES keys with a
 * min/max range are what the telemetry generator emits after installation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelemetryKeySpec(
        @Schema(description = "Whether the key is stored as an ATTRIBUTE or as a TIMESERIES value")
        TelemetryType telemetryType,

        @Schema(description = "Key name", example = "battery")
        String key,

        @Schema(description = "Value data type")
        DataType dataType,

        @Schema(description = "Minimum value used when generating sample data (numeric keys)", example = "0")
        Double minValue,

        @Schema(description = "Maximum value used when generating sample data (numeric keys)", example = "100")
        Double maxValue,

        @Schema(description = "Default/initial value for attributes")
        String defaultValue
) implements Serializable {

    public enum TelemetryType {
        ATTRIBUTE, TIMESERIES
    }

    public enum DataType {
        DOUBLE, LONG, BOOLEAN, STRING
    }

    public boolean isTimeseries() {
        return telemetryType == null || telemetryType == TelemetryType.TIMESERIES;
    }

    public boolean isNumeric() {
        return dataType == null || dataType == DataType.DOUBLE || dataType == DataType.LONG;
    }

}
