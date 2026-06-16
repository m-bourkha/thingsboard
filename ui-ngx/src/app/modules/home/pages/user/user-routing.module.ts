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
import { UsersTableConfigResolver } from '@modules/home/pages/user/users-table-config.resolver';
import { Authority } from '@shared/models/authority.enum';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { EntityType } from '@shared/models/entity-type.models';
import { MenuId } from '@core/services/menu.models';
import {
  entityGroupBreadcrumbResolver,
  entityGroupScopeBreadcrumbLabelFunction
} from '@modules/home/pages/customer/customer-routing.module';

const routes: Routes = [
  {
    path: 'users',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN],
      breadcrumb: {
        menuId: MenuId.users
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN],
          redirectTo: '/users/all'
        }
      },
      {
        path: 'all',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'user.users',
          breadcrumb: {
            menuId: MenuId.users_all,
            label: 'user.all',
            icon: 'account_circle'
          }
        },
        resolve: {
          entitiesTableConfig: UsersTableConfigResolver
        }
      },
      {
        path: 'groups',
        data: {
          auth: [Authority.TENANT_ADMIN],
          entityType: EntityType.USER,
          title: 'entityGroup.user-groups',
          breadcrumb: {
            menuId: MenuId.users_groups,
            label: 'entityGroup.groups',
            icon: 'group_work'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              entityType: EntityType.USER,
              title: 'entityGroup.user-groups'
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
              auth: [Authority.TENANT_ADMIN],
              breadcrumb: {
                labelFunction: entityGroupScopeBreadcrumbLabelFunction,
                icon: 'group_work'
              } as BreadCrumbConfig<any>
            },
            children: [
              {
                path: '',
                pathMatch: 'full',
                redirectTo: 'users/all'
              },
              {
                path: 'users/all',
                component: EntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN],
                  title: 'user.users'
                },
                resolve: {
                  entitiesTableConfig: UsersTableConfigResolver
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
            icon: 'account_circle'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'user.user',
        },
        resolve: {
          entitiesTableConfig: UsersTableConfigResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    UsersTableConfigResolver
  ]
})
export class UserRoutingModule { }
