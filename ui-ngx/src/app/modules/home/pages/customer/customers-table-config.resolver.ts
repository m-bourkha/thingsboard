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

import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Customer } from '@app/shared/models/customer.model';
import { CustomerService } from '@app/core/http/customer.service';
import { CustomerComponent } from '@modules/home/pages/customer/customer.component';
import { CustomerTabsComponent } from '@home/pages/customer/customer-tabs.component';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { environment } from '@env/environment';

@Injectable()
export class CustomersTableConfigResolver  {

  private readonly config: EntityTableConfig<Customer> = new EntityTableConfig<Customer>();
  private currentEntityGroupId: string | null = null;
  private currentParentCustomerId: string | null = null;

  constructor(private customerService: CustomerService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private store: Store<AppState>,
              private entityGroupService: EntityGroupService) {

    this.config.entityType = EntityType.CUSTOMER;
    this.config.entityComponent = CustomerComponent;
    this.config.entityTabsComponent = CustomerTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.CUSTOMER);
    this.config.entityResources = entityTypeResources.get(EntityType.CUSTOMER);
    const authState = getCurrentAuthState(this.store);

    this.config.columns.push(
      new DateEntityTableColumn<Customer>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Customer>('title', 'customer.title', '25%'),
      new EntityTableColumn<Customer>('email', 'contact.email', '25%'),
      new EntityTableColumn<Customer>('country', 'contact.country', '25%'),
      new EntityTableColumn<Customer>('city', 'contact.city', '25%')
    );
    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('customer.manage-customer-users'),
        icon: 'account_circle',
        isEnabled: (customer) => !customer.additionalInfo || !customer.additionalInfo.isPublic,
        onAction: ($event, entity) => this.manageCustomerUsers($event, entity)
      },
      {
        name: this.translate.instant('customer.manage-customer-customers'),
        icon: 'supervisor_account',
        isEnabled: (customer) =>
          !!this.currentEntityGroupId
          && this.currentRecursionDepth() < environment.customerHierarchyMaxDepth
          && (!customer.additionalInfo || !customer.additionalInfo.isPublic),
        onAction: ($event, entity) => this.manageCustomerCustomers($event, entity)
      },
      {
        name: this.translate.instant('customer.manage-customer-assets'),
        nameFunction: (customer) => {
          return customer.additionalInfo && customer.additionalInfo.isPublic
          ? this.translate.instant('customer.manage-public-assets')
          : this.translate.instant('customer.manage-customer-assets');
        },
        icon: 'domain',
        isEnabled: (customer) => true,
        onAction: ($event, entity) => this.manageCustomerAssets($event, entity)
      },
      {
        name: this.translate.instant('customer.manage-customer-devices'),
        nameFunction: (customer) => {
          return customer.additionalInfo && customer.additionalInfo.isPublic
            ? this.translate.instant('customer.manage-public-devices')
            : this.translate.instant('customer.manage-customer-devices');
        },
        icon: 'devices_other',
        isEnabled: (customer) => true,
        onAction: ($event, entity) => this.manageCustomerDevices($event, entity)
      },
      {
        name: this.translate.instant('customer.manage-customer-dashboards'),
        nameFunction: (customer) => {
          return customer.additionalInfo && customer.additionalInfo.isPublic
            ? this.translate.instant('customer.manage-public-dashboards')
            : this.translate.instant('customer.manage-customer-dashboards');
        },
        icon: 'dashboard',
        isEnabled: (customer) => true,
        onAction: ($event, entity) => this.manageCustomerDashboards($event, entity)
      });
    if (authState.edgesSupportEnabled) {
      this.config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-edges'),
          nameFunction: (customer) => {
            return customer.additionalInfo && customer.additionalInfo.isPublic
              ? this.translate.instant('customer.manage-public-edges')
              : this.translate.instant('customer.manage-customer-edges');
          },
          icon: 'router',
          isEnabled: (customer) => true,
          onAction: ($event, entity) => this.manageCustomerEdges($event, entity)
        }
      );
    }

    this.config.deleteEntityTitle = customer => this.translate.instant('customer.delete-customer-title', { customerTitle: customer.title });
    this.config.deleteEntityContent = () => this.translate.instant('customer.delete-customer-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('customer.delete-customers-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('customer.delete-customers-text');

    this.config.entitiesFetchFunction = pageLink => this.customerService.getCustomers(pageLink);
    this.config.loadEntity = id => this.customerService.getCustomer(id.id);
    this.config.saveEntity = customer => this.customerService.saveCustomer(customer);
    this.config.deleteEntity = id => this.customerService.deleteCustomer(id.id);
    this.config.onEntityAction = action => this.onCustomerAction(action, this.config);
    this.config.deleteEnabled = (customer) => customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
    this.config.entitySelectionEnabled = (customer) => customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
    this.config.detailsReadonly = (customer) => customer && customer.additionalInfo && customer.additionalInfo.isPublic;
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<Customer>> | EntityTableConfig<Customer> {
    const { entityGroupId, parentCustomerId } = this.collectAncestorIds(route);
    this.currentEntityGroupId = entityGroupId;
    this.currentParentCustomerId = parentCustomerId;

    if (parentCustomerId) {
      this.config.entitiesFetchFunction = pageLink =>
        this.customerService.getCustomerCustomers(parentCustomerId, pageLink);
      this.config.saveEntity = customer =>
        this.customerService.saveSubCustomer(parentCustomerId, customer);
      this.config.addEnabled = true;
      this.config.entitiesDeleteEnabled = false;
      this.config.addActionDescriptors = [];
      return this.customerService.getCustomer(parentCustomerId).pipe(
        map(customer => {
          this.config.tableTitle = `${customer.title}: ${this.translate.instant('customer.customers')}`;
          return this.config;
        })
      );
    } else if (entityGroupId) {
      this.config.entitiesFetchFunction = pageLink =>
        this.entityGroupService.getEntitiesByGroup<Customer>(entityGroupId, pageLink);
      this.config.saveEntity = customer => this.customerService.saveCustomer(customer);
      this.config.addEnabled = true;
      this.config.entitiesDeleteEnabled = false;
      this.config.addActionDescriptors = [
        {
          name: this.translate.instant('entityGroup.add-customers'),
          icon: 'add',
          isEnabled: () => true,
          onAction: () => {
            this.homeDialogs.addCustomersToGroup(entityGroupId).subscribe(result => {
              if (result) {
                this.config.updateData();
              }
            });
          }
        }
      ];
      return this.entityGroupService.getEntityGroup(entityGroupId).pipe(
        map(group => {
          this.config.tableTitle = `${group.name}: ${this.translate.instant('customer.customers')}`;
          return this.config;
        })
      );
    } else {
      this.config.tableTitle = this.translate.instant('customer.customers');
      this.config.entitiesFetchFunction = pageLink => this.customerService.getCustomers(pageLink);
      this.config.saveEntity = customer => this.customerService.saveCustomer(customer);
      this.config.addEnabled = true;
      this.config.entitiesDeleteEnabled = true;
      this.config.addActionDescriptors = [];
      return this.config;
    }
  }

  private collectAncestorIds(route: ActivatedRouteSnapshot): { entityGroupId: string | null; parentCustomerId: string | null } {
    let cur: ActivatedRouteSnapshot | null = route;
    let entityGroupId: string | null = null;
    let parentCustomerId: string | null = null;
    while (cur) {
      if (!entityGroupId && cur.params?.entityGroupId) {
        entityGroupId = cur.params.entityGroupId;
      }
      if (!parentCustomerId && cur.params?.customerId) {
        parentCustomerId = cur.params.customerId;
      }
      cur = cur.parent;
    }
    return { entityGroupId, parentCustomerId };
  }

  private currentRecursionDepth(): number {
    // Count customer levels in the URL: a `customers` segment whose preceding `:customerId` is a real
    // customer (not a group id, i.e. the segment two back is not `groups`).
    const segments = this.router.url.split('?')[0].split('/').filter(s => !!s);
    let depth = 0;
    for (let i = 2; i < segments.length; i++) {
      if (segments[i] === 'customers' && segments[i - 2] !== 'groups') {
        depth++;
      }
    }
    return depth;
  }

  private openCustomer($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}`);
  }

  manageCustomerUsers($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/users`);
  }

  manageCustomerCustomers($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    const currentUrl = this.router.url.split('?')[0];
    let target: string;
    if (this.currentParentCustomerId) {
      // Already inside a sub-customers list (".../<cid>/customers/all"). Drill into the clicked
      // sub-customer directly (customer -> customer), without re-injecting a group segment.
      const base = currentUrl.replace(/\/all$/, '');
      target = `${base}/${customer.id.id}/customers/all`;
    } else {
      // Viewing a group's customers list (".../groups/:eg/customers/all").
      // Drill into the recursive node ".../groups/:eg/:cid/customers/all".
      const base = currentUrl.replace(/\/customers\/all$/, '');
      target = `${base}/${customer.id.id}/customers/all`;
    }
    this.router.navigateByUrl(target);
  }

  manageCustomerAssets($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/assets`);
  }

  manageCustomerDevices($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/devices`);
  }

  manageCustomerDashboards($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/dashboards`);
  }

  manageCustomerEdges($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/edgeInstances`);
  }

  onCustomerAction(action: EntityAction<Customer>, config: EntityTableConfig<Customer>): boolean {
    switch (action.action) {
      case 'open':
        this.openCustomer(action.event, action.entity);
        return true;
      case 'manageUsers':
        this.manageCustomerUsers(action.event, action.entity);
        return true;
      case 'manageCustomers':
        this.manageCustomerCustomers(action.event, action.entity);
        return true;
      case 'manageAssets':
        this.manageCustomerAssets(action.event, action.entity);
        return true;
      case 'manageDevices':
        this.manageCustomerDevices(action.event, action.entity);
        return true;
      case 'manageDashboards':
        this.manageCustomerDashboards(action.event, action.entity);
        return true;
      case 'manageEdges':
        this.manageCustomerEdges(action.event, action.entity);
        return true;
    }
    return false;
  }

}
