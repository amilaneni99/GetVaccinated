package com.abhinav.getvaccinated.service.impl;

import com.abhinav.getvaccinated.dto.Center;
import com.abhinav.getvaccinated.dto.Centres;
import com.abhinav.getvaccinated.service.VaccinationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class VaccinationServiceImpl implements VaccinationService {

    private Firestore db;

    public VaccinationServiceImpl() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new ClassPathResource("firebase-service-account.json").getInputStream());
        FirebaseOptions firebaseOptions = FirebaseOptions
                .builder()
                .setCredentials(googleCredentials)
                .build();
        db = FirestoreClient.getFirestore(FirebaseApp.initializeApp(firebaseOptions, "get-vaccinated"));
    }

    @Override
    public List<String> getAvailableVaccinationCentres() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<String> response = restTemplate.exchange("https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByDistrict?district_id=571&date=12-05-2021", HttpMethod.GET, entity, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        Centres centres = objectMapper.readValue(response.getBody(),Centres.class);
        return centres.getCenters().stream().filter(this.getAgeLimitPredicate(18)).map(center -> center.getName()).collect(Collectors.toList());
    }


    @Scheduled(cron = "0 */5 * * * *")
    void dummyAPICall() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getForObject("https://fathomless-scrubland-09643.herokuapp.com/hello",String.class);
    }


    @Override
    @Scheduled(fixedRate = 20000)
    public void getAvailableVaccinationCentresScheduled() throws JsonProcessingException, ExecutionException, InterruptedException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
        HttpEntity entity = new HttpEntity(headers);

        for (HashMap map: this.getWatchList()) {
            StringBuilder finalMessage = new StringBuilder();
            List<String> dates = new ArrayList<>();
            ZoneId zoneId = ZoneId.of("Asia/Kolkata");
            LocalDate date = LocalDate.now(zoneId);
            for(int i=0;i<7;i++){
                String dateString = date.getDayOfMonth()+"-"+((date.getMonthValue()<10)?"0"+date.getMonthValue():date.getMonthValue())+"-"+date.getYear();
                date = date.plusDays(1);
                String url = "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByDistrict?district_id="+map.get("districtCode")+"&date="+dateString;
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                ObjectMapper objectMapper = new ObjectMapper();
                Centres centres = objectMapper.readValue(response.getBody(),Centres.class);
                int age = Integer.parseInt(map.get("age")+"");
                if(centres.getCenters().stream().anyMatch(this.getAgeLimitPredicate(age))) {
//                    List<String> centresList = new ArrayList<>();
//                    centres.getCenters().stream().filter(this.getAgeLimitPredicate(age)).forEach(center -> centresList.add(center.getName()));
//                    System.out.println(String.join(",",centresList));
                    dates.add(dateString);
                    db.collection("watchlist").document(map.get("id").toString()).delete();
                }
            }
            if (dates.size() > 0) {
                finalMessage.append("Vaccines for "+map.get("age")+" available on: "+String.join(",",dates));
            }
            if(!finalMessage.toString().equals("")) {
                this.sendMessage(finalMessage.toString(),map.get("phone").toString());
            }
        }
    }

    List<HashMap> getWatchList() throws ExecutionException, InterruptedException {
        List<HashMap> resultMap = new ArrayList<>();
        ApiFuture<QuerySnapshot> query = db.collection("watchlist").get();
        QuerySnapshot querySnapshot = query.get();
        List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            HashMap map = new HashMap();
            map.put("id",document.getId());
            map.put("districtCode",document.get("districtCode"));
            map.put("age", document.get("age"));
            map.put("phone", document.getString("phone"));
            resultMap.add(map);
        }
        return resultMap;
    }


    Predicate<Center> getAgeLimitPredicate(int age) {
        Predicate<Center> predicate = center -> center.getSessions().stream().anyMatch(session -> (session.getMin_age_limit() <= age && session.getAvailable_capacity() >= 5));
        return predicate;
    }

    Predicate<Center> getAvailabiltityPredicate(int limit) {
        Predicate<Center> predicate = center -> center.getSessions().stream().anyMatch(session -> session.getAvailable_capacity() >= limit);
        return predicate;
    }

    @Override
    public void sendMessage(String body, String phone) throws ExecutionException, InterruptedException {
        String[] creds = this.getTwilioCreds();
        Twilio.init(creds[0], creds[1]);
        Message message = Message.creator(
                new com.twilio.type.PhoneNumber("+91"+phone),
                new com.twilio.type.PhoneNumber("+18304200819"),
                body)
                .setStatusCallback(URI.create("http://postb.in/1234abcd"))
                .create();

        System.out.println(message.getSid());
    }

    @Override
    public void addToWatchList(int districtCode, int age, String phone) {
        HashMap data = new HashMap();
        data.put("districtCode", districtCode);
        data.put("age", age);
        data.put("phone", phone);

        db.collection("watchlist").add(data);
    }

    @Override
    public String[] getTwilioCreds() throws ExecutionException, InterruptedException {
        DocumentSnapshot documentSnapshot = db.collection("twilio-creds").document("bRZXlO0Qx3Po461FiNdo").get().get();
        String ACCOUNT_SID = documentSnapshot.getString("accountSID");
        String AUTH_TOKEN = documentSnapshot.getString("authToken");
        return new String[]{ACCOUNT_SID,AUTH_TOKEN};
    }
}
