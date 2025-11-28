package it.gov.pagopa.wallet.repository;

import it.gov.pagopa.wallet.model.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends MongoRepository<Wallet, String> {

    Optional<Wallet> findByIdAndUserId(String id, String userId);

    List<Wallet> findByUserId(String userId);

    Optional<Wallet> findByUserIdAndIban(String userId, String iban);

    List<Wallet> findByInitiativeIdAndFamilyId(String initiativeId, String familyId);

    @Query(
            value  = "{ 'initiativeId': ?0, 'voucherEndDate': { '$gte': ?1, '$lt': ?2 }, 'accruedCents': 0}"
    )
    Page<Wallet> findVoucherExpiredIntoRange(
            String initiativeId,
            Date startUtc,
            Date endUtc,
            Pageable pageable
    );
}
