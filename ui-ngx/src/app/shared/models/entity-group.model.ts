///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { EntityId } from '@shared/models/id/entity-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { EntityType } from '@shared/models/entity-type.models';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';

export interface EntityGroupColumnConfiguration {
  key: string;
  label: string;
  sortable: boolean;
  defaultVisible: boolean;
}

export interface EntityGroupSettings {
  enableSearch: boolean;
  enableAdd: boolean;
  enableDelete: boolean;
  enableBulkOps: boolean;
}

export interface EntityGroupActionConfiguration {
  name: string;
  icon: string;
  type: 'OPEN' | 'RUN_JS';
  payload: any;
}

export interface EntityGroupConfiguration {
  columns: EntityGroupColumnConfiguration[];
  settings: EntityGroupSettings;
  actions: EntityGroupActionConfiguration[];
}

export interface OwnerInfo {
  ownerId: EntityId;
  ownerName?: string;
}

export interface EntityGroup extends BaseData<EntityGroupId>, HasTenantId, HasVersion, ExportableEntity<EntityGroupId> {
  tenantId?: TenantId;
  ownerId?: EntityId;
  type: EntityType;
  name: string;
  groupAll: boolean;
  publicGroup: boolean;
  configuration?: EntityGroupConfiguration;
  additionalInfo?: any;
}
