# Entity Groups — Clean-Slate Design

**Status.** Design — not yet implemented. Replaces the partial `Group` / `GroupPermission` / `Role` scaffold on branch `pe-features-rbac`.
**Scope.** DEVICE, ASSET, CUSTOMER (extensible — adding USER/DASHBOARD/ENTITY_VIEW/EDGE later requires zero schema or controller change).
**Companion.** Layered patterns this design relies on are documented in [`THINGSBOARD_ARCHITECTURE.md`](./THINGSBOARD_ARCHITECTURE.md).

---

## Summary

Rebuilds ThingsBoard PE-style **Entity Groups** + **RBAC** as a fully generic, open-closed subsystem. Three independent entities — `EntityGroup` (typed, owner-scoped, polymorphic), `Role` (tenant-scoped permission set), `GroupPermission` (UserGroup × Role × EntityGroup) — plus a small set of generic infrastructure pieces:

- One polymorphic `entity_group_relation` table for multi-membership.
- An `EntityGroupQueryStrategy` registry for type-safe entity lookups (one bean per groupable type).
- An `OwnerService` walking the Customer→Tenant chain.
- An `AllEntityGroupSyncListener` materializing the per-owner "All" group via Spring event listeners.
- A single reusable Angular `<tb-entity-groups-tab>` component + resolver parameterized by `entityType` on the route.

Everything follows the layered pattern documented in `THINGSBOARD_ARCHITECTURE.md` (Controller → TbXService → XService → XServiceImpl → XDao → JPA), with full cache + evict-event + audit + version + tenant isolation + `EntityType`/`Resource`/`EntityIdFactory` registration parity with `Customer`.

User decisions baked in:
- **Scope:** DEVICE, ASSET, CUSTOMER (extensible).
- **Layer:** Grouping + Full Permissions (Role + GroupPermission).
- **Hierarchy:** Full PE-style (Tenant → Customer → sub-Customer …).
- **"All" group:** Materialized (auto-created + auto-synced via Spring event listener).

---

## 1 — Context

ThingsBoard CE has no native concept of named, multi-membership, typed entity groups, nor of role-based access bound to those groups; PE provides this but is closed-source. The fork's `pe-features-rbac` branch began an ad-hoc implementation with three entities (`Group`, `Role`, `GroupPermission`) but the design is non-generic — each new entity type would force schema and controller additions, and the join model conflates "group of devices" and "group of users". A clean-slate redesign yields a single open-closed core that satisfies the four PE feature pillars (multi-membership, typed groups, owner hierarchy, RBAC) and lets future entity types plug in via a single strategy bean.

## 2 — Domain model

All POJOs live under `common/data/src/main/java/org/thingsboard/server/common/data/group/`. Mirror reference: `Customer.java`.

### 2.1 `EntityGroup` — domain POJO

Package: `org.thingsboard.server.common.data.group`. Extends `BaseDataWithAdditionalInfo<EntityGroupId>`. Implements `HasTenantId`, `HasName`, `HasVersion`, `ExportableEntity<EntityGroupId>`.

| Field | Type | Notes |
|---|---|---|
| `tenantId` | `TenantId` | always set, used by tenant gate |
| `ownerId` | `EntityId` | TenantId or CustomerId — serialized as `{entityType, id}` |
| `type` | `EntityType` | discriminator: `DEVICE` / `ASSET` / `CUSTOMER` / future |
| `name` | `String` | `@NoXss`, `@Length(fieldName="name")` |
| `groupAll` | `boolean` | true only for the materialized "All <T>" group; non-deletable, non-renamable |
| `publicGroup` | `boolean` | anonymous read flag |
| `configuration` | `EntityGroupConfiguration` (jsonb) | column/setting/action config |
| `version` | `Long` | `@Getter @Setter`, optimistic |
| `additionalInfo` | `JsonNode` | from base |
| `externalId` | `EntityGroupId` | for export |

### 2.2 `EntityGroupConfiguration` — jsonb sub-POJO

Plain Java class (Lombok `@Data`). Fields:
- `List<EntityGroupColumnConfiguration> columns` — `{key:String, label:String, sortable:boolean, defaultVisible:boolean}`
- `EntityGroupSettings settings` — `{enableSearch, enableAdd, enableDelete, enableBulkOps}`
- `List<EntityGroupActionConfiguration> actions` — `{name, icon, type:OPEN|RUN_JS, payload:JsonNode}`

### 2.3 `EntityGroupId`

Final class extending `UUIDBased implements EntityId`. Path: `common/data/.../id/EntityGroupId.java`. `getEntityType()` returns `EntityType.ENTITY_GROUP`. Must be registered in `EntityIdFactory`.

### 2.4 `EntityGroupRelation` — join POJO (key class)

