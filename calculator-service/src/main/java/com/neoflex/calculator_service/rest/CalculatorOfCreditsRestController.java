package com.neoflex.calculator_service.rest;

import com.neoflex.calculator_service.service.CalculatorService;
import com.neoflex.calculator_service.service.PrescoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.calculator.api.CreditCalculatorApi;
import net.proselyte.calculator.dto.CreditDto;
import net.proselyte.calculator.dto.LoanOfferDto;
import net.proselyte.calculator.dto.LoanStatementRequestDto;
import net.proselyte.calculator.dto.ScoringDataDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CalculatorOfCreditsRestController implements CreditCalculatorApi {

    private final CalculatorService calculatorService;

    @Override
    public ResponseEntity<CreditDto> calculateCredit(ScoringDataDto scoringDataDto) {
        return ResponseEntity.ok(this.calculatorService.calculateCredit(scoringDataDto));
    }

    @Override
    public ResponseEntity<List<LoanOfferDto>> calculateOffers(LoanStatementRequestDto loanStatementRequestDto) {
        return ResponseEntity.ok(this.calculatorService.getOffers(loanStatementRequestDto));
    }
}
