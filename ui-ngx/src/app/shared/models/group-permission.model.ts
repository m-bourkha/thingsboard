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
import { GroupPermissionId } from '@shared/models/id/group-permission-id';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { RoleId } from '@shared/models/id/role-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { EntityType } from '@shared/models/entity-type.models';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';

export interface GroupPermission extends BaseData<GroupPermissionId>, HasTenantId, HasVersion, ExportableEntity<GroupPermissionId> {
  tenantId?: TenantId;
  /** Must reference a group of type USER. */
  userGroupId: EntityGroupId;
  roleId: RoleId;
  /** Null for generic (non-entity-scoped) roles. */
  entityGroupId?: EntityGroupId;
  /** Denormalized; null when entityGroupId is null. */
  entityGroupType?: EntityType;
  publicPermission: boolean;
}
