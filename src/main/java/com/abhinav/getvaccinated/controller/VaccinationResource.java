package com.abhinav.getvaccinated.controller;

import com.abhinav.getvaccinated.service.VaccinationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.minidev.json.parser.ParseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
public class VaccinationResource {

    private RestTemplate restTemplate;
    private VaccinationService vaccinationService;

    public VaccinationResource(VaccinationService vaccinationService) {
        this.restTemplate = new RestTemplate();
        this.vaccinationService = vaccinationService;
    }

    @GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public String greet() {
        return "Hello";
    }

    @GetMapping(value = "/centres", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin
    public String getVaccinationCentres(@RequestParam String districtId, @RequestParam String date) throws JsonProcessingException, ParseException {
//        vaccinationService.sendMessage("");
        System.out.print(String.join(vaccinationService.getAvailableVaccinationCentres().toString(),","));
        return "Success";
    }

    @PostMapping(value = "/watchlist/add", produces = {MediaType.APPLICATION_JSON_VALUE})
    public void addToWatchList(@RequestParam int districtCode, @RequestParam int age, @RequestParam String phone) {
        this.vaccinationService.addToWatchList(districtCode, age, phone);
    }
}
