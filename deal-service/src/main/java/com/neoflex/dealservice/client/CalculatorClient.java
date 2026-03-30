package com.neoflex.dealservice.client;

import com.neoflex.deal.dto.CreditDto;
import com.neoflex.deal.dto.LoanOfferDto;
import com.neoflex.deal.dto.LoanStatementRequestDto;
import com.neoflex.deal.dto.ScoringDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculatorClient {

    @Value("${calculator-api.api-get-offers}")
    private String API_GET_OFFERS;

    @Value("${calculator-api.api-calculate-credit}")
    private String API_CALCULATE_CREDIT;

    private final RestClient restClient;

    public List<LoanOfferDto> getOffers(LoanStatementRequestDto request) {
        return restClient.post()
                .uri(API_GET_OFFERS)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<LoanOfferDto>>() {});
    }

    public CreditDto calculateCredit(ScoringDataDto request) {
        return restClient.post()
                .uri(API_CALCULATE_CREDIT)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<CreditDto>() {});
    }
}
