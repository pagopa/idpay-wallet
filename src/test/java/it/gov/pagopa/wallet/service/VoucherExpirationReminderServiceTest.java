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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import java.time.LocalDate;
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

    @InjectMocks
    VoucherExpirationReminderBatchServiceImpl voucherExpirationReminderBatchService;

    private static final String NOTIFICATION_SERVER = "mock-server";
    private static final String NOTIFICATION_TOPIC = "mock-topic";
    private static final String USER_ID = "TEST_USER_ID";
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String INITIATIVE_NAME = "TEST_INITIATIVE_NAME";
    private static final String USER_MAIL = "USERMAIL";
    private static final String NAME = "NAME";
    private static final String SURNAME = "SURNAME";
    private static final String SERVICE_ID = "serviceid";

    private static final int DAYS_NUMBER = 3;
    private static final LocalDate EXPIRATION_DATE = LocalDate.now().plusDays(DAYS_NUMBER);

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
                NOTIFICATION_SERVER,
                NOTIFICATION_TOPIC
        );
    }

    @Test
    void runBatchManually_successWithWallets() {

        List<Wallet> walletList = new ArrayList<>();
        walletList.add(TEST_WALLET_1);
        walletList.add(TEST_WALLET_2);

        when(walletRepositoryMock.findByInitiativeIdAndVoucherEndDateBefore(INITIATIVE_ID, EXPIRATION_DATE))
                .thenReturn(walletList);

        Mockito.doNothing()
                .when(notificationProducerMock)
                .sendNotification(any(NotificationQueueDTO.class));

        // Act
        voucherExpirationReminderBatchService.runBatchManually(INITIATIVE_ID, DAYS_NUMBER);

        // Assert
        //Verify that the repository has been called 1 time
        verify(walletRepositoryMock, times(1)).findByInitiativeIdAndVoucherEndDateBefore(INITIATIVE_ID, EXPIRATION_DATE);
        //Verify that the NotificationProducer was called 2 time
        verify(notificationProducerMock, times(2)).sendNotification(any(NotificationQueueDTO.class));
        //Verify that the ErrorProducer has NEVER been called
        verify(errorProducerMock, never()).sendEvent(any());
    }

    @Test
    void runBatchManually_successWithNoWallets() {

        when(walletRepositoryMock.findByInitiativeIdAndVoucherEndDateBefore(INITIATIVE_ID, EXPIRATION_DATE))
                .thenReturn(Collections.emptyList());

        //act
        voucherExpirationReminderBatchService.runBatchManually(INITIATIVE_ID, DAYS_NUMBER);

        //assert
        //Verify that the repository has been called 1 time
        verify(walletRepositoryMock, times(1)).findByInitiativeIdAndVoucherEndDateBefore(INITIATIVE_ID, EXPIRATION_DATE);

        //Verify that the NotificationProducer has NEVER been called
        verify(notificationProducerMock, never()).sendNotification(any(NotificationQueueDTO.class));

        // Verify that the ErrorProducer has NEVER been called
        verify(errorProducerMock, never()).sendEvent(any());
    }


    @Test
    void runBatchManually_errorSendingNotification() {
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(TEST_WALLET_1);

        when(walletRepositoryMock.findByInitiativeIdAndVoucherEndDateBefore(INITIATIVE_ID, EXPIRATION_DATE))
                .thenReturn(walletList);

        //Simulates an exception when sending the notification
        doThrow(new RuntimeException("Kafka connection failed")).when(notificationProducerMock)
                .sendNotification(any(NotificationQueueDTO.class));

        // Act
        voucherExpirationReminderBatchService.runBatchManually(INITIATIVE_ID, DAYS_NUMBER);

        // Assert
        // Verify that the repository has been called 1 time
        verify(walletRepositoryMock, times(1)).findByInitiativeIdAndVoucherEndDateBefore(INITIATIVE_ID, EXPIRATION_DATE);

        // Verify that the NotificationProducer was called 1 time (and failed)
        verify(notificationProducerMock, times(1)).sendNotification(any(NotificationQueueDTO.class));

        // Verify that the ErrorProducer has been called 1 time
        verify(errorProducerMock, times(1)).sendEvent(any(Message.class));
    }


    @Test
    void runBatchManually_errorInFirstNotification() {
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(TEST_WALLET_1);
        walletList.add(TEST_WALLET_2);


        when(walletRepositoryMock.findByInitiativeIdAndVoucherEndDateBefore(INITIATIVE_ID, EXPIRATION_DATE))
                .thenReturn(walletList);

        //The first send fails, but not the second.
        doThrow(new RuntimeException("Kafka connection failed for wallet 1"))
                .doNothing() // The second wallet will be successful (if the cycle continues)
                .when(notificationProducerMock).sendNotification(any(NotificationQueueDTO.class));

        // Act
        voucherExpirationReminderBatchService.runBatchManually(INITIATIVE_ID, DAYS_NUMBER);

        // Assert
        // Verify that the NotificationProducer was called 2 time
        verify(notificationProducerMock, times(2)).sendNotification(any(NotificationQueueDTO.class));

        // Verify that the ErrorProducer has been called 1 time
        verify(errorProducerMock, times(1)).sendEvent(any(Message.class));
    }


}
