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
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { GroupPermission } from '@shared/models/group-permission.model';

@Injectable({
  providedIn: 'root'
})
export class GroupPermissionService {

  constructor(private http: HttpClient) {}

  public getGroupPermission(groupPermissionId: string, config?: RequestConfig): Observable<GroupPermission> {
    return this.http.get<GroupPermission>(`/api/groupPermission/${groupPermissionId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveGroupPermission(groupPermission: GroupPermission, config?: RequestConfig): Observable<GroupPermission> {
    return this.http.post<GroupPermission>('/api/groupPermission', groupPermission, defaultHttpOptionsFromConfig(config));
  }

  public deleteGroupPermission(groupPermissionId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/groupPermission/${groupPermissionId}`, defaultHttpOptionsFromConfig(config));
  }

  public getByUserGroup(userGroupId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<GroupPermission>> {
    return this.http.get<PageData<GroupPermission>>(
      `/api/userGroup/${userGroupId}/groupPermissions${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  public getByEntityGroup(entityGroupId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<GroupPermission>> {
    return this.http.get<PageData<GroupPermission>>(
      `/api/entityGroup/${entityGroupId}/groupPermissions${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  public getByRole(roleId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<GroupPermission>> {
    return this.http.get<PageData<GroupPermission>>(
      `/api/role/${roleId}/groupPermissions${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }
}
