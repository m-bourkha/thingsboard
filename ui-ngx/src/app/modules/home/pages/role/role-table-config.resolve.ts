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
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { ActivatedRouteSnapshot } from '@angular/router';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { Role, roleTypeTranslations } from '@shared/models/role.model';
import { RoleService } from '@core/http/role.service';
import { RoleTableHeaderComponent } from '@home/pages/role/role-table-header.component';
import { RoleDialogComponent, RoleDialogData } from '@home/components/role/role-dialog.component';

@Injectable()
export class RolesTableConfigResolver {

  private readonly config: EntityTableConfig<Role> = new EntityTableConfig<Role>();

  constructor(
    private datePipe: DatePipe,
    private roleService: RoleService,
    private translate: TranslateService,
    private dialog: MatDialog
  ) {
    this.config.selectionEnabled = true;
    this.config.entityType = EntityType.ROLE;
    this.config.rowPointer = true;
    this.config.detailsPanelEnabled = false;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ROLE);
    this.config.entityResources = entityTypeResources.get(EntityType.ROLE);

    this.config.headerComponent = RoleTableHeaderComponent;
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.addEntity = () => this.roleDialog(null, true);

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

    this.config.deleteEntity = id => this.roleService.deleteRole(id.id);

    this.config.entitiesFetchFunction = pageLink => this.roleService.getRoles(undefined, pageLink);

    this.config.cellActionDescriptors = this.configureCellActions();

    this.config.handleRowClick = ($event, role) => {
      this.editRole($event, role);
      return true;
    };
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<Role> {
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<Role>> {
    return [
      {
        name: this.translate.instant('action.edit'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.editRole($event, entity)
      }
    ];
  }

  private editRole($event, role: Role): void {
    $event?.stopPropagation();
    this.roleDialog(role, false).subscribe(res => res ? this.config.updateData() : null);
  }

  private roleDialog(role: Role, isAdd = false): Observable<Role> {
    return this.dialog.open<RoleDialogComponent, RoleDialogData, Role>(RoleDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        role
      }
    }).afterClosed();
  }
}
