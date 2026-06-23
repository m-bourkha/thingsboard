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

import { inject, NgModule } from '@angular/core';
import { ResolveFn, Route, RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { UsersTableConfigResolver } from '../user/users-table-config.resolver';
import { CustomersTableConfigResolver } from './customers-table-config.resolver';
import { DevicesTableConfigResolver } from '@modules/home/pages/device/devices-table-config.resolver';
import { AssetsTableConfigResolver } from '../asset/assets-table-config.resolver';
import { DashboardsTableConfigResolver } from '@modules/home/pages/dashboard/dashboards-table-config.resolver';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { dashboardBreadcumbLabelFunction, DashboardResolver } from '@home/pages/dashboard/dashboard-routing.module';
import { EdgesTableConfigResolver } from '@home/pages/edge/edges-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { MenuId } from '@core/services/menu.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { environment } from '@env/environment';
import { EntityGroupService } from '@core/http/entity-group.service';
import { CustomerService } from '@app/core/http/customer.service';
import { EntityGroup } from '@shared/models/entity-group.model';
import { Customer } from '@shared/models/customer.model';

export const entityGroupBreadcrumbResolver: ResolveFn<EntityGroup> =
  (route) => inject(EntityGroupService).getEntityGroup(route.params.entityGroupId);

export const customerBreadcrumbResolver: ResolveFn<Customer> =
  (route) => inject(CustomerService).getCustomer(route.params.customerId);

export const entityGroupScopeBreadcrumbLabelFunction: BreadCrumbLabelFunction<any> =
  ((route) => route.data.entityGroup?.name);

export const customerScopeBreadcrumbLabelFunction: BreadCrumbLabelFunction<any> =
  ((route, translate) =>
    `${route.data.customer?.title ?? ''}: ${translate.instant('customer.customers')}`);

function customerNode(depth: number): Route {
  return {
    path: ':customerId',
    resolve: {
      customer: customerBreadcrumbResolver
    },
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        labelFunction: customerScopeBreadcrumbLabelFunction,
        icon: 'supervisor_account'
      } as BreadCrumbConfig<any>
    },
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'customers/all'
      },
      {
        path: 'customers',
        component: RouterTabsComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          useChildrenRoutesForTabs: true
        },
        children: buildCustomerScopeRoutes(depth - 1, false)
      }
    ]
  };
}

function buildCustomerScopeRoutes(depth: number, isRoot: boolean): Routes {
  return [
    {
      path: 'all',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'customer.customers',
        breadcrumb: {
          ...(isRoot ? { menuId: MenuId.customers_all } : {}),
          label: 'customer.all',
          icon: 'supervisor_account'
        }
      },
      resolve: {
        entitiesTableConfig: CustomersTableConfigResolver
      }
    },
    {
      path: 'groups',
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        entityType: EntityType.CUSTOMER,
        title: 'entityGroup.customer-groups',
        breadcrumb: {
          ...(isRoot ? { menuId: MenuId.customers_groups } : {}),
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
            entityType: EntityType.CUSTOMER,
            title: 'entityGroup.customer-groups'
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
              redirectTo: 'customers/all'
            },
            {
              path: 'customers/all',
              component: EntitiesTableComponent,
              data: {
                auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                title: 'customer.customers'
              },
              resolve: {
                entitiesTableConfig: CustomersTableConfigResolver
              }
            },
            ...(depth > 0 ? [customerNode(depth)] : [])
          ]
        }
      ]
    },
    ...(depth > 0 && !isRoot ? [customerNode(depth)] : [])
  ];
}

const routes: Routes = [
  {
    path: 'customers',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN],
      breadcrumb: {
        menuId: MenuId.customers
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN],
          redirectTo: '/customers/all'
        }
      },
      ...buildCustomerScopeRoutes(environment.customerHierarchyMaxDepth, true),
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'supervisor_account'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN],
          title: 'customer.customers'
        },
        resolve: {
          entitiesTableConfig: CustomersTableConfigResolver
        }
      },
      {
        path: ':customerId/users',
        component: RouterTabsComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          useChildrenRoutesForTabs: true,
          breadcrumb: {
            label: 'user.customer-users',
            icon: 'account_circle'
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
              auth: [Authority.TENANT_ADMIN],
              title: 'user.customer-users',
              breadcrumb: {
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
              auth: [Authority.TENANT_ADMIN],
              title: 'user.customer-users'
            },
            resolve: {
              entitiesTableConfig: UsersTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/devices',
        data: {
          breadcrumb: {
            label: 'customer.devices',
            icon: 'devices_other'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.devices',
              devicesType: 'customer'
            },
            resolve: {
              entitiesTableConfig: DevicesTableConfigResolver
            }
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
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.devices',
              devicesType: 'customer'
            },
            resolve: {
              entitiesTableConfig: DevicesTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/assets',
        data: {
          breadcrumb: {
            label: 'customer.assets',
            icon: 'domain'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.assets',
              assetsType: 'customer'
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
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.assets',
              assetsType: 'customer'
            },
            resolve: {
              entitiesTableConfig: AssetsTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/edgeInstances',
        data: {
          breadcrumb: {
            label: 'customer.edges',
            icon: 'router'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.edges',
              edgesType: 'customer'
            },
            resolve: {
              entitiesTableConfig: EdgesTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'router'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.edges',
              edgesType: 'customer'
            },
            resolve: {
              entitiesTableConfig: EdgesTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/dashboards',
        data: {
          breadcrumb: {
            label: 'customer.dashboards',
            icon: 'dashboard'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.dashboards',
              dashboardsType: 'customer'
            },
            resolve: {
              entitiesTableConfig: DashboardsTableConfigResolver
            }
          },
          {
            path: ':dashboardId',
            component: DashboardPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: dashboardBreadcumbLabelFunction,
                icon: 'dashboard'
              } as BreadCrumbConfig<DashboardPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'customer.dashboard',
              widgetEditMode: false
            },
            resolve: {
              dashboard: DashboardResolver
            }
          }
        ]
      },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    CustomersTableConfigResolver
  ]
})
export class CustomerRoutingModule { }
