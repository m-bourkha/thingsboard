/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;

import java.util.List;
import java.util.UUID;

public interface EntityGroupRepository extends JpaRepository<EntityGroupEntity, UUID> {

    @Query("SELECT g FROM EntityGroupEntity g WHERE g.tenantId = :tenantId " +
           "AND g.ownerId = :ownerId AND g.entityType = :entityType " +
           "AND (:textSearch IS NULL OR ilike(g.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupEntity> findByOwnerAndType(
            @Param("tenantId") UUID tenantId,
            @Param("ownerId") UUID ownerId,
            @Param("entityType") String entityType,
            @Param("textSearch") String textSearch,
            Pageable pageable);

    EntityGroupEntity findByTenantIdAndOwnerIdAndEntityTypeAndName(
            UUID tenantId, UUID ownerId, String entityType, String name);

    @Query(value = "SELECT * FROM entity_group WHERE owner_id = :ownerId AND entity_type = :entityType AND group_all = true LIMIT 1",
           nativeQuery = true)
    EntityGroupEntity findAllGroupByOwnerAndType(
            @Param("ownerId") UUID ownerId,
            @Param("entityType") String entityType);

    @Query(value = "SELECT g.* FROM entity_group g " +
                   "INNER JOIN entity_group_relation r ON r.group_id = g.id " +
                   "WHERE g.tenant_id = :tenantId AND r.entity_id = :entityId AND r.entity_type = :entityType",
           nativeQuery = true)
    List<EntityGroupEntity> findGroupsByEntityId(
            @Param("tenantId") UUID tenantId,
            @Param("entityId") UUID entityId,
            @Param("entityType") String entityType);

    @Modifying
    @Query(value = "INSERT INTO entity_group_relation (group_id, entity_id, entity_type, created_time) " +
                   "VALUES (:groupId, :entityId, :entityType, :createdTime) " +
                   "ON CONFLICT (group_id, entity_id) DO NOTHING",
           nativeQuery = true)
    void addEntityToGroup(
            @Param("groupId") UUID groupId,
            @Param("entityId") UUID entityId,
            @Param("entityType") String entityType,
            @Param("createdTime") long createdTime);

    @Modifying
    @Query(value = "DELETE FROM entity_group_relation WHERE group_id = :groupId AND entity_id = :entityId",
           nativeQuery = true)
    void removeEntityFromGroup(
            @Param("groupId") UUID groupId,
            @Param("entityId") UUID entityId);

    @Modifying
    @Query(value = "DELETE FROM entity_group_relation WHERE entity_id = :entityId AND entity_type = :entityType " +
                   "AND group_id IN (SELECT id FROM entity_group WHERE tenant_id = :tenantId)",
           nativeQuery = true)
    void removeEntityFromAllGroups(
            @Param("tenantId") UUID tenantId,
            @Param("entityId") UUID entityId,
            @Param("entityType") String entityType);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM entity_group_relation WHERE group_id = :groupId AND entity_id = :entityId)",
           nativeQuery = true)
    boolean isEntityInGroup(
            @Param("groupId") UUID groupId,
            @Param("entityId") UUID entityId);

    @Query(value = "SELECT entity_id FROM entity_group_relation " +
                   "WHERE group_id = :groupId AND entity_type = :entityType",
           nativeQuery = true)
    List<UUID> findEntityIdsByGroupIdAndType(
            @Param("groupId") UUID groupId,
            @Param("entityType") String entityType);

    @Query("SELECT g FROM EntityGroupEntity g WHERE g.tenantId = :tenantId " +
           "AND (:textSearch IS NULL OR ilike(g.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupEntity> findByTenantId(
            @Param("tenantId") UUID tenantId,
            @Param("textSearch") String textSearch,
            Pageable pageable);
}
