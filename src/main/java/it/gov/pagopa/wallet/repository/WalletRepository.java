package it.gov.pagopa.wallet.repository;

import it.gov.pagopa.wallet.model.Wallet;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends MongoRepository<Wallet, String> {

    List<Wallet> findByUserId(String userId);

    Optional<Wallet> findByUserIdAndIban(String userId, String iban);

    List<Wallet> findByInitiativeIdAndFamilyId(String initiativeId, String familyId);

    Page<Wallet> findByInitiativeIdAndVoucherEndDateBefore(String initiativeId, LocalDate voucherEndDate, Pageable pageable);

}
