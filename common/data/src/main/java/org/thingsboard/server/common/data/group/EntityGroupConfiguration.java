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
package org.thingsboard.server.common.data.group;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class EntityGroupConfiguration {

    private List<EntityGroupColumnConfiguration> columns;
    private EntityGroupSettings settings;
    private List<EntityGroupActionConfiguration> actions;

    @Data
    public static class EntityGroupColumnConfiguration {
        private String key;
        private String label;
        private boolean sortable;
        private boolean defaultVisible;
    }

    @Data
    public static class EntityGroupSettings {
        private boolean enableSearch;
        private boolean enableAdd;
        private boolean enableDelete;
        private boolean enableBulkOps;
    }

    @Data
    public static class EntityGroupActionConfiguration {
        private String name;
        private String icon;
        private ActionType type;
        private JsonNode payload;

        public enum ActionType {
            OPEN, RUN_JS
        }
    }
}
