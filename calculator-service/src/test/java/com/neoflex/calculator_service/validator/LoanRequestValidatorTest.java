package com.neoflex.calculator_service.validator;

import com.neoflex.calculator_service.exception.PrescoringException;
import com.neoflex.calculator_service.exception.ScoringException;
import net.proselyte.calculator.dto.EmploymentDto;
import net.proselyte.calculator.dto.EmploymentStatus;
import net.proselyte.calculator.dto.Gender;
import net.proselyte.calculator.dto.LoanStatementRequestDto;
import net.proselyte.calculator.dto.MaritalStatus;
import net.proselyte.calculator.dto.Position;
import net.proselyte.calculator.dto.ScoringDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class LoanRequestValidatorTest {

    private LoanRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LoanRequestValidator();
        ReflectionTestUtils.setField(validator, "MIN_AGE", 18);
        ReflectionTestUtils.setField(validator, "MIN_AMOUNT", new BigDecimal("20000"));
        ReflectionTestUtils.setField(validator, "MIN_TERM", 6);
    }

    @Test
    void validate_WithValidRequest_ShouldPass() {
        // Given
        LoanStatementRequestDto request = new LoanStatementRequestDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                "ivan@mail.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456"
        );

        // Then
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void validate_WithAgeLessThanMin_ShouldThrowException() {
        // Given
        LoanStatementRequestDto request = new LoanStatementRequestDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                "ivan@mail.com",
                LocalDate.now().minusYears(17),
                "1234",
                "123456"
        );

        // Then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(PrescoringException.class)
                .hasMessageContaining("Возраст клиента");
    }

    @Test
    void validate_WithAmountLessThanMin_ShouldThrowException() {
        // Given
        LoanStatementRequestDto request = new LoanStatementRequestDto(
                new BigDecimal("10000"),
                12,
                "Ivan",
                "Ivanov",
                "ivan@mail.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456"
        );

        // Then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(PrescoringException.class)
                .hasMessageContaining("Сумма кредита");
    }

    @Test
    void validate_WithTermLessThanMin_ShouldThrowException() {
        // Given
        LoanStatementRequestDto request = new LoanStatementRequestDto(
                new BigDecimal("500000"),
                3,
                "Ivan",
                "Ivanov",
                "ivan@mail.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456"
        );

        // Then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(PrescoringException.class)
                .hasMessageContaining("Срок кредита");
    }

    @Test
    void validate_WithInvalidFirstName_ShouldThrowException() {
        // Given
        LoanStatementRequestDto request = new LoanStatementRequestDto(
                new BigDecimal("500000"),
                12,
                "I",
                "Ivanov",
                "ivan@mail.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456"
        );

        // Then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(PrescoringException.class)
                .hasMessageContaining("firstName");
    }

    @Test
    void validate_WithInvalidLastName_ShouldThrowException() {
        // Given
        LoanStatementRequestDto request = new LoanStatementRequestDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "I",
                "ivan@mail.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456"
        );

        // Then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(PrescoringException.class)
                .hasMessageContaining("lastName");
    }

    @Test
    void validate_WithInvalidEmail_ShouldThrowException() {
        // Given
        LoanStatementRequestDto request = new LoanStatementRequestDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                "invalid-email",
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456"
        );

        // Then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(PrescoringException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void validate_WithInvalidPassportSeries_ShouldThrowException() {
        // Given
        LoanStatementRequestDto request = new LoanStatementRequestDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                "ivan@mail.com",
                LocalDate.of(1990, 1, 1),
                "12",
                "123456"
        );

        // Then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(PrescoringException.class)
                .hasMessageContaining("серии паспорта");
    }

    @Test
    void validate_WithInvalidPassportNumber_ShouldThrowException() {
        // Given
        LoanStatementRequestDto request = new LoanStatementRequestDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                "ivan@mail.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "12"
        );

        // Then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(PrescoringException.class)
                .hasMessageContaining("номера паспорта");
    }

    @Test
    void validate_WithValidMiddleName_ShouldPass() {
        // Given
        LoanStatementRequestDto request = new LoanStatementRequestDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                "ivan@mail.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456"
        );
        request.setMiddleName("Ivanovich");

        // Then
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void performHardChecks_WithValidData_ShouldPass() {
        // Given
        EmploymentDto employment = new EmploymentDto(
                EmploymentStatus.EMPLOYED,
                "7707083893",
                new BigDecimal("100000"),
                Position.ENGINEER,
                24,
                12
        );

        ScoringDataDto data = new ScoringDataDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                Gender.MALE,
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456",
                LocalDate.of(2010, 1, 1),
                "Branch",
                MaritalStatus.SINGLE,
                0,
                employment,
                "40817810000000000001",
                false,
                false
        );

        // Then
        assertThatCode(() -> validator.performHardChecks(data)).doesNotThrowAnyException();
    }

    @Test
    void performHardChecks_WithAgeLessThan20_ShouldThrowException() {
        // Given
        EmploymentDto employment = new EmploymentDto(
                EmploymentStatus.EMPLOYED,
                "7707083893",
                new BigDecimal("100000"),
                Position.ENGINEER,
                24,
                12
        );

        ScoringDataDto data = new ScoringDataDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                Gender.MALE,
                LocalDate.now().minusYears(18),
                "1234",
                "123456",
                LocalDate.of(2010, 1, 1),
                "Branch",
                MaritalStatus.SINGLE,
                0,
                employment,
                "40817810000000000001",
                false,
                false
        );

        // Then
        assertThatThrownBy(() -> validator.performHardChecks(data))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("Возраст");
    }

    @Test
    void performHardChecks_WithAgeGreaterThan65_ShouldThrowException() {
        // Given
        EmploymentDto employment = new EmploymentDto(
                EmploymentStatus.EMPLOYED,
                "7707083893",
                new BigDecimal("100000"),
                Position.ENGINEER,
                24,
                12
        );

        ScoringDataDto data = new ScoringDataDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                Gender.MALE,
                LocalDate.now().minusYears(70),
                "1234",
                "123456",
                LocalDate.of(2010, 1, 1),
                "Branch",
                MaritalStatus.SINGLE,
                0,
                employment,
                "40817810000000000001",
                false,
                false
        );

        // Then
        assertThatThrownBy(() -> validator.performHardChecks(data))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("Возраст");
    }

    @Test
    void performHardChecks_WithUnemployed_ShouldThrowException() {
        // Given
        EmploymentDto employment = new EmploymentDto(
                EmploymentStatus.UNEMPLOYED,
                null,
                BigDecimal.ZERO,
                Position.UNEMPLOYED,
                0,
                0
        );

        ScoringDataDto data = new ScoringDataDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                Gender.MALE,
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456",
                LocalDate.of(2010, 1, 1),
                "Branch",
                MaritalStatus.SINGLE,
                0,
                employment,
                "40817810000000000001",
                false,
                false
        );

        // Then
        assertThatThrownBy(() -> validator.performHardChecks(data))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("Безработные");
    }

    @Test
    void performHardChecks_WithTotalExperienceLessThan18_ShouldThrowException() {
        // Given
        EmploymentDto employment = new EmploymentDto(
                EmploymentStatus.EMPLOYED,
                "7707083893",
                new BigDecimal("100000"),
                Position.ENGINEER,
                12,
                12
        );

        ScoringDataDto data = new ScoringDataDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                Gender.MALE,
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456",
                LocalDate.of(2010, 1, 1),
                "Branch",
                MaritalStatus.SINGLE,
                0,
                employment,
                "40817810000000000001",
                false,
                false
        );

        // Then
        assertThatThrownBy(() -> validator.performHardChecks(data))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("Общий стаж");
    }

    @Test
    void performHardChecks_WithCurrentExperienceLessThan3_ShouldThrowException() {
        // Given
        EmploymentDto employment = new EmploymentDto(
                EmploymentStatus.EMPLOYED,
                "7707083893",
                new BigDecimal("100000"),
                Position.ENGINEER,
                24,
                2
        );

        ScoringDataDto data = new ScoringDataDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                Gender.MALE,
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456",
                LocalDate.of(2010, 1, 1),
                "Branch",
                MaritalStatus.SINGLE,
                0,
                employment,
                "40817810000000000001",
                false,
                false
        );

        // Then
        assertThatThrownBy(() -> validator.performHardChecks(data))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("Текущий стаж");
    }

    @Test
    void performHardChecks_WithAmountExceeding24Salaries_ShouldThrowException() {
        // Given
        EmploymentDto employment = new EmploymentDto(
                EmploymentStatus.EMPLOYED,
                "7707083893",
                new BigDecimal("100000"),
                Position.ENGINEER,
                24,
                12
        );

        ScoringDataDto data = new ScoringDataDto(
                new BigDecimal("2500000"),
                12,
                "Ivan",
                "Ivanov",
                Gender.MALE,
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456",
                LocalDate.of(2010, 1, 1),
                "Branch",
                MaritalStatus.SINGLE,
                0,
                employment,
                "40817810000000000001",
                false,
                false
        );

        // Then
        assertThatThrownBy(() -> validator.performHardChecks(data))
                .isInstanceOf(ScoringException.class)
                .hasMessageContaining("превышает 24 зарплаты");
    }
}