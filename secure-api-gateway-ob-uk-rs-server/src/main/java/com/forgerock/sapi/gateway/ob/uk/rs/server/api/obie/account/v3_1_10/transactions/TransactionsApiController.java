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
package com.forgerock.sapi.gateway.ob.uk.rs.server.api.obie.account.v3_1_10.transactions;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.forgerock.sapi.gateway.ob.uk.common.datamodel.account.FRExternalPermissionsCode;
import com.forgerock.sapi.gateway.ob.uk.common.datamodel.converter.v3.account.FRTransactionConverter;
import com.forgerock.sapi.gateway.ob.uk.common.error.OBErrorException;
import com.forgerock.sapi.gateway.ob.uk.common.error.OBRIErrorType;
import com.forgerock.sapi.gateway.ob.uk.rs.obie.api.account.v3_1_10.transactions.TransactionsApi;
import com.forgerock.sapi.gateway.ob.uk.rs.server.common.util.AccountDataInternalIdFilter;
import com.forgerock.sapi.gateway.ob.uk.rs.server.common.util.PaginationUtil;
import com.forgerock.sapi.gateway.ob.uk.rs.server.service.account.consent.AccountResourceAccessService;
import com.forgerock.sapi.gateway.rcs.consent.store.datamodel.account.v3_1_10.AccountAccessConsent;
import com.forgerock.sapi.gateway.rs.resource.store.repo.entity.account.FRTransaction;
import com.forgerock.sapi.gateway.rs.resource.store.repo.mongo.accounts.transactions.FRTransactionRepository;

import uk.org.openbanking.datamodel.v3.account.OBReadDataTransaction6;
import uk.org.openbanking.datamodel.v3.account.OBReadTransaction6;
import uk.org.openbanking.datamodel.v3.account.OBTransaction6;

@Controller("TransactionsApiV3.1.10")
public class TransactionsApiController implements TransactionsApi {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final int pageLimitTransactions;

    private final com.forgerock.sapi.gateway.rs.resource.store.repo.mongo.accounts.transactions.FRTransactionRepository FRTransactionRepository;

    private final AccountDataInternalIdFilter accountDataInternalIdFilter;

    private final AccountResourceAccessService accountResourceAccessService;

    private final Set<FRExternalPermissionsCode> transactionsPermissions = Set.of(FRExternalPermissionsCode.READTRANSACTIONSBASIC,
            FRExternalPermissionsCode.READTRANSACTIONSDETAIL,
            FRExternalPermissionsCode.READTRANSACTIONSCREDITS,
            FRExternalPermissionsCode.READTRANSACTIONSDEBITS);

    public TransactionsApiController(@Value("${rs.page.default.transaction.size:120}") int pageLimitTransactions,
            FRTransactionRepository FRTransactionRepository,
            AccountDataInternalIdFilter accountDataInternalIdFilter, @Qualifier("v3.1.10DefaultAccountResourceAccessService") AccountResourceAccessService accountResourceAccessService) {
        this.pageLimitTransactions = pageLimitTransactions;
        this.FRTransactionRepository = FRTransactionRepository;
        this.accountDataInternalIdFilter = accountDataInternalIdFilter;
        this.accountResourceAccessService = accountResourceAccessService;
    }

    @Override
    public ResponseEntity<OBReadTransaction6> getAccountTransactions(String accountId,
            int page,
            String authorization,
            String xFapiAuthDate,
            LocalDateTime fromBookingDateTime,
            LocalDateTime toBookingDateTime,
            String xFapiCustomerIpAddress,
            String xFapiInteractionId,
            String xCustomerUserAgent,
            String consentId,
            String apiClientId) throws OBErrorException {
        logger.info("getAccountTransactions for accountId: {}, consentId: {}, apiClientId: {}", accountId, consentId, apiClientId);
        logger.debug("transactionStore request transactionFrom {} transactionTo {} ",
                fromBookingDateTime, toBookingDateTime);

        if (toBookingDateTime == null) {
            toBookingDateTime = LocalDateTime.now();
        }
        if (fromBookingDateTime == null) {
            fromBookingDateTime = toBookingDateTime.minusYears(100);
        }

        final AccountAccessConsent consent = accountResourceAccessService.getConsentForResourceAccess(consentId, apiClientId, accountId);
        checkPermissions(consent);

        Page<FRTransaction> response = FRTransactionRepository.byAccountIdAndBookingDateTimeBetweenWithPermissions(accountId,
                new Date(fromBookingDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()), new Date(toBookingDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()), consent.getRequestObj().getData().getPermissions(),
                PageRequest.of(page, pageLimitTransactions, Sort.Direction.ASC, "bookingDateTime"));

        List<OBTransaction6> transactions = response.getContent()
                .stream()
                .map(FRTransaction::getTransaction)
                .map(FRTransactionConverter::toOBTransaction6)
                .map(t -> accountDataInternalIdFilter.apply(t))
                .collect(Collectors.toList());

        //Package the answer
        int totalPages = response.getTotalPages();

        return ResponseEntity.ok(new OBReadTransaction6()
                .data(new OBReadDataTransaction6().transaction(transactions))
                .links(PaginationUtil.generateLinks(buildGetAccountTransactionUri(accountId), page, totalPages))
                .meta(PaginationUtil.generateMetaData(totalPages)));
    }

