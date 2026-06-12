///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { ActivatedRouteSnapshot } from '@angular/router';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { Role, roleTypeTranslations } from '@shared/models/role.model';
import { RoleService } from '@core/http/role.service';
import { RoleTableHeaderComponent } from '@home/pages/role/role-table-header.component';
import { RoleComponent } from '@home/components/role/role.component';
import { RoleTabsComponent } from '@home/components/role/role-tabs.component';

@Injectable()
export class RolesTableConfigResolver {

  private readonly config: EntityTableConfig<Role> = new EntityTableConfig<Role>();

  constructor(
    private datePipe: DatePipe,
    private roleService: RoleService,
    private translate: TranslateService
  ) {
    this.config.selectionEnabled = true;
    this.config.entityType = EntityType.ROLE;
    this.config.rowPointer = true;
    this.config.entityComponent = RoleComponent;
    this.config.entityTabsComponent = RoleTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ROLE);
    this.config.entityResources = entityTypeResources.get(EntityType.ROLE);

    this.config.headerComponent = RoleTableHeaderComponent;
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.columns.push(
      new DateEntityTableColumn<Role>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<Role>('name', 'role.name', '33%'),
      new EntityTableColumn<Role>('type', 'role.type', '33%',
        role => this.translate.instant(roleTypeTranslations.get(role.type))
      ),
      new EntityTableColumn<Role>('description', 'role.description', '33%',
        role => role.description || '')
    );

    this.config.deleteEntityTitle = role => this.translate.instant('role.delete-title', {roleName: role.name});
    this.config.deleteEntityContent = () => this.translate.instant('role.delete-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('role.delete-roles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('role.delete-roles-text');

    this.config.entitiesFetchFunction = pageLink => this.roleService.getRoles(undefined, pageLink);
    this.config.loadEntity = id => this.roleService.getRole(id.id);
    this.config.saveEntity = role => this.roleService.saveRole(role);
    this.config.deleteEntity = id => this.roleService.deleteRole(id.id);
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<Role> {
    return this.config;
  }
}
