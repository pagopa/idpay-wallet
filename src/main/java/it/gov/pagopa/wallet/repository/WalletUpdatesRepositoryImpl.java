package it.gov.pagopa.wallet.repository;

import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.model.Wallet.Fields;
import it.gov.pagopa.wallet.model.Wallet.RefundHistory;
import java.math.BigDecimal;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class WalletUpdatesRepositoryImpl implements WalletUpdatesRepository {

  private static final String FIELD_INITIATIVE_ID = Fields.initiativeId;
  private static final String FIELD_USER_ID = Fields.userId;
  private static final String FIELD_STATUS = Fields.status;
  private static final String FIELD_IBAN = Fields.iban;
  private static final String FIELD_AMOUNT = Fields.amount;
  private static final String FIELD_ACCRUED = Fields.accrued;
  private static final String FIELD_REFUNDED = Fields.refunded;
  private static final String FIELD_NTRX = Fields.nTrx;
  private static final String FIELD_NINSTR = Fields.nInstr;
  private static final String FIELD_HISTORY = Fields.refundHistory;

  private final MongoTemplate mongoTemplate;

  public WalletUpdatesRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Wallet deleteIban(String initiativeId, String userId, String iban) {
    log.trace(
        "[DELETE_OPERATION] [DELETE_IBAN] Deleting IBAN from wallet with initiativeId: {}",
        initiativeId);

    return mongoTemplate.findAndModify(
        Query.query(Criteria
            .where(FIELD_INITIATIVE_ID).is(initiativeId)
            .and(FIELD_USER_ID).is(userId)
            .and(FIELD_IBAN).is(iban)),
        new Update().unset(FIELD_IBAN),
        FindAndModifyOptions.options().returnNew(true),
        Wallet.class);
  }

  @Override
  public Wallet enrollIban(String initiativeId, String userId, String iban) {
    log.trace(
        "[ENROLL_IBAN] Deleting IBAN from wallet with initiativeId: {}",
        initiativeId);

    return mongoTemplate.findAndModify(
        Query.query(Criteria
            .where(FIELD_INITIATIVE_ID).is(initiativeId)
            .and(FIELD_USER_ID).is(userId)),
        new Update().set(FIELD_IBAN, iban),
        FindAndModifyOptions.options().returnNew(true),
        Wallet.class);
  }

  @Override
  public Wallet rewardTransaction(String initiativeId, String userId, BigDecimal amount,
      BigDecimal accrued, Long nTrx) {

    log.trace(
        "[UPDATE_WALLET_FROM_TRANSACTION] [REWARD_TRANSACTION] Updating Wallet [amount: {}, accrued: {}, nTrx: {}]",
        amount, accrued, nTrx);

    return mongoTemplate.findAndModify(
        Query.query(Criteria
            .where(FIELD_INITIATIVE_ID).is(initiativeId)
            .and(FIELD_USER_ID).is(userId)),
        new Update().set(FIELD_AMOUNT, amount).set(FIELD_ACCRUED, accrued).set(FIELD_NTRX, nTrx),
        FindAndModifyOptions.options().returnNew(true),
        Wallet.class);
  }

  @Override
  public void processRefund(String initiativeId, String userId, BigDecimal refunded, Map<String, RefundHistory> history) {

    log.trace(
        "[PROCESS_REFUND] Updating Wallet [refunded: {}]", refunded);

    mongoTemplate.updateFirst(
        Query.query(Criteria
            .where(FIELD_INITIATIVE_ID).is(initiativeId)
            .and(FIELD_USER_ID).is(userId)),
        new Update().inc(FIELD_REFUNDED, refunded).set(FIELD_HISTORY, history),
        Wallet.class);
  }

  @Override
  public Wallet updateInstrumentNumber(String initiativeId, String userId, int nInstr){
    log.trace(
        "[UPDATE_INSTRUMENT_NUMBER] Updating Wallet [nInstr: {}]",
        nInstr);

    return mongoTemplate.findAndModify(
        Query.query(Criteria
            .where(FIELD_INITIATIVE_ID).is(initiativeId)
            .and(FIELD_USER_ID).is(userId)),
        new Update().set(FIELD_NINSTR, nInstr),
        FindAndModifyOptions.options().returnNew(true),
        Wallet.class);
  }

  @Override
  public Wallet decreaseInstrumentNumber(String initiativeId, String userId) {
    log.trace(
        "[DECREASE_INSTRUMENT_NUMBER] Updating Wallet");

    return mongoTemplate.findAndModify(
        Query.query(Criteria
            .where(FIELD_INITIATIVE_ID).is(initiativeId)
            .and(FIELD_USER_ID).is(userId)),
        new Update().inc(FIELD_NINSTR, -1),
        FindAndModifyOptions.options().returnNew(true),
        Wallet.class);
  }

  @Override
  public void setStatus(String initiativeId, String userId, String status) {
    log.trace(
        "[SET_STATUS] Updating Wallet [status: {}]",
        status);

    mongoTemplate.updateFirst(
        Query.query(Criteria
            .where(FIELD_INITIATIVE_ID).is(initiativeId)
            .and(FIELD_USER_ID).is(userId)),
        new Update().set(FIELD_STATUS, status),
        Wallet.class);
  }
}
