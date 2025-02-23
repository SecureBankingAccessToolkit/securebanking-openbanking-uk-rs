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
package com.forgerock.sapi.gateway.ob.uk.rs.server.service.idempotency;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.forgerock.sapi.gateway.ob.uk.common.error.OBErrorException;
import com.forgerock.sapi.gateway.rs.resource.store.repo.entity.payment.PaymentSubmission;

/**
 * This implementation is aimed at Payments which can have at most a single payment per consent (all payments except for VRP).
 *
 * These payments have a primary key matching the consentId.
 * When payments are inserted into the repo a {@link DuplicateKeyException} is raised if the payment already exists,
 * when this occurs the idempotency data for the payment is checked and if it matches then the existing payment is
 * returned as if it were created by this call.
 *
 * See {@link IdempotentPaymentService} documentation for known limitations of this approach.
 */
public class SinglePaymentForConsentIdempotentPaymentService<T extends PaymentSubmission<R>, R, REPO extends MongoRepository<T, String>>
        implements IdempotentPaymentService<T, R> {

    private final REPO paymentRepo;

    public SinglePaymentForConsentIdempotentPaymentService(REPO paymentRepo) {
        this.paymentRepo = paymentRepo;
    }

    @Override
    public Optional<T> findExistingPayment(R frPaymentRequest, String consentId, String apiClientId, String idempotencyKey) throws OBErrorException {
        final Optional<T> existingPayment = paymentRepo.findById(consentId);
        if (existingPayment.isPresent()) {
            validateExistingPaymentIdempotencyData(frPaymentRequest, idempotencyKey, existingPayment.get());
        }
        return existingPayment;
    }

    @Override
    public T savePayment(T paymentSubmission) throws OBErrorException {
        try {
            requireNonNull(paymentSubmission.getIdempotencyKey());

            // Force the paymentId to the consentId - guards against this being set incorrectly in the calling code
            paymentSubmission.setId(paymentSubmission.getConsentId());
            return paymentRepo.insert(paymentSubmission);
        } catch (DuplicateKeyException ex) {
            // Payment already exists for this consentId, return the existing payment if the idempotency fields are correct
            final Optional<T> paymentQueryResult = paymentRepo.findById(paymentSubmission.getId());
            if (paymentQueryResult.isPresent()) {
                final T existingPayment = paymentQueryResult.get();
                validateExistingPaymentIdempotencyData(paymentSubmission.getPayment(), paymentSubmission.getIdempotencyKey(), existingPayment);
                return existingPayment;
            } else {
                throw new IllegalStateException("Failed to insert payment - expected to find a payment for id: " + paymentSubmission.getId(), ex);
            }
        }
    }
}
