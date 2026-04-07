package com.neoflex.dealservice.client;

import com.neoflex.deal.dto.CreditDto;
import com.neoflex.deal.dto.LoanOfferDto;
import com.neoflex.deal.dto.LoanStatementRequestDto;
import com.neoflex.deal.dto.ScoringDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalculatorClient {

    @Value("${calculator-api.api-get-offers}")
    private String API_GET_OFFERS;

    @Value("${calculator-api.api-calculate-credit}")
    private String API_CALCULATE_CREDIT;

    private final RestClient restClient;

//    @Async
    public List<LoanOfferDto> getOffers(LoanStatementRequestDto request, Map<String, String> context) {
        var currentContext = context;
        if (currentContext != null) MDC.setContextMap(currentContext);

        try {
            log.info("Отправка расчета кредитных предложений при помощи - {calculator-api} для пользователя с client_id: [{}], для сделки с statement_id: [{}], с суммой: [{}], с trace_id: [{}]",
                    MDC.get("client_id"), MDC.get("statement_id"), MDC.get("amount"), MDC.get("trace_id"));

            return restClient.post()
                    .uri(API_GET_OFFERS)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<LoanOfferDto>>() {});
        } finally {
            MDC.clear();
        }

    }

//    @Async
    public CreditDto calculateCredit(ScoringDataDto request) {
        return restClient.post()
                .uri(API_CALCULATE_CREDIT)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<CreditDto>() {});
    }
}
