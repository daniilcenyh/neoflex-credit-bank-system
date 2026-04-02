package com.neoflex.dealservice.service;

import com.neoflex.deal.dto.ApplicationStatus;
import com.neoflex.deal.dto.CreditDto;
import com.neoflex.deal.dto.EmploymentDto;
import com.neoflex.deal.dto.EmploymentStatus;
import com.neoflex.deal.dto.FinishRegistrationRequestDto;
import com.neoflex.deal.dto.Gender;
import com.neoflex.deal.dto.LoanOfferDto;
import com.neoflex.deal.dto.LoanStatementRequestDto;
import com.neoflex.deal.dto.MaritalStatus;
import com.neoflex.deal.dto.Position;
import com.neoflex.deal.dto.ScoringDataDto;
import com.neoflex.dealservice.client.CalculatorClient;
import com.neoflex.dealservice.domain.entity.Client;
import com.neoflex.dealservice.domain.entity.Credit;
import com.neoflex.dealservice.domain.entity.Passport;
import com.neoflex.dealservice.domain.entity.Statement;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealServiceTest {
    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PassportRepository passportRepository;

    @Mock
    private StatementRepository statementRepository;

    @Mock
    private CreditRepository creditRepository;

    @Mock
    private CalculatorClient calculatorClient;

    @InjectMocks
    private DealService dealService;

    private LoanStatementRequestDto createValidLoanStatementRequest() {
        LoanStatementRequestDto request = new LoanStatementRequestDto();
        request.setPassportSeries("1234");
        request.setPassportNumber("567890");
        request.setEmail("test@example.com");
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setMiddleName("Иванович");
        request.setBirthdate(LocalDate.of(1990, 1, 1));
        return request;
    }

    private Client createClient(UUID id) {
        Passport passport = Passport.builder()
                .series("1234")
                .number("567890")
                .build();
        return Client.builder()
                .clientId(id)
                .email("test@example.com")
                .firstName("Иван")
                .lastName("Иванов")
                .middleName("Иванович")
                .birthDate(LocalDate.of(1990, 1, 1))
                .passport(passport)
                .build();
    }

    private Statement createStatement(UUID id) {
        Statement statement = Statement.builder()
                .statementId(id)
                .status(ApplicationStatus.PREAPPROVAL)
                .client(createClient(UUID.randomUUID()))
                .build();
        return statement;
    }

    private LoanOfferDto createLoanOfferDto(UUID statementId, BigDecimal rate) {
        return new LoanOfferDto(
                statementId,
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(110000),
                12,
                BigDecimal.valueOf(9500),
                rate,
                true,
                false
        );
    }

    private FinishRegistrationRequestDto createValidFinishRegistrationRequest() {
        FinishRegistrationRequestDto request = new FinishRegistrationRequestDto();
        request.setGender(Gender.MALE);
        request.setMaritalStatus(MaritalStatus.MARRIED);
        request.setDependentAmount(1);
        request.setPassportIssueDate(LocalDate.of(2015, 5, 5));
        request.setPassportIssueBranch("ОВД г. Москвы");

        EmploymentDto employment = new EmploymentDto();
        employment.setSalary(BigDecimal.valueOf(80000));
        employment.setPosition(Position.MIDDLE_MANAGER);
        employment.setEmployerINN("1234567890");
        employment.setEmploymentStatus(EmploymentStatus.EMPLOYED);
        employment.setWorkExperienceTotal(60);
        employment.setWorkExperienceCurrent(36);
        request.setEmployment(employment);

        request.setAccountNumber("40817810099910004312");
        return request;
    }

    @Test
    void calculateStatement_shouldThrowPassportAlreadyExistException_whenPassportExists() {
        LoanStatementRequestDto request = createValidLoanStatementRequest();
        when(passportRepository.existsBySeriesAndNumber(request.getPassportSeries(), request.getPassportNumber()))
                .thenReturn(true);

        assertThrows(PassportAlreadyExistException.class, () -> dealService.calculateStatement(request));

        verify(passportRepository, never()).save(any());
        verify(clientRepository, never()).save(any());
        verify(statementRepository, never()).save(any());
        verify(calculatorClient, never()).getOffers(any());
    }

    @Test
    void calculateStatement_shouldCreateClientAndStatementAndReturnSortedOffers() {
        LoanStatementRequestDto request = createValidLoanStatementRequest();
        when(passportRepository.existsBySeriesAndNumber(any(), any())).thenReturn(false);

        Client savedClient = createClient(UUID.randomUUID());
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);

        Statement savedStatement = createStatement(UUID.randomUUID());
        when(statementRepository.save(any(Statement.class))).thenReturn(savedStatement);

        List<LoanOfferDto> mockOffers = new ArrayList<>(List.of(
                createLoanOfferDto(null, BigDecimal.valueOf(12.0)),
                createLoanOfferDto(null, BigDecimal.valueOf(10.0))
        ));
        when(calculatorClient.getOffers(any(LoanStatementRequestDto.class))).thenReturn(mockOffers);

        List<LoanOfferDto> result = dealService.calculateStatement(request);

        assertNotNull(result);
        assertEquals(2, result.size());
        // Проверка, что statementId проставлен в каждом предложении
        result.forEach(offer -> assertEquals(savedStatement.getStatementId(), offer.getStatementId()));
        // Проверка сортировки по убыванию rate (первое предложение должно быть с большей ставкой)
        assertTrue(result.get(0).getRate().compareTo(result.get(1).getRate()) > 0);

        verify(clientRepository, times(1)).save(any(Client.class));
        verify(statementRepository, times(1)).save(any(Statement.class));
        verify(calculatorClient, times(1)).getOffers(request);
    }

    @Test
    void selectOffer_shouldThrowEmptyStatementIdException_whenStatementIdIsNull() {
        LoanOfferDto request = createLoanOfferDto(null, BigDecimal.valueOf(10.0));

        assertThrows(EmptyStatementIdException.class, () -> dealService.selectOffer(request));

        verify(statementRepository, never()).findById(any());
        verify(statementRepository, never()).save(any());
    }

    @Test
    void selectOffer_shouldThrowStatementNotFoundException_whenStatementNotFound() {
        UUID statementId = UUID.randomUUID();
        LoanOfferDto request = createLoanOfferDto(statementId, BigDecimal.valueOf(10.0));
        when(statementRepository.findById(statementId)).thenReturn(Optional.empty());

        assertThrows(StatementNotFoundException.class, () -> dealService.selectOffer(request));

        verify(statementRepository, never()).save(any());
    }

    @Test
    void selectOffer_shouldUpdateStatusAndSaveAppliedOffer_whenValid() {
        UUID statementId = UUID.randomUUID();
        LoanOfferDto request = createLoanOfferDto(statementId, BigDecimal.valueOf(10.0));
        Statement existingStatement = createStatement(statementId);
        when(statementRepository.findById(statementId)).thenReturn(Optional.of(existingStatement));

        dealService.selectOffer(request);

        assertEquals(ApplicationStatus.PREPARE_DOCUMENTS, existingStatement.getStatus());
        assertEquals(request, existingStatement.getAppliedOffer());
        assertTrue(existingStatement.getStatusHistories().stream()
                .anyMatch(h -> h.getStatus() == ApplicationStatus.PREPARE_DOCUMENTS));
        verify(statementRepository, times(1)).save(existingStatement);
    }

    @Test
    void calculateCredit_shouldThrowEmptyStatementIdException_whenStatementIdIsNull() {
        assertThrows(EmptyStatementIdException.class,
                () -> dealService.calculateCredit(null, new FinishRegistrationRequestDto()));
    }

    @Test
    void calculateCredit_shouldThrowEmptyFinishRegistrationException_whenRequestIsNull() {
        assertThrows(EmptyFinishRegistrationException.class,
                () -> dealService.calculateCredit(UUID.randomUUID(), null));
    }

    @Test
    void calculateCredit_shouldThrowStatementNotFoundException_whenStatementNotFound() {
        UUID statementId = UUID.randomUUID();
        when(statementRepository.findById(statementId)).thenReturn(Optional.empty());

        assertThrows(StatementNotFoundException.class,
                () -> dealService.calculateCredit(statementId, new FinishRegistrationRequestDto()));
    }

    @Test
    void calculateCredit_shouldThrowOfferNotSelectedException_whenAppliedOfferIsNull() {
        UUID statementId = UUID.randomUUID();
        Statement statement = createStatement(statementId);
        statement.setAppliedOffer(null);
        when(statementRepository.findById(statementId)).thenReturn(Optional.of(statement));

        assertThrows(OfferNotSelectedException.class,
                () -> dealService.calculateCredit(statementId, createValidFinishRegistrationRequest()));
    }

    @Test
    void calculateCredit_shouldThrowClientNotFoundException_whenClientIsNull() {
        UUID statementId = UUID.randomUUID();
        Statement statement = createStatement(statementId);
        statement.setAppliedOffer(createLoanOfferDto(statementId, BigDecimal.valueOf(10.0)));
        statement.setClient(null);
        when(statementRepository.findById(statementId)).thenReturn(Optional.of(statement));

        assertThrows(ClientNotFoundException.class,
                () -> dealService.calculateCredit(statementId, createValidFinishRegistrationRequest()));
    }

    @Test
    void calculateCredit_shouldThrowIllegalStateException_whenPassportIsNull() {
        UUID statementId = UUID.randomUUID();
        Client client = createClient(UUID.randomUUID());
        client.setPassport(null);
        Statement statement = createStatement(statementId);
        statement.setAppliedOffer(createLoanOfferDto(statementId, BigDecimal.valueOf(10.0)));
        statement.setClient(client);
        when(statementRepository.findById(statementId)).thenReturn(Optional.of(statement));

        assertThrows(IllegalStateException.class,
                () -> dealService.calculateCredit(statementId, createValidFinishRegistrationRequest()));
    }

    @Test
    void calculateCredit_shouldSuccessfullyCalculateCreditAndUpdateStatement() {
        UUID statementId = UUID.randomUUID();
        Client client = createClient(UUID.randomUUID());
        LoanOfferDto appliedOffer = createLoanOfferDto(statementId, BigDecimal.valueOf(12.0));
        Statement statement = createStatement(statementId);
        statement.setAppliedOffer(appliedOffer);
        statement.setClient(client);

        FinishRegistrationRequestDto finishRequest = createValidFinishRegistrationRequest();

        when(statementRepository.findById(statementId)).thenReturn(Optional.of(statement));

        CreditDto creditDto = new CreditDto();
        creditDto.setAmount(BigDecimal.valueOf(200000));
        creditDto.setTerm(24);
        creditDto.setMonthlyPayment(BigDecimal.valueOf(9500));
        creditDto.setRate(BigDecimal.valueOf(12.0));
        creditDto.setPsk(BigDecimal.valueOf(15.5));
        creditDto.setIsInsuranceEnabled(true);
        creditDto.setIsSalaryClient(false);
        creditDto.setPaymentSchedule(List.of());
        when(calculatorClient.calculateCredit(any(ScoringDataDto.class))).thenReturn(creditDto);

        Credit savedCredit = Credit.builder().creditId(UUID.randomUUID()).build();
        when(creditRepository.save(any(Credit.class))).thenReturn(savedCredit);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(statementRepository.save(any(Statement.class))).thenReturn(statement);

        dealService.calculateCredit(statementId, finishRequest);

        // Проверка обновления клиента
        assertNotNull(client.getGender());
        assertNotNull(client.getMaritalStatus());
        assertEquals(1, client.getDependentAmount());
        assertNotNull(client.getPassport().getIssueDate());
        assertNotNull(client.getPassport().getIssueBranch());
        assertNotNull(client.getEmployment());

        // Проверка создания кредита и обновления сделки
        assertNotNull(statement.getCredit());
        assertEquals(ApplicationStatus.CC_APPROVED, statement.getStatus());

        verify(creditRepository, times(1)).save(any(Credit.class));
        verify(clientRepository, times(1)).save(client);
        verify(statementRepository, times(1)).save(statement);
    }
}





























