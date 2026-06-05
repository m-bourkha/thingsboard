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
package org.thingsboard.server.dao.group.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.group.EntityGroupQueryStrategy;
import org.thingsboard.server.dao.sql.customer.CustomerRepository;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomerEntityGroupQueryStrategy implements EntityGroupQueryStrategy<Customer> {

    @Autowired
    private CustomerRepository customerRepository;

    public static final Map<String,String> entityGroupCustomerColumnMap = new HashMap<>();
    static {
        entityGroupCustomerColumnMap.put("createdTime", "created_time");

    }

    @Override
    public EntityType supportedType() {
        return EntityType.CUSTOMER;
    }

    @Override
    public PageData<Customer> findEntitiesInGroup(TenantId tenantId, EntityGroupId groupId, PageLink pageLink) {
        return DaoUtil.toPageData(customerRepository.findByEntityGroupId(
                groupId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink,entityGroupCustomerColumnMap)));
    }
}
