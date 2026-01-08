package com.winwin.travel.authapi.controller;

import com.winwin.travel.authapi.dto.ProcessRequest;
import com.winwin.travel.authapi.dto.ProcessResponse;
import com.winwin.travel.authapi.service.ProcessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProcessController {

    private final ProcessService processService;

    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }

    @PostMapping("/process")
    public ResponseEntity<ProcessResponse> process(
            @RequestBody ProcessRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();
        String result = processService.processText(email, request.getText());

        return ResponseEntity.ok(new ProcessResponse(result));
    }
}

