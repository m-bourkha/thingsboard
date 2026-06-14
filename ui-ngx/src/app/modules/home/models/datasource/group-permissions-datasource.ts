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

import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount, take, tap } from 'rxjs/operators';
import { GroupPermissionInfo } from '@shared/models/group-permission.model';
import { GroupPermissionService } from '@core/http/group-permission.service';

export class GroupPermissionsDatasource implements DataSource<GroupPermissionInfo> {

  private permissionsSubject = new BehaviorSubject<GroupPermissionInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<GroupPermissionInfo>>(emptyPageData<GroupPermissionInfo>());

  public pageData$ = this.pageDataSubject.asObservable();
  public selection = new SelectionModel<GroupPermissionInfo>(true, []);

  private allPermissions: Observable<GroupPermissionInfo[]>;

  constructor(private groupPermissionService: GroupPermissionService) {}

  connect(collectionViewer: CollectionViewer): Observable<GroupPermissionInfo[]> {
    return this.permissionsSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.permissionsSubject.complete();
    this.pageDataSubject.complete();
  }

  loadPermissions(entityGroupId: string, pageLink: PageLink, reload: boolean = false): Observable<PageData<GroupPermissionInfo>> {
    if (reload) {
      this.allPermissions = null;
    }
    const result = new ReplaySubject<PageData<GroupPermissionInfo>>();
    this.fetchPermissions(entityGroupId, pageLink).pipe(
      tap(() => this.selection.clear()),
      catchError(() => of(emptyPageData<GroupPermissionInfo>()))
    ).subscribe(pageData => {
      this.permissionsSubject.next(pageData.data);
      this.pageDataSubject.next(pageData);
      result.next(pageData);
    });
    return result;
  }

  private fetchPermissions(entityGroupId: string, pageLink: PageLink): Observable<PageData<GroupPermissionInfo>> {
    return this.getAllPermissions(entityGroupId).pipe(
      map(data => pageLink.filterData(data))
    );
  }

  private getAllPermissions(entityGroupId: string): Observable<GroupPermissionInfo[]> {
    if (!this.allPermissions) {
      this.allPermissions = this.groupPermissionService.getByEntityGroup(
        entityGroupId,
        new PageLink(1024)
      ).pipe(
        map(page => page.data),
        publishReplay(1),
        refCount()
      );
    }
    return this.allPermissions;
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.permissionsSubject.pipe(
      map(permissions => numSelected === permissions.length)
    );
  }

  isEmpty(): Observable<boolean> {
    return this.permissionsSubject.pipe(
      map(permissions => !permissions.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map(pageData => pageData.totalElements)
    );
  }

  masterToggle() {
    this.permissionsSubject.pipe(
      tap(permissions => {
        const numSelected = this.selection.selected.length;
        if (numSelected === permissions.length) {
          this.selection.clear();
        } else {
          permissions.forEach(row => this.selection.select(row));
        }
      }),
      take(1)
    ).subscribe();
  }
}
