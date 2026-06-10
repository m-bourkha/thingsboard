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

import { DatePipe } from '@angular/common';
import {
  DateEntityTableColumn,
  EntityTableColumn
} from '@home/models/entity/entities-table-config.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import {
  EntityGroupColumnConfiguration,
  EntityGroupColumnType,
  EntityGroupSortOrder,
  entityGroupEntityFieldsByType
} from '@shared/models/entity-group.model';

function columnTitle(columnConfig: EntityGroupColumnConfiguration, entityType: EntityType): string {
  if (columnConfig.title && columnConfig.title.trim().length) {
    return columnConfig.title;
  }
  if (columnConfig.type === EntityGroupColumnType.ENTITY_FIELD) {
    const field = entityGroupEntityFieldsByType(entityType).find(f => f.value === columnConfig.key);
    return field ? field.name : columnConfig.key;
  }
  return columnConfig.key;
}

/**
 * Builds the entity-table columns for a group's entity grid from the group's saved column configuration.
 * Shared by the customers table resolver and the generic grouped-entities resolver so the Columns tab,
 * the customers table and the devices/assets grids all render the same configured columns.
 */
export function buildEntityGroupTableColumns(columns: EntityGroupColumnConfiguration[],
                                             entityType: EntityType,
                                             datePipe: DatePipe): EntityTableColumn<any>[] {
  const width = `${Math.max(10, Math.floor(100 / Math.max(1, columns.length)))}%`;
  return columns.map(columnConfig => {
    const title = columnTitle(columnConfig, entityType);
    let column: EntityTableColumn<any>;
    if (columnConfig.type === EntityGroupColumnType.ENTITY_FIELD && columnConfig.key === 'createdTime') {
      column = new DateEntityTableColumn<any>('createdTime', title, datePipe, width);
    } else if (columnConfig.type === EntityGroupColumnType.ENTITY_FIELD) {
      const key = columnConfig.key;
      column = new EntityTableColumn<any>(key, title, width, entity => entity[key] ?? '');
    } else {
      // Attribute / latest-telemetry values are not available on the fetched entity yet.
      column = new EntityTableColumn<any>(columnConfig.key, title, width, () => '');
    }
    column.ignoreTranslate = true;
    column.mobileHide = !!columnConfig.mobileHide;
    return column;
  });
}

/**
 * Resolves the default sort order for a group's entity grid: the first column whose sort order is not NONE.
 */
export function entityGroupTableSortOrder(columns: EntityGroupColumnConfiguration[]): SortOrder | undefined {
  const sortColumn = columns.find(
    columnConfig => columnConfig.sortOrder && columnConfig.sortOrder !== EntityGroupSortOrder.NONE);
  if (!sortColumn) {
    return undefined;
  }
  return {
    property: sortColumn.key,
    direction: sortColumn.sortOrder === EntityGroupSortOrder.ASC ? Direction.ASC : Direction.DESC
  };
}
