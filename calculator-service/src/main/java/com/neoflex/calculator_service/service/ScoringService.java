package com.neoflex.calculator_service.service;

import net.proselyte.calculator.dto.CreditDto;
import net.proselyte.calculator.dto.ScoringDataDto;

public interface ScoringService {

    CreditDto calculateCredit(ScoringDataDto data);
}