Pure POJO (no top-level entity), used for service-level membership ops + audit payloads. Fields:
- `EntityGroupId groupId`
- `EntityId entityId` — polymorphic
- `EntityType entityType` — denormalized (must match the group's type)
- `long createdTime`

Does **not** implement `HasTenantId` (not a top-level entity); inherits tenant from group. Not exportable. Audit events for add/remove are emitted by the service using `ADDED_TO_ENTITY` / `REMOVED_FROM_ENTITY` `ActionType`s with payload `{groupId, groupName}`.

### 2.5 `Role` — domain POJO

Package: `org.thingsboard.server.common.data.role`. Extends `BaseDataWithAdditionalInfo<RoleId>`. Implements `HasTenantId`, `HasName`, `HasVersion`, `ExportableEntity<RoleId>`.

Fields: `tenantId`, `name`, `type` (`RoleType.GENERIC` | `RoleType.GROUP`), `permissions` (`JsonNode` — map of `Resource → List<Operation>`), `version`, `additionalInfo`, `externalId`.

### 2.6 `RoleId`

Path: `common/data/.../id/RoleId.java`. Register in `EntityIdFactory`.

### 2.7 `GroupPermission` — domain POJO

Package: `org.thingsboard.server.common.data.permission`. Extends `BaseData<GroupPermissionId>`. Implements `HasTenantId`, `HasVersion`, `ExportableEntity<GroupPermissionId>`.

Fields:
- `tenantId`
- `userGroupId` (`EntityGroupId`) — must reference a group of `type=USER` (validated)
- `roleId` (`RoleId`)
- `entityGroupId` (`EntityGroupId`) — the entity group being granted (any type; `null` allowed only for generic role)
- `entityGroupType` (`EntityType`) — denormalized for index
- `publicPermission` (`boolean`)
- `version`
- `externalId`

### 2.8 `GroupPermissionId`

Path: `common/data/.../id/GroupPermissionId.java`. Register in `EntityIdFactory`.

### 2.9 New `EntityType` enum values

In `common/data/.../EntityType.java` add (replacing the existing scaffold entries):
- `ENTITY_GROUP(45, "entity_group")`
- `ROLE(46, "tb_role")`
- `GROUP_PERMISSION(47, "group_permission")`

## 3 — Database schema

Repo uses **plain SQL** schema files, not Liquibase. New schema goes into `dao/src/main/resources/sql/schema-entities.sql` and `dao/src/main/resources/sql/schema-entities-idx.sql`.

### 3.1 `entity_group` table

```
id uuid PK
created_time bigint NOT NULL
tenant_id uuid NOT NULL
owner_type varchar(32) NOT NULL          -- 'TENANT' | 'CUSTOMER'
owner_id uuid NOT NULL
entity_type varchar(32) NOT NULL          -- 'DEVICE' | 'ASSET' | 'CUSTOMER' | ...
name varchar(255) NOT NULL
group_all boolean NOT NULL DEFAULT false
public_group boolean NOT NULL DEFAULT false
configuration jsonb
additional_info jsonb
external_id uuid
version bigint
CONSTRAINT entity_group_unq UNIQUE (tenant_id, owner_id, entity_type, name)
CONSTRAINT entity_group_external_id_unq UNIQUE (tenant_id, external_id)
```

Partial unique index ensuring at most one "All" group per (owner, type):
```
CREATE UNIQUE INDEX entity_group_all_unq ON entity_group(owner_id, entity_type) WHERE group_all = true;
```

Lookup indexes:
- `(tenant_id, owner_id, entity_type)` — Groups tab listing
- `(tenant_id, entity_type)` — admin views

### 3.2 `entity_group_relation` table

```
group_id uuid NOT NULL
entity_id uuid NOT NULL
entity_type varchar(32) NOT NULL
created_time bigint NOT NULL
PRIMARY KEY (group_id, entity_id)
```

Indexes:
- PK serves "list entities in group"
- `(entity_id, entity_type)` — "what groups does this entity belong to"

FK `(group_id) REFERENCES entity_group(id) ON DELETE CASCADE` — drop group ⇒ relations gone.

### 3.3 `tb_role` table

```
id uuid PK
created_time bigint NOT NULL
tenant_id uuid NOT NULL
name varchar(255) NOT NULL
role_type varchar(16) NOT NULL    -- 'GENERIC' | 'GROUP'
permissions jsonb NOT NULL
additional_info jsonb
external_id uuid
version bigint
CONSTRAINT role_name_unq UNIQUE (tenant_id, name)
CONSTRAINT role_external_id_unq UNIQUE (tenant_id, external_id)
```

### 3.4 `group_permission` table

```
id uuid PK
created_time bigint NOT NULL
tenant_id uuid NOT NULL
user_group_id uuid NOT NULL
role_id uuid NOT NULL
entity_group_id uuid                       -- null = generic
entity_group_type varchar(32)              -- null when entity_group_id null
public_permission boolean NOT NULL DEFAULT false
external_id uuid
version bigint
CONSTRAINT group_perm_unq UNIQUE (tenant_id, user_group_id, role_id, entity_group_id)
```

Indexes:
- `(tenant_id, user_group_id)` — "what permissions does this user-group have"
- `(tenant_id, entity_group_id)` — "who can see this group"
- `(role_id)` — for role-change cache evictions

## 4 — Backend layers (per entity)

### 4.1 `EntityGroup`

| Layer | Path | Mirror reference |
|---|---|---|
| Domain POJO | `common/data/.../group/EntityGroup.java` | `Customer.java` |
| Jsonb POJO | `common/data/.../group/EntityGroupConfiguration.java` | `DeviceTransportConfiguration.java` |
| EntityId | `common/data/.../id/EntityGroupId.java` | `CustomerId.java` |
| JPA entity | `dao/.../model/sql/EntityGroupEntity.java` | `CustomerEntity.java` |
| Repository | `dao/.../sql/group/EntityGroupRepository.java` | `CustomerRepository.java` |
| DAO IF | `dao/.../group/EntityGroupDao.java` | `CustomerDao.java` |
| JPA DAO | `dao/.../sql/group/JpaEntityGroupDao.java` | `JpaCustomerDao.java` |
| Validator | `dao/.../service/validator/EntityGroupDataValidator.java` | `CustomerDataValidator.java` |
| Service IF | `common/dao-api/.../group/EntityGroupService.java` | `CustomerService.java` |
| Service Impl | `dao/.../group/EntityGroupServiceImpl.java` (bean `EntityGroupDaoService`) | `CustomerServiceImpl.java` |
| Cache key | `common/cache/.../group/EntityGroupCacheKey.java` (`tenantId, ownerId, entityType, name`) | `CustomerCacheKey.java` |
| Cache evict | `common/cache/.../group/EntityGroupCacheEvictEvent.java` (record) | `CustomerCacheEvictEvent.java` |
| Caffeine cfg | `common/cache/.../group/EntityGroupCaffeineCache.java` | `CustomerCaffeineCache.java` |
| Redis cfg | `common/cache/.../group/EntityGroupRedisCache.java` | `CustomerRedisCache.java` |
| Cache constant | `CacheConstants.ENTITY_GROUP_CACHE = "entityGroups"` | replaces existing `GROUP_CACHE` |
| Tb facade IF | `application/.../service/entitiy/group/TbEntityGroupService.java` | `TbCustomerService.java` |
| Tb facade impl | `application/.../service/entitiy/group/DefaultTbEntityGroupService.java` | `DefaultTbCustomerService.java` |
| Controller | `application/.../controller/EntityGroupController.java` | `CustomerController.java` |

**Controller endpoints:**

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/entityGroup/{entityGroupId}` | TENANT_ADMIN, CUSTOMER_USER | load one group |
| POST | `/api/entityGroup` | TENANT_ADMIN, CUSTOMER_USER | create/update; `type` in body |
| DELETE | `/api/entityGroup/{entityGroupId}` | TENANT_ADMIN, CUSTOMER_USER | delete (rejects `groupAll=true`) |
| GET | `/api/entityGroups/{groupType}` | TENANT_ADMIN, CUSTOMER_USER | list all groups of type for current user's owner |
| GET | `/api/owner/{ownerType}/{ownerId}/entityGroups/{groupType}` | TENANT_ADMIN | list groups for a specific owner |
| GET | `/api/entityGroup/{entityGroupId}/entities` (paged) | TENANT_ADMIN, CUSTOMER_USER | **polymorphic** — dispatches via `EntityGroupQueryStrategy` (§6) |
| POST | `/api/entityGroup/{entityGroupId}/addEntities` (body: `List<EntityId>`) | TENANT_ADMIN, CUSTOMER_USER | add memberships |
| POST | `/api/entityGroup/{entityGroupId}/removeEntities` (body: `List<EntityId>`) | TENANT_ADMIN, CUSTOMER_USER | remove memberships |
| GET | `/api/entity/{entityType}/{entityId}/entityGroups` | TENANT_ADMIN, CUSTOMER_USER | "what groups does this entity belong to" |
| POST | `/api/entityGroup/{entityGroupId}/makePublic` / `makePrivate` | TENANT_ADMIN | toggle |
| POST | `/api/entityGroup/{entityGroupId}/changeOwner/{ownerType}/{ownerId}` | TENANT_ADMIN | re-parent |

Authorization: every endpoint uses `checkEntityGroupId(id, Operation.X)` (added to `BaseController`) which loads the group, checks `Resource.ENTITY_GROUP`, then resolves the owner chain to confirm the current user has access to the owner.

### 4.2 `Role`

Mirror of §4.1 with `Role` substituted. Notable differences:
- No "All"/`groupAll` field.
- Cache key: `(tenantId, name)`.
- `CacheConstants.ROLE_CACHE = "roles"`.
- Controller endpoints:

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/role/{roleId}` | TENANT_ADMIN, CUSTOMER_USER | get role |
| POST | `/api/role` | TENANT_ADMIN | create/update |
| DELETE | `/api/role/{roleId}` | TENANT_ADMIN | delete (rejected if in use by GroupPermission) |
| GET | `/api/roles?type=GENERIC\|GROUP&pageSize=&page=` | TENANT_ADMIN | paged list |

### 4.3 `GroupPermission`

Mirror of §4.1.
- Cache key: `(tenantId, userGroupId, entityGroupId)` and a second key for `(userId → effectivePermissions)` (used by §7).
- `CacheConstants.GROUP_PERMISSION_CACHE = "groupPermissions"`.
- Controller endpoints:

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/groupPermission/{groupPermissionId}` | TENANT_ADMIN | one |
| POST | `/api/groupPermission` | TENANT_ADMIN | create/update |
| DELETE | `/api/groupPermission/{groupPermissionId}` | TENANT_ADMIN | delete |
| GET | `/api/userGroup/{userGroupId}/groupPermissions` (paged) | TENANT_ADMIN | by user-group |
| GET | `/api/entityGroup/{entityGroupId}/groupPermissions` (paged) | TENANT_ADMIN | by entity-group |
| GET | `/api/role/{roleId}/groupPermissions` (paged) | TENANT_ADMIN | by role |

### 4.4 Registrations (all three)

- `EntityType` enum: as in §2.9.
- `Resource` enum (`application/.../service/security/permission/Resource.java`): add `ENTITY_GROUP(ENTITY_GROUP)`, `ROLE(ROLE)`, `GROUP_PERMISSION(GROUP_PERMISSION)` (clean up existing scaffold entries).
- `EntityIdFactory`: register all three IDs.
- `TenantAdminPermissions`: `put(Resource.ENTITY_GROUP, tenantEntityPermissionChecker)`, same for `ROLE`, `GROUP_PERMISSION`.
- `CustomerUserPermissions`: read-only checker for `ENTITY_GROUP` and `ROLE`; no access to `GROUP_PERMISSION`.
- `SysAdminPermissions`: no entries (tenant-scoped).

## 5 — "All <EntityType>" auto-materialization

### 5.1 Component

Single Spring `@Component`: `application/.../service/entitiy/group/AllEntityGroupSyncListener.java`.

```
class AllEntityGroupSyncListener {
   @TransactionalEventListener(SaveEntityEvent.class)   onSave(...)
   @TransactionalEventListener(DeleteEntityEvent.class) onDelete(...)
}
```

### 5.2 Behavior

- Filter on `event.getEntityId().getEntityType() ∈ EntityGroupTypes.GROUPABLE` — a static `Set<EntityType>` (today: `DEVICE, ASSET, CUSTOMER`; future types add to this set in one place).
- On save:
  1. Resolve `ownerId = OwnerService.getOwner(entity)` (Device/Asset → `customerId` if set else `tenantId`; Customer → `parentCustomerId` if set else `tenantId`).
  2. `entityGroupService.findOrCreateAllGroup(tenantId, ownerId, entityType)` — idempotent upsert using the partial unique index; on conflict falls back to lookup.
  3. `entityGroupService.addEntityToGroup(allGroup.getId(), entityId)` — idempotent (PK on `(group_id, entity_id)` makes the insert ignore-on-conflict).
  4. On owner change (`oldEntity != null && oldEntity.ownerId != entity.ownerId`): remove from old "All" group and add to new — also re-evaluate explicit groups (only remove from groups whose owner is no longer an ancestor of the new owner).
- On delete: remove all `entity_group_relation` rows for the entity (single `DELETE WHERE entity_id = ?`).

### 5.3 Seam

Public methods added to `EntityGroupService`:
- `EntityGroup findOrCreateAllGroup(TenantId, EntityId ownerId, EntityType)`
- `void addEntityToGroup(EntityGroupId, EntityId)` (idempotent)
- `void removeEntityFromGroup(EntityGroupId, EntityId)`
- `void removeEntityFromAllGroups(TenantId, EntityId)`

This keeps Device/Asset/Customer service code unchanged.

### 5.4 Concurrency

The first-save race for the "All" group is handled by the partial unique index `entity_group_all_unq`: any second concurrent insert gets `DataIntegrityViolationException`, caught and re-queried.

## 6 — Polymorphic entity-membership queries

### 6.1 Strategy interface

`dao/.../group/EntityGroupQueryStrategy.java`:

```
interface EntityGroupQueryStrategy<E extends HasId<?>> {
    EntityType supportedType();
    PageData<E> findEntitiesInGroup(TenantId tenantId, EntityGroupId groupId, PageLink pageLink);
}
```

### 6.2 Concrete strategies (one per groupable type)

- `DeviceEntityGroupQueryStrategy` — joins `device d ON d.id = egr.entity_id WHERE egr.group_id = ? AND egr.entity_type = 'DEVICE'`, returns `PageData<Device>`. Repository method: `DeviceRepository.findByEntityGroupId(...)`.
- `AssetEntityGroupQueryStrategy` — same for `asset`.
- `CustomerEntityGroupQueryStrategy` — same for `customer`.

Each strategy is a `@Component` constructor-injected with the entity-type's existing repository.

### 6.3 Registry

`EntityGroupQueryStrategyRegistry` — a `@Component` that builds `Map<EntityType, EntityGroupQueryStrategy<?>>` from all `EntityGroupQueryStrategy` beans on Spring startup (via `@Autowired List<EntityGroupQueryStrategy<?>>`). Single `dispatch` method:

```
@SuppressWarnings("unchecked")
public <E> PageData<E> findEntitiesInGroup(EntityGroup g, PageLink pl) {
   var strat = (EntityGroupQueryStrategy<E>) registry.get(g.getType());
   if (strat == null) throw new IllegalStateException("No strategy for " + g.getType());
   return strat.findEntitiesInGroup(g.getTenantId(), g.getId(), pl);
}
```

### 6.4 Controller dispatch surface

`EntityGroupController.getEntitiesInGroup(...)` returns `PageData<?>` (Jackson serializes polymorphically by type). The strategy registry is the *only* place new entity types touch the codebase backend-side.

### 6.5 Performance note

Per-type typed SQL means the query planner uses each entity table's indexes natively — no polymorphic SQL with `CASE`/`UNION`. The `(group_id, entity_id)` PK on `entity_group_relation` makes the join driver-side cheap.

## 7 — Hierarchy & permission resolution

### 7.1 `OwnerService`

Interface in `common/dao-api/.../owner/OwnerService.java`, impl in `dao/.../owner/OwnerServiceImpl.java`. Methods:
- `List<EntityId> getOwnerHierarchy(TenantId, EntityId)` — returns `[entityId, ownerId, parentOwnerId, ..., tenantId]`.
- `EntityId getOwner(TenantId, EntityId)` — single step.

Walks via:
- Device/Asset → `customerId` (if null → tenant) — uses existing `CustomerService.findCustomerById` chain.
- Customer → `parentCustomerId` → ... → `null` ⇒ tenant.
- EntityGroup → its `ownerId`.

Cache (`OWNER_HIERARCHY_CACHE`, key = `EntityId`): 60s TTL or evict on `SaveEntityEvent` whose oldEntity owner differs.

### 7.2 `GroupPermissionService.resolveUserPermissions(UserId)`

Returns `MergedUserPermissions` POJO:
```
class MergedUserPermissions {
   Map<Resource, Set<Operation>> genericPermissions;
   Map<EntityGroupId, Set<Operation>> groupSpecificPermissions;
   Set<EntityGroupId> readableGroups;
}
```

Algorithm:
1. Find all user-groups containing the user (via `entity_group_relation` rows where `entity_type='USER'`).
2. Join `group_permission` on `user_group_id IN (...)` → list of `(roleId, entityGroupId)`.
3. Load roles (cached); merge `permissions` jsonb per role-type into the right bucket.
4. For each granted `entityGroupId`: walk its owner chain via `OwnerService` — every entity whose owner appears in that chain inherits the group's role.

### 7.3 Cache

- `USER_PERMISSIONS_CACHE` (new), key = `UserId`.
- Evict on: any `SaveEntityEvent`/`DeleteEntityEvent` for `GROUP_PERMISSION`, `ROLE`, `USER` (group change), `CUSTOMER` (parent change), or `EntityGroupRelation` change involving a user-group.
- Single `@TransactionalEventListener` in `UserPermissionsCacheEvictListener` handles all five sources by inspecting `entityType`.

### 7.4 Access-control hook

`DefaultAccessControlService.getPermissionChecker(user, resource)` consults `MergedUserPermissions` when the resource is `DEVICE`/`ASSET`/`CUSTOMER`/`ENTITY_GROUP`. For a fetched entity it asks: "is entity's owner in the chain of any group the user has the relevant operation on?". Existing `tenantEntityPermissionChecker` semantics remain as the fallback; group-permission layer sits above it.

## 8 — Frontend shared infrastructure

### 8.1 Models (`ui-ngx/src/app/shared/models/`)

- `entity-group.model.ts` — `EntityGroup` interface, `EntityGroupConfiguration`, `EntityGroupColumnConfiguration`, `EntityGroupSettings`, `EntityGroupActionConfiguration`, `OwnerInfo`.
- `role.model.ts` — `Role`, `RoleType`, `RolePermissionMap`.
- `group-permission.model.ts` — `GroupPermission`.

Register all three in `entity-type.models.ts` (`EntityType.ENTITY_GROUP`, `ROLE`, `GROUP_PERMISSION` + `entityTypeResources` + `entityTypeTranslations`).

### 8.2 HTTP services (`ui-ngx/src/app/core/http/`)

- `entity-group.service.ts` — `getEntityGroupsByOwner(ownerId, type)`, `getEntityGroup(id)`, `saveEntityGroup(g)`, `deleteEntityGroup(id)`, `getEntitiesByGroup<T>(groupId, pageLink)`, `addEntitiesToGroup(groupId, ids[])`, `removeEntitiesFromGroup(groupId, ids[])`, `makePublic(id)`, `makePrivate(id)`, `changeOwner(id, ownerId)`.
- `role.service.ts` — CRUD + `getRoles(type, pageLink)`.
- `group-permission.service.ts` — CRUD + `getByUserGroup`, `getByEntityGroup`, `getByRole`.

Mirror reference: `customer.service.ts`.

### 8.3 Reusable Groups-tab component

Path: `ui-ngx/src/app/modules/home/components/group/entity-groups-tab.component.ts`.

```
@Component({ selector: 'tb-entity-groups-tab', ... })
class EntityGroupsTabComponent implements OnInit {
  @Input() entityType: EntityType;       // alt: read from ActivatedRoute.data.entityType
  @Input() ownerId: EntityId;             // current Tenant or Customer
  groups$: Observable<PageData<EntityGroup>>;
  ngOnInit(): void {
     this.entityType ??= this.route.snapshot.data.entityType;
     this.groups$ = this.entityGroupService
        .getEntityGroupsByOwner(this.ownerId, this.entityType, this.pageLink);
  }
  onOpenGroup(g) { router.navigate([`/${urlFor(this.entityType)}/groups`, g.id.id, 'entities']); }
}
```

Reads `entityType` from `@Input` (preferred) or `route.data.entityType` (fallback when used as a routed page). Reuses `EntitiesTableComponent` for rendering by building an `EntityTableConfig<EntityGroup>` in-component (or via the resolver in §8.4).

### 8.4 Shared resolver `EntityGroupsTableConfigResolver`

Path: `ui-ngx/src/app/modules/home/components/group/entity-groups-table-config.resolver.ts`.

```
@Injectable()
class EntityGroupsTableConfigResolver implements Resolve<EntityTableConfig<EntityGroup>> {
  resolve(route: ActivatedRouteSnapshot) {
     const entityType = route.data.entityType as EntityType;
     const ownerId    = resolveOwnerFromRoute(route);                 // tenant or customer
     const cfg = new EntityTableConfig<EntityGroup>();
     cfg.entityType         = EntityType.ENTITY_GROUP;
     cfg.entityComponent    = EntityGroupComponent;
     cfg.entityTranslations = entityTypeTranslations.get(EntityType.ENTITY_GROUP);
     cfg.entityResources    = entityTypeResources.get(EntityType.ENTITY_GROUP);
     cfg.componentsData     = { groupType: entityType, ownerId };
     cfg.columns.push(new EntityTableColumn('name', 'entityGroup.name', '40%'),
                      new EntityTableColumn('type', 'entityGroup.type', '20%'),
                      new BooleanEntityTableColumn('publicGroup', 'entityGroup.public', '10%'));
     cfg.entitiesFetchFunction = pl => svc.getEntityGroupsByOwner(ownerId, entityType, pl);
     cfg.loadEntity            = id => svc.getEntityGroup(id.id);
     cfg.saveEntity            = g  => svc.saveEntityGroup({ ...g, type: entityType, ownerId });
     cfg.deleteEntity          = id => svc.deleteEntityGroup(id.id);
     cfg.deleteEnabled         = g  => !g.groupAll;
     cfg.cellActionDescriptors.push({ name: 'entityGroup.open', icon: 'list',
        onAction: (e, g) => router.navigate([...]) });
     return cfg;
  }
}
```

Provided **once** in `HomeComponentsModule` (not per feature module, since it's reused).

### 8.5 Drill-down list view

Route per groupable kind:
```
{ path: 'groups/:entityGroupId/entities',
  component: EntitiesTableComponent,
  data: { entityType: EntityType.DEVICE },
  resolve: { entitiesTableConfig: GroupedEntitiesTableConfigResolver } }
```

`GroupedEntitiesTableConfigResolver` (new shared) reads `entityType` + `entityGroupId` from the route, then **picks** the existing per-type table config (`DevicesTableConfigResolver`, `AssetsTableConfigResolver`, `CustomersTableConfigResolver`) via an injected `Map<EntityType, EntityTableConfigResolver>` and replaces its `entitiesFetchFunction` with `entityGroupService.getEntitiesByGroup(groupId, pl)`. Columns, EntityComponent, delete dialog text are inherited from the per-type resolver — drill-down view looks identical to the normal list view.

## 9 — Frontend per-entity wiring (minimal)

For each of `device-routing.module.ts`, `asset-routing.module.ts`, `customer-routing.module.ts`:

**Imports** (one line each):
```
import { EntityGroupsTableConfigResolver, GroupedEntitiesTableConfigResolver }
  from '@home/components/group/...';
import { EntityType } from '@shared/models/entity-type.models';
```

**Routes** (two child routes added under the existing list route):
```
{ path: 'groups',
  component: EntitiesTableComponent,
  data: { entityType: EntityType.DEVICE, title: 'entityGroup.device-groups',
          breadcrumb: { label: 'entityGroup.device-groups', icon: 'group_work' } },
  resolve: { entitiesTableConfig: EntityGroupsTableConfigResolver } },
{ path: 'groups/:entityGroupId/entities',
  component: EntitiesTableComponent,
  data: { entityType: EntityType.DEVICE },
  resolve: { entitiesTableConfig: GroupedEntitiesTableConfigResolver } }
```

**Module providers**: nothing — both resolvers are provided in `HomeComponentsModule`.

Same diff for Asset (`EntityType.ASSET`) and Customer (`EntityType.CUSTOMER`). Three lines of routing per module — that is the entire per-type cost.

## 10 — Permissions wiring

### 10.1 Resource enum

Replace existing scaffold entries in `Resource.java`:
```
ENTITY_GROUP(EntityType.ENTITY_GROUP),
ROLE(EntityType.ROLE),
GROUP_PERMISSION(EntityType.GROUP_PERMISSION),
```

### 10.2 Permission tables

| Permissions class | Entries |
|---|---|
| `TenantAdminPermissions` | `ENTITY_GROUP → tenantEntityPermissionChecker`; `ROLE → tenantEntityPermissionChecker`; `GROUP_PERMISSION → tenantEntityPermissionChecker` |
| `CustomerUserPermissions` | `ENTITY_GROUP → entityGroupCustomerChecker` (new: walks owner chain to confirm user's customer); `ROLE → readOnlyChecker`; **no** `GROUP_PERMISSION` |
| `SysAdminPermissions` | none |

### 10.3 `@PreAuthorize` strings

- All `EntityGroupController` endpoints except `changeOwner` → `hasAnyAuthority('TENANT_ADMIN','CUSTOMER_USER')`.
- `changeOwner`, `makePublic`/`makePrivate` → `hasAuthority('TENANT_ADMIN')`.
- `RoleController` all → `hasAuthority('TENANT_ADMIN')` except `getRoleById` which is also `CUSTOMER_USER` (for displaying role names in UI).
- `GroupPermissionController` all → `hasAuthority('TENANT_ADMIN')`.

## 11 — Cache wiring

### 11.1 New `CacheConstants` entries

Replace existing partial entries with:
```
public static final String ENTITY_GROUP_CACHE       = "entityGroups";
public static final String ROLE_CACHE               = "roles";
public static final String GROUP_PERMISSION_CACHE   = "groupPermissions";
public static final String USER_PERMISSIONS_CACHE   = "userPermissions";
public static final String OWNER_HIERARCHY_CACHE    = "ownerHierarchy";
```

### 11.2 Caffeine + Redis bean classes

Five new pairs in `common/cache/.../<x>/`:
- `EntityGroupCaffeineCache.java` + `EntityGroupRedisCache.java`
- `RoleCaffeineCache.java` + `RoleRedisCache.java`
- `GroupPermissionCaffeineCache.java` + `GroupPermissionRedisCache.java`
- `UserPermissionsCaffeineCache.java` + `UserPermissionsRedisCache.java`
- `OwnerHierarchyCaffeineCache.java` + `OwnerHierarchyRedisCache.java`

Mirror: `CustomerCaffeineCache.java` / `CustomerRedisCache.java`. Each pulls TTL/size from `cache.specs.<name>.*` in `application/src/main/resources/thingsboard.yml` — five new `cache.specs.entityGroups`, `roles`, `groupPermissions`, `userPermissions`, `ownerHierarchy` blocks must be added there.

### 11.3 Evict events

- `EntityGroupCacheEvictEvent(TenantId, EntityId ownerId, EntityType type, String newName, String oldName)`
- `RoleCacheEvictEvent(TenantId, String newName, String oldName)`
- `GroupPermissionCacheEvictEvent(TenantId, EntityGroupId userGroupId, EntityGroupId entityGroupId)`
- `UserPermissionsCacheEvictEvent(TenantId, UserId)` — single record

Single `UserPermissionsCacheEvictListener` (`@Component`) listens to all four above events + global `SaveEntityEvent` of types `{USER, CUSTOMER}` (parent-customer or user-group membership changed) and evicts the affected user(s) — uses `EntityGroupService.getUsersInGroup` to expand a user-group to user IDs.

## 12 — Audit & events

For each new entity:
- `SaveEntityEvent`/`DeleteEntityEvent` published from `*ServiceImpl` (mandatory for edge sync + versioning).
- `logEntityActionService.logEntityAction(...)` in `DefaultTb*Service` with `ActionType.ADDED|UPDATED|DELETED`.

For `EntityGroupRelation` (membership):
- No `SaveEntityEvent` (not a top-level entity), **but** audit via `logEntityAction` with `ActionType.ADDED_TO_ENTITY` / `REMOVED_FROM_ENTITY`. Payload includes `{groupId, groupName}` so the audit log shows "Device X added to group Y".
- Also emit a custom `EntityGroupRelationChangedEvent` for the user-permissions evict listener.

The All-group sync listener uses `ActionType.ADDED_TO_ENTITY` *only for explicit user-added memberships*; auto-add to All groups is **not** audited (would flood the log on bulk imports).

## 13 — i18n keys

Namespaces in `ui-ngx/src/assets/locale/locale.constant-en_US.json`:

```
entityGroup.entity-groups, entityGroup.name, entityGroup.type, entityGroup.public,
entityGroup.owner, entityGroup.add, entityGroup.delete-title, entityGroup.delete-text,
entityGroup.all-devices, entityGroup.all-assets, entityGroup.all-customers,
entityGroup.add-entities, entityGroup.remove-entities, entityGroup.change-owner,
entityGroup.make-public, entityGroup.make-private, entityGroup.device-groups,
entityGroup.asset-groups, entityGroup.customer-groups,
role.roles, role.name, role.type, role.generic, role.group, role.permissions,
role.add, role.delete-title, role.delete-text,
groupPermission.permissions, groupPermission.user-group, groupPermission.entity-group,
groupPermission.role, groupPermission.public, groupPermission.add, groupPermission.delete-title
```

## 14 — Tests

### 14.1 Backend

| Test | Path | Base class |
|---|---|---|
| `EntityGroupControllerTest` | `application/src/test/.../controller/` | `AbstractControllerTest` |
| `EntityGroupServiceTest` | `dao/src/test/.../group/` | `AbstractServiceTest` |
| `EntityGroupDataValidatorTest` | `dao/src/test/.../service/validator/` | plain JUnit |
| `RoleControllerTest`, `RoleServiceTest`, `RoleDataValidatorTest` | parallel paths | same |
| `GroupPermissionControllerTest`, `GroupPermissionServiceTest`, `GroupPermissionDataValidatorTest` | parallel paths | same |
| `AllEntityGroupSyncListenerTest` | `application/src/test/.../service/entitiy/group/` | `AbstractServiceTest` — verifies a Device save creates an "All Devices" group on first save and adds it on subsequent saves |
| `EntityGroupQueryStrategyRegistryTest` | `dao/src/test/.../group/` | Spring context test — confirms all three strategies are registered |

Key scenarios:
- create/update/delete + version increment + cache evict.
- 403 from a tenant accessing another tenant's group.
- All-group is auto-created and non-deletable.
- owner chain walk: device under sub-customer inherits root tenant access.
- `GroupPermission` granting READ on a Customer group lets the user see all child devices.

### 14.2 Frontend (Karma)

- `entity-groups-tab.component.spec.ts` — verifies the component reads `entityType` from route data, renders different titles for DEVICE vs CUSTOMER, calls `getEntityGroupsByOwner` with the right type.
- `entity-groups-table-config.resolver.spec.ts` — verifies resolver builds a config whose `entitiesFetchFunction` calls the service with the route's entityType.

## 15 — Verification plan (end-to-end manual)

1. `mvn clean install -DskipTests` and run the monolith (`application` Spring Boot main).
2. Log in as `tenant@thingsboard.org`.
3. Navigate `Devices → Groups` tab — should be empty (no devices yet).
4. Create three devices (`d1`, `d2`, `d3`) via `Devices → +`. After each save, refresh the `Devices → Groups` tab → an `All Devices` group should appear with 3 members (verified via `Open` action).
5. Click `+` in the Groups tab → create `Critical Devices` group; add `d1`,`d2` to it.
6. Go to `Roles` (new admin menu entry) → create role `device-reader` with `permissions: { DEVICE: [READ] }`.
7. Go to `Users` → create user-group `ops-team`; add a customer user to it.
8. Go to `Group Permissions` → create `(ops-team, device-reader, Critical Devices)`.
9. Log in as that customer user → only sees `d1`,`d2` under `Devices`. `d3` is invisible.
10. Verify cache by checking `/actuator/caches` (or via logs): `userPermissions` cache populated on first request, evicted when the GroupPermission is deleted.
11. Move `d3` from Tenant ownership to a sub-customer: verify the All Devices group of the sub-customer now includes `d3` while the tenant-level All Devices group no longer does.

---

## Open-closed proof

Adding `EntityType.DASHBOARD` later requires only:
1. Add `DASHBOARD` to `EntityGroupTypes.GROUPABLE` set (1 line).
2. Create `DashboardEntityGroupQueryStrategy` `@Component` (1 file, ~30 lines).
3. Add `Dashboard` to permission-checker dispatch if needed (1 line).
4. In `dashboard-routing.module.ts`, add the two routes from §9.

No DDL, no controller change, no new service.

---

## Critical files to be created / modified

**SQL schema**
- `dao/src/main/resources/sql/schema-entities.sql` — append `entity_group`, `entity_group_relation`, `tb_role`, `group_permission` table DDL
- `dao/src/main/resources/sql/schema-entities-idx.sql` — append the indexes from §3

**Backend — open-closed seams (the design's load-bearing pieces)**
- `application/.../controller/EntityGroupController.java` — single generic controller parameterized by `{groupType}`
- `application/.../service/entitiy/group/AllEntityGroupSyncListener.java` — the `@TransactionalEventListener` that auto-materializes "All" groups, the seam that keeps Device/Asset/Customer service code untouched
- `dao/.../group/EntityGroupQueryStrategy.java` + `EntityGroupQueryStrategyRegistry.java` — the open-closed extension point; one bean per groupable type
- `common/dao-api/.../owner/OwnerService.java` + `dao/.../owner/OwnerServiceImpl.java` — the hierarchy walker
- `application/.../service/security/permission/DefaultAccessControlService.java` — extended with group-permission resolution
- `application/.../service/security/permission/UserPermissionsCacheEvictListener.java` — single cache-evict choke point

**Frontend — open-closed seams**
- `ui-ngx/src/app/modules/home/components/group/entity-groups-table-config.resolver.ts` — single shared resolver, parameterized by `route.data.entityType`
- `ui-ngx/src/app/modules/home/components/group/grouped-entities-table-config.resolver.ts` — drill-down resolver that wraps per-type resolvers
- `ui-ngx/src/app/modules/home/components/group/entity-groups-tab.component.ts` — reusable Groups-tab component
- `ui-ngx/src/app/core/http/entity-group.service.ts`, `role.service.ts`, `group-permission.service.ts`

**Files to delete (clean slate)**
- Backend: `common/data/.../group/Group.java`, `GroupId.java`; `common/data/.../group/grouppermissions/GroupPermission.java`, `GroupPermissionId.java`; `common/data/.../rbac/Role.java`, `RoleId.java`; `dao/.../model/sql/GroupEntity.java`, `GroupPermissionEntity.java`, `RoleEntity.java`; `dao/.../sql/group/**`; `dao/.../group/**`; `dao/.../sql/group/permessions/**`; `dao/.../group/grouppermissions/**`; `dao/.../role/**`; `common/dao-api/.../group/grouppermission/GroupPermissionService.java`; `application/.../service/entitiy/group/**`, `entitiy/grouppermission/**`, `entitiy/rbacrole/**`; `application/.../controller/GroupController.java`, `GroupPermissionController.java`, `RoleController.java`; `dao/.../service/validator/GroupPermissionDataValidator.java`
- Frontend: `ui-ngx/src/app/modules/home/pages/groups/**`; `ui-ngx/src/app/modules/home/pages/admin/roles/**`; `ui-ngx/src/app/modules/home/components/group/**`; `ui-ngx/src/app/core/http/groups.service.ts`, `group-permissions.service.ts`; `ui-ngx/src/app/modules/home/models/datasource/group-permission-datasource.ts`
