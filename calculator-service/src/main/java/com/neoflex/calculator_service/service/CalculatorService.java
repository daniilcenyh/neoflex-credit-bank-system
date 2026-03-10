package com.neoflex.calculator_service.service;

import com.neoflex.calculator_service.metrics.annotation.BusinessMetric;
import io.micrometer.observation.annotation.Observed;
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
@Observed(name = "calculator.service")
public class CalculatorService {
    private final PrescoringService prescoringService;
    private final ScoringService scoringService;

    @BusinessMetric(
            value = "calculator.offers.calculated",
            tags = {"operation=calculate", "type=offers"}
    )
    @Observed(name = "calculator.offers", contextualName = "get-credit-offers")
    public List<LoanOfferDto> getOffers(LoanStatementRequestDto request) {
        long startTime = System.currentTimeMillis();
        log.debug("Получен запрос на расчет предложений: {}", request);

        List<LoanOfferDto> offers = this.prescoringService.generateOffers(request);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Расчет предложений выполнен за {} мс, найдено {} предложений", duration, offers.size());

        return offers;
    }

    @BusinessMetric(
            value = "calculator.credit.calculated",
            tags = {"operation=calculate", "type=credit"}
    )
    @Observed(name = "calculator.credit", contextualName = "calculate-full-credit")
    public CreditDto calculateCredit(ScoringDataDto scoringData) {
        long startTime = System.currentTimeMillis();
        log.debug("Получен запрос на расчет кредита: {}", scoringData);

        var credit = this.scoringService.calculateCredit(scoringData);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Расчет кредита выполнен за {} мс", duration);

        return credit;
    }
}
