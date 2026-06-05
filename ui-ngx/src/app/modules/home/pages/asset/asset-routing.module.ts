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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { AssetsTableConfigResolver } from './assets-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { MenuId } from '@core/services/menu.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { GroupedEntitiesTableConfigResolver } from '@home/components/group/grouped-entities-table-config.resolver';

export const assetRoutes: Routes = [
  {
    path: 'assets',
    data: {
      breadcrumb: {
        menuId: MenuId.assets
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'asset.assets',
          assetsType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: AssetsTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'domain'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'asset.assets',
          assetsType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: AssetsTableConfigResolver
        }
      },
      {
        path: 'groups',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          entityType: EntityType.ASSET,
          title: 'entityGroup.asset-groups',
          breadcrumb: {
            label: 'entityGroup.asset-groups',
            icon: 'group_work'
          }
        },
        resolve: {
          entitiesTableConfig: EntityGroupsTableConfigResolver
        }
      },
      {
        path: 'groups/:entityGroupId/entities',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          entityType: EntityType.ASSET
        },
        resolve: {
          entitiesTableConfig: GroupedEntitiesTableConfigResolver
        }
      }
    ]
  }
];

const routes: Routes = [
  {
    path: 'assets',
    pathMatch: 'full',
    redirectTo: '/entities/assets'
  },
  {
    path: 'assets/:entityId',
    redirectTo: '/entities/assets/:entityId'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    AssetsTableConfigResolver
  ]
})
export class AssetRoutingModule { }
