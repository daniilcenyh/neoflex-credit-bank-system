package com.neoflex.calculator_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.calculator.dto.CreditDto;
import net.proselyte.calculator.dto.LoanOfferDto;
import net.proselyte.calculator.dto.LoanStatementRequestDto;
import net.proselyte.calculator.dto.ScoringDataDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculatorService {
    private final PrescoringService prescoringService;
    private final ScoringService scoringService;

    public List<LoanOfferDto> getOffers(LoanStatementRequestDto request) {
        return this.prescoringService.generateOffers(request);
    }

    public CreditDto calculateCredit(ScoringDataDto scoringData) {
        return this.scoringService.calculateCredit(scoringData);
    }
}
