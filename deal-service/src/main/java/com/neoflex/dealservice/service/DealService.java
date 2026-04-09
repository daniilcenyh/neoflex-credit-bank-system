package com.neoflex.dealservice.service;

import com.neoflex.deal.dto.ApplicationStatus;
import com.neoflex.deal.dto.ChangeType;
import com.neoflex.deal.dto.CreditDto;
import com.neoflex.deal.dto.CreditStatus;
import com.neoflex.deal.dto.EmploymentDto;
import com.neoflex.deal.dto.FinishRegistrationRequestDto;
import com.neoflex.deal.dto.LoanOfferDto;
import com.neoflex.deal.dto.LoanStatementRequestDto;
import com.neoflex.deal.dto.ScoringDataDto;
import com.neoflex.dealservice.client.CalculatorClient;
import com.neoflex.dealservice.domain.entity.Client;
import com.neoflex.dealservice.domain.entity.Credit;
import com.neoflex.dealservice.domain.entity.Employment;
import com.neoflex.dealservice.domain.entity.Passport;
import com.neoflex.dealservice.domain.entity.Statement;
import com.neoflex.dealservice.domain.entity.StatusHistory;
import com.neoflex.dealservice.domain.repository.ClientRepository;
import com.neoflex.dealservice.domain.repository.CreditRepository;
import com.neoflex.dealservice.domain.repository.PassportRepository;
import com.neoflex.dealservice.domain.repository.StatementRepository;
import com.neoflex.dealservice.exception.ClientNotFoundException;
import com.neoflex.dealservice.exception.EmptyFinishRegistrationException;
import com.neoflex.dealservice.exception.EmptyStatementIdException;
import com.neoflex.dealservice.exception.OfferNotSelectedException;
import com.neoflex.dealservice.exception.PassportAlreadyExistException;
import com.neoflex.dealservice.exception.StatementNotFoundException;
import com.neoflex.dealservice.metrics.annotation.BusinessMetric;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Observed(name = "deal.service")
public class DealService {

    private final ClientRepository clientRepository;
    private final PassportRepository passportRepository;
    private final StatementRepository statementRepository;
    private final CreditRepository creditRepository;
//    private final CalculatorClient calculatorClient;
    private final CalculatorService calculatorService;

    @Transactional
    @BusinessMetric(
            value = "deal.statement.calculated",
            tags = {"operation=calculate", "type=statement"}
    )
    @Observed(name = "deal.statement", contextualName = "calculate-statement")
    public List<LoanOfferDto> calculateStatement(LoanStatementRequestDto request) {
        // 1) проверка перед занесением паспорта пользователя в БД, на то что этот паспорт уже есть
        if (passportRepository.existsBySeriesAndNumber(request.getPassportSeries(), request.getPassportNumber())) {
            log.error("Данный паспорт с данными [{}, {}], уже существует", request.getPassportSeries(), request.getPassportNumber());
            throw new PassportAlreadyExistException("Паспорт с серией " + request.getPassportSeries() + " и номером " + request.getPassportNumber() + " уже существует");
        }
        // 2) маппинг из запроса в сущность (поля какие доступны)
        Passport newPassportToSave = buildPassport(request);

        // 3) маппинг клиента из запроса в сущность
        Client newClientToSave = buildClient(request);

        newClientToSave.setPassport(newPassportToSave);

        // 4) сохраняем текущие данные в БД
        var savedClient = clientRepository.save(newClientToSave);

        // 5) создание statement
        Statement newStatementToSave = Statement.builder()
                .status(ApplicationStatus.PREAPPROVAL)
                .client(savedClient)
                .build();

        // создаем запись в историю
        StatusHistory newStatusHistory = StatusHistory.builder()
                .status(ApplicationStatus.PREAPPROVAL)
                .changeType(ChangeType.AUTOMATIC)
                .statement(newStatementToSave)
                .build();

        newStatementToSave.setStatusHistories(List.of(newStatusHistory));
        var savedStatement = statementRepository.save(newStatementToSave);


        try {
            MDC.put("client_id", savedClient.getClientId().toString());
            MDC.put("statement_id", savedStatement.getStatementId().toString());
            MDC.put("amount", request.getAmount().toString());
            MDC.put("statement_status", savedStatement.getStatus().toString());

            // 6) отправка запроса в calculator-api
            List<LoanOfferDto> offers = calculatorService.getOffers(request, MDC.getCopyOfContextMap());
            for (LoanOfferDto offer: offers) {
                offer.setStatementId(savedStatement.getStatementId());
            }

            offers.sort(Comparator.comparing(LoanOfferDto::getRate).reversed());

            log.info("Сгенерировано [{}] предложений", offers.size());
            return offers;
        } finally {
            MDC.remove("client_id");
            MDC.remove("statement_id");
            MDC.remove("amount");
            MDC.remove("statement_status");
        }
    }

