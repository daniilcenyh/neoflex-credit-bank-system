package com.neoflex.calculator_service.service.impl;

import com.neoflex.calculator_service.service.PrescoringService;
import com.neoflex.calculator_service.util.AnnuityCalculator;
import com.neoflex.calculator_service.validator.LoanRequestValidator;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.proselyte.calculator.dto.LoanOfferDto;
import net.proselyte.calculator.dto.LoanStatementRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescoringServiceImpl implements PrescoringService {

    private final LoanRequestValidator validator;

    @Value("${credit.base-rate}")
    private BigDecimal BASE_RATE;

    @Value("${credit.insurance-cost}")
    private BigDecimal INSURANCE_COST;

    @Value("${credit.insurance-rate-discount}")
    private BigDecimal INSURANCE_RATE_DISCOUNT;

    @Value("${credit.salary-rate-discount}")
    private BigDecimal SALARY_RATE_DISCOUNT;

    // Предоставление вариантов кредитов учитывая изначальные данные пользователя
    @Override
    public List<LoanOfferDto> generateOffers(LoanStatementRequestDto request) {
        log.info("Начало генерации предложений для запроса: {}", request);

        this.validator.validate(request);

        List<LoanOfferDto> offers = new ArrayList<>();

        offers.add(createOffer(request, false, false));
        offers.add(createOffer(request, false, true));
        offers.add(createOffer(request, true, false));
        offers.add(createOffer(request, true, true));

        offers.sort(Comparator.comparing(LoanOfferDto::getRate));

        log.info("Сгенерировано {} предложений", offers.size());
        return offers;
    }

    // Расчет варианта кредита
    private LoanOfferDto createOffer(LoanStatementRequestDto request,
                                     boolean isInsuranceEnabled,
                                     boolean isSalaryClient) {

        BigDecimal rate = BASE_RATE;
        BigDecimal totalAmount = request.getAmount();

        // Применяем скидки
        if (isInsuranceEnabled) {
            rate = rate.subtract(INSURANCE_RATE_DISCOUNT);
            totalAmount = totalAmount.add(INSURANCE_COST);
        }

        if (isSalaryClient) {
            rate = rate.subtract(SALARY_RATE_DISCOUNT);
        }

        // Рассчитываем ежемесячный платеж
        BigDecimal monthlyPayment = AnnuityCalculator.calculateMonthlyPayment(
                totalAmount,
                rate,
                request.getTerm()
        );

        return new LoanOfferDto(
                UUID.randomUUID(),
                request.getAmount(),
                totalAmount,
                request.getTerm(),
                monthlyPayment,
                rate,
                isInsuranceEnabled,
                isSalaryClient
                );
    }


}
