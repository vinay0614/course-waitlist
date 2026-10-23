package com.vinay.aiassistance.courseregistration.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinay.aiassistance.courseregistration.model.Learner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class MockLearnerRepository {
    private final Map<String, Learner> learners;

    public MockLearnerRepository() {
        this(new ObjectMapper());
    }

    public MockLearnerRepository(ObjectMapper mapper) {
        try (var input = new ClassPathResource("mock-data/learners.json").getInputStream()) {
            List<Learner> loaded = mapper.readValue(input, new TypeReference<>() {
            });
            this.learners = new ConcurrentHashMap<>(loaded.stream().collect(Collectors.toMap(Learner::learnerId, l -> l)));
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Unable to load required mock learner data from mock-data/learners.json", e);
        }
    }

    public Optional<Learner> findById(String learnerId) {
        return Optional.ofNullable(learners.get(learnerId));
    }

    /**
     * Runtime fixture hook; packaged JSON remains immutable seed data.
     */
    public void setActiveStatus(String learnerId, boolean active) {
        learners.computeIfPresent(learnerId, (id, learner) -> new Learner(id, learner.completedPrerequisites(), active));
    }
}
