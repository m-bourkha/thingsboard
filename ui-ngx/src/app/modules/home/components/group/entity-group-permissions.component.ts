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

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { PageLink } from '@shared/models/page/page-link';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { GroupPermissionService } from '@core/http/group-permission.service';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { forkJoin, merge, Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { EntityGroup } from '@shared/models/entity-group.model';
import { GroupPermissionInfo } from '@shared/models/group-permission.model';
import { GroupPermissionsDatasource } from '../../models/datasource/group-permissions-datasource';
import {
  AddGroupPermissionDialogComponent,
  AddGroupPermissionDialogData
} from '@home/components/group/add-group-permission-dialog.component';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { FormBuilder } from '@angular/forms';

@Component({
  selector: 'tb-entity-group-permissions',
  templateUrl: './entity-group-permissions.component.html',
  styleUrls: ['./entity-group-permissions.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class EntityGroupPermissionsComponent extends PageComponent implements AfterViewInit, OnInit, OnDestroy {

  displayedColumns = ['select', 'roleName', 'userGroupName', 'userGroupOwnerName', 'actions'];
  pageLink: PageLink;
  hidePageSize = false;
  textSearchMode = false;
  dataSource: GroupPermissionsDatasource;

  activeValue = false;
  dirtyValue = false;
  entityGroupValue: EntityGroup;

  viewsInited = false;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        if (this.viewsInited) {
          this.updateData(true);
        }
      }
    }
  }

  @Input()
  set entityGroup(group: EntityGroup) {
    if (this.entityGroupValue !== group) {
      this.entityGroupValue = group;
      if (this.viewsInited) {
        this.resetSortAndFilter(this.activeValue);
        if (!this.activeValue) {
          this.dirtyValue = true;
        }
      }
    }
  }

  @ViewChild('searchInput') searchInputField: ElementRef;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  textSearch = this.fb.control('', { nonNullable: true });

  private widgetResize$: ResizeObserver;
  private destroy$ = new Subject<void>();

  constructor(
    protected store: Store<AppState>,
    private groupPermissionService: GroupPermissionService,
    public translate: TranslateService,
    public dialog: MatDialog,
    private dialogService: DialogService,
    private cd: ChangeDetectorRef,
    private elementRef: ElementRef,
    private fb: FormBuilder,
    private zone: NgZone
  ) {
    super(store);
    this.dirtyValue = !this.activeValue;
    const sortOrder: SortOrder = { property: 'roleName', direction: Direction.ASC };
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new GroupPermissionsDatasource(this.groupPermissionService);
  }

  ngOnInit(): void {
    this.widgetResize$ = new ResizeObserver(() => {
      this.zone.run(() => {
        const showHidePageSize = this.elementRef.nativeElement.offsetWidth < hidePageSizePixelValue;
        if (showHidePageSize !== this.hidePageSize) {
          this.hidePageSize = showHidePageSize;
          this.cd.markForCheck();
        }
      });
    });
    this.widgetResize$.observe(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((prev, current) => (this.pageLink.textSearch ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.paginator.pageIndex = 0;
      this.pageLink.textSearch = value.trim();
      this.updateData();
    });

    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(this.sort.sortChange, this.paginator.page).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateData());

    this.viewsInited = true;
    if (this.activeValue && this.entityGroupValue) {
      this.updateData(true);
    }
  }

  updateData(reload: boolean = false): void {
    this.pageLink.page = this.paginator.pageIndex;
    this.pageLink.pageSize = this.paginator.pageSize;
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadPermissions(this.entityGroupValue.id.id, this.pageLink, reload);
  }

  enterFilterMode(): void {
    this.textSearchMode = true;
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode(): void {
    this.textSearchMode = false;
    this.textSearch.reset();
  }

  resetSortAndFilter(update: boolean = true): void {
    this.pageLink.textSearch = null;
    this.textSearch.reset('', { emitEvent: false });
    this.paginator.pageIndex = 0;
    const sortable = this.sort.sortables.get('roleName');
    this.sort.active = sortable.id;
    this.sort.direction = 'asc';
    if (update) {
      this.updateData(true);
    }
  }

  reload(): void {
    this.updateData(true);
  }

  addPermission($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddGroupPermissionDialogComponent, AddGroupPermissionDialogData, boolean>(
      AddGroupPermissionDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { entityGroup: this.entityGroupValue }
      }
    ).afterClosed().subscribe(result => {
      if (result) {
        this.reload();
      }
    });
  }

  deletePermission($event: Event, permission: GroupPermissionInfo): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('groupPermission.delete-title'),
      this.translate.instant('groupPermission.delete-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(result => {
      if (result) {
        this.groupPermissionService.deleteGroupPermission(permission.id.id).subscribe(() => this.reload());
      }
    });
  }

  deletePermissions($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.dataSource.selection.selected.length > 0) {
      this.dialogService.confirm(
        this.translate.instant('groupPermission.delete-permissions-title',
          { count: this.dataSource.selection.selected.length }),
        this.translate.instant('groupPermission.delete-permissions-text'),
        this.translate.instant('action.no'),
        this.translate.instant('action.yes'),
        true
      ).subscribe(result => {
        if (result) {
          const tasks: Observable<any>[] = this.dataSource.selection.selected.map(
            perm => this.groupPermissionService.deleteGroupPermission(perm.id.id)
          );
          forkJoin(tasks).subscribe(() => this.reload());
        }
      });
    }
  }
}
