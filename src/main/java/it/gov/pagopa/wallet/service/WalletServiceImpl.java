package it.gov.pagopa.wallet.service;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.wallet.connector.OnboardingRestConnector;
import it.gov.pagopa.wallet.connector.PaymentInstrumentRestConnector;
import it.gov.pagopa.wallet.connector.PaymentRestConnector;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.dto.mapper.TimelineMapper;
import it.gov.pagopa.wallet.dto.mapper.WalletMapper;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeCreationRequest;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeEnrichedResponse;
import it.gov.pagopa.wallet.enums.BeneficiaryType;
import it.gov.pagopa.wallet.enums.ChannelTransaction;
import it.gov.pagopa.wallet.enums.SyncTrxStatus;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.event.producer.ErrorProducer;
import it.gov.pagopa.wallet.event.producer.IbanProducer;
import it.gov.pagopa.wallet.event.producer.NotificationProducer;
import it.gov.pagopa.wallet.event.producer.TimelineProducer;
import it.gov.pagopa.wallet.exception.WalletUpdateException;
import it.gov.pagopa.wallet.exception.custom.*;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.model.Wallet.RefundHistory;
import it.gov.pagopa.wallet.repository.WalletRepository;
import it.gov.pagopa.wallet.repository.WalletUpdatesRepository;
import it.gov.pagopa.wallet.utils.AuditUtilities;
import it.gov.pagopa.wallet.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.iban4j.IbanUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.*;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.*;

@Slf4j
@Service
public class WalletServiceImpl implements WalletService {

  public static final String SERVICE_ENROLL_IBAN = "ENROLL_IBAN";
  public static final String SERVICE_UNSUBSCRIBE = "UNSUBSCRIBE";
  public static final String SERVICE_CHECK_IBAN_OUTCOME = "CHECK_IBAN_OUTCOME";
  public static final String SERVICE_ENROLL_INSTRUMENT_ISSUER = "ENROLL_INSTRUMENT_ISSUER";
  public static final String SERVICE_PROCESS_REFUND = "PROCESS_REFUND";
  public static final String SERVICE_PROCESS_TRANSACTION = "PROCESS_TRANSACTION";
  public static final String SERVICE_COMMAND_DELETE_INITIATIVE = "DELETE_INITIATIVE";
  public static final String WALLET_STATUS_UNSUBSCRIBED_MESSAGE = "wallet in status unsubscribed";
  public static final String SERVICE_ENROLL_INSTRUMENT_CODE = "ENROLL_INSTRUMENT_CODE";
  private final WalletRepository walletRepository;
  private final WalletUpdatesRepository walletUpdatesRepository;
  private final PaymentInstrumentRestConnector paymentInstrumentRestConnector;
  private final PaymentRestConnector paymentRestConnector;
  private final OnboardingRestConnector onboardingRestConnector;
  private final IbanProducer ibanProducer;
  private final TimelineProducer timelineProducer;
  private final WalletMapper walletMapper;
  private final TimelineMapper timelineMapper;
  private final ErrorProducer errorProducer;
  private final NotificationProducer notificationProducer;
  private final AuditUtilities auditUtilities;
  private final String timelineServer;
  private final String timelineTopic;
  private final String notificationServer;
  private final String notificationTopic;
  private final String ibanServer;
  private final String ibanTopic;
  private final String transactionServer;
  private final String transactionTopic;
  private final boolean isFormalControlIban;
  private final int pageSize;
  private final long delay;

    public WalletServiceImpl(WalletRepository walletRepository,
                           WalletUpdatesRepository walletUpdatesRepository,
                           PaymentInstrumentRestConnector paymentInstrumentRestConnector,
                           PaymentRestConnector  paymentRestConnector,
                           OnboardingRestConnector onboardingRestConnector,
                           IbanProducer ibanProducer,
                           TimelineProducer timelineProducer,
                           WalletMapper walletMapper,
                           TimelineMapper timelineMapper,
                           ErrorProducer errorProducer,
                           NotificationProducer notificationProducer,
                           AuditUtilities auditUtilities,
                           @Value("${spring.cloud.stream.binders.kafka-timeline.environment.spring.cloud.stream.kafka.binder.brokers}") String timelineServer,
                           @Value("${spring.cloud.stream.bindings.walletQueue-out-1.destination}") String timelineTopic,
                           @Value("${spring.cloud.stream.binders.kafka-notification.environment.spring.cloud.stream.kafka.binder.brokers}") String notificationServer,
                           @Value("${spring.cloud.stream.bindings.walletQueue-out-2.destination}") String notificationTopic,
                           @Value("${spring.cloud.stream.binders.kafka-iban.environment.spring.cloud.stream.kafka.binder.brokers}") String ibanServer,
                           @Value("${spring.cloud.stream.bindings.walletQueue-out-0.destination}") String ibanTopic,
                           @Value("${spring.cloud.stream.binders.kafka-re.environment.spring.cloud.stream.kafka.binder.brokers}") String transactionServer,
                           @Value("${spring.cloud.stream.bindings.consumerRefund-in-0.destination}") String transactionTopic,
                           @Value("${app.iban.formalControl}") boolean isFormalControlIban,
                           @Value("${app.delete.paginationSize}") int pageSize,
                           @Value("${app.delete.delayTime}") long delay) {
    this.walletRepository = walletRepository;
    this.walletUpdatesRepository = walletUpdatesRepository;
    this.paymentInstrumentRestConnector = paymentInstrumentRestConnector;
    this.paymentRestConnector = paymentRestConnector;
    this.onboardingRestConnector = onboardingRestConnector;
    this.ibanProducer = ibanProducer;
    this.timelineProducer = timelineProducer;
    this.walletMapper = walletMapper;
    this.timelineMapper = timelineMapper;
    this.errorProducer = errorProducer;
    this.notificationProducer = notificationProducer;
    this.auditUtilities = auditUtilities;
    this.timelineServer = timelineServer;
    this.timelineTopic = timelineTopic;
    this.notificationServer = notificationServer;
    this.notificationTopic = notificationTopic;
    this.ibanServer = ibanServer;
    this.ibanTopic = ibanTopic;
    this.transactionServer = transactionServer;
    this.transactionTopic = transactionTopic;
    this.isFormalControlIban = isFormalControlIban;
    this.pageSize = pageSize;
    this.delay = delay;
  }

