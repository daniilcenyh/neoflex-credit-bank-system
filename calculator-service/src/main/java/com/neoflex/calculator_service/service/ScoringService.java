package com.neoflex.calculator_service.service;

import com.neoflex.calculator_service.util.AnnuityCalculator;
import com.neoflex.calculator_service.validator.LoanRequestValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.calculator.dto.CreditDto;
import net.proselyte.calculator.dto.EmploymentStatus;
import net.proselyte.calculator.dto.Gender;
import net.proselyte.calculator.dto.MaritalStatus;
import net.proselyte.calculator.dto.PaymentScheduleElementDto;
import net.proselyte.calculator.dto.Position;
import net.proselyte.calculator.dto.ScoringDataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService{

    @Value("${credit.base-rate}")
    private BigDecimal BASE_RATE;

    @Value("${credit.insurance-rate-discount}")
    private BigDecimal INSURANCE_RATE_DISCOUNT;

    @Value("${credit.insurance-cost}")
    private BigDecimal INSURANCE_COST;

    @Value("${credit.salary-rate-discount}")
    private BigDecimal SALARY_RATE_DISCOUNT;

    private final LoanRequestValidator validator;

    public CreditDto calculateCredit(ScoringDataDto data) {
        log.info("Начало скоринга для клиента: {} {}, сумма: {}",
                data.getFirstName(), data.getLastName(), data.getAmount());

        this.validator.performHardChecks(data);

        BigDecimal rate = calculateBaseRateWithServices(data);

        rate = applyAllScoringRules(rate, data);

        log.debug("Итоговая ставка после всех модификаторов: {}%", rate);

        return calculateCreditParameters(data, rate);
    }



    private BigDecimal calculateBaseRateWithServices(ScoringDataDto data) {
        BigDecimal rate = BASE_RATE;

        if (data.getIsInsuranceEnabled()) {
            rate = rate.subtract(INSURANCE_RATE_DISCOUNT);
            log.debug("Применена скидка за страховку: -{}%", INSURANCE_RATE_DISCOUNT);
        }

        if (data.getIsSalaryClient()) {
            rate = rate.subtract(SALARY_RATE_DISCOUNT);
            log.debug("Применена скидка за зарплатного клиента: -{}%", SALARY_RATE_DISCOUNT);
        }

        return rate;
    }

    private BigDecimal applyAllScoringRules(BigDecimal rate, ScoringDataDto data) {
        log.debug("Применение правил скоринга к ставке {}%", rate);

        rate = applyEmploymentStatusRule(rate, data.getEmployment().getEmploymentStatus());

        rate = applyPositionRule(rate, data.getEmployment().getPosition());

        rate = applyMaritalStatusRule(rate, data.getMaritalStatus());

        rate = applyGenderAgeRule(rate, data.getGender(), data.getBirthdate());

        return rate;
    }

    private BigDecimal applyEmploymentStatusRule(BigDecimal rate, EmploymentStatus status) {
        switch (status) {
            case SELF_EMPLOYED:
                rate = rate.add(BigDecimal.valueOf(2));
                log.debug("Самозанятый: +2% -> {}", rate);
                break;
            case BUSINESS_OWNER:
                rate = rate.add(BigDecimal.valueOf(1));
                log.debug("Владелец бизнеса: +1% -> {}", rate);
                break;
            case EMPLOYED:
                log.debug("Наемный работник: без изменений");
                break;
            default:
                break;
        }
        return rate;
    }

    private BigDecimal applyPositionRule(BigDecimal rate, Position position) {
        switch (position) {
            case TOP_MANAGER:
                rate = rate.subtract(BigDecimal.valueOf(3));
                log.debug("Топ-менеджер: -3% -> {}", rate);
                break;
            case MIDDLE_MANAGER:
                rate = rate.subtract(BigDecimal.valueOf(2));
                log.debug("Менеджер среднего звена: -2% -> {}", rate);
                break;
            default:
                log.debug("Должность {}: без изменений", position);
                break;
        }
        return rate;
    }

    private BigDecimal applyMaritalStatusRule(BigDecimal rate, MaritalStatus status) {
        switch (status) {
            case MARRIED:
                rate = rate.subtract(BigDecimal.valueOf(3));
                log.debug("Женат/замужем: -3% -> {}", rate);
                break;
            case DIVORCED:
                rate = rate.add(BigDecimal.valueOf(1));
                log.debug("Разведен: +1% -> {}", rate);
                break;
            default:
                log.debug("Семейное положение {}: без изменений", status);
                break;
        }
        return rate;
    }

    private BigDecimal applyGenderAgeRule(BigDecimal rate, Gender gender, LocalDate birthdate) {
        int age = calculateAge(birthdate);

        if (gender == Gender.FEMALE && age >= 32 && age <= 60) {
            rate = rate.subtract(BigDecimal.valueOf(3));
            log.debug("Женщина {} лет: -3% -> {}", age, rate);
        } else if (gender == Gender.MALE && age >= 30 && age <= 55) {
            rate = rate.subtract(BigDecimal.valueOf(3));
            log.debug("Мужчина {} лет: -3% -> {}", age, rate);
        } else {
            log.debug("Пол {} возраст {}: без изменений", gender, age);
        }

        return rate;
    }

    private CreditDto calculateCreditParameters(ScoringDataDto data, BigDecimal rate) {
        log.debug("Расчет параметров кредита со ставкой {}%", rate);

        BigDecimal totalAmount = data.getAmount();
        if (data.getIsInsuranceEnabled()) {
            totalAmount = totalAmount.add(INSURANCE_COST);
        }

        BigDecimal monthlyPayment = AnnuityCalculator.calculateMonthlyPayment(
                totalAmount,
                rate,
                data.getTerm()
        );

        BigDecimal psk = AnnuityCalculator.calculatePsk(
                totalAmount,
                monthlyPayment,
                data.getTerm()
        );

        List<PaymentScheduleElementDto> paymentSchedule = AnnuityCalculator.buildPaymentSchedule(
                totalAmount,
                rate,
                data.getTerm(),
                LocalDate.now().plusMonths(1)
        );

        CreditDto credit = new CreditDto(
                totalAmount,
                data.getTerm(),
                monthlyPayment,
                rate,
                psk,
                data.getIsInsuranceEnabled(),
                data.getIsSalaryClient(),
                paymentSchedule
        );

        log.info("Расчет кредита завершен: ежемесячный платеж={}, ПСК={}%", monthlyPayment, psk);
        return credit;
    }

    private int calculateAge(LocalDate birthdate) {
        return Period.between(birthdate, LocalDate.now()).getYears();
    }
}