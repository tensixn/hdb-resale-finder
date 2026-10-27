package com.abcde.hdbresale.service;

import com.abcde.hdbresale.domain.User;
import com.abcde.hdbresale.dto.ProfileDto;
import com.abcde.hdbresale.dto.WeightsDto;
import com.abcde.hdbresale.exception.ApiException;
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

    /** The account page (UC-14): email, saved weights, saved financial profile and last search. */
    @Transactional(readOnly = true)
    public ProfileDto profile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(ApiException::unauthenticated);
        ProfileDto.LastSearch last = criteriaRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(c -> new ProfileDto.LastSearch(c.getWorkplace1(), c.getWorkplace2(), c.getTransportModes(),
                        c.getMinPrice(), c.getMaxPrice(), c.getFlatTypes(), c.getTowns(), c.getCreatedAt()))
                .orElse(null);
        return new ProfileDto(user.getEmail(), user.getCreatedAt(), WeightsDto.of(weightService.forUser(userId)),
                cpfGrantService.savedProfile(userId).orElse(null),
                cpfGrantService.savedProfileUpdatedAt(userId).orElse(null), last);
    }
}
