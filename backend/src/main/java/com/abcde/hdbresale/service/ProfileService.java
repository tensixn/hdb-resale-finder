package com.abcde.hdbresale.service;

import com.abcde.hdbresale.dto.ProfileDto;
import com.abcde.hdbresale.repository.SearchCriteriaRepository;
import com.abcde.hdbresale.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Control class for UC-14: the signed-in user's account, saved weights and saved financial profile (FR-33). */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final LivabilityWeightService weightService;
    private final CpfGrantService cpfGrantService;
    private final SearchCriteriaRepository criteriaRepository;

    public ProfileService(UserRepository userRepository, LivabilityWeightService weightService,
                          CpfGrantService cpfGrantService, SearchCriteriaRepository criteriaRepository) {
        this.userRepository = userRepository;
        this.weightService = weightService;
        this.cpfGrantService = cpfGrantService;
        this.criteriaRepository = criteriaRepository;
    }

    @Transactional(readOnly = true)
    public ProfileDto profile(Long userId) {
        // TODO (Lab 4): implement profile
        throw new UnsupportedOperationException("TODO: ProfileService.profile");
    }
}
