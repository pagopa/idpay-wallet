package it.gov.pagopa.wallet.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.model.Wallet.Fields;
import it.gov.pagopa.wallet.model.Wallet.RefundHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private static final String FIELD_FAMILY_ID = Fields.familyId;
    private static final String FIELD_STATUS = Fields.status;
    private static final String FIELD_IBAN = Fields.iban;
    private static final String FIELD_AMOUNT_CENTS = Fields.amountCents;
    private static final String FIELD_ACCRUED_CENTS = Fields.accruedCents;
    private static final String FIELD_REFUNDED_CENTS = Fields.refundedCents;
    private static final String FIELD_NTRX = Fields.nTrx;
    private static final String FIELD_NINSTR = Fields.nInstr;
    private static final String FIELD_HISTORY = Fields.refundHistory;
    private static final String FIELD_LAST_COUNTER_UPDATE = Fields.lastCounterUpdate;
    private static final String FIELD_SUSPENSION_DATE = Fields.suspensionDate;
    private static final String FIELD_UPDATE_DATE = Fields.updateDate;
    private static final String FIELD_COUNTER_VERSION = Fields.counterVersion;
    private static final String FIELD_COUNTER_HISTORY = Fields.counterHistory;
    private final MongoTemplate mongoTemplate;

    public WalletUpdatesRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void deleteIban(String initiativeId, String userId, String status) {
        log.trace(
                "[DELETE_OPERATION] [DELETE_IBAN] Deleting IBAN from wallet with initiativeId: {}",
                initiativeId);

        mongoTemplate.updateFirst(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId).and(FIELD_USER_ID).is(userId)),
                new Update().unset(FIELD_IBAN).set(FIELD_STATUS, status).set(FIELD_UPDATE_DATE, LocalDateTime.now()),
                Wallet.class);
    }

    @Override
    public void enrollIban(String initiativeId, String userId, String iban, String status) {
        log.trace("[ENROLL_IBAN] Deleting IBAN from wallet with initiativeId: {}", initiativeId);

        mongoTemplate.updateFirst(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId).and(FIELD_USER_ID).is(userId)),
                new Update().set(FIELD_IBAN, iban).set(FIELD_STATUS, status).set(FIELD_UPDATE_DATE, LocalDateTime.now()),
                Wallet.class);
    }

    @Override
    public void suspendWallet(String initiativeId, String userId, String status, LocalDateTime localDateTime) {
        log.trace("[SUSPEND_WALLET] Suspend wallet with initiativeId: {}", initiativeId);

        mongoTemplate.updateFirst(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId).and(FIELD_USER_ID).is(userId)),
                new Update().set(FIELD_STATUS, status).set(FIELD_UPDATE_DATE, localDateTime).set(FIELD_SUSPENSION_DATE, localDateTime),
                Wallet.class);
    }

    @Override
    public void readmitWallet(String initiativeId, String userId, String status, LocalDateTime localDateTime) {
        log.trace("[READMIT_WALLET] Readmit wallet with initiativeId: {}", initiativeId);

        mongoTemplate.updateFirst(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId).and(FIELD_USER_ID).is(userId)),
                new Update().set(FIELD_STATUS, status).set(FIELD_UPDATE_DATE, localDateTime).set(FIELD_SUSPENSION_DATE, null),
                Wallet.class);
    }

    @Override
    public Wallet rewardTransaction(
            String initiativeId, String userId, LocalDateTime trxElaborationTimestamp, Long amountCents, Long accruedCents, Long counterVersion) {

        log.trace(
                "[UPDATE_WALLET_FROM_TRANSACTION] [REWARD_TRANSACTION] Updating Wallet [amount: {}, accrued: {}]",
                amountCents,
                accruedCents);

        return mongoTemplate.findAndModify(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId).andOperator(
                                Criteria.where(FIELD_USER_ID).is(userId)
                        )
                ),
                new Update().set(FIELD_AMOUNT_CENTS, amountCents)
                        .set(FIELD_ACCRUED_CENTS, accruedCents)
                        .inc(FIELD_NTRX, 1)
                        .set(FIELD_LAST_COUNTER_UPDATE, LocalDateTime.now())
                        .set(FIELD_UPDATE_DATE, LocalDateTime.now())
                        .set(FIELD_COUNTER_VERSION, counterVersion),
                FindAndModifyOptions.options().returnNew(true),
                Wallet.class);
    }


    @Override
    public boolean rewardFamilyTransaction(
            String initiativeId, String familyId, LocalDateTime trxElaborationTimestamp, Long amountCents, Long counterVersion) {

        log.trace("[UPDATE_WALLET_FROM_TRANSACTION][REWARD_TRANSACTION] Updating Family Wallet [amount: {}]", amountCents);

        UpdateResult result = mongoTemplate.updateMulti(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId).and(FIELD_FAMILY_ID).is(familyId)
                ),
                new Update()
                        .set(FIELD_AMOUNT_CENTS, amountCents)
                        .set(FIELD_LAST_COUNTER_UPDATE, LocalDateTime.now())
                        .set(FIELD_UPDATE_DATE, LocalDateTime.now())
                        .set(FIELD_COUNTER_VERSION, counterVersion),
                Wallet.class);

        return result.getModifiedCount() == result.getMatchedCount();
    }

    @Override
    public void processRefund(
            String initiativeId, String userId, Long refundedCents, Map<String, RefundHistory> history) {

        log.trace("[PROCESS_REFUND] Updating Wallet [refunded: {}]", refundedCents);

        mongoTemplate.updateFirst(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId).and(FIELD_USER_ID).is(userId)),
                new Update().set(FIELD_REFUNDED_CENTS, refundedCents).set(FIELD_HISTORY, history).set(FIELD_UPDATE_DATE, LocalDateTime.now()),
                Wallet.class);
    }

    @Override
    public void updateInstrumentNumber(
            String initiativeId, String userId, int nInstr, String status) {
        log.trace("[UPDATE_INSTRUMENT_NUMBER] Updating Wallet [nInstr: {}]", nInstr);

        mongoTemplate.updateFirst(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId).and(FIELD_USER_ID).is(userId)),
                new Update().set(FIELD_NINSTR, nInstr).set(FIELD_STATUS, status).set(FIELD_UPDATE_DATE, LocalDateTime.now()),
                Wallet.class);
    }

    @Override
    public void decreaseInstrumentNumber(String initiativeId, String userId, String status) {
        log.trace("[DECREASE_INSTRUMENT_NUMBER] Updating Wallet");

        mongoTemplate.updateFirst(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId).and(FIELD_USER_ID).is(userId)),
                new Update().inc(FIELD_NINSTR, -1).set(FIELD_STATUS, status).set(FIELD_UPDATE_DATE, LocalDateTime.now()),
                Wallet.class);
    }

    @Override
    public List<Wallet> deletePaged(String initiativeId, int pageSize) {
        log.trace("[DELETE_PAGED] Deleting wallet in pages");
        Pageable pageable = PageRequest.of(0, pageSize);
        return mongoTemplate.findAllAndRemove(
                Query.query(Criteria.where(Fields.initiativeId).is(initiativeId)).with(pageable),
                Wallet.class
        );
    }

    @Override
    public Wallet rewardFamilyUserTransaction(String initiativeId, String userId, LocalDateTime elaborationDateTime, List<Long> counterHistory, Long accruedCents) {
        log.trace(
                "[UPDATE_WALLET_FROM_TRANSACTION] [REWARD_TRANSACTION] Updating Wallet [accrued: {}]",
                accruedCents);

        return mongoTemplate.findAndModify(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId).andOperator(
                                Criteria.where(FIELD_USER_ID).is(userId)
                        )
                ),
                new Update()
                        .set(FIELD_ACCRUED_CENTS, accruedCents)
                        .inc(FIELD_NTRX, 1)
                        .set(FIELD_LAST_COUNTER_UPDATE, LocalDateTime.now())
                        .set(FIELD_UPDATE_DATE, LocalDateTime.now())
                        .set(FIELD_COUNTER_HISTORY, counterHistory),
                FindAndModifyOptions.options().returnNew(true),
                Wallet.class);
    }
}
