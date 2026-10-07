package it.gov.pagopa.wallet.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.model.Wallet.RefundHistory;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletUpdatesRepositoryImplTest {

    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String USER_ID = "USER_ID";

    @Mock
    private MongoTemplate mongoTemplate;

    private WalletUpdatesRepositoryImpl walletUpdatesRepository;

    @BeforeEach
    void setUp() {
        walletUpdatesRepository = new WalletUpdatesRepositoryImpl(mongoTemplate);
    }

    @Test
    void deleteIban_shouldUnsetIbanAndUpdateStatus() {
        walletUpdatesRepository.deleteIban(INITIATIVE_ID, USER_ID, "SUSPENDED");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(Wallet.class));

        assertInitiativeUserQuery(queryCaptor.getValue(), INITIATIVE_ID, USER_ID);
        Document updateDocument = updateCaptor.getValue().getUpdateObject();

        Document unset = (Document) updateDocument.get("$unset");
        Document set = (Document) updateDocument.get("$set");

        assertNotNull(unset);
        assertEquals(1, unset.get("iban"));
        assertEquals("SUSPENDED", set.get("status"));
        assertNotNull(set.get("updateDate"));
    }

    @Test
    void enrollIban_shouldSetIbanStatusAndUpdateDate() {
        walletUpdatesRepository.enrollIban(INITIATIVE_ID, USER_ID, "IT60X0542811101000000123456", "ENROLLED");

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(Wallet.class));

        Document set = (Document) updateCaptor.getValue().getUpdateObject().get("$set");
        assertEquals("IT60X0542811101000000123456", set.get("iban"));
        assertEquals("ENROLLED", set.get("status"));
        assertNotNull(set.get("updateDate"));
    }

    @Test
    void suspendWallet_shouldSetStatusUpdateDateAndSuspensionDate() {
        LocalDateTime now = LocalDateTime.now();

        walletUpdatesRepository.suspendWallet(INITIATIVE_ID, USER_ID, "SUSPENDED", now);

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(Wallet.class));

        Document set = (Document) updateCaptor.getValue().getUpdateObject().get("$set");
        assertEquals("SUSPENDED", set.get("status"));
        assertEquals(now, set.get("updateDate"));
        assertEquals(now, set.get("suspensionDate"));
    }

    @Test
    void readmitWallet_shouldClearSuspensionDate() {
        LocalDateTime now = LocalDateTime.now();

        walletUpdatesRepository.readmitWallet(INITIATIVE_ID, USER_ID, "ENROLLED", now);

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(Wallet.class));

        Document set = (Document) updateCaptor.getValue().getUpdateObject().get("$set");
        assertEquals("ENROLLED", set.get("status"));
        assertEquals(now, set.get("updateDate"));
        assertNull(set.get("suspensionDate"));
    }

    @Test
    void rewardTransaction_shouldApplySetAndIncAndReturnWallet() {
        Wallet expected = Wallet.builder().initiativeId(INITIATIVE_ID).userId(USER_ID).build();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Wallet.class)))
                .thenReturn(expected);

        Wallet result = walletUpdatesRepository.rewardTransaction(INITIATIVE_ID, USER_ID, LocalDateTime.now(), 1000L, 200L, 3L);

        assertEquals(expected, result);

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(any(Query.class), updateCaptor.capture(), any(FindAndModifyOptions.class), eq(Wallet.class));

        Document updateDocument = updateCaptor.getValue().getUpdateObject();
        Document set = (Document) updateDocument.get("$set");
        Document inc = (Document) updateDocument.get("$inc");

        assertEquals(1000L, set.get("amountCents"));
        assertEquals(200L, set.get("accruedCents"));
        assertEquals(3L, set.get("counterVersion"));
        assertNotNull(set.get("lastCounterUpdate"));
        assertNotNull(set.get("updateDate"));
        assertEquals(1, inc.get("nTrx"));
    }

    @Test
    void rewardFamilyTransaction_shouldReturnTrueWhenAllMatchedRowsAreModified() {
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(Wallet.class)))
                .thenReturn(UpdateResult.acknowledged(2L, 2L, null));

        boolean result = walletUpdatesRepository.rewardFamilyTransaction(INITIATIVE_ID, "FAMILY_ID", LocalDateTime.now(), 1200L, 5L);

        assertTrue(result);
    }

    @Test
    void rewardFamilyTransaction_shouldReturnFalseWhenSomeRowsAreNotModified() {
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(Wallet.class)))
                .thenReturn(UpdateResult.acknowledged(2L, 1L, null));

        boolean result = walletUpdatesRepository.rewardFamilyTransaction(INITIATIVE_ID, "FAMILY_ID", LocalDateTime.now(), 1200L, 5L);

        assertFalse(result);
    }

    @Test
    void processRefund_shouldSetRefundDataAndUpdateDate() {
        Map<String, RefundHistory> history = Map.of("k1", new RefundHistory(1L));

        walletUpdatesRepository.processRefund(INITIATIVE_ID, USER_ID, 300L, history);

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(Wallet.class));

        Document set = (Document) updateCaptor.getValue().getUpdateObject().get("$set");
        assertEquals(300L, set.get("refundedCents"));
        assertEquals(history, set.get("refundHistory"));
        assertNotNull(set.get("updateDate"));
    }

    @Test
    void deletePaged_shouldUsePageSizeInFindAllAndRemove() {
        walletUpdatesRepository.deletePaged(INITIATIVE_ID, 50);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).findAllAndRemove(queryCaptor.capture(), eq(Wallet.class));

        assertEquals(INITIATIVE_ID, queryCaptor.getValue().getQueryObject().get("initiativeId"));
        assertEquals(0L, queryCaptor.getValue().getSkip());
        assertEquals(50, queryCaptor.getValue().getLimit());
    }

    @Test
    void rewardFamilyUserTransaction_shouldSetCounterHistoryAndIncNtrx() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Wallet.class)))
                .thenReturn(Wallet.builder().build());

        walletUpdatesRepository.rewardFamilyUserTransaction(INITIATIVE_ID, USER_ID, LocalDateTime.now(), List.of(10L, 20L), 900L);

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(any(Query.class), updateCaptor.capture(), any(FindAndModifyOptions.class), eq(Wallet.class));

        Document updateDocument = updateCaptor.getValue().getUpdateObject();
        Document set = (Document) updateDocument.get("$set");
        Document inc = (Document) updateDocument.get("$inc");

        assertEquals(900L, set.get("accruedCents"));
        assertEquals(List.of(10L, 20L), set.get("counterHistory"));
        assertNotNull(set.get("lastCounterUpdate"));
        assertNotNull(set.get("updateDate"));
        assertEquals(1, inc.get("nTrx"));
    }

    @Test
    void updateReminderNotifiedDate_shouldSetReminderDateAndUpdateDate() {
        LocalDateTime reminderDate = LocalDateTime.now();

        walletUpdatesRepository.updateReminderNotifiedDate(INITIATIVE_ID, USER_ID, reminderDate);

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(Wallet.class));

        Document set = (Document) updateCaptor.getValue().getUpdateObject().get("$set");
        assertEquals(reminderDate, set.get("reminderNotifiedDate"));
        assertNotNull(set.get("updateDate"));
    }

    private void assertInitiativeUserQuery(Query query, String initiativeId, String userId) {
        Document queryObject = query.getQueryObject();
        assertEquals(initiativeId, queryObject.get("initiativeId"));
        assertEquals(userId, queryObject.get("userId"));
    }
}

