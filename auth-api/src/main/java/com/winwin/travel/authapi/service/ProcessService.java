package com.winwin.travel.authapi.service;

import com.winwin.travel.authapi.entity.ProcessingLog;
import com.winwin.travel.authapi.entity.User;
import com.winwin.travel.authapi.repository.ProcessingLogRepository;
import com.winwin.travel.authapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ProcessService {

    private final RestTemplate restTemplate;
    private final ProcessingLogRepository processingLogRepository;
    private final UserRepository userRepository;
    private final String dataApiUrl;
    private final String internalToken;

    public ProcessService(
            ProcessingLogRepository processingLogRepository,
            UserRepository userRepository,
            @Value("${data-api.url}") String dataApiUrl,
            @Value("${data-api.internal-token}") String internalToken
    ) {
        this.restTemplate = new RestTemplate();
        this.processingLogRepository = processingLogRepository;
        this.userRepository = userRepository;
        this.dataApiUrl = dataApiUrl;
        this.internalToken = internalToken;
    }

    public String processText(String email, String inputText) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", internalToken);
        headers.set("Content-Type", "application/json");

        Map<String, String> requestBody = Map.of("text", inputText);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                dataApiUrl + "/api/transform",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        String outputText = (String) response.getBody().get("result");

        ProcessingLog log = new ProcessingLog(user.getId(), inputText, outputText);
        processingLogRepository.save(log);

        return outputText;
    }
}