    private static Client buildClient(LoanStatementRequestDto request) {
        return Client.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .birthDate(request.getBirthdate())
                .build();
    }

    private static Passport buildPassport(LoanStatementRequestDto request) {
        return Passport.builder()
                .series(request.getPassportSeries())
                .number(request.getPassportNumber())
                .updated(Instant.now())
                .issueBranch(null)
                .issueDate(null)
                .build();
    }

    @Transactional
    @BusinessMetric(
            value = "deal.offer.selected",
            tags = {"operation=select", "type=offer"}
    )
    @Observed(name = "deal.offer", contextualName = "select-offer")
    public void selectOffer(LoanOfferDto request) {
        log.info("Выбор предложения для заявки: {}", request.getStatementId());
        // 1) получение и проверка на пустоту statementId
        var statementId = request.getStatementId();
        if (statementId == null) {
            log.error("Выбранный кредит пришел с пустым statementId.");
            throw new EmptyStatementIdException("");
        }

        // 2) поиск statement по id
        var statement = statementRepository.findById(statementId)
                .orElseThrow(() -> {
                    log.error("Сделка с id: [{}], не найдена", statementId);
                    return new StatementNotFoundException("");
                });

        // 3) смена статуса для состояния сделки
        statement.setStatus(ApplicationStatus.PREPARE_DOCUMENTS);
        log.debug("Статус сделки с ID: [{}] был изменена на STATUS: [{}]", statementId, statement.getStatus());

        // 4) сохранение в сделке выбранного предложения
        statement.setAppliedOffer(request);
        log.debug("Для сделки с с ID: [{}], было выбрано предложение: [{}]", statementId, request);

        statementSaveHistory(statement, ApplicationStatus.PREPARE_DOCUMENTS, ChangeType.AUTOMATIC);

        statementRepository.save(statement);
        log.info("Кредитное предложение было успешно сохранено для сделки с ID: [{}]", statementId);
    }

    @Transactional
    @BusinessMetric(
            value = "deal.credit.calculated",
            tags = {"operation=calculate", "type=credit"}
    )
    @Observed(name = "deal.credit", contextualName = "calculate-credit")
    public void calculateCredit(UUID statementId, FinishRegistrationRequestDto request) {
        log.info("Начало расчёта кредита для сделки ID: {}", statementId);
        // проверка входных данных
        if (statementId == null) {
            log.error("statementId не может быть null");
            throw new EmptyStatementIdException("Идентификатор заявки не может быть null");
        }
        if (request == null) {
            log.error("FinishRegistrationRequestDto не может быть null");
            throw new EmptyFinishRegistrationException("Данные для завершения регистрации не могут быть null");
        }

        // 1) поиск сделки в БД
        var statement = statementRepository.findById(statementId)
                .orElseThrow(() -> {
                    log.error("Сделка с id: [{}], не найдена", statementId);
                    return new StatementNotFoundException("Заявка с ID " + statementId + " не найдена");
                });

        log.debug("Найдена сделка ID: [{}], статус: [{}], клиент ID: [{}]",
                statementId, statement.getStatus(),
                statement.getClient() != null ? statement.getClient().getClientId() : null);

        // проверка выбранного предложения клиентом
        LoanOfferDto appliedOffer = statement.getAppliedOffer();
        if (appliedOffer == null) {
            log.error("Для сделки ID: [{}] не выбрано кредитное предложение", statementId);
            throw new OfferNotSelectedException("Для заявки не выбрано кредитное предложение");
        }

        // 2) получение клиента из сущности сделки
        var client = statement.getClient();
        if (client == null) {
            log.error("Клиент для сделки с ID: [{}] не найден", statementId);
            throw new ClientNotFoundException("Клиент для заявки " + statementId + " не найден");
        }

        // проверка наличия паспорта у клиента
        Passport passport = client.getPassport();
        if (passport == null) {
            log.error("У клиента ID: [{}] отсутствуют паспортные данные", client.getClientId());
            throw new IllegalStateException("Отсутствуют паспортные данные клиента");
        }

        // 3) обновление недостающих данных в client
        var updatedClient = updateClientField(request, client);
        log.debug("Успешно обновлены данные для клиента с ID сделки: [{}]", statementId);

        // 4) сборка данных для ScoringDataDto
        ScoringDataDto scoringData = buildScoringData(request, statement, updatedClient, appliedOffer, passport);;
        log.debug("Сформирован запрос для calculator-service: amount={}, term={}, isInsurance={}, isSalary={}",
                scoringData.getAmount(), scoringData.getTerm(),
                scoringData.getIsInsuranceEnabled(), scoringData.getIsSalaryClient());

        // 5) отправка запроса в калькулятор
        CreditDto creditDto = calculatorService.calculateCredit(scoringData);
        log.debug("Получен ответ от calculator-service: amount={}, term={}, monthlyPayment={}, rate={}",
                creditDto.getAmount(), creditDto.getTerm(),
                creditDto.getMonthlyPayment(), creditDto.getRate());

        // 6) создание и сохранение сущности credit
        Credit creditToSave = Credit.builder()
                .amount(creditDto.getAmount())
                .term(creditDto.getTerm())
                .monthlyPayment(creditDto.getMonthlyPayment())
                .rate(creditDto.getRate())
                .psk(creditDto.getPsk())
                .updated(Instant.now())
                .isInsuranceEnabled(creditDto.getIsInsuranceEnabled())
                .isSalaryClient(creditDto.getIsSalaryClient())
                .paymentSchedule(creditDto.getPaymentSchedule())
                .creditStatus(CreditStatus.ISSUED)
                .build();

        var savedCredit = creditRepository.save(creditToSave);
        log.info("Создан кредит ID: [{}] для сделки ID: [{}]", savedCredit.getCreditId(), statementId);

        // обновление сделки
        statement.setCredit(savedCredit);
        statement.setStatus(ApplicationStatus.CC_APPROVED);
        statement.setUpdated(Instant.now());
        statementSaveHistory(statement, ApplicationStatus.CC_APPROVED, ChangeType.AUTOMATIC);

        Client savedClient = clientRepository.save(updatedClient);
        Statement savedStatement = statementRepository.save(statement);

        log.info("Кредит успешно рассчитан для сделки ID: [{}]. Кредит ID: [{}], статус сделки: [{}], клиент ID: [{}]",
                statementId, savedCredit.getCreditId(), savedStatement.getStatus(), savedClient.getClientId());
    }

