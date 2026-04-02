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

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculatorService {

    private final CalculatorClient calculatorClient;

    // TODO: добавить паттерн RETRY + Circuit Breaker
    public List<LoanOfferDto> getOffers(LoanStatementRequestDto request) {
        return calculatorClient.getOffers(request);
    }

    // TODO: добавить паттерн RETRY + Circuit Breaker
    public CreditDto calculateCredit(ScoringDataDto request) {
        return calculatorClient.calculateCredit(request);
    }
}