    @Override
    public ResponseEntity<OBReadTransaction6> getAccountStatementTransactions(String statementId,
            String accountId,
            int page,
            String authorization,
            String xFapiAuthDate,
            LocalDateTime fromBookingDateTime,
            LocalDateTime toBookingDateTime,
            String xFapiCustomerIpAddress,
            String xFapiInteractionId,
            String xCustomerUserAgent,
            String consentId,
            String apiClientId) throws OBErrorException {
        logger.info("getAccountStatementTransactions from account id {}, statement id {}, consentId: {}, apiClientId: {} fromBookingDate {} toBookingDate {} pageNumber {} ",
                accountId, statementId, consentId, apiClientId, fromBookingDateTime, toBookingDateTime, page);

        if (toBookingDateTime == null) {
            toBookingDateTime = LocalDateTime.now();
        }
        if (fromBookingDateTime == null) {
            fromBookingDateTime = toBookingDateTime.minus(Period.ofYears(100));
        }

        final AccountAccessConsent consent = accountResourceAccessService.getConsentForResourceAccess(consentId, apiClientId, accountId);
        checkPermissions(consent);

        Page<FRTransaction> response = FRTransactionRepository.byAccountIdAndStatementIdAndBookingDateTimeBetweenWithPermissions(accountId, statementId,
                new Date(fromBookingDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()), new Date(toBookingDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()), consent.getRequestObj().getData().getPermissions(),
                PageRequest.of(page, pageLimitTransactions, Sort.Direction.ASC, "bookingDateTime"));

        List<OBTransaction6> transactions = response.getContent()
                .stream()
                .map(FRTransaction::getTransaction)
                .map(FRTransactionConverter::toOBTransaction6)
                .map(t -> accountDataInternalIdFilter.apply(t))
                .collect(Collectors.toList());

        //Package the answer
        int totalPages = response.getTotalPages();

        return ResponseEntity.ok(new OBReadTransaction6().data(new OBReadDataTransaction6().transaction(transactions))
                .links(PaginationUtil.generateLinks(buildGetAccountStatementTransactionUri(accountId, statementId), page, totalPages))
                .meta(PaginationUtil.generateMetaData(totalPages)));
    }

    @Override
    public ResponseEntity<OBReadTransaction6> getTransactions(int page,
            String authorization,
            String xFapiAuthDate,
            LocalDateTime fromBookingDateTime,
            LocalDateTime toBookingDateTime,
            String xFapiCustomerIpAddress,
            String xFapiInteractionId,
            String xCustomerUserAgent,
            String consentId,
            String apiClientId) throws OBErrorException{
        logger.info("getTransactions for consentId: {}, apiClientId: {}, fromBookingDate {} toBookingDate {} pageNumber {} ",
                consentId, apiClientId, fromBookingDateTime, toBookingDateTime, page);

        if (toBookingDateTime == null) {
            toBookingDateTime = LocalDateTime.now();
        }
        if (fromBookingDateTime == null) {
            fromBookingDateTime = toBookingDateTime.minus(Period.ofYears(100));
        }

        final AccountAccessConsent consent = accountResourceAccessService.getConsentForResourceAccess(consentId, apiClientId);
        checkPermissions(consent);

        Page<FRTransaction> body = FRTransactionRepository.byAccountIdInAndBookingDateTimeBetweenWithPermissions(consent.getAuthorisedAccountIds(),
                new Date(fromBookingDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()), new Date(toBookingDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()), consent.getRequestObj().getData().getPermissions(),
                PageRequest.of(page, pageLimitTransactions, Sort.Direction.ASC, "bookingDateTime"));

        List<OBTransaction6> transactions = body.getContent()
                .stream()
                .map(FRTransaction::getTransaction)
                .map(FRTransactionConverter::toOBTransaction6)
                .map(t -> accountDataInternalIdFilter.apply(t))
                .collect(Collectors.toList());

        //Package the answer
        int totalPages = body.getTotalPages();

        return  ResponseEntity.ok(new OBReadTransaction6().data(new OBReadDataTransaction6().transaction(transactions))
                .links(PaginationUtil.generateLinks(buildGetTransactionsUri(), page, totalPages))
                .meta(PaginationUtil.generateMetaData(totalPages)));
    }

    private String buildGetAccountTransactionUri(String accountId) {
        return linkTo(getClass()).slash("accounts").slash(accountId).slash("transactions").toString();
    }

    private String buildGetAccountStatementTransactionUri(String accountId, String statementId) {
        return linkTo(getClass()).slash("accounts").slash(accountId).slash("statements").slash(statementId).slash("transactions").toString();
    }

    private String buildGetTransactionsUri() {
        return linkTo(getClass()).slash("transactions").toString();
    }

    void checkPermissions(AccountAccessConsent consent) throws OBErrorException {
        final List<FRExternalPermissionsCode> permissions = consent.getRequestObj().getData().getPermissions();
        if (transactionsPermissions.stream().noneMatch(permissions::contains)) {
            throw new OBErrorException(OBRIErrorType.PERMISSIONS_INVALID, "at least one of: " + transactionsPermissions);
        }
    }

}
