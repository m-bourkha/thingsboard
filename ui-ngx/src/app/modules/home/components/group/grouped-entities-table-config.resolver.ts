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
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
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

@Injectable()
export class GroupedEntitiesTableConfigResolver implements Resolve<EntityTableConfig<BaseData<HasId>>> {

  constructor(
    private entityGroupService: EntityGroupService,
    private translate: TranslateService,
    private datePipe: DatePipe
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
    config.tableTitle = this.translate.instant('entityGroup.group-entities');

    config.entitiesFetchFunction = pageLink =>
      this.entityGroupService.getEntitiesByGroup(entityGroupId, pageLink);

    return this.entityGroupService.getEntityGroup(entityGroupId).pipe(
      map(group => {
        this.buildColumns(config, entityType, group);
        return config;
      })
    );
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
