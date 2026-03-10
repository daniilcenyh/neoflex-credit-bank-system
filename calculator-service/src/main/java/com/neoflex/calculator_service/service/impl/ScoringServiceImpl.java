package com.neoflex.calculator_service.service.impl;

import com.neoflex.calculator_service.exception.ScoringException;
import com.neoflex.calculator_service.service.ScoringService;
import com.neoflex.calculator_service.util.AnnuityCalculator;
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
public class ScoringServiceImpl implements ScoringService {

    @Value("${credit.base-rate}")
    private BigDecimal BASE_RATE;

    @Value("${credit.insurance-rate-discount}")
    private BigDecimal INSURANCE_RATE_DISCOUNT;

    @Value("${credit.salary-rate-discount}")
    private BigDecimal SALARY_RATE_DISCOUNT;

    // Основной метод скоринга и расчета кредита
    public CreditDto calculateCredit(ScoringDataDto data) {
        log.info("Начало скоринга для клиента: {} {}, сумма: {}",
                data.getFirstName(), data.getLastName(), data.getAmount());

        // 1. Проверки на отказ (hard checks)
        performHardChecks(data);

        // 2. Расчет базовой ставки с учетом выбранных услуг
        BigDecimal rate = calculateBaseRateWithServices(data);

        // 3. Применение всех скоринговых модификаторов
        rate = applyAllScoringRules(rate, data);

        log.debug("Итоговая ставка после всех модификаторов: {}%", rate);

        // 4. Расчет кредита
        return calculateCreditParameters(data, rate);
    }

    // ЖЁСТКИЕ ПРОВЕРКИ ДАННЫХ ПОЛЬЗОВАТЕЛЯ
    private void performHardChecks(ScoringDataDto data) {
        log.debug("Выполнение жестких проверок");

        // Проверка возраста
        int age = calculateAge(data.getBirthdate());
        if (age < 20 || age > 65) {
            throw new ScoringException(
                    String.format("Возраст %d лет не подходит (требуется 20-65 лет)", age));
        }

        // Проверка статуса занятости
        if (data.getEmployment().getEmploymentStatus() == EmploymentStatus.UNEMPLOYED) {
            throw new ScoringException("Безработные клиенты не рассматриваются");
        }

        // Проверка стажа
        if (data.getEmployment().getWorkExperienceTotal() < 18) {
            throw new ScoringException(
                    String.format("Общий стаж %d мес. меньше требуемых 18 мес.",
                            data.getEmployment().getWorkExperienceTotal()));
        }

        if (data.getEmployment().getWorkExperienceCurrent() < 3) {
            throw new ScoringException(
                    String.format("Текущий стаж %d мес. меньше требуемых 3 мес.",
                            data.getEmployment().getWorkExperienceCurrent()));
        }

        // Проверка соотношения кредита к зарплате
        BigDecimal maxPossibleAmount = data.getEmployment().getSalary()
                .multiply(BigDecimal.valueOf(24));
        if (data.getAmount().compareTo(maxPossibleAmount) > 0) {
            throw new ScoringException(
                    String.format("Сумма кредита %s превышает 24 зарплаты (%s)",
                            data.getAmount(), maxPossibleAmount));
        }

        log.debug("Жесткие проверки пройдены успешно");
    }

    // Расчет базовой ставки с учетом услуг (страховка/зарплатный)
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

    // Применение всех правил скоринга
    private BigDecimal applyAllScoringRules(BigDecimal rate, ScoringDataDto data) {
        log.debug("Применение правил скоринга к ставке {}%", rate);

        // 1. Влияние статуса занятости
        rate = applyEmploymentStatusRule(rate, data.getEmployment().getEmploymentStatus());

        // 2. Влияние должности
        rate = applyPositionRule(rate, data.getEmployment().getPosition());

        // 3. Влияние семейного положения
        rate = applyMaritalStatusRule(rate, data.getMaritalStatus());

        // 4. Влияние пола и возраста
        rate = applyGenderAgeRule(rate, data.getGender(), data.getBirthdate());

        return rate;
    }

    // Правило: Статус занятости
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
                // UNEMPLOYED уже отсекли в жестких проверках
                break;
        }
        return rate;
    }

    // Правило: Должность
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

    // Правило: Семейное положение
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

    // Правило: Пол и возраст
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

    // Расчет параметров кредита
    private CreditDto calculateCreditParameters(ScoringDataDto data, BigDecimal rate) {
        log.debug("Расчет параметров кредита со ставкой {}%", rate);

        // Определяем сумму кредита (с учетом страховки)
        BigDecimal totalAmount = data.getAmount();
        if (data.getIsInsuranceEnabled()) {
            totalAmount = totalAmount.add(BigDecimal.valueOf(100000));
        }

        // Рассчитываем ежемесячный платеж
        BigDecimal monthlyPayment = AnnuityCalculator.calculateMonthlyPayment(
                totalAmount,
                rate,
                data.getTerm()
        );

        // Рассчитываем ПСК
        BigDecimal psk = AnnuityCalculator.calculatePsk(
                totalAmount,
                monthlyPayment,
                data.getTerm()
        );

        // Генерируем график платежей
        List<PaymentScheduleElementDto> paymentSchedule = AnnuityCalculator.buildPaymentSchedule(
                totalAmount,
                rate,
                data.getTerm(),
                LocalDate.now().plusMonths(1) // первый платеж через месяц
        );

        // Создаем и возвращаем DTO
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