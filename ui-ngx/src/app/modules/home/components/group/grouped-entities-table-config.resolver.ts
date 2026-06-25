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

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import {
  CellActionDescriptor,
  GroupActionDescriptor,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityType, entityTypeTranslations, entityTypeResources } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import {
  defaultEntityGroupColumns,
  EntityGroup
} from '@shared/models/entity-group.model';
import {
  buildEntityGroupTableColumns,
  entityGroupTableSortOrder
} from '@home/components/group/entity-group-table-columns.utils';
import { DeviceTableHeaderComponent } from '@home/pages/device/device-table-header.component';
import { DeviceComponent } from '@home/pages/device/device.component';
import { DeviceTabsComponent } from '@home/pages/device/device-tabs.component';
import { MatDialog } from '@angular/material/dialog';
import { DeviceWizardDialogComponent } from '@home/components/wizard/device-wizard-dialog.component';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { Device, DeviceInfo } from '@shared/models/device.models';
import { DeviceService } from '@core/http/device.service';
import { DialogService } from '@core/services/dialog.service';
import { EntityId } from '@shared/models/id/entity-id';

@Injectable()
export class GroupedEntitiesTableConfigResolver implements Resolve<EntityTableConfig<BaseData<HasId>>> {

  constructor(
    private entityGroupService: EntityGroupService,
    private deviceService: DeviceService,
    private dialogService: DialogService,
    private translate: TranslateService,
    private datePipe: DatePipe,
    private dialog: MatDialog
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<BaseData<HasId>>> {
    const entityGroupId: string = route.params.entityGroupId;
    const entityType = route.data.entityType as EntityType;

    const config = new EntityTableConfig<BaseData<HasId>>();
    config.entityType = entityType;
    config.entityTranslations = entityTypeTranslations.get(entityType);
    config.entityResources = entityTypeResources.get(entityType);
    config.addEnabled = false;
    config.entitiesDeleteEnabled = false;
    config.deleteEnabled = () => false;
    config.detailsPanelEnabled = false;

    if (entityType === EntityType.DEVICE) {
      config.componentsData = {
        deviceInfoFilter: {}
      };
      config.headerComponent = DeviceTableHeaderComponent;
      config.addEnabled = true;
      config.addEntity = () => this.addDeviceToGroup(entityGroupId);

      // Enable the device details side panel, like the All devices tab.
      config.detailsPanelEnabled = true;
      config.entityComponent = DeviceComponent;
      config.entityTabsComponent = DeviceTabsComponent;
      config.loadEntity = id => this.deviceService.getDeviceInfo(id.id);
      config.saveEntity = device => this.deviceService.saveDevice(device as Device).pipe(
        mergeMap(savedDevice => this.deviceService.getDeviceInfo(savedDevice.id.id)));

      // Per-row and bulk "Remove from group" actions. The enabled group action also
      // surfaces the selection checkboxes column.
      config.cellActionDescriptors = this.configureCellActions(config, entityGroupId);
      config.groupActionDescriptors = this.configureGroupActions(config, entityGroupId);
    }

    config.entitiesFetchFunction = pageLink =>
      this.entityGroupService.getEntitiesByGroup(entityGroupId, pageLink,
        config.componentsData?.deviceInfoFilter?.deviceProfileId?.id);

    return this.entityGroupService.getEntityGroup(entityGroupId).pipe(
      map(group => {
        config.tableTitle = config.entityTranslations?.typePlural
          ? `${group.name}: ${this.translate.instant(config.entityTranslations.typePlural)}`
          : group.name;
        this.buildColumns(config, entityType, group);
        return config;
      })
    );
  }

  private addDeviceToGroup(entityGroupId: string): Observable<Device> {
    return this.dialog.open<DeviceWizardDialogComponent, AddEntityDialogData<BaseData<HasId>>, Device>(
      DeviceWizardDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
      }).afterClosed().pipe(
      mergeMap((device) => {
        if (device) {
          return this.entityGroupService.addEntitiesToGroup(entityGroupId, [device.id]).pipe(map(() => device));
        }
        return of(null);
      })
    );
  }

  private configureCellActions(config: EntityTableConfig<BaseData<HasId>>,
                               entityGroupId: string): Array<CellActionDescriptor<BaseData<HasId>>> {
    return [
      {
        name: this.translate.instant('entityGroup.remove-from-group'),
        icon: 'group_remove',
        isEnabled: () => true,
        onAction: ($event, entity) => this.removeFromGroup($event, config, entityGroupId, entity as DeviceInfo)
      }
    ];
  }

  private configureGroupActions(config: EntityTableConfig<BaseData<HasId>>,
                                entityGroupId: string): Array<GroupActionDescriptor<BaseData<HasId>>> {
    return [
      {
        name: this.translate.instant('entityGroup.remove-from-group'),
        icon: 'group_remove',
        isEnabled: true,
        onAction: ($event, entities) =>
          this.removeDevicesFromGroup($event, config, entityGroupId, entities as DeviceInfo[])
      }
    ];
  }

  private removeFromGroup($event: Event, config: EntityTableConfig<BaseData<HasId>>,
                          entityGroupId: string, device: DeviceInfo): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('entityGroup.remove-device-from-group-title', {deviceName: device.name}),
      this.translate.instant('entityGroup.remove-device-from-group-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
      if (res) {
        this.entityGroupService.removeEntitiesFromGroup(entityGroupId, [device.id]).subscribe(
          () => config.updateData()
        );
      }
    });
  }

  private removeDevicesFromGroup($event: Event, config: EntityTableConfig<BaseData<HasId>>,
                                 entityGroupId: string, devices: DeviceInfo[]): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('entityGroup.remove-devices-from-group-title', {count: devices.length}),
      this.translate.instant('entityGroup.remove-devices-from-group-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
      if (res) {
        const entityIds: EntityId[] = devices.map(device => device.id);
        this.entityGroupService.removeEntitiesFromGroup(entityGroupId, entityIds).subscribe(
          () => config.updateData()
        );
      }
    });
  }

  private buildColumns(config: EntityTableConfig<BaseData<HasId>>, entityType: EntityType, group: EntityGroup): void {
    const savedColumns = group?.configuration?.columns ?? [];
    // Use the same default columns the Columns tab seeds with, so the tab and grid stay consistent.
    const configuredColumns = savedColumns.length ? savedColumns : defaultEntityGroupColumns(entityType);

    config.columns.push(...buildEntityGroupTableColumns(configuredColumns, entityType, this.datePipe));

    const sortOrder = entityGroupTableSortOrder(configuredColumns);
    if (sortOrder) {
      config.defaultSortOrder = sortOrder;
    }
  }
}
