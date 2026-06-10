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
import { ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, switchMap, take } from 'rxjs/operators';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { TranslateService } from '@ngx-translate/core';
import {
  checkBoxCell,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityGroup } from '@shared/models/entity-group.model';
import { EntityGroupService } from '@core/http/entity-group.service';
import { CustomerService } from '@core/http/customer.service';
import { EntityId } from '@shared/models/id/entity-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { Authority } from '@shared/models/authority.enum';
import { EntityGroupComponent } from '@home/components/group/entity-group.component';
import { EntityGroupTabsComponent } from '@home/components/group/entity-group-tabs.component';

@Injectable()
export class EntityGroupsTableConfigResolver implements Resolve<EntityTableConfig<EntityGroup>> {

  constructor(
    private store: Store<AppState>,
    private entityGroupService: EntityGroupService,
    private customerService: CustomerService,
    private translate: TranslateService,
    private router: Router
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<EntityGroup>> {
    const entityType = route.data.entityType as EntityType;
    const parentCustomerId = this.findAncestorParam(route, 'customerId');

    return this.store.pipe(
      select(selectAuthUser),
      take(1),
      map(authUser => {
        let ownerId: EntityId;
        if (parentCustomerId) {
          ownerId = new CustomerId(parentCustomerId);
        } else if (authUser.authority === Authority.CUSTOMER_USER) {
          ownerId = new CustomerId(authUser.customerId);
        } else {
          ownerId = new TenantId(authUser.tenantId);
        }

        const config = new EntityTableConfig<EntityGroup>();
        config.entityType = EntityType.ENTITY_GROUP;
        config.entityComponent = EntityGroupComponent;
        config.entityTabsComponent = EntityGroupTabsComponent;
        // Keep the tab bar visible in edit mode (default true would hide all tabs and force the
        // Details tab) so Columns/Settings/Actions stay editable in their own tabs.
        config.hideDetailsTabsOnEdit = false;
        config.entityTranslations = entityTypeTranslations.get(EntityType.ENTITY_GROUP);
        config.entityResources = entityTypeResources.get(EntityType.ENTITY_GROUP);
        config.tableTitle = this.translate.instant(`entityGroup.${entityType.toLowerCase()}-groups`);

        config.columns = [
          new EntityTableColumn<EntityGroup>('name', 'entityGroup.name', '40%',
            entity => entity.name),
          new EntityTableColumn<EntityGroup>('type', 'entityGroup.type', '20%',
            entity => entity.type),
          new EntityTableColumn<EntityGroup>('publicGroup', 'entityGroup.public', '10%',
            entity => checkBoxCell(entity.publicGroup), () => ({}), false),
        ];

        config.deleteEnabled = entity => !entity.groupAll;
        config.entitiesDeleteEnabled = true;
        config.addEnabled = true;

        config.deleteEntityTitle = entity =>
          this.translate.instant('entityGroup.delete-title', { entityGroupName: entity.name });
        config.deleteEntityContent = () =>
          this.translate.instant('entityGroup.delete-text');
        config.deleteEntitiesTitle = count =>
          this.translate.instant('entityGroup.delete-entities-title', { count });
        config.deleteEntitiesContent = () =>
          this.translate.instant('entityGroup.delete-text');

        config.entitiesFetchFunction = pageLink =>
          this.entityGroupService.getEntityGroupsByOwner(ownerId, entityType, pageLink);

        config.loadEntity = id =>
          this.entityGroupService.getEntityGroup(id.id);

        config.saveEntity = group =>
          this.entityGroupService.saveEntityGroup({ ...group, type: entityType, ownerId });

        config.deleteEntity = id =>
          this.entityGroupService.deleteEntityGroup(id.id);

        const entityTypePlural = entityType.toLowerCase() + 's';
        const openGroup = ($event: Event, entity: EntityGroup) => {
          if ($event) { $event.stopPropagation(); }
          const base = this.router.url.split('?')[0].replace(/\/groups$/, '');
          this.router.navigateByUrl(`${base}/groups/${entity.id.id}/${entityTypePlural}/all`);
        };

      config.cellActionDescriptors = [
          {
            name: this.translate.instant('entityGroup.open'),
            icon: 'list',
            isEnabled: () => true,
            onAction: ($event, entity) => openGroup($event, entity)
          },
          {
            name: this.translate.instant('entityGroup.details'),
            icon: 'edit',
            isEnabled: () => true,
            onAction: ($event, entity) => config.toggleEntityDetails($event, entity)
          }
        ];

        config.handleRowClick = ($event, entity) => {
          openGroup($event, entity);
          return true;
        };

        config.componentsData = { groupType: entityType, ownerId };

        return config;
      }),
      switchMap(config => {
        if (parentCustomerId) {
          return this.customerService.getCustomer(parentCustomerId).pipe(
            map(customer => {
              config.tableTitle =
                `${customer.title}: ${this.translate.instant(`entityGroup.${entityType.toLowerCase()}-groups`)}`;
              return config;
            })
          );
        }
        return of(config);
      })
    );
  }

  private findAncestorParam(route: ActivatedRouteSnapshot, paramName: string): string | null {
    let cur: ActivatedRouteSnapshot | null = route;
    while (cur) {
      if (cur.params?.[paramName]) {
        return cur.params[paramName];
      }
      cur = cur.parent;
    }
    return null;
  }
}
