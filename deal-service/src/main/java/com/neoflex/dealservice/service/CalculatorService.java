package com.neoflex.dealservice.service;

import com.neoflex.deal.dto.CreditDto;
import com.neoflex.deal.dto.LoanOfferDto;
import com.neoflex.deal.dto.LoanStatementRequestDto;
import com.neoflex.deal.dto.ScoringDataDto;
import com.neoflex.dealservice.client.CalculatorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculatorService {

    private final CalculatorClient calculatorClient;

    // TODO: добавить паттерн RETRY + Circuit Breaker
    public List<LoanOfferDto> getOffers(LoanStatementRequestDto request,  Map<String, String> context) {
        return calculatorClient.getOffers(request, context);
    }

    // TODO: добавить паттерн RETRY + Circuit Breaker
    public CreditDto calculateCredit(ScoringDataDto request) {
        return calculatorClient.calculateCredit(request);
    }
}
