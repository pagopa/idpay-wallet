package it.gov.pagopa.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wallet.config.ServiceExceptionConfig;
import it.gov.pagopa.wallet.config.WalletErrorManagerConfig;
import it.gov.pagopa.wallet.dto.ReminderBatchRequestDTO;
import it.gov.pagopa.wallet.exception.custom.ReminderBatchException;
import it.gov.pagopa.wallet.service.VoucherExpirationReminderBatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(
        value = {VoucherExpirationReminderBatchController.class},
        excludeAutoConfiguration =  { UserDetailsServiceAutoConfiguration.class , SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({ServiceExceptionConfig.class, WalletErrorManagerConfig.class})

class VoucherExpirationReminderBatchControllerTest {

    private static final String BASE_URL = "http://localhost:8080/idpay/wallet";
    private static final String REMINDER_BATCH_URL = "/batch/run";
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String INITIATIVE_ID_2 = "TEST_INITIATIVE_ID_2";
    private static final int DAYS_NUMBER = 3;
    private static final int DEFAULT_EXPIRING_DAY = 3;



    @MockitoBean
    VoucherExpirationReminderBatchService voucherExpirationReminderBatchServiceMock;

    @Autowired
    protected MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void runReminderBatch_deprecatedPath_ok() throws Exception {

        Mockito.doNothing().when(voucherExpirationReminderBatchServiceMock).runReminderBatch(INITIATIVE_ID, DAYS_NUMBER);

        mvc.perform(
                        MockMvcRequestBuilders.post(
                                                BASE_URL
                                                + REMINDER_BATCH_URL
                                                + "/"
                                                + INITIATIVE_ID
                                                )
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        Mockito.verify(voucherExpirationReminderBatchServiceMock)
                .runReminderBatch(INITIATIVE_ID, DAYS_NUMBER);
    }

    @Test
    void runReminderBatch_body_multipleInitiatives_ok() throws Exception {
        ReminderBatchRequestDTO request = ReminderBatchRequestDTO.builder()
                .initiativeIds(List.of(INITIATIVE_ID, INITIATIVE_ID_2))
                .build();

        mvc.perform(
                        MockMvcRequestBuilders.post(BASE_URL + REMINDER_BATCH_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        Mockito.verify(voucherExpirationReminderBatchServiceMock)
                .runReminderBatch(List.of(INITIATIVE_ID, INITIATIVE_ID_2), DEFAULT_EXPIRING_DAY);
    }

    @Test
    void runReminderBatch_body_singleInitiative_prodScenario_ok() throws Exception {
        ReminderBatchRequestDTO request = ReminderBatchRequestDTO.builder()
                .initiativeIds(List.of(INITIATIVE_ID))
                .build();

        mvc.perform(
                        MockMvcRequestBuilders.post(BASE_URL + REMINDER_BATCH_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        Mockito.verify(voucherExpirationReminderBatchServiceMock)
                .runReminderBatch(List.of(INITIATIVE_ID), DEFAULT_EXPIRING_DAY);
    }

    @Test
    void runReminderBatch_body_emptyInitiatives_badRequest() throws Exception {
        ReminderBatchRequestDTO request = ReminderBatchRequestDTO.builder()
                .initiativeIds(List.of())
                .build();

        mvc.perform(
                        MockMvcRequestBuilders.post(BASE_URL + REMINDER_BATCH_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andReturn();

        Mockito.verify(voucherExpirationReminderBatchServiceMock, Mockito.never())
                .runReminderBatch(Mockito.anyList(), anyInt());
    }

    @Test
    void runReminderBatch_body_serviceFailure_internalServerError() throws Exception {
        ReminderBatchRequestDTO request = ReminderBatchRequestDTO.builder()
                .initiativeIds(List.of(INITIATIVE_ID, INITIATIVE_ID_2))
                .build();

        // The service reports at least one failed initiative: the endpoint must surface a 5xx
        Mockito.doThrow(new ReminderBatchException("The reminder batch failed for the following initiatives: [" + INITIATIVE_ID + "]"))
                .when(voucherExpirationReminderBatchServiceMock)
                .runReminderBatch(List.of(INITIATIVE_ID, INITIATIVE_ID_2), DEFAULT_EXPIRING_DAY);

        mvc.perform(
                        MockMvcRequestBuilders.post(BASE_URL + REMINDER_BATCH_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andReturn();

        Mockito.verify(voucherExpirationReminderBatchServiceMock)
                .runReminderBatch(List.of(INITIATIVE_ID, INITIATIVE_ID_2), DEFAULT_EXPIRING_DAY);
    }


}