    private static @NonNull ScoringDataDto buildScoringData(FinishRegistrationRequestDto request,
                                                            Statement statement,
                                                            Client client,
                                                            LoanOfferDto appliedOffer,
                                                            Passport passport) {
        return new ScoringDataDto(
                appliedOffer.getRequestedAmount(),
                appliedOffer.getTerm(),
                client.getFirstName(),
                client.getLastName(),
                client.getGender(),
                client.getBirthDate(),
                passport.getSeries(),
                passport.getNumber(),
                passport.getIssueDate(),
                passport.getIssueBranch(),
                client.getMaritalStatus(),
                request.getDependentAmount(),
                request.getEmployment(),
                request.getAccountNumber(),
                appliedOffer.getIsInsuranceEnabled(),
                appliedOffer.getIsSalaryClient()
        );
    }

    private Client updateClientField(FinishRegistrationRequestDto request, Client client) {

        client.setGender(request.getGender());
        client.setMaritalStatus(request.getMaritalStatus());
        client.setDependentAmount(request.getDependentAmount());
        client.setAccountNumber(request.getAccountNumber());

        Passport passport = client.getPassport();
        if (passport != null) {
            passport.setIssueDate(request.getPassportIssueDate());
            passport.setIssueBranch(request.getPassportIssueBranch());
            passport.setUpdated(Instant.now());
        }

        // обновление или создание employment
        EmploymentDto employmentDto = request.getEmployment();
        if (employmentDto != null) {
            Employment employment = client.getEmployment();
            // если такого employment еще нет, то создаем новый
            if (employment == null) {
                employment = Employment.builder()
                        .client(client)
                        .updated(Instant.now())
                        .salary(employmentDto.getSalary())
                        .position(employmentDto.getPosition())
                        .employerINN(employmentDto.getEmployerINN())
                        .employmentStatus(employmentDto.getEmploymentStatus())
                        .workExperienceTotal(employmentDto.getWorkExperienceTotal())
                        .workExperienceCurrent(employmentDto.getWorkExperienceCurrent())
                        .build();

                client.setEmployment(employment);
            } else {
                client.getEmployment().setUpdated(Instant.now());
                client.getEmployment().setSalary(employmentDto.getSalary());
                client.getEmployment().setPosition(employmentDto.getPosition());
                client.getEmployment().setEmployerINN(employmentDto.getEmployerINN());
                client.getEmployment().setEmploymentStatus(employmentDto.getEmploymentStatus());
                client.getEmployment().setWorkExperienceTotal(employmentDto.getWorkExperienceTotal());
                client.getEmployment().setWorkExperienceCurrent(employmentDto.getWorkExperienceCurrent());
            }
        }

        client.setUpdated(Instant.now());

        return client;
    }

    private static void statementSaveHistory(Statement statement, ApplicationStatus status, ChangeType type) {

        StatusHistory history = StatusHistory.builder()
                .status(status)
                .changeType(type)
                .statement(statement)
                .build();

        if (statement.getStatusHistories() == null) {
            statement.setStatusHistories(new ArrayList<>());
        }

        statement.getStatusHistories().add(history);
    }
}





















