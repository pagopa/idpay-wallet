package it.gov.pagopa.wallet.repository;

import it.gov.pagopa.wallet.model.Wallet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends MongoRepository<Wallet, String> {

  Optional<Wallet> findByInitiativeIdAndUserId(String initiativeId, String userId);

  List<Wallet> findByUserId(String userId);

  Optional<Wallet> findByUserIdAndIban(String userId, String iban);

  List<Wallet> findByInitiativeIdAndFamilyId(String initiativeId, String familyId);

}
