package com.neoflex.calculator_service.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.calculator.dto.PaymentScheduleElementDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class AnnuityCalculator {
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    public BigDecimal calculateMonthlyPayment(BigDecimal principal,
                                              BigDecimal annualRate,
                                              int termMonths) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal payment = principal
                    .divide(BigDecimal.valueOf(termMonths), MONEY_SCALE, MONEY_ROUNDING);
            log.debug("Zero rate detected. Monthly payment = principal/term = {}", payment);
            return payment;
        }

        BigDecimal monthlyRate = toMonthlyRate(annualRate);
        log.debug("calculateMonthlyPayment: principal={}, annualRate={}, term={}, monthlyRate={}",
                principal, annualRate, termMonths, monthlyRate);

        BigDecimal onePlusRPowN = BigDecimal.ONE.add(monthlyRate, MC).pow(termMonths, MC);

        BigDecimal numerator = monthlyRate.multiply(onePlusRPowN, MC);

        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE, MC);

        BigDecimal payment = principal
                .multiply(numerator.divide(denominator, MC))
                .setScale(MONEY_SCALE, MONEY_ROUNDING);

        log.debug("Monthly payment result: {}", payment);
        return payment;
    }

    public List<PaymentScheduleElementDto> buildPaymentSchedule(BigDecimal principal,
                                                                BigDecimal annualRate,
                                                                int termMonths,
                                                                LocalDate firstPaymentDate) {
        BigDecimal monthlyRate = toMonthlyRate(annualRate);
        BigDecimal monthlyPayment = calculateMonthlyPayment(principal, annualRate, termMonths);

        List<PaymentScheduleElementDto> schedule = new ArrayList<>(termMonths);
        BigDecimal remainingDebt = principal.setScale(MONEY_SCALE, MONEY_ROUNDING);

        for (int i = 1; i <= termMonths; i++) {
            BigDecimal interestPayment = remainingDebt
                    .multiply(monthlyRate, MC)
                    .setScale(MONEY_SCALE, MONEY_ROUNDING);

            BigDecimal debtPayment;
            BigDecimal totalPayment;

            if (i == termMonths) {
                debtPayment = remainingDebt;
                totalPayment = debtPayment.add(interestPayment);
            } else {
                debtPayment = monthlyPayment.subtract(interestPayment);
                totalPayment = monthlyPayment;
            }

            remainingDebt = remainingDebt
                    .subtract(debtPayment)
                    .setScale(MONEY_SCALE, MONEY_ROUNDING);

            if (remainingDebt.compareTo(BigDecimal.ZERO) < 0) {
                remainingDebt = BigDecimal.ZERO;
            }

            schedule.add(new PaymentScheduleElementDto(i,
                    firstPaymentDate.plusMonths((long) (i - 1)),
                    totalPayment,
                    interestPayment,
                    debtPayment,
                    remainingDebt
                    )
            );

            log.debug("Payment #{}: total={}, interest={}, debt={}, remaining={}",
                    i, totalPayment, interestPayment, debtPayment, remainingDebt);
        }

        return List.copyOf(schedule);
    }

    public BigDecimal calculatePsk(BigDecimal principal,
                                   BigDecimal monthlyPayment,
                                   int termMonths) {
        if (principal == null || monthlyPayment == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        if (principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal must be positive");
        }
        if (termMonths <= 0) {
            throw new IllegalArgumentException("Term must be positive");
        }

        BigDecimal totalPaid = monthlyPayment.multiply(BigDecimal.valueOf(termMonths), MC);
        BigDecimal termYears = BigDecimal.valueOf(termMonths)
                .divide(BigDecimal.valueOf(12), MC);

        if (totalPaid.compareTo(principal) == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE);
        }

        BigDecimal psk = totalPaid
                .subtract(principal)
                .divide(principal, MC)
                .divide(termYears, MC)
                .multiply(BigDecimal.valueOf(100))
                .setScale(MONEY_SCALE, MONEY_ROUNDING);

        log.debug("PSK: totalPaid={}, termYears={}, psk={}", totalPaid, termYears, psk);
        return psk;
    }

    private BigDecimal toMonthlyRate(BigDecimal annualRate) {
        if (annualRate == null) {
            throw new IllegalArgumentException("Annual rate cannot be null");
        }
        return annualRate.divide(BigDecimal.valueOf(12 * 100), MC);
    }
}
