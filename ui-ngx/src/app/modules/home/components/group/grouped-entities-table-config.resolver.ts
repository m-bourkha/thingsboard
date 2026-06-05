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
import { EntityTableConfig, EntityTableColumn } from '@home/models/entity/entities-table-config.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityType, entityTypeTranslations, entityTypeResources } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';

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

    if (entityType === EntityType.CUSTOMER) {
      config.columns.push(
        new EntityTableColumn<any>('title', 'customer.title', '25%', entity => entity.title),
        new EntityTableColumn<any>('email', 'contact.email', '25%', entity => entity.email),
        new EntityTableColumn<any>('country', 'contact.country', '25%', entity => entity.country),
        new EntityTableColumn<any>('city', 'contact.city', '25%', entity => entity.city)
      );
    }

    return of(config);
  }
}
