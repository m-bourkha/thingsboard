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
import { EntityGroup } from '@shared/models/entity-group.model';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

@Injectable({
  providedIn: 'root'
})
export class EntityGroupService {

  constructor(private http: HttpClient) {}

  public getEntityGroup(id: string, config?: RequestConfig): Observable<EntityGroup> {
    return this.http.get<EntityGroup>(`/api/entityGroup/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupsByOwner(
    ownerId: EntityId,
    entityType: EntityType,
    pageLink?: PageLink,
    config?: RequestConfig
  ): Observable<PageData<EntityGroup>> {
    const query = pageLink ? pageLink.toQuery() : '?pageSize=1000&page=0';
    return this.http.get<PageData<EntityGroup>>(
      `/api/owner/${ownerId.entityType}/${ownerId.id}/entityGroups/${entityType}${query}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  public getEntityGroups(entityType: EntityType, pageLink?: PageLink, config?: RequestConfig): Observable<PageData<EntityGroup>> {
    const query = pageLink ? pageLink.toQuery() : '?pageSize=1000&page=0';
    return this.http.get<PageData<EntityGroup>>(
      `/api/entityGroups/${entityType}${query}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  public saveEntityGroup(entityGroup: EntityGroup, config?: RequestConfig): Observable<EntityGroup> {
    return this.http.post<EntityGroup>('/api/entityGroup', entityGroup, defaultHttpOptionsFromConfig(config));
  }

  public deleteEntityGroup(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/entityGroup/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public getEntitiesByGroup<T>(groupId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<T>> {
    return this.http.get<PageData<T>>(
      `/api/entityGroup/${groupId}/entities${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  public addEntitiesToGroup(groupId: string, entityIds: EntityId[], config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/entityGroup/${groupId}/addEntities`, entityIds, defaultHttpOptionsFromConfig(config));
  }

  public removeEntitiesFromGroup(groupId: string, entityIds: EntityId[], config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/entityGroup/${groupId}/removeEntities`, entityIds, defaultHttpOptionsFromConfig(config));
  }

  public makePublic(groupId: string, config?: RequestConfig): Observable<EntityGroup> {
    return this.http.post<EntityGroup>(`/api/entityGroup/${groupId}/makePublic`, null, defaultHttpOptionsFromConfig(config));
  }

  public makePrivate(groupId: string, config?: RequestConfig): Observable<EntityGroup> {
    return this.http.post<EntityGroup>(`/api/entityGroup/${groupId}/makePrivate`, null, defaultHttpOptionsFromConfig(config));
  }

  public changeOwner(groupId: string, ownerType: string, ownerId: string, config?: RequestConfig): Observable<EntityGroup> {
    return this.http.post<EntityGroup>(
      `/api/entityGroup/${groupId}/changeOwner/${ownerType}/${ownerId}`,
      null,
      defaultHttpOptionsFromConfig(config)
    );
  }

  public getEntityGroupsByEntity(entityType: EntityType, entityId: string, config?: RequestConfig): Observable<EntityGroup[]> {
    return this.http.get<EntityGroup[]>(`/api/entity/${entityType}/${entityId}/entityGroups`, defaultHttpOptionsFromConfig(config));
  }
}
