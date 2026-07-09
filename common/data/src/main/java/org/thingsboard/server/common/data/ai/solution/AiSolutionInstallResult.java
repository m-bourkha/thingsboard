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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * Per-item status report returned by the installer / uninstaller. Rendered by the UI as the
 * "Solution Installation" summary. Individual item failures do not abort the whole run.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of installing or uninstalling an AI solution")
public record AiSolutionInstallResult(
        @Schema(description = "Overall outcome: true if every item succeeded")
        boolean success,

        @Schema(description = "Per-item creation/deletion status")
        List<Item> items
) implements Serializable {

    public enum Status {
        CREATED, SKIPPED, DELETED, FAILED
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            @Schema(description = "Kind of item, e.g. DEVICE_PROFILE, DEVICE, ROLE, USER, ALARM, CALCULATED_FIELD")
            String type,

            @Schema(description = "Name of the item")
            String name,

            @Schema(description = "Outcome for this item")
            Status status,

            @Schema(description = "Details, e.g. an error message when status is FAILED")
            String message,

            @Schema(description = "Id of the created entity, when applicable")
            String entityId,

            @Schema(description = "Entity type of the created entity, when applicable")
            String entityType
    ) implements Serializable {

        public static Item created(String type, String name, String entityId, String entityType) {
            return new Item(type, name, Status.CREATED, null, entityId, entityType);
        }

        public static Item skipped(String type, String name, String message) {
            return new Item(type, name, Status.SKIPPED, message, null, null);
        }

        public static Item failed(String type, String name, String message) {
            return new Item(type, name, Status.FAILED, message, null, null);
        }

        public static Item deleted(String type, String name) {
            return new Item(type, name, Status.DELETED, null, null, null);
        }
    }

    public static AiSolutionInstallResult of(List<Item> items) {
        boolean success = items.stream().noneMatch(i -> i.status() == Status.FAILED);
        return new AiSolutionInstallResult(success, items);
    }

}
