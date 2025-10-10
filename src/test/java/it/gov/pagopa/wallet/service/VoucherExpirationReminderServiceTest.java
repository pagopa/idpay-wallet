package it.gov.pagopa.wallet.service;

import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.enums.*;
import it.gov.pagopa.wallet.event.producer.ErrorProducer;
import it.gov.pagopa.wallet.event.producer.NotificationProducer;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.Message;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherExpirationReminderServiceTest {

    @Mock
    ErrorProducer errorProducerMock;
    @Mock
    NotificationProducer notificationProducerMock;
    @Mock
    WalletRepository walletRepositoryMock;
    VoucherExpirationReminderBatchServiceImpl voucherExpirationReminderBatchService;

    private static final String NOTIFICATION_SERVER = "mock-server";
    private static final String NOTIFICATION_TOPIC = "mock-topic";
    private static final int BLOCK_REMINDER_BATCH = 100;
    private static final String USER_ID = "TEST_USER_ID";
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String INITIATIVE_NAME = "TEST_INITIATIVE_NAME";
    private static final String USER_MAIL = "USERMAIL";
    private static final String NAME = "NAME";
    private static final String SURNAME = "SURNAME";
    private static final String SERVICE_ID = "serviceid";

    @Value("${app.wallet.expiringDay}")
    private int expiringDay;
    private final LocalDate expirationDate = LocalDate.now().plusDays(expiringDay);
    private final String expirationDateString = expirationDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    private static final Pageable pageable = PageRequest.of(0, 100);

    private static final Wallet TEST_WALLET_1 =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .serviceId(SERVICE_ID)
                    .channel(Channel.WEB)
                    .initiativeName(INITIATIVE_NAME)
                    .name(NAME)
                    .surname(SURNAME)
                    .userMail(USER_MAIL)
                    .build();

    private static final Wallet TEST_WALLET_2 =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .serviceId(SERVICE_ID)
                    .channel(Channel.WEB)
                    .initiativeName(INITIATIVE_NAME)
                    .name(NAME)
                    .surname(SURNAME)
                    .userMail(USER_MAIL)
                    .build();

    @BeforeEach
    void setup() {
        voucherExpirationReminderBatchService = new VoucherExpirationReminderBatchServiceImpl(
                walletRepositoryMock,
                notificationProducerMock,
                errorProducerMock,
                BLOCK_REMINDER_BATCH,
                NOTIFICATION_SERVER,
                NOTIFICATION_TOPIC
        );
    }

    @Test
    void runReminderBatch_successWithWallets() {

        List<Wallet> walletList = new ArrayList<>();
        walletList.add(TEST_WALLET_1);
        walletList.add(TEST_WALLET_2);

        Page<Wallet> walletPage = new PageImpl<>(walletList, PageRequest.of(0, walletList.size()), walletList.size());

        when(walletRepositoryMock.findByInitiativeIdAndVoucherEndDateIgnoringTime(INITIATIVE_ID, expirationDateString, pageable))
                .thenReturn(walletPage);

        Mockito.doNothing()
                .when(notificationProducerMock)
                .sendNotification(any(NotificationQueueDTO.class));

        // Act
        voucherExpirationReminderBatchService.runReminderBatch(INITIATIVE_ID, expiringDay);

        // Assert
        //Verify that the repository has been called 1 time
        verify(walletRepositoryMock, times(1)).findByInitiativeIdAndVoucherEndDateIgnoringTime(INITIATIVE_ID, expirationDateString, pageable);
        //Verify that the NotificationProducer was called 2 time
        verify(notificationProducerMock, times(2)).sendNotification(any(NotificationQueueDTO.class));
        //Verify that the ErrorProducer has NEVER been called
        verify(errorProducerMock, never()).sendEvent(any());
    }

    @Test
    void runReminderBatch_successWithNoWallets() {
        Page<Wallet> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 10),
                0);

        when(walletRepositoryMock.findByInitiativeIdAndVoucherEndDateIgnoringTime(INITIATIVE_ID, expirationDateString, pageable))
                .thenReturn(emptyPage);

        //act
        voucherExpirationReminderBatchService.runReminderBatch(INITIATIVE_ID, expiringDay);

        //assert
        //Verify that the repository has been called 1 time
        verify(walletRepositoryMock, times(1)).findByInitiativeIdAndVoucherEndDateIgnoringTime(INITIATIVE_ID, expirationDateString, pageable);

        //Verify that the NotificationProducer has NEVER been called
        verify(notificationProducerMock, never()).sendNotification(any(NotificationQueueDTO.class));

        // Verify that the ErrorProducer has NEVER been called
        verify(errorProducerMock, never()).sendEvent(any());
    }


    @Test
    void runReminderBatch_errorSendingNotification() {
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(TEST_WALLET_1);

        Page<Wallet> walletPage = new PageImpl<>(walletList, PageRequest.of(0, walletList.size()), walletList.size());

        when(walletRepositoryMock.findByInitiativeIdAndVoucherEndDateIgnoringTime(INITIATIVE_ID, expirationDateString, pageable))
                .thenReturn(walletPage);

        //Simulates an exception when sending the notification
        doThrow(new RuntimeException("Kafka connection failed")).when(notificationProducerMock)
                .sendNotification(any(NotificationQueueDTO.class));

        // Act
        voucherExpirationReminderBatchService.runReminderBatch(INITIATIVE_ID, expiringDay);

        // Assert
        // Verify that the repository has been called 1 time
        verify(walletRepositoryMock, times(1)).findByInitiativeIdAndVoucherEndDateIgnoringTime(INITIATIVE_ID, expirationDateString, pageable);

        // Verify that the NotificationProducer was called 1 time (and failed)
        verify(notificationProducerMock, times(1)).sendNotification(any(NotificationQueueDTO.class));

        // Verify that the ErrorProducer has been called 1 time
        verify(errorProducerMock, times(1)).sendEvent(any(Message.class));
    }


    @Test
    void runReminderBatch_errorInFirstNotification() {
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(TEST_WALLET_1);
        walletList.add(TEST_WALLET_2);

        Page<Wallet> walletPage = new PageImpl<>(walletList, PageRequest.of(0, walletList.size()), walletList.size());


        when(walletRepositoryMock.findByInitiativeIdAndVoucherEndDateIgnoringTime(INITIATIVE_ID, expirationDateString, pageable))
                .thenReturn(walletPage);

        //The first send fails, but not the second.
        doThrow(new RuntimeException("Kafka connection failed for wallet 1"))
                .doNothing() // The second wallet will be successful (if the cycle continues)
                .when(notificationProducerMock).sendNotification(any(NotificationQueueDTO.class));

        // Act
        voucherExpirationReminderBatchService.runReminderBatch(INITIATIVE_ID, expiringDay);

        // Assert
        // Verify that the NotificationProducer was called 2 time
        verify(notificationProducerMock, times(2)).sendNotification(any(NotificationQueueDTO.class));

        // Verify that the ErrorProducer has been called 1 time
        verify(errorProducerMock, times(1)).sendEvent(any(Message.class));
    }


}