  @Override
  public EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    performanceLog(startTime, "GET_ENROLLMENT_STATUS");
    return new EnrollmentStatusDTO(wallet.getStatus());
  }

  @Override
  public WalletDTO getWalletDetail(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();
    Wallet wallet = getWallet(initiativeId, userId);
    performanceLog(startTime, "GET_WALLET_DETAIL");
    return walletMapper.toInitiativeDTO(wallet);
  }

  @Override
  public WalletDTO getWalletDetailIssuer(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();
    Wallet wallet = getWallet(initiativeId, userId);
    performanceLog(startTime, "GET_WALLET_DETAIL_ISSUER");
    return walletMapper.toIssuerInitiativeDTO(wallet);
  }

  private Wallet getWallet(String initiativeId, String userId) {
    return walletRepository
            .findById(generateWalletId(userId, initiativeId))
            .orElseThrow(() -> new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, initiativeId)));
  }

    @Override
  public void enrollInstrument(String initiativeId, String userId, String idWallet, String channel) {
    long startTime = System.currentTimeMillis();

    log.info("[ENROLL_INSTRUMENT] Checking the status of initiative {}", initiativeId);
    auditUtilities.logEnrollmentInstrument(userId, initiativeId, idWallet, channel);

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);


    if (WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT.equals(wallet.getInitiativeRewardType())) {
      auditUtilities.logEnrollmentInstrumentKO(
          userId, initiativeId, idWallet, "the initiative is discount type");
      log.error("[ENROLL_INSTRUMENT] It is not possible to enroll a payment instrument for the discount type initiative {}", initiativeId);
      throw new EnrollmentNotAllowedException(
              ENROLL_INSTRUMENT_DISCOUNT_INITIATIVE, PAYMENT_INSTRUMENT_ENROLL_NOT_ALLOWED_DISCOUNT_MSG);
    }

    checkEndDate(wallet.getInitiativeEndDate(), initiativeId);

    if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      auditUtilities.logEnrollmentInstrumentKO(
          userId, initiativeId, idWallet, WALLET_STATUS_UNSUBSCRIBED_MESSAGE);
      log.error("[ENROLL_INSTRUMENT] The user {} has unsubscribed from initiative {}", userId, initiativeId);
      throw new UserUnsubscribedException(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, initiativeId));
    }
    InstrumentCallBodyDTO dto =
        new InstrumentCallBodyDTO(userId, initiativeId, idWallet, channel,
            WalletConstants.INSTRUMENT_TYPE_CARD);

    try {
      log.info("[ENROLL_INSTRUMENT] Calling Payment Instrument");
      paymentInstrumentRestConnector.enrollInstrument(dto);
      performanceLog(startTime, "ENROLL_INSTRUMENT");
    } catch (ServiceException e) {
      sendRejectedInstrumentToTimeline(initiativeId, userId, channel,
          WalletConstants.INSTRUMENT_TYPE_CARD, WalletConstants.REJECTED_ADD_INSTRUMENT);
      log.error("[ENROLL_INSTRUMENT] Error in Payment Instrument Request");
      auditUtilities.logEnrollmentInstrumentKO(
          userId, initiativeId, idWallet, "error in payment instrument request");
      performanceLog(startTime, "ENROLL_INSTRUMENT");
      throw e;
    }
  }

  @Override
  public void deleteInstrument(String initiativeId, String userId, String instrumentId, String channel) {
    long startTime = System.currentTimeMillis();

    log.info("[DELETE_INSTRUMENT] Checking the status of initiative {}", initiativeId);
    auditUtilities.logInstrumentDeleted(userId, initiativeId);

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);

    checkEndDate(wallet.getInitiativeEndDate(), initiativeId);

    DeactivationBodyDTO dto = new DeactivationBodyDTO(userId, initiativeId, instrumentId, channel);

    try {
      paymentInstrumentRestConnector.deleteInstrument(dto);
      performanceLog(startTime, "DELETE_INSTRUMENT");
    } catch (ServiceException e) {
      sendRejectedInstrumentToTimeline(initiativeId, userId, channel,
          null, WalletConstants.REJECTED_DELETE_INSTRUMENT);
      performanceLog(startTime, "DELETE_INSTRUMENT");
      throw e;
    }
  }

  @Override
  public void enrollIban(
      String initiativeId, String userId, String iban, String channel, String description) {
    long startTime = System.currentTimeMillis();

    log.info("[ENROLL_IBAN] Checking the status of initiative {}", initiativeId);
    auditUtilities.logEnrollmentIban(userId, initiativeId, channel);

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);

    if (WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT.equals(wallet.getInitiativeRewardType())) {
      performanceLog(startTime, SERVICE_ENROLL_IBAN);
      auditUtilities.logEnrollmentIbanKO(
          "the initiative is discount type", userId, initiativeId, channel);
      log.error("[ENROLL_IBAN] It is not possible enroll an iban for the discount type initiative {}", initiativeId);
      throw new EnrollmentNotAllowedException(
              ENROLL_IBAN_DISCOUNT_INITIATIVE, String.format(IBAN_ENROLL_NOT_ALLOWED_DISCOUNT_MSG, initiativeId));
    }

    checkEndDate(wallet.getInitiativeEndDate(), initiativeId);
    if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      performanceLog(startTime, SERVICE_ENROLL_IBAN);
      auditUtilities.logEnrollmentIbanKO(
          WALLET_STATUS_UNSUBSCRIBED_MESSAGE, userId, initiativeId, channel);
      log.error("[ENROLL_IBAN] The user {} has unsubscribed from initiative {}",userId, initiativeId);
      throw new UserUnsubscribedException(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, initiativeId));
    }

    if (wallet.getIban() != null && (wallet.getIban().equals(iban))) {
      log.info("[ENROLL_IBAN] The IBAN matches with the one already enrolled");
      performanceLog(startTime, SERVICE_ENROLL_IBAN);
      auditUtilities.logEnrollmentIbanKO(
          "the IBAN matches with the one already enrolled", userId, initiativeId, channel);
      return;
    }

    iban = iban.toUpperCase();
    if (!iban.startsWith("IT")) {
      performanceLog(startTime, SERVICE_ENROLL_IBAN);
      auditUtilities.logEnrollmentIbanValidationKO(iban);
      log.error("[ENROLL_IBAN] Iban inserted from the user {} is not italian", userId);
      throw new InvalidIbanException(String.format(ERROR_IBAN_NOT_ITALIAN, iban));
    }
    if (isFormalControlIban) {
      formalControl(iban);
    }
    wallet.setIban(iban);
    IbanQueueDTO ibanQueueDTO =
        new IbanQueueDTO(userId, initiativeId, iban, description, channel, LocalDateTime.now());

    walletUpdatesRepository.enrollIban(initiativeId, userId, iban, setStatus(wallet));
    auditUtilities.logEnrollmentIbanComplete(userId, initiativeId, iban);

    try {
      log.info("[ENROLL_IBAN] Sending event to IBAN");
      ibanProducer.sendIban(ibanQueueDTO);
    } catch (Exception e) {
      log.error("[ENROLL_IBAN] An error has occurred. Sending message to Error queue", e);
      auditUtilities.logEnrollmentIbanKO(
          "error in sending request to checkIban", userId, initiativeId, channel);
      final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(ibanQueueDTO);
      this.sendToQueueError(e, errorMessage, ibanServer, ibanTopic);
      performanceLog(startTime, SERVICE_ENROLL_IBAN);
    }

    performanceLog(startTime, SERVICE_ENROLL_IBAN);
  }

  @Override
  public void suspendWallet(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    if (!wallet.getStatus().equals(WalletStatus.SUSPENDED)) {
      if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
        performanceLog(startTime, WalletConstants.SUSPENSION);
        auditUtilities.logSuspensionKO(userId, initiativeId);
        log.error("[SUSPENSION] The user {} has unsubscribed from initiative {}", userId, initiativeId);
        throw new UserUnsubscribedException(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, initiativeId));
      }

      LocalDateTime localDateTime = LocalDateTime.now();
      String backupStatus = wallet.getStatus();
      try {
        walletUpdatesRepository.suspendWallet(
            initiativeId, userId, WalletStatus.SUSPENDED, localDateTime);
        log.info("[SUSPENSION] Sending event to ONBOARDING");
        onboardingRestConnector.suspendOnboarding(initiativeId, userId);
      } catch (Exception e) {
        auditUtilities.logSuspensionKO(userId, initiativeId);
        this.rollbackWallet(backupStatus, wallet);
        performanceLog(startTime, WalletConstants.SUSPENSION);
        throw e;
      }
       sendToTimeline(timelineMapper.suspendToTimeline(initiativeId, userId, localDateTime));
      sendSuspensionReadmissionNotification(
          WalletConstants.SUSPENSION, initiativeId, userId, wallet.getInitiativeName());

      log.info("[SUSPENSION] Wallet is suspended from the initiative {}", initiativeId);
      auditUtilities.logSuspension(userId, initiativeId);
    }
    performanceLog(startTime, WalletConstants.SUSPENSION);
  }

  @Override
  public void readmitWallet(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      performanceLog(startTime, WalletConstants.READMISSION);
      auditUtilities.logReadmissionKO(userId, initiativeId);
      log.info(
          "[READMISSION] Wallet readmission to the initiative {} is not possible", initiativeId);
      throw new UserUnsubscribedException(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, initiativeId));
    }

    LocalDateTime localDateTime = LocalDateTime.now();
    String backupStatus = wallet.getStatus();
    LocalDateTime backupSuspensionDate = wallet.getSuspensionDate();
    String readmittedStatus;
    if (WalletConstants.INITIATIVE_REWARD_TYPE_REFUND.equals(wallet.getInitiativeRewardType())) {
      readmittedStatus = WalletStatus.getByBooleans(wallet.getIban() != null,
          wallet.getNInstr() > 0).name();
    } else {
      readmittedStatus = WalletStatus.REFUNDABLE.name();
    }
    try {
      walletUpdatesRepository.readmitWallet(initiativeId, userId, readmittedStatus, localDateTime);
      log.info("[READMISSION] Sending event to ONBOARDING");
      onboardingRestConnector.readmitOnboarding(initiativeId, userId);
    } catch (Exception e) {
      auditUtilities.logReadmissionKO(userId, initiativeId);
      log.info("[READMISSION] Wallet readmission to the initiative {} is failed", initiativeId);
      wallet.setStatus(backupStatus);
      wallet.setSuspensionDate(backupSuspensionDate);
      wallet.setUpdateDate(localDateTime);
      walletRepository.save(wallet);
      performanceLog(startTime, WalletConstants.READMISSION);
      throw e;
    }
    sendToTimeline(timelineMapper.readmitToTimeline(initiativeId, userId, localDateTime));
    sendSuspensionReadmissionNotification(
        WalletConstants.READMISSION, initiativeId, userId, wallet.getInitiativeName());

    log.info("[READMISSION] Wallet is readmitted to the initiative {}", initiativeId);
    auditUtilities.logReadmission(userId, initiativeId);
    performanceLog(startTime, WalletConstants.READMISSION);
  }

  @Override
  public InitiativeListDTO getInitiativeList(String userId) {
    long startTime = System.currentTimeMillis();
    List<Wallet> walletList = walletRepository.findByUserId(userId);
    walletList.sort(Comparator.comparing(Wallet::getAcceptanceDate).reversed());
    InitiativeListDTO initiativeListDTO = new InitiativeListDTO();
    List<WalletDTO> walletDTOList = new ArrayList<>();

    for (Wallet wallet : walletList) {
      walletDTOList.add(walletMapper.toInitiativeDTO(wallet));
    }
    initiativeListDTO.setInitiativeList(walletDTOList);

    performanceLog(startTime, "GET_INITIATIVE_LIST");
    return initiativeListDTO;
  }

  @Override
  public void createWallet(EvaluationDTO evaluationDTO) {
    log.info(
        "[CREATE_WALLET] EvaluationDTO received. userId: {}; initiativeId: {}; status: {}",
        evaluationDTO.getUserId(),
        evaluationDTO.getInitiativeId(),
        evaluationDTO.getStatus());

    long startTime = System.currentTimeMillis();

    /**
     * Condizione modificata per impedire che un familiare di un nucleo già partecipante
     * all'iniziativa generi un wallet separato, questa condizione sarà da rettificare nel caso di gestione
     * di diverse condizioni per iniziativa con un controllo su proprietà da definire che ne indichi il
     * tipo di iniziativa, e gestita con, per le diverse tipologie
     */
    if (WalletConstants.STATUS_ONBOARDING_OK.equals(evaluationDTO.getStatus())
//        || WalletConstants.STATUS_JOINED.equals(evaluationDTO.getStatus())
    ) {
      Wallet wallet = walletMapper.map(evaluationDTO);

      if (evaluationDTO.getFamilyId() != null) {
        List<Wallet> familyWallets =
            walletRepository.findByInitiativeIdAndFamilyId(
                evaluationDTO.getInitiativeId(), evaluationDTO.getFamilyId());
        if (!familyWallets.isEmpty()) {
          wallet.setAmountCents(familyWallets.get(0).getAmountCents());
        }
      }

      if (WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT.equals(
          evaluationDTO.getInitiativeRewardType())) {
        wallet.setStatus(WalletStatus.REFUNDABLE.name());
        wallet.setNInstr(1);
        paymentInstrumentRestConnector.enrollDiscountInitiative(
            InstrumentFromDiscountDTO.builder()
                .initiativeId(evaluationDTO.getInitiativeId())
                .userId(evaluationDTO.getUserId())
                .build());
      }

      log.info("[POST_PAYMENT_BAR_CODE_EXTENDED] Create the vocuher and return his start and end Date");
      TransactionBarCodeEnrichedResponse response = paymentRestConnector.createExtendedTransaction(TransactionBarCodeCreationRequest
              .builder()
              .initiativeId(evaluationDTO.getInitiativeId())
              .voucherAmountCents(evaluationDTO.getBeneficiaryBudgetCents())
              .build(), evaluationDTO.getUserId());
      wallet.setVoucherStartDate(Utilities.getLocalDate(response.getTrxDate()));
      wallet.setVoucherEndDate(Utilities.getLocalDate(response.getTrxEndDate()));

      walletRepository.save(wallet);
      sendToTimeline(timelineMapper.onboardingToTimeline(evaluationDTO));

      auditUtilities.logCreatedWallet(evaluationDTO.getUserId(), evaluationDTO.getInitiativeId());

    } else if (WalletConstants.STATUS_JOINED.equals(evaluationDTO.getStatus())) {
      auditUtilities.logCreateWalletStoppedForJoin(evaluationDTO.getUserId(), evaluationDTO.getInitiativeId());
    }

    performanceLog(startTime, "CREATE_WALLET");
  }

  @Override
  public void unsubscribe(String initiativeId, String userId, String channel) {
    long startTime = System.currentTimeMillis();

    log.info("[UNSUBSCRIBE] Unsubscribing user {} on initiative {}", userId, initiativeId);
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    LocalDateTime now = LocalDateTime.now();
    String statusTemp = wallet.getStatus();
    wallet.setRequestUnsubscribeDate(now);
    UnsubscribeCallDTO unsubscribeCallDTO =
        new UnsubscribeCallDTO(
            initiativeId, userId, wallet.getRequestUnsubscribeDate().toString(), channel);
    try {
      paymentInstrumentRestConnector.disableAllInstrument(unsubscribeCallDTO);
      log.info("[UNSUBSCRIBE] Payment instruments disabled on initiative {} for user {}",
          initiativeId, userId);
    } catch (ServiceException e) {
      performanceLog(startTime, SERVICE_UNSUBSCRIBE);
      auditUtilities.logUnsubscribeKO(
          userId, initiativeId, "request of disabling all payment instruments failed");
      log.info(
          "[UNSUBSCRIBE] Request of disabling all payment instruments on initiative {} for user {} failed",
          initiativeId, userId);
      throw e;
    }
    try {
      onboardingRestConnector.disableOnboarding(unsubscribeCallDTO);
      log.info("[UNSUBSCRIBE] Onboarding disabled on initiative {} for user {}", initiativeId,
          userId);
    } catch (ServiceException e) {
      paymentInstrumentRestConnector.rollback(initiativeId, userId);
      performanceLog(startTime, SERVICE_UNSUBSCRIBE);
      auditUtilities.logUnsubscribeKO(
          userId, initiativeId, "request of disabling onboarding failed");
      log.info("[UNSUBSCRIBE] Request of disabling onboarding on initiative {} for user {} failed",
          initiativeId, userId);
      throw e;
    }
    try {
      if (!wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
        wallet.setStatus(WalletStatus.UNSUBSCRIBED);
        wallet.setNInstr(0);
        wallet.setUpdateDate(now);
        walletRepository.save(wallet);
        auditUtilities.logUnsubscribe(userId, initiativeId);
        log.info("[UNSUBSCRIBE] Wallet disabled on initiative {} for user {}", initiativeId,
            userId);
      }
      performanceLog(startTime, SERVICE_UNSUBSCRIBE);
    } catch (Exception e) {
      this.rollbackWallet(statusTemp, wallet);
      onboardingRestConnector.rollback(initiativeId, userId);
      paymentInstrumentRestConnector.rollback(initiativeId, userId);
      performanceLog(startTime, SERVICE_UNSUBSCRIBE);
      auditUtilities.logUnsubscribeKO(
          userId, initiativeId, "request of disabling wallet failed");
      log.info("[UNSUBSCRIBE] Request of disabling wallet on initiative {} for user {} failed",
          initiativeId, userId);
      throw e;
    }
    QueueOperationDTO queueOperationDTO =
            timelineMapper.unsubscribeToTimeline(
                    wallet.getInitiativeId(),
                    wallet.getUserId(),
                    wallet.getRequestUnsubscribeDate()
            );
    sendToTimeline(queueOperationDTO);
  }


  @Override
  public void processTransaction(Message<RewardTransactionDTO> rewardTransactionDTOMessage) {
    RewardTransactionDTO rewardTransactionDTO = rewardTransactionDTOMessage.getPayload();
    long startTime = System.currentTimeMillis();
    String trxStatus = rewardTransactionDTO.getStatus();

    if(trxStatus.equals("CAPTURED")){
        log.info("[PROCESS_TRANSACTION with status captured]");
        String initiativeId = rewardTransactionDTO.getRewards().isEmpty() ? null :
                rewardTransactionDTO.getRewards().keySet().iterator().next();
        if(initiativeId!=null){
            updateWalletFromTransactionCaptured(initiativeId,rewardTransactionDTO.getUserId());
        }
    }

    if (SyncTrxStatus.EXPIRED.name().equals(trxStatus) ||
        SyncTrxStatus.REFUNDED.name().equals(trxStatus)) {
      log.info("[PROCESS_TRANSACTION] Encountered transaction with id {} with status {}, " +
                      "unsubscribing from the initiative {}",
              rewardTransactionDTO.getId(), trxStatus, rewardTransactionDTO.getInitiativeId());
      try {
        unsubscribe(rewardTransactionDTO.getInitiativeId(),
                rewardTransactionDTO.getUserId(), rewardTransactionDTO.getChannel());
      } catch (Exception e) {
        log.error("[PROCESS_TRANSACTION] Encountered error while processing initiative " +
                "unsubscribe due to transaction {} in status {}",
                rewardTransactionDTO.getId(), rewardTransactionDTO.getStatus());
        final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(rewardTransactionDTO);
        sendToQueueError(e, rewardTransactionDTOMessage, errorMessage, transactionServer, transactionTopic);
      }
    }

    if (!rewardTransactionDTO.getStatus().equals("REWARDED")
        && !(ChannelTransaction.isChannelPresent(rewardTransactionDTO.getChannel())
        && (rewardTransactionDTO.getStatus().equals("AUTHORIZED")
        || rewardTransactionDTO.getStatus().equals("CANCELLED")))) {
      log.info("[PROCESS_TRANSACTION] Transaction not in status REWARDED, skipping message");
      performanceLog(startTime, SERVICE_PROCESS_TRANSACTION);
      return;
    }
    log.info("[PROCESS_TRANSACTION] New trx from Rule Engine");
    rewardTransactionDTO
        .getRewards()
        .forEach(
            (initiativeId, reward) -> {
              log.info("[PROCESS_TRANSACTION] Processing initiative: {}", initiativeId);
              try {
                updateWalletFromTransaction(
                    initiativeId,
                    rewardTransactionDTO,
                    reward.getCounters(),
                    reward.getAccruedRewardCents());
              } catch (WalletUpdateException e) {
                log.error(
                    "[PROCESS_TRANSACTION] An error has occurred. Sending message to Error queue",
                    e);
                final MessageBuilder<?> errorMessage =
                    MessageBuilder.withPayload(rewardTransactionDTO);
                this.sendToQueueError(e, errorMessage, transactionServer, transactionTopic);
                performanceLog(startTime, SERVICE_PROCESS_TRANSACTION);
              }
            });
    performanceLog(startTime, SERVICE_PROCESS_TRANSACTION);
  }

  @Override
  public void updateWallet(WalletPIBodyDTO walletPIDTO) {
    long startTime = System.currentTimeMillis();

    log.info("[UPDATE_WALLET] New revoke from PM");
    for (WalletPIDTO walletPI : walletPIDTO.getWalletDTOlist()) {
      Wallet wallet =
          walletRepository
              .findById(generateWalletId(walletPI.getUserId(), walletPI.getInitiativeId()))
              .orElse(null);
      if (wallet == null) {
        log.info(
            "[UPDATE_WALLET] Wallet with initiativeId {} not found", walletPI.getInitiativeId());
        continue;
      }
      wallet.setNInstr(wallet.getNInstr() - 1);
      walletUpdatesRepository.decreaseInstrumentNumber(
          walletPI.getInitiativeId(), walletPI.getUserId(), setStatus(wallet));
      QueueOperationDTO queueOperationDTO =
          timelineMapper.deleteInstrumentToTimeline(
              wallet.getInitiativeId(),
              wallet.getUserId(),
              walletPI.getMaskedPan(),
              walletPI.getBrandLogo(),
              walletPI.getBrand());

      sendToTimeline(queueOperationDTO);
    }
    performanceLog(startTime, "UPDATE_WALLET");
  }

  @Override
  public void processAck(InstrumentAckDTO instrumentAckDTO) {

    long startTime = System.currentTimeMillis();

    log.info(
        "[PROCESS_ACK] Processing new ack {} from PaymentInstrument",
        instrumentAckDTO.getOperationType());

    Wallet wallet =
        walletRepository
            .findById(generateWalletId(instrumentAckDTO.getUserId(), instrumentAckDTO.getInitiativeId()))
            .orElse(null);

    if (wallet == null) {
      log.error("[PROCESS_ACK] Wallet not found for the user {}", instrumentAckDTO.getUserId());
      performanceLog(startTime, "PROCESS_ACK");
      throw new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, instrumentAckDTO.getInitiativeId()));
    }

    if (!instrumentAckDTO.getOperationType().equals(WalletConstants.REJECTED_ADD_INSTRUMENT)) {
      wallet.setNInstr(instrumentAckDTO.getNinstr());

      String status = WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT.equals(wallet.getInitiativeRewardType()) ?
              wallet.getStatus() : setStatus(wallet);

      walletUpdatesRepository.updateInstrumentNumber(
              instrumentAckDTO.getInitiativeId(),
              instrumentAckDTO.getUserId(),
              instrumentAckDTO.getNinstr(),
              status);

    }

    QueueOperationDTO queueOperationDTO = timelineMapper.ackToTimeline(instrumentAckDTO);

    sendToTimeline(queueOperationDTO);
    performanceLog(startTime, "PROCESS_ACK");
  }

  @Override
  public void processRefund(RefundDTO refundDTO) {
    long startTime = System.currentTimeMillis();

    if (BeneficiaryType.MERCHANT.equals(refundDTO.getBeneficiaryType())) {
      log.info("[PROCESS_REFUND] Beneficiary type is a merchant, skipping message");
      performanceLog(startTime, SERVICE_PROCESS_REFUND);
      return;
    }

    log.info("[PROCESS_REFUND] Processing new refund");

    Wallet wallet =
        walletRepository
            .findById(generateWalletId(refundDTO.getBeneficiaryId(), refundDTO.getInitiativeId()))
            .orElse(null);

    if (wallet == null) {
      log.info("[PROCESS_REFUND] Wallet not found, skipping message");
      performanceLog(startTime, SERVICE_PROCESS_REFUND);
      return;
    }

    Map<String, RefundHistory> history = wallet.getRefundHistory();

    if (history == null) {
      history = new HashMap<>();
    }

    if (history.containsKey(refundDTO.getRewardNotificationId())
        && history.get(refundDTO.getRewardNotificationId()).getFeedbackProgressive()
        >= refundDTO.getFeedbackProgressive()) {
      log.info("[PROCESS_REFUND] Feedback already processed, skipping message");
      performanceLog(startTime, SERVICE_PROCESS_REFUND);
      return;
    }

    Long refunded = refundDTO.getRewardCents();

    history.put(
        refundDTO.getRewardNotificationId(), new RefundHistory(refundDTO.getFeedbackProgressive()));

    walletUpdatesRepository.processRefund(
        refundDTO.getInitiativeId(),
        refundDTO.getBeneficiaryId(),
        wallet.getRefundedCents() - refunded,
        history);

    QueueOperationDTO queueOperationDTO = timelineMapper.refundToTimeline(refundDTO);

    sendToTimeline(queueOperationDTO);
    sendRefundNotification(refundDTO);

    performanceLog(startTime, SERVICE_PROCESS_REFUND);
  }

  @Override
  public void enrollInstrumentIssuer(String initiativeId, String userId, InstrumentIssuerDTO body) {
    long startTime = System.currentTimeMillis();

    log.info("[ENROLL_INSTRUMENT_ISSUER] Checking the status of initiative {}", initiativeId);
    auditUtilities.logEnrollmentInstrumentIssuer(userId, initiativeId, body.getChannel());

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);

    if (WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT.equals(wallet.getInitiativeRewardType())) {
      performanceLog(startTime, SERVICE_ENROLL_INSTRUMENT_ISSUER);
      log.error("[ENROLL_INSTRUMENT_ISSUER] It is not possible to enroll a payment instrument for the discount type initiative {}", initiativeId);
      throw new EnrollmentNotAllowedException(
              ENROLL_INSTRUMENT_DISCOUNT_INITIATIVE, String.format(PAYMENT_INSTRUMENT_ENROLL_NOT_ALLOWED_DISCOUNT_MSG, initiativeId));
    }

    checkEndDate(wallet.getInitiativeEndDate(), initiativeId);

    if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      performanceLog(startTime, SERVICE_ENROLL_INSTRUMENT_ISSUER);
      log.error("[ENROLL_INSTRUMENT_ISSUER] The user {} has unsubscribed from initiative {}", userId, initiativeId);
      throw new UserUnsubscribedException(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, initiativeId));
    }

    InstrumentIssuerCallDTO instrumentIssuerCallDTO =
        InstrumentIssuerCallDTO.builder()
            .initiativeId(initiativeId)
            .userId(userId)
            .hpan(body.getHpan())
            .channel(body.getChannel())
            .instrumentType(WalletConstants.INSTRUMENT_TYPE_CARD)
            .brandLogo(body.getBrandLogo())
            .brand(body.getBrand())
            .maskedPan(body.getMaskedPan())
            .build();

    try {
      log.info("[ENROLL_INSTRUMENT_ISSUER] Calling Payment Instrument");
      paymentInstrumentRestConnector.enrollInstrumentIssuer(instrumentIssuerCallDTO);
      performanceLog(startTime, SERVICE_ENROLL_INSTRUMENT_ISSUER);
    } catch (ServiceException e) {
      sendRejectedInstrumentToTimeline(initiativeId, userId, body.getChannel(),
          WalletConstants.INSTRUMENT_TYPE_CARD, WalletConstants.REJECTED_ADD_INSTRUMENT);
      log.error("[ENROLL_INSTRUMENT_ISSUER] Error in Payment Instrument Request");
      performanceLog(startTime, SERVICE_ENROLL_INSTRUMENT_ISSUER);
      throw e;
    }
  }

  @Override
  public void processCommand(QueueCommandOperationDTO queueCommandOperationDTO) {
    if ((SERVICE_COMMAND_DELETE_INITIATIVE).equals(queueCommandOperationDTO.getOperationType())) {
      long startTime = System.currentTimeMillis();

      List<Wallet> deletedWallets = new ArrayList<>();
      List<Wallet> fetchedWallets;

      do {
        fetchedWallets = walletUpdatesRepository.deletePaged(queueCommandOperationDTO.getEntityId(), pageSize);
        deletedWallets.addAll(fetchedWallets);
        try{
          Thread.sleep(delay);
        } catch (InterruptedException e){
          log.error("An error has occurred while waiting {}", e.getMessage());
          Thread.currentThread().interrupt();
        }
      } while (fetchedWallets.size() == pageSize);

      log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: wallet", queueCommandOperationDTO.getEntityId());
      deletedWallets.forEach(deletedWallet -> auditUtilities.logDeletedWallet(deletedWallet.getUserId(), deletedWallet.getInitiativeId()));
      performanceLog(startTime, SERVICE_COMMAND_DELETE_INITIATIVE);
    }
  }

  @Override
  public void enrollInstrumentCode(String initiativeId, String userId, String channel) {
    long startTime = System.currentTimeMillis();

    log.info("[ENROLL_INSTRUMENT_CODE] Checking the status of initiative {}", initiativeId);
    auditUtilities.logEnrollmentInstrumentCode(userId, initiativeId, channel);

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);

    if (WalletConstants.INITIATIVE_REWARD_TYPE_REFUND.equals(wallet.getInitiativeRewardType())) {
      performanceLog(startTime, SERVICE_ENROLL_INSTRUMENT_CODE);
      auditUtilities.logEnrollmentInstrumentCodeKO(
          userId, initiativeId, "the initiative is refund type", channel);
      log.error("[ENROLL_INSTRUMENT_CODE] It is not possible to enroll an idpayCode for the refund type initiative {}", initiativeId);
      throw new EnrollmentNotAllowedException(
              ENROLL_INSTRUMENT_REFUND_INITIATIVE, String.format(PAYMENT_INSTRUMENT_ENROLL_NOT_ALLOWED_REFUND_MSG, initiativeId));
    }

    checkEndDate(wallet.getInitiativeEndDate(), initiativeId);

    if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      performanceLog(startTime, SERVICE_ENROLL_INSTRUMENT_CODE);
      auditUtilities.logEnrollmentInstrumentCodeKO(
          userId, initiativeId, WALLET_STATUS_UNSUBSCRIBED_MESSAGE, channel);
      log.error("[ENROLL_INSTRUMENT_CODE] The user {} has unsubscribed from initiative {}", userId, initiativeId);
      throw new UserUnsubscribedException(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, initiativeId));
    }

    InstrumentCallBodyDTO dto = InstrumentCallBodyDTO.builder()
        .userId(userId)
        .initiativeId(initiativeId)
        .channel(channel)
        .instrumentType(WalletConstants.INSTRUMENT_TYPE_IDPAYCODE)
        .build();

    try {
      log.info("[ENROLL_INSTRUMENT_CODE] Calling Payment Instrument");
      paymentInstrumentRestConnector.enrollInstrumentCode(dto);
      performanceLog(startTime, SERVICE_ENROLL_INSTRUMENT_CODE);
    } catch (ServiceException e) {
      sendRejectedInstrumentToTimeline(initiativeId, userId, dto.getChannel(),
          dto.getInstrumentType(), WalletConstants.REJECTED_ADD_INSTRUMENT);

      log.error("[ENROLL_INSTRUMENT_CODE] Error in Payment Instrument Request");
      auditUtilities.logEnrollmentInstrumentCodeKO(
          userId, initiativeId, "error in payment instrument request", channel);

      performanceLog(startTime, SERVICE_ENROLL_INSTRUMENT_CODE);
      throw e;
    }
  }


  private void updateWalletFromTransactionCaptured(String initiativeId, String userId ){
      Wallet userWallet = walletRepository.findById(generateWalletId(userId, initiativeId)).orElse(null);
      if (userWallet == null) {
          log.info("[UPDATE_WALLET_FROM_TRANSACTION_CAPTURED] No wallet found for user {} and initiativeId {}",
                  userId, initiativeId);
          return;
      }
      userWallet.setAmountCents(0L);
      userWallet.setUpdateDate(LocalDateTime.now());
      walletRepository.save(userWallet);
  }
  private void updateWalletFromTransaction(
      String initiativeId,
      RewardTransactionDTO rewardTransactionDTO,
      Counters counters,
      Long accruedRewardCents) {

    if (!(ChannelTransaction.isChannelPresent(rewardTransactionDTO.getChannel())
        && rewardTransactionDTO.getStatus().equals("REWARDED"))) {

      Wallet userWallet = walletRepository.findById(generateWalletId(rewardTransactionDTO.getUserId(), initiativeId)).orElse(null);

      if (userWallet == null) {
        log.info("[UPDATE_WALLET_FROM_TRANSACTION] No wallet found for user {} and initiativeId {}",
            rewardTransactionDTO.getUserId(), initiativeId);
        return;
      }

      if (userWallet.getFamilyId() == null) {
        rewardUserTransaction(initiativeId, rewardTransactionDTO, counters, userWallet);
      }
      else {
        rewardFamilyUserTransaction(initiativeId, rewardTransactionDTO, counters, userWallet);
      }
    }

    log.info("[UPDATE_WALLET_FROM_TRANSACTION] Sending transaction to Timeline");
    sendToTimeline(timelineMapper.transactionToTimeline(initiativeId, rewardTransactionDTO, accruedRewardCents));
  }

  private void rewardFamilyUserTransaction(String initiativeId, RewardTransactionDTO rewardTransactionDTO, Counters counters, Wallet userWallet) {
    if(userWallet.getCounterVersion() < counters.getVersion()) {
      log.info(
              "[UPDATE_WALLET_FROM_TRANSACTION][FAMILY_WALLET] Family {} total reward: {}",
              userWallet.getFamilyId(),
              counters.getTotalRewardCents());

      Long budgetCents = rewardTransactionDTO.getVoucherAmountCents() != null ? rewardTransactionDTO.getVoucherAmountCents() : counters.getInitiativeBudgetCents();
      boolean updateResult =
              walletUpdatesRepository.rewardFamilyTransaction(
                      initiativeId,
                      userWallet.getFamilyId(),
                      rewardTransactionDTO.getElaborationDateTime(),
                      budgetCents - counters.getTotalRewardCents(),
                      counters.getVersion());

      if (!updateResult) {
        throw new WalletUpdateException(
                "[UPDATE_WALLET_FROM_TRANSACTION][FAMILY_WALLET] Something went wrong updating wallet(s) of family having id: %s"
                        .formatted(userWallet.getFamilyId()));
      }
    }
    if(!userWallet.getCounterHistory().contains(counters.getVersion())){
      userWallet.getCounterHistory().add(counters.getVersion());
      walletUpdatesRepository.rewardFamilyUserTransaction(
              initiativeId,
              rewardTransactionDTO.getUserId(),
              rewardTransactionDTO.getElaborationDateTime(),
              userWallet.getCounterHistory(),
              userWallet.getAccruedCents() +
                      rewardTransactionDTO
                      .getRewards()
                      .get(initiativeId)
                      .getAccruedRewardCents()
              );
    }
  }

  private void rewardUserTransaction(String initiativeId, RewardTransactionDTO rewardTransactionDTO, Counters counters, Wallet userWallet) {
    if(userWallet.getCounterVersion() < counters.getVersion()) {
      Long budgetCents = rewardTransactionDTO.getVoucherAmountCents() != null ? rewardTransactionDTO.getVoucherAmountCents() : counters.getInitiativeBudgetCents();
      walletUpdatesRepository.rewardTransaction(initiativeId,
              rewardTransactionDTO.getUserId(),
              rewardTransactionDTO.getElaborationDateTime(),
              budgetCents - counters.getTotalRewardCents(),
              counters.getTotalRewardCents(),
              counters.getVersion());
    }
  }

  private Wallet findByInitiativeIdAndUserId(String initiativeId, String userId) {
    return walletRepository
            .findById(generateWalletId(userId, initiativeId))
            .orElseThrow(
                    () -> new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, initiativeId)));
  }

  private String setStatus(Wallet wallet) {
    if (!wallet.getStatus().equals(WalletStatus.SUSPENDED)) {
      boolean hasIban = wallet.getIban() != null;
      boolean hasInstrument = wallet.getNInstr() > 0;
      return WalletStatus.getByBooleans(hasIban, hasInstrument).name();
    }
    return WalletStatus.SUSPENDED;
  }

  @Override
  public void processIbanOutcome(IbanQueueWalletDTO iban) {
    long startTime = System.currentTimeMillis();

    if (!iban.getStatus().equals(WalletConstants.STATUS_KO)) {
      log.info("[CHECK_IBAN_OUTCOME] Skipping outcome with status {}.", iban.getStatus());
      sendToTimeline(
          timelineMapper.ibanToTimeline(
              iban.getInitiativeId(), iban.getUserId(), iban.getIban(), iban.getChannel()));
      performanceLog(startTime, SERVICE_CHECK_IBAN_OUTCOME);
      return;
    }

    Wallet wallet =
        walletRepository
            .findById(generateWalletId(iban.getUserId(), iban.getInitiativeId()))
            .orElse(null);

    if (wallet == null) {
      log.warn("[CHECK_IBAN_OUTCOME] Wallet not found. Skipping message");
      performanceLog(startTime, SERVICE_CHECK_IBAN_OUTCOME);
      auditUtilities.logIbanDeletedKO(
          iban.getUserId(), iban.getInitiativeId(), iban.getIban(), "wallet not found");
      return;
    }

    if (!iban.getIban().equals(wallet.getIban())) {
      log.warn(
          "[CHECK_IBAN_OUTCOME] The IBAN contained in the message is different from the IBAN currently enrolled.");
      performanceLog(startTime, SERVICE_CHECK_IBAN_OUTCOME);
      auditUtilities.logIbanDeletedKO(
          iban.getUserId(), iban.getInitiativeId(), iban.getIban(), "iban mismatch");
      return;
    }

    wallet.setIban(null);

    walletUpdatesRepository.deleteIban(iban.getInitiativeId(), iban.getUserId(), setStatus(wallet));
    sendCheckIban(iban);
    auditUtilities.logIbanDeleted(iban.getUserId(), iban.getInitiativeId(), iban.getIban());

    performanceLog(startTime, SERVICE_CHECK_IBAN_OUTCOME);
  }

  private void sendRejectedInstrumentToTimeline(String initiativeId, String userId, String channel,
      String instrumentType, String operationType) {
    InstrumentAckDTO instrumentAckDTO = InstrumentAckDTO.builder()
        .userId(userId)
        .initiativeId(initiativeId)
        .instrumentType(instrumentType)
        .operationType(operationType)
        .channel(channel)
        .operationDate(LocalDateTime.now())
        .build();
    sendToTimeline(timelineMapper.ackToTimeline(instrumentAckDTO));
  }

  private void sendCheckIban(IbanQueueWalletDTO iban) {
    NotificationQueueDTO notificationQueueDTO =
        NotificationQueueDTO.builder()
            .operationType(WalletConstants.CHECKIBAN_KO)
            .userId(iban.getUserId())
            .initiativeId(iban.getInitiativeId())
            .iban(iban.getIban())
            .status(WalletConstants.STATUS_KO)
            .build();

    sendNotification(notificationQueueDTO);
  }

  private void sendRefundNotification(RefundDTO refundDTO) {
    NotificationQueueDTO notificationQueueDTO =
        NotificationQueueDTO.builder()
            .operationType(WalletConstants.REFUND)
            .userId(refundDTO.getBeneficiaryId())
            .initiativeId(refundDTO.getInitiativeId())
            .rewardNotificationId(refundDTO.getRewardNotificationId())
            .refundReward(refundDTO.getEffectiveRewardCents())
            .rejectionCode(refundDTO.getRejectionCode())
            .rejectionReason(refundDTO.getRejectionReason())
            .refundDate(refundDTO.getExecutionDate())
            .refundFeedbackProgressive(refundDTO.getFeedbackProgressive())
            .refundCro(refundDTO.getCro())
            .status(refundDTO.getStatus())
            .build();

    sendNotification(notificationQueueDTO);
  }

  private void sendSuspensionReadmissionNotification(
      String operationType, String initiativeId, String userId, String initiativeName) {
    NotificationQueueDTO notificationQueueDTO =
        NotificationQueueDTO.builder()
            .operationType(operationType)
            .userId(userId)
            .initiativeId(initiativeId)
            .initiativeName(initiativeName)
            .build();

    sendNotification(notificationQueueDTO);
  }

  private void sendNotification(NotificationQueueDTO notificationQueueDTO) {
    try {
      log.info("[SEND_NOTIFICATION] Sending event to Notification");
      notificationProducer.sendNotification(notificationQueueDTO);
    } catch (Exception e) {
      log.error("[SEND_NOTIFICATION] An error has occurred. Sending message to Error queue");
      final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(notificationQueueDTO);
      this.sendToQueueError(e, errorMessage, notificationServer, notificationTopic);
    }
  }

  private void formalControl(String iban) {
    IbanUtil.validate(iban);
  }

  private void rollbackWallet(String statusToRollback, Wallet wallet) {
    log.info("[ROLLBACK_WALLET] Wallet, old status: {}", wallet.getStatus());
    if (WalletStatus.UNSUBSCRIBED.equals(wallet.getStatus())) {
      wallet.setRequestUnsubscribeDate(null);
    }
    if (WalletStatus.SUSPENDED.equals(wallet.getStatus())) {
      wallet.setSuspensionDate(null);
    }
    wallet.setStatus(statusToRollback);
    wallet.setUpdateDate(LocalDateTime.now());
    walletRepository.save(wallet);
    log.info("[ROLLBACK_WALLET] Rollback wallet, new status: {}", wallet.getStatus());
  }

  private void checkEndDate(LocalDate endDate, String initiativeId) {
      LocalDate requestDate = LocalDate.now();

      if (requestDate.isAfter(endDate)) {
        log.info("[CHECK_END_DATE] The operation is not allowed because the initiative {} has already ended", initiativeId);
        throw new InitiativeInvalidException(String.format(INITIATIVE_ENDED_MSG, initiativeId));
      }
  }

  private void sendToTimeline(QueueOperationDTO queueOperationDTO) {
    try {
      log.info("[SEND_TO_TIMELINE] Sending queue message to Timeline");
      timelineProducer.sendEvent(queueOperationDTO);
    } catch (Exception exception) {
      log.error("[SEND_TO_TIMELINE] An error has occurred. Sending message to Error queue");
      final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(queueOperationDTO);
      this.sendToQueueError(exception, errorMessage, timelineServer, timelineTopic);
    }
  }

  private void sendToQueueError(
          Exception e, Message oldMessage, MessageBuilder<?> errorMessage, String server, String topic) {
      errorMessage.setHeader(WalletConstants.ERROR_MSG_HEADER_RETRY,
              oldMessage.getHeaders().get(WalletConstants.ERROR_MSG_HEADER_RETRY));
      sendToQueueError(e, errorMessage, server, topic);
  }

  private void sendToQueueError(
      Exception e, MessageBuilder<?> errorMessage, String server, String topic) {
    errorMessage
        .setHeader(WalletConstants.ERROR_MSG_HEADER_SRC_TYPE, WalletConstants.KAFKA)
        .setHeader(WalletConstants.ERROR_MSG_HEADER_SRC_SERVER, server)
        .setHeader(WalletConstants.ERROR_MSG_HEADER_SRC_TOPIC, topic)
        .setHeader(WalletConstants.ERROR_MSG_HEADER_DESCRIPTION, WalletConstants.ERROR_QUEUE)
        .setHeader(WalletConstants.ERROR_MSG_HEADER_RETRYABLE, true)
        .setHeader(WalletConstants.ERROR_MSG_HEADER_STACKTRACE, e.getStackTrace())
        .setHeader(WalletConstants.ERROR_MSG_HEADER_CLASS, e.getClass())
        .setHeader(WalletConstants.ERROR_MSG_HEADER_MESSAGE, e.getMessage());
    errorProducer.sendEvent(errorMessage.build());
  }

  private void performanceLog(long startTime, String service) {
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
        service,
        System.currentTimeMillis() - startTime);
  }

  private String generateWalletId(String userId, String initiativeId){
    return userId.concat("_").concat(initiativeId);
  }

  @Override
  public InitiativesWithInstrumentDTO getInitiativesWithInstrument(String idWallet, String userId) {
    long startTime = System.currentTimeMillis();
    List<InitiativesStatusDTO> initiativesStatusDTO = new ArrayList<>();

    InitiativeListDTO initiativeListDTO = this.getInitiativeList(userId);

    log.info(
        "[GET_INSTRUMENT_DETAIL_ON_INITIATIVES] Get detail about payment instrument and its status on initiatives");
    try {
      log.info("[GET_INSTRUMENT_DETAIL_ON_INITIATIVES] Calling Payment Instrument");
      InstrumentDetailDTO instrumentDetailDTO =
          paymentInstrumentRestConnector.getInstrumentInitiativesDetail(
              idWallet, userId, WalletConstants.FILTER_INSTRUMENT_STATUS_LIST);

      if (initiativeListDTO.getInitiativeList().isEmpty()) {
        return walletMapper.toInstrumentOnInitiativesDTO(
            idWallet, instrumentDetailDTO, initiativesStatusDTO);
      }

      log.info("[GET_INSTRUMENT_DETAIL_ON_INITIATIVES] Get all initiatives still active for user");
      for (WalletDTO wallet : initiativeListDTO.getInitiativeList()) {
        if (!wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)
            && !wallet.getInitiativeEndDate().isBefore(LocalDate.now())
            && WalletConstants.INITIATIVE_REWARD_TYPE_REFUND.equals(
            wallet.getInitiativeRewardType())) {
          initiativesStatusDTO.add(walletMapper.toInstrStatusOnInitiativeDTO(wallet));
        }
      }

      if (!instrumentDetailDTO.getInitiativeList().isEmpty()) {
        List<StatusOnInitiativeDTO> instrStatusList = instrumentDetailDTO.getInitiativeList();

        Map<String, StatusOnInitiativeDTO> instrumentStatusOnInitiativeMap =
            instrStatusList.stream()
                .collect(
                    Collectors.toMap(StatusOnInitiativeDTO::getInitiativeId, Function.identity()));

        log.info(
            "[GET_INSTRUMENT_DETAIL_ON_INITIATIVES] Updating initiatives list with payment status");
        initiativesStatusDTO =
            initiativesStatusDTO.stream()
                .map(
                    i ->
                        new InitiativesStatusDTO(
                            i.getInitiativeId(),
                            i.getInitiativeName(),
                            (instrumentStatusOnInitiativeMap.get(i.getInitiativeId()) != null
                                ? instrumentStatusOnInitiativeMap
                                .get(i.getInitiativeId())
                                .getIdInstrument()
                                : null),
                            (instrumentStatusOnInitiativeMap.get(i.getInitiativeId()) != null
                                ? instrumentStatusOnInitiativeMap
                                .get(i.getInitiativeId())
                                .getStatus()
                                : WalletConstants.INSTRUMENT_STATUS_DEFAULT)))
                .toList();
      }

      performanceLog(startTime, "GET_INSTRUMENT_DETAIL_ON_INITIATIVES");
      return walletMapper.toInstrumentOnInitiativesDTO(
          idWallet, instrumentDetailDTO, initiativesStatusDTO);
    } catch (ServiceException e) {
      log.error("[GET_INSTRUMENT_DETAIL_ON_INITIATIVES] Error in Payment Instrument Request");
      performanceLog(startTime, "GET_INSTRUMENT_DETAIL_ON_INITIATIVES");
      throw e;
    }
  }
}
