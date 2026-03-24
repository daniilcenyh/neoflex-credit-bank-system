package com.neoflex.calculator_service.service;

import com.neoflex.calculator_service.validator.LoanRequestValidator;
import jakarta.xml.bind.ValidationException;
import net.proselyte.calculator.dto.LoanOfferDto;
import net.proselyte.calculator.dto.LoanStatementRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PrescoringServiceTest {

    @Mock
    private LoanRequestValidator validator;

    @InjectMocks
    private PrescoringService prescoringService;

    private LoanStatementRequestDto validRequest;

    private final BigDecimal BASE_RATE = new BigDecimal("15.0");
    private final BigDecimal INSURANCE_COST = new BigDecimal("100000");
    private final BigDecimal INSURANCE_RATE_DISCOUNT = new BigDecimal("3.0");
    private final BigDecimal SALARY_RATE_DISCOUNT = new BigDecimal("1.0");
    private final BigDecimal MIN_AMOUNT = new BigDecimal("20000");
    private final Integer MIN_TERM = 6;
    private final Integer MIN_AGE = 18;

    @BeforeEach
    void setUp() {
        // Устанавливаем значения через ReflectionTestUtils так как они @Value
        ReflectionTestUtils.setField(prescoringService, "BASE_RATE", new BigDecimal("15.0"));
        ReflectionTestUtils.setField(prescoringService, "INSURANCE_COST", new BigDecimal("100000"));
        ReflectionTestUtils.setField(prescoringService, "INSURANCE_RATE_DISCOUNT", new BigDecimal("3.0"));
        ReflectionTestUtils.setField(prescoringService, "SALARY_RATE_DISCOUNT", new BigDecimal("1.0"));

        validRequest = new LoanStatementRequestDto(
                new BigDecimal("500000"),
                12,
                "Ivan",
                "Ivanov",
                "ivan@mail.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "123456"
        );
    }

    @Test
    void generateOffers_WithValidRequest_ShouldReturnFourOffers() throws ValidationException {
        // Given
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Then
        assertThat(offers).isNotNull();
        assertThat(offers).hasSize(4);

        // Проверяем все комбинации через прямое сравнение
        boolean hasWithoutInsuranceWithoutSalary = false;
        boolean hasWithoutInsuranceWithSalary = false;
        boolean hasWithInsuranceWithoutSalary = false;
        boolean hasWithInsuranceWithSalary = false;

        for (LoanOfferDto offer : offers) {
            if (!offer.getIsInsuranceEnabled() && !offer.getIsSalaryClient()) {
                hasWithoutInsuranceWithoutSalary = true;
            }
            if (!offer.getIsInsuranceEnabled() && offer.getIsSalaryClient()) {
                hasWithoutInsuranceWithSalary = true;
            }
            if (offer.getIsInsuranceEnabled() && !offer.getIsSalaryClient()) {
                hasWithInsuranceWithoutSalary = true;
            }
            if (offer.getIsInsuranceEnabled() && offer.getIsSalaryClient()) {
                hasWithInsuranceWithSalary = true;
            }
        }

        assertThat(hasWithoutInsuranceWithoutSalary).isTrue();
        assertThat(hasWithoutInsuranceWithSalary).isTrue();
        assertThat(hasWithInsuranceWithoutSalary).isTrue();
        assertThat(hasWithInsuranceWithSalary).isTrue();

        verify(validator, times(1)).validate(validRequest);
    }

    @Test
    void generateOffers_WithoutInsuranceAndWithoutSalary_ShouldCalculateCorrectly() throws ValidationException {
        // Given
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Находим нужное предложение
        LoanOfferDto targetOffer = null;
        for (LoanOfferDto offer : offers) {
            if (!offer.getIsInsuranceEnabled() && !offer.getIsSalaryClient()) {
                targetOffer = offer;
                break;
            }
        }

        // Then
        assertThat(targetOffer).isNotNull();
        assertThat(targetOffer.getIsInsuranceEnabled()).isFalse();
        assertThat(targetOffer.getIsSalaryClient()).isFalse();
        assertThat(targetOffer.getRate()).isEqualTo(BASE_RATE);
        assertThat(targetOffer.getTotalAmount()).isEqualTo(validRequest.getAmount());
        assertThat(targetOffer.getRequestedAmount()).isEqualTo(validRequest.getAmount());
        assertThat(targetOffer.getTerm()).isEqualTo(validRequest.getTerm());
        assertThat(targetOffer.getMonthlyPayment()).isNotNull();
        assertThat(targetOffer.getMonthlyPayment()).isPositive();
    }

    @Test
    void generateOffers_WithInsuranceOnly_ShouldCalculateCorrectly() throws ValidationException {
        // Given
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Находим нужное предложение
        LoanOfferDto targetOffer = null;
        for (LoanOfferDto offer : offers) {
            if (offer.getIsInsuranceEnabled() && !offer.getIsSalaryClient()) {
                targetOffer = offer;
                break;
            }
        }

        // Then
        assertThat(targetOffer).isNotNull();
        assertThat(targetOffer.getIsInsuranceEnabled()).isTrue();
        assertThat(targetOffer.getIsSalaryClient()).isFalse();
        assertThat(targetOffer.getRate()).isEqualTo(BASE_RATE.subtract(INSURANCE_RATE_DISCOUNT));
        assertThat(targetOffer.getTotalAmount()).isEqualTo(validRequest.getAmount().add(INSURANCE_COST));
        assertThat(targetOffer.getMonthlyPayment()).isPositive();
    }

    @Test
    void generateOffers_WithSalaryOnly_ShouldCalculateCorrectly() throws ValidationException {
        // Given
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Находим нужное предложение
        LoanOfferDto targetOffer = null;
        for (LoanOfferDto offer : offers) {
            if (!offer.getIsInsuranceEnabled() && offer.getIsSalaryClient()) {
                targetOffer = offer;
                break;
            }
        }

        // Then
        assertThat(targetOffer).isNotNull();
        assertThat(targetOffer.getIsInsuranceEnabled()).isFalse();
        assertThat(targetOffer.getIsSalaryClient()).isTrue();
        assertThat(targetOffer.getRate()).isEqualTo(BASE_RATE.subtract(SALARY_RATE_DISCOUNT));
        assertThat(targetOffer.getTotalAmount()).isEqualTo(validRequest.getAmount());
        assertThat(targetOffer.getMonthlyPayment()).isPositive();
    }

    @Test
    void generateOffers_WithInsuranceAndSalary_ShouldCalculateCorrectly() throws ValidationException {
        // Given
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Находим нужное предложение
        LoanOfferDto targetOffer = null;
        for (LoanOfferDto offer : offers) {
            if (offer.getIsInsuranceEnabled() && offer.getIsSalaryClient()) {
                targetOffer = offer;
                break;
            }
        }

        // Then
        assertThat(targetOffer).isNotNull();
        assertThat(targetOffer.getIsInsuranceEnabled()).isTrue();
        assertThat(targetOffer.getIsSalaryClient()).isTrue();
        assertThat(targetOffer.getRate())
                .isEqualTo(BASE_RATE
                        .subtract(INSURANCE_RATE_DISCOUNT)
                        .subtract(SALARY_RATE_DISCOUNT));
        assertThat(targetOffer.getTotalAmount()).isEqualTo(validRequest.getAmount().add(INSURANCE_COST));
        assertThat(targetOffer.getMonthlyPayment()).isPositive();
    }

    @Test
    void generateOffers_ShouldGenerateUniqueStatementIds() throws ValidationException {
        // Given
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Then
        // Проверяем, что все ID уникальны
        for (int i = 0; i < offers.size(); i++) {
            for (int j = i + 1; j < offers.size(); j++) {
                assertThat(offers.get(i).getStatementId())
                        .isNotEqualTo(offers.get(j).getStatementId());
            }
        }
    }

    @Test
    void generateOffers_WhenValidationFails_ShouldThrowException() {
        // Given
        doThrow(new RuntimeException("Validation failed"))
                .when(validator).validate(any(LoanStatementRequestDto.class));

        // When & Then
        assertThatThrownBy(() -> prescoringService.generateOffers(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Validation failed");

        verify(validator, times(1)).validate(validRequest);
    }

    @Test
    void generateOffers_WithLargeDiscounts_ShouldHandleNegativeRates() {
        // Given
        ReflectionTestUtils.setField(prescoringService, "INSURANCE_RATE_DISCOUNT", new BigDecimal("20.0"));
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Then
        for (LoanOfferDto offer : offers) {
            // Ставка может быть отрицательной
            if (offer.getIsInsuranceEnabled()) {
                assertThat(offer.getRate()).isNegative();
            }
            // Но платеж всегда положительный
            assertThat(offer.getMonthlyPayment()).isPositive();
        }
    }

    @Test
    void generateOffers_WithNegativeRate_ShouldHandleCorrectly() throws ValidationException {
        // Given
        ReflectionTestUtils.setField(prescoringService, "BASE_RATE", new BigDecimal("-5.0"));
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Then
        for (LoanOfferDto offer : offers) {
            assertThat(offer.getMonthlyPayment()).isPositive();
        }
    }

    @Test
    void generateOffers_WithZeroInsuranceCost_ShouldHandleCorrectly() throws ValidationException {
        // Given
        ReflectionTestUtils.setField(prescoringService, "INSURANCE_COST", BigDecimal.ZERO);
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Then
        for (LoanOfferDto offer : offers) {
            if (offer.getIsInsuranceEnabled()) {
                assertThat(offer.getTotalAmount()).isEqualTo(validRequest.getAmount());
            }
        }
    }

    @Test
    void generateOffers_WithZeroDiscounts_ShouldHandleCorrectly() throws ValidationException {
        // Given
        ReflectionTestUtils.setField(prescoringService, "INSURANCE_RATE_DISCOUNT", BigDecimal.ZERO);
        ReflectionTestUtils.setField(prescoringService, "SALARY_RATE_DISCOUNT", BigDecimal.ZERO);
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Then
        for (LoanOfferDto offer : offers) {
            assertThat(offer.getRate()).isEqualByComparingTo(BASE_RATE);
        }
    }

    @Test
    void generateOffers_WithLargeDiscounts_ShouldHandleCorrectly() throws ValidationException {
        // Given
        BigDecimal largeDiscount = new BigDecimal("20.0");
        ReflectionTestUtils.setField(prescoringService, "INSURANCE_RATE_DISCOUNT", largeDiscount);
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Then
        for (LoanOfferDto offer : offers) {
            if (offer.getIsInsuranceEnabled()) {
                // Ставка может стать отрицательной
                assertThat(offer.getMonthlyPayment()).isPositive();
            }
        }
    }

    @Test
    void generateOffers_WithNullRequest_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> prescoringService.generateOffers(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void generateOffers_WithMinimumAge_ShouldWork() throws ValidationException {
        // Given
        LocalDate birthdate = LocalDate.now().minusYears(18);
        validRequest.setBirthdate(birthdate);
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Then
        assertThat(offers).hasSize(4);
    }

    @Test
    void generateOffers_ShouldSetCorrectFieldsForAllOffers() throws ValidationException {
        // Given
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Then
        for (LoanOfferDto offer : offers) {
            assertThat(offer.getStatementId()).isNotNull();
            assertThat(offer.getRequestedAmount()).isEqualTo(validRequest.getAmount());
            assertThat(offer.getTerm()).isEqualTo(validRequest.getTerm());
            assertThat(offer.getMonthlyPayment()).isNotNull();
            assertThat(offer.getRate()).isNotNull();
            assertThat(offer.getIsInsuranceEnabled()).isNotNull();
            assertThat(offer.getIsSalaryClient()).isNotNull();
        }
    }

    @Test
    void generateOffers_WithPrecisionCheck_ShouldHaveCorrectScale() throws ValidationException {
        // Given
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        // Then
        for (LoanOfferDto offer : offers) {
            assertThat(offer.getRate().scale()).isLessThanOrEqualTo(4);
            assertThat(offer.getMonthlyPayment().scale()).isLessThanOrEqualTo(2);
            assertThat(offer.getTotalAmount().scale()).isLessThanOrEqualTo(2);
            assertThat(offer.getRequestedAmount().scale()).isLessThanOrEqualTo(2);
        }
    }

    @Test
    void generateOffers_WithInsuranceDiscountOnly_ShouldApplyCorrectly() throws ValidationException {
        // Given
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        LoanOfferDto withInsurance = null;
        LoanOfferDto withoutInsurance = null;

        for (LoanOfferDto offer : offers) {
            if (offer.getIsInsuranceEnabled() && !offer.getIsSalaryClient()) {
                withInsurance = offer;
            }
            if (!offer.getIsInsuranceEnabled() && !offer.getIsSalaryClient()) {
                withoutInsurance = offer;
            }
        }

        // Then
        assertThat(withInsurance).isNotNull();
        assertThat(withoutInsurance).isNotNull();
        assertThat(withInsurance.getRate())
                .isEqualTo(withoutInsurance.getRate().subtract(new BigDecimal("3.0")));
    }

    @Test
    void generateOffers_WithSalaryDiscountOnly_ShouldApplyCorrectly() throws ValidationException {
        // Given
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        LoanOfferDto withSalary = null;
        LoanOfferDto withoutSalary = null;

        for (LoanOfferDto offer : offers) {
            if (!offer.getIsInsuranceEnabled() && offer.getIsSalaryClient()) {
                withSalary = offer;
            }
            if (!offer.getIsInsuranceEnabled() && !offer.getIsSalaryClient()) {
                withoutSalary = offer;
            }
        }

        // Then
        assertThat(withSalary).isNotNull();
        assertThat(withoutSalary).isNotNull();
        assertThat(withSalary.getRate())
                .isEqualTo(withoutSalary.getRate().subtract(new BigDecimal("1.0")));
    }

    @Test
    void generateOffers_WithInsuranceCost_ShouldAddCorrectly() throws ValidationException {
        // Given
        BigDecimal amount = new BigDecimal("500000");
        validRequest.setAmount(amount);
        doNothing().when(validator).validate(any(LoanStatementRequestDto.class));

        // When
        List<LoanOfferDto> offers = prescoringService.generateOffers(validRequest);

        LoanOfferDto withInsurance = null;
        LoanOfferDto withoutInsurance = null;

        for (LoanOfferDto offer : offers) {
            if (offer.getIsInsuranceEnabled() && !offer.getIsSalaryClient()) {
                withInsurance = offer;
            }
            if (!offer.getIsInsuranceEnabled() && !offer.getIsSalaryClient()) {
                withoutInsurance = offer;
            }
        }

        // Then
        assertThat(withInsurance).isNotNull();
        assertThat(withoutInsurance).isNotNull();
        assertThat(withInsurance.getTotalAmount())
                .isEqualTo(withoutInsurance.getTotalAmount().add(new BigDecimal("100000")));
    }
}