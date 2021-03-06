package com.abhinav.getvaccinated.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface VaccinationService {
    List<String> getAvailableVaccinationCentres() throws JsonProcessingException;

    @Scheduled(fixedRate = 2000)
    void getAvailableVaccinationCentresScheduled() throws JsonProcessingException, ExecutionException, InterruptedException;

    void sendMessage(String body, String phone) throws ExecutionException, InterruptedException;

    void addToWatchList(int districtCode, int age, String phone);

    String[] getTwilioCreds() throws ExecutionException, InterruptedException;
}
