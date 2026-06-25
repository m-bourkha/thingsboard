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
import { DevicesTableConfigResolver } from '@modules/home/pages/device/devices-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { GroupedEntitiesTableConfigResolver } from '@home/components/group/grouped-entities-table-config.resolver';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import {
  entityGroupBreadcrumbResolver,
  entityGroupScopeBreadcrumbLabelFunction
} from '@modules/home/pages/customer/customer-routing.module';

export const deviceRoutes: Routes = [
  {
    path: 'devices',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      useChildrenRoutesForTabs: true,
      breadcrumb: {
        menuId: MenuId.devices
      }
    },
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'all'
      },
      {
        path: 'all',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'device.devices',
          devicesType: 'tenant',
          breadcrumb: {
            label: 'device.all',
            icon: 'devices_other'
          }
        },
        resolve: {
          entitiesTableConfig: DevicesTableConfigResolver
        }
      },
      {
        path: 'groups',
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          entityType: EntityType.DEVICE,
          title: 'entityGroup.device-groups',
          breadcrumb: {
            label: 'entityGroup.groups',
            icon: 'group_work'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              entityType: EntityType.DEVICE,
              title: 'entityGroup.device-groups'
            },
            resolve: {
              entitiesTableConfig: EntityGroupsTableConfigResolver
            }
          },
          {
            path: ':entityGroupId',
            resolve: {
              entityGroup: entityGroupBreadcrumbResolver
            },
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              breadcrumb: {
                labelFunction: entityGroupScopeBreadcrumbLabelFunction,
                icon: 'group_work'
              } as BreadCrumbConfig<any>
            },
            children: [
              {
                path: '',
                pathMatch: 'full',
                redirectTo: 'devices/all'
              },
              {
                path: 'devices/all',
                component: EntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  entityType: EntityType.DEVICE,
                  title: 'entityGroup.group-entities'
                },
                resolve: {
                  entitiesTableConfig: GroupedEntitiesTableConfigResolver
                }
              }
            ]
          }
        ]
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'devices_other'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'device.devices',
          devicesType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: DevicesTableConfigResolver
        }
      }
    ]
  }
];

const routes: Routes = [
  {
    path: 'devices',
    pathMatch: 'full',
    redirectTo: '/entities/devices'
  },
  {
    path: 'devices/:entityId',
    redirectTo: '/entities/devices/:entityId'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    DevicesTableConfigResolver
  ]
})
export class DeviceRoutingModule { }
