/*
 * Copyright © 2020-2025 ForgeRock AS (obst@forgerock.com)
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
package com.forgerock.sapi.gateway.rs.resource.store.repo.mongo.v4.accounts.products;

import com.forgerock.sapi.gateway.ob.uk.common.datamodel.account.FRExternalPermissionsCode;
import com.forgerock.sapi.gateway.rs.resource.store.repo.entity.v4.account.FRProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FRProductRepositoryCustom {

    Page<FRProduct> byAccountIdWithPermissions(
            @Param("accountId") String accountId,
            @Param("permission") List<FRExternalPermissionsCode> permissions,
            Pageable pageable);

    Page<FRProduct> byAccountIdInWithPermissions(
            @Param("accountIds") List<String> accountIds,
            @Param("permission") List<FRExternalPermissionsCode> permissions,
            Pageable pageable);

}
