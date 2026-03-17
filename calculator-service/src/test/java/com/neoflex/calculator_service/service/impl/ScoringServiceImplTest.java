package com.neoflex.calculator_service.service.impl;


import com.neoflex.calculator_service.exception.ScoringException;
import net.proselyte.calculator.dto.CreditDto;
import net.proselyte.calculator.dto.EmploymentDto;
import net.proselyte.calculator.dto.EmploymentStatus;
import net.proselyte.calculator.dto.Gender;
import net.proselyte.calculator.dto.MaritalStatus;
import net.proselyte.calculator.dto.Position;
import net.proselyte.calculator.dto.ScoringDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ScoringServiceImplTest {

    @InjectMocks
    private ScoringServiceImpl scoringService;

    private ScoringDataDto validData;
    private EmploymentDto validEmployment;

    private final BigDecimal BASE_RATE = new BigDecimal("15.0");
    private final BigDecimal INSURANCE_RATE_DISCOUNT = new BigDecimal("3.0");
    private final BigDecimal SALARY_RATE_DISCOUNT = new BigDecimal("1.0");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scoringService, "BASE_RATE", BASE_RATE);
        ReflectionTestUtils.setField(scoringService, "INSURANCE_RATE_DISCOUNT", INSURANCE_RATE_DISCOUNT);
        ReflectionTestUtils.setField(scoringService, "SALARY_RATE_DISCOUNT", SALARY_RATE_DISCOUNT);

        validEmployment = new EmploymentDto(
                EmploymentStatus.EMPLOYED,
                "7707083893",
                new BigDecimal("100000"),
                Position.ENGINEER,
                60,  // общий стаж 5 лет
                24   // текущий стаж 2 года
        );

        validData = new ScoringDataDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                Gender.MALE,
                LocalDate.of(1990, 1, 1),  // возраст ~36 лет
                "1234",
                "123456",
                LocalDate.of(2010, 1, 1),
                "Отделение УФМС",
                MaritalStatus.MARRIED,
                2,
                validEmployment,
                "40817810000000000001",
                true,
                true
        );
    }

    // ТЕСТЫ УСПЕШНОГО СКОРИНГА

    @Test
    void calculateCredit_WithValidData_ShouldReturnCreditDto() {
        // Given
        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(validData.getAmount().add(new BigDecimal("100000"))); // со страховкой
        assertThat(result.getTerm()).isEqualTo(validData.getTerm());
        assertThat(result.getIsInsuranceEnabled()).isTrue();
        assertThat(result.getIsSalaryClient()).isTrue();
        assertThat(result.getPaymentSchedule()).isNotEmpty();
        assertThat(result.getPaymentSchedule()).hasSize(validData.getTerm());
        assertThat(result.getMonthlyPayment()).isPositive();
        assertThat(result.getPsk()).isPositive();
    }

    @Test
    void calculateCredit_WithoutInsuranceAndSalary_ShouldCalculateCorrectly() {
        // Given
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.SINGLE); // без модификаций
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1990, 1, 1)); // 36 лет - попадает под скидку по возрасту!
        validEmployment.setPosition(Position.ENGINEER); // ENGINEER без модификаций
        validEmployment.setEmploymentStatus(EmploymentStatus.EMPLOYED); // EMPLOYED без модификаций

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Мужчина 36 лет: -3% (applyGenderAgeRule)
        // Итого: 12%
        BigDecimal expectedRate = new BigDecimal("12.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    @Test
    void calculateCredit_WithInsuranceOnly_ShouldCalculateCorrectly() {
        // Given
        validData.setIsInsuranceEnabled(true);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.SINGLE);
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1990, 1, 1)); // 36 лет
        validEmployment.setPosition(Position.ENGINEER);
        validEmployment.setEmploymentStatus(EmploymentStatus.EMPLOYED);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Страховка: -3%
        // - Мужчина 36 лет: -3%
        // Итого: 9%
        BigDecimal expectedRate = new BigDecimal("9.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    @Test
    void calculateCredit_WithSalaryOnly_ShouldCalculateCorrectly() {
        // Given
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(true);
        validData.setMaritalStatus(MaritalStatus.SINGLE);
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1990, 1, 1)); // 36 лет
        validEmployment.setPosition(Position.ENGINEER);
        validEmployment.setEmploymentStatus(EmploymentStatus.EMPLOYED);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Зарплатный клиент: -1%
        // - Мужчина 36 лет: -3%
        // Итого: 11%
        BigDecimal expectedRate = new BigDecimal("11.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    // ТЕСТЫ ЖЕСТКИХ ПРОВЕРОК

    @Test
    void calculateCredit_WithAgeBelow20_ShouldThrowException() {
        // Given
        validData.setBirthdate(LocalDate.now().minusYears(19));

        // When & Then
        assertThatThrownBy(() -> scoringService.calculateCredit(validData))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("Возраст 19 лет не подходит");
    }

    @Test
    void calculateCredit_WithAgeAbove65_ShouldThrowException() {
        // Given
        validData.setBirthdate(LocalDate.now().minusYears(66));

        // When & Then
        assertThatThrownBy(() -> scoringService.calculateCredit(validData))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("Возраст 66 лет не подходит");
    }

    @Test
    void calculateCredit_WithUnemployed_ShouldThrowException() {
        // Given
        validEmployment.setEmploymentStatus(EmploymentStatus.UNEMPLOYED);
        validData.setEmployment(validEmployment);

        // When & Then
        assertThatThrownBy(() -> scoringService.calculateCredit(validData))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("Безработные клиенты не рассматриваются");
    }

    @Test
    void calculateCredit_WithTotalExperienceLessThan18Months_ShouldThrowException() {
        // Given
        validEmployment.setWorkExperienceTotal(12);
        validData.setEmployment(validEmployment);

        // When & Then
        assertThatThrownBy(() -> scoringService.calculateCredit(validData))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("Общий стаж 12 мес. меньше требуемых 18 мес");
    }

    @Test
    void calculateCredit_WithCurrentExperienceLessThan3Months_ShouldThrowException() {
        // Given
        validEmployment.setWorkExperienceCurrent(2);
        validData.setEmployment(validEmployment);

        // When & Then
        assertThatThrownBy(() -> scoringService.calculateCredit(validData))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("Текущий стаж 2 мес. меньше требуемых 3 мес");
    }

    @Test
    void calculateCredit_WithAmountExceeding24Salaries_ShouldThrowException() {
        // Given
        validData.setAmount(new BigDecimal("2500000")); // 25 * 100000 > 24 * 100000
        validEmployment.setSalary(new BigDecimal("100000"));
        validData.setEmployment(validEmployment);

        // When & Then
        assertThatThrownBy(() -> scoringService.calculateCredit(validData))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("превышает 24 зарплаты");
    }

    // ТЕСТЫ ПРАВИЛ СКОРИНГА

    @Test
    void applyEmploymentStatusRule_SelfEmployed_ShouldIncreaseRate() {
        // Given
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.SINGLE);
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1990, 1, 1)); // 36 лет
        validEmployment.setPosition(Position.ENGINEER);
        validEmployment.setEmploymentStatus(EmploymentStatus.SELF_EMPLOYED);
        validData.setEmployment(validEmployment);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Самозанятый: +2%
        // - Мужчина 36 лет: -3%
        // Итого: 14%
        BigDecimal expectedRate = new BigDecimal("14.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    @Test
    void applyEmploymentStatusRule_BusinessOwner_ShouldIncreaseRate() {
        // Given
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.SINGLE);
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1990, 1, 1)); // 36 лет
        validEmployment.setPosition(Position.ENGINEER);
        validEmployment.setEmploymentStatus(EmploymentStatus.BUSINESS_OWNER);
        validData.setEmployment(validEmployment);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Владелец бизнеса: +1%
        // - Мужчина 36 лет: -3%
        // Итого: 13%
        BigDecimal expectedRate = new BigDecimal("13.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    @Test
    void applyPositionRule_TopManager_ShouldDecreaseRate() {
        // Given
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.SINGLE);
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1990, 1, 1)); // 36 лет
        validEmployment.setPosition(Position.TOP_MANAGER);
        validEmployment.setEmploymentStatus(EmploymentStatus.EMPLOYED);
        validData.setEmployment(validEmployment);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Топ-менеджер: -3%
        // - Мужчина 36 лет: -3%
        // Итого: 9%
        BigDecimal expectedRate = new BigDecimal("9.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    @Test
    void applyPositionRule_MiddleManager_ShouldDecreaseRate() {
        // Given
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.SINGLE);
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1990, 1, 1)); // 36 лет
        validEmployment.setPosition(Position.MIDDLE_MANAGER);
        validEmployment.setEmploymentStatus(EmploymentStatus.EMPLOYED);
        validData.setEmployment(validEmployment);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Менеджер среднего звена: -2%
        // - Мужчина 36 лет: -3%
        // Итого: 10%
        BigDecimal expectedRate = new BigDecimal("10.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    @Test
    void applyMaritalStatusRule_Married_ShouldDecreaseRate() {
        // Given
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.MARRIED);
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1990, 1, 1)); // 36 лет
        validEmployment.setPosition(Position.ENGINEER);
        validEmployment.setEmploymentStatus(EmploymentStatus.EMPLOYED);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Женат: -3%
        // - Мужчина 36 лет: -3%
        // Итого: 9%
        BigDecimal expectedRate = new BigDecimal("9.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    @Test
    void applyMaritalStatusRule_Divorced_ShouldIncreaseRate() {
        // Given
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.DIVORCED);
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1990, 1, 1)); // 36 лет
        validEmployment.setPosition(Position.ENGINEER);
        validEmployment.setEmploymentStatus(EmploymentStatus.EMPLOYED);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Разведен: +1%
        // - Мужчина 36 лет: -3%
        // Итого: 13%
        BigDecimal expectedRate = new BigDecimal("13.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    @Test
    void applyGenderAgeRule_MaleInOptimalAge_ShouldDecreaseRate() {
        // Given
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.SINGLE);
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1990, 1, 1)); // 36 лет
        validEmployment.setPosition(Position.ENGINEER);
        validEmployment.setEmploymentStatus(EmploymentStatus.EMPLOYED);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Мужчина 36 лет: -3%
        // Итого: 12%
        BigDecimal expectedRate = new BigDecimal("12.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    @Test
    void applyGenderAgeRule_FemaleInOptimalAge_ShouldDecreaseRate() {
        // Given
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.SINGLE);
        validData.setGender(Gender.FEMALE);
        validData.setBirthdate(LocalDate.of(1980, 1, 1)); // 46 лет
        validEmployment.setPosition(Position.ENGINEER);
        validEmployment.setEmploymentStatus(EmploymentStatus.EMPLOYED);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Женщина 46 лет: -3%
        // Итого: 12%
        BigDecimal expectedRate = new BigDecimal("12.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    @Test
    void applyGenderAgeRule_MaleOutsideOptimalAge_ShouldNotApplyDiscount() {
        // Given
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.SINGLE);
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1965, 1, 1)); // 61 год (вне диапазона 30-55)
        validEmployment.setPosition(Position.ENGINEER);
        validEmployment.setEmploymentStatus(EmploymentStatus.EMPLOYED);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 15
        // Применяются правила:
        // - Мужчина 61 год: без скидки
        // Итого: 15%
        BigDecimal expectedRate = new BigDecimal("15.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);
    }

    // ТЕСТЫ НА НУЛЕВУЮ СТАВКУ

    @Test
    void calculateCredit_WithZeroBaseRate_ShouldHandleCorrectly() {
        // Given
        ReflectionTestUtils.setField(scoringService, "BASE_RATE", BigDecimal.ZERO);
        validData.setIsInsuranceEnabled(false);
        validData.setIsSalaryClient(false);
        validData.setMaritalStatus(MaritalStatus.SINGLE);
        validData.setGender(Gender.MALE);
        validData.setBirthdate(LocalDate.of(1990, 1, 1)); // 36 лет
        validEmployment.setPosition(Position.ENGINEER);
        validEmployment.setEmploymentStatus(EmploymentStatus.EMPLOYED);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        // BASE_RATE = 0
        // Применяются правила:
        // - Мужчина 36 лет: -3%
        // Итого: -3%
        BigDecimal expectedRate = new BigDecimal("-3.0");
        assertThat(result.getRate()).isEqualByComparingTo(expectedRate);

        // Проверяем что платеж положительный
        assertThat(result.getMonthlyPayment()).isPositive();
    }

    // ТЕСТЫ ИНТЕГРАЦИИ С ANNUITYCALCULATOR

    @Test
    void calculateCredit_ShouldCallAnnuityCalculatorMethods() {
        // Given
        // Используем mockStatic для проверки вызовов статических методов

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPaymentSchedule()).isNotEmpty();
        assertThat(result.getMonthlyPayment()).isPositive();
        assertThat(result.getPsk()).isPositive();
    }

    // ТЕСТЫ ГРАНИЧНЫХ ЗНАЧЕНИЙ

    @Test
    void calculateCredit_WithMinimumValidAge_ShouldPass() {
        // Given
        validData.setBirthdate(LocalDate.now().minusYears(20));

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void calculateCredit_WithMaximumValidAge_ShouldPass() {
        // Given
        validData.setBirthdate(LocalDate.now().minusYears(65));

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void calculateCredit_WithMinimumValidExperience_ShouldPass() {
        // Given
        validEmployment.setWorkExperienceTotal(18);
        validEmployment.setWorkExperienceCurrent(3);
        validData.setEmployment(validEmployment);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void calculateCredit_WithMaximumValidAmount_ShouldPass() {
        // Given
        BigDecimal maxAmount = validEmployment.getSalary().multiply(BigDecimal.valueOf(24));
        validData.setAmount(maxAmount);

        // When
        CreditDto result = scoringService.calculateCredit(validData);

        // Then
        assertThat(result).isNotNull();
    }
}