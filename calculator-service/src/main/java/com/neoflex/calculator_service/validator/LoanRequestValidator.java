package com.neoflex.calculator_service.validator;

import com.neoflex.calculator_service.exception.PrescoringException;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.calculator.dto.LoanStatementRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LoanRequestValidator {

    @Value("${credit.min-age}")
    private int MIN_AGE;
    @Value("${credit.min-amount}")
    private BigDecimal MIN_AMOUNT;
    @Value("${credit.min-term}")
    private int MIN_TERM;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z]{2,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-z0-9A-Z_!#$%&'*+/=?`{|}~^.-]+@[a-z0-9A-Z.-]+$");
    private static final Pattern PASSPORT_SERIES_PATTERN = Pattern.compile("^\\d{4}$");
    private static final Pattern PASSPORT_NUMBER_PATTERN = Pattern.compile("^\\d{6}$");

    // Валидирование данных согласно ТЗ
    public void validate(LoanStatementRequestDto request) {
        log.debug("Прескоринг заявки: amount={}, term={}, birthdate={}",
                request.getAmount(), request.getTerm(), request.getBirthdate());

        List<String> violations = new ArrayList<>();

        checkAge(request.getBirthdate(), violations);
        checkAmount(request.getAmount(), violations);
        checkTerm(request.getTerm(), violations);
        checkName("firstName", request.getFirstName(), violations);
        checkName("lastName", request.getLastName(), violations);
        checkEmail(request.getEmail(), violations);
        checkPassportSeria(request.getPassportSeries(), violations);
        checkPassportNumber(request.getPassportNumber(), violations);

        if (request.getMiddleName() != null && !request.getMiddleName().isEmpty()) {
            checkName("middleName", request.getMiddleName(), violations);
        }

        if (!violations.isEmpty()) {
            String message = String.join("; ", violations);
            log.warn("Прескоринг не пройден: {}", message);
            throw new PrescoringException(message);
        }

        log.debug("Прескоринг пройден успешно");
    }

    // Проверки

    private void checkPassportSeria(String value, List<String> violations) {
        if (value == null || value.isBlank()) return;
        if (!PASSPORT_SERIES_PATTERN.matcher(value).matches()) {
            violations.add("Поле серии паспорта должно содержать только 4 арабские цифры, получено: '%s'"
                    .formatted(value));
        }
    }

    private void checkPassportNumber(String value, List<String> violations) {
        if (value == null || value.isBlank()) return;
        if (!PASSPORT_NUMBER_PATTERN.matcher(value).matches()) {
            violations.add("Поле номера паспорта должно содержать только 6 арабских цифр, получено: '%s'"
                    .formatted(value));
        }
    }

    private void checkEmail(String email, List<String> violations) {
        if (email == null || email.isBlank()) return;
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            violations.add("Email пользователя (%s) должен содержать корректные символы".formatted(email));
        }
    }

    private void checkAge(LocalDate birthdate, List<String> violations) {
        if (birthdate == null) return;
        int age = Period.between(birthdate, LocalDate.now()).getYears();
        if (age < MIN_AGE) {
            violations.add("Возраст клиента (%d лет) должен быть не менее %d лет"
                    .formatted(age, MIN_AGE));
        }
    }

    private void checkAmount(BigDecimal amount, List<String> violations) {
        if (amount == null) return;
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            violations.add("Сумма кредита (%s руб.) не может быть меньше %s руб."
                    .formatted(amount.toPlainString(), MIN_AMOUNT.toPlainString()));
        }
    }

    private void checkTerm(Integer term, List<String> violations) {
        if (term == null) return;
        if (term < MIN_TERM) {
            violations.add("Срок кредита (%d мес.) не может быть меньше %d мес."
                    .formatted(term, MIN_TERM));
        }
    }

    private void checkName(String field, String value, List<String> violations) {
        if (value == null || value.isBlank()) return;
        if (!NAME_PATTERN.matcher(value).matches()) {
            violations.add("Поле '%s' должно содержать только латинские буквы (2–30 символов), получено: '%s'"
                    .formatted(field, value));
        }
    }
}
