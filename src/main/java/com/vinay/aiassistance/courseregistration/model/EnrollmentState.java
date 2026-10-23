package com.vinay.aiassistance.courseregistration.model;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;

public class EnrollmentState {
    private final CourseOffering offering;
    private final Set<String> enrolled = new LinkedHashSet<>();
    private final List<String> waitlist = new ArrayList<>();

    public EnrollmentState(CourseOffering offering) {
        this.offering = offering;
    }

    public CourseOffering offering() {
        return offering;
    }

    public Set<String> enrolled() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(enrolled));
    }

    public List<String> waitlist() {
        return List.copyOf(waitlist);
    }

    public int availableSeats() {
        return offering.capacity() - enrolled.size();
    }

    public boolean contains(String learnerId) {
        return enrolled.contains(learnerId) || waitlist.contains(learnerId);
    }

    public boolean isEnrolled(String learnerId) {
        return enrolled.contains(learnerId);
    }

    public boolean enroll(String learnerId) {
        return enrolled.add(learnerId);
    }

    public boolean waitlist(String learnerId) {
        if (contains(learnerId)) return false;
        return waitlist.add(learnerId);
    }

    public boolean removeEnrolled(String learnerId) {
        return enrolled.remove(learnerId);
    }

    public boolean removeWaitlisted(String learnerId) {
        return waitlist.remove(learnerId);
    }
}
