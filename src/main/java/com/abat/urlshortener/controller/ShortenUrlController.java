package com.abat.urlshortener.controller;

import com.abat.urlshortener.dtos.ShortenUrlRequestDto;
import com.abat.urlshortener.dtos.ShortenUrlResponseDto;
import com.abat.urlshortener.entity.ShortenUrl;
import com.abat.urlshortener.service.ShortenUrlService;
import com.abat.urlshortener.util.CustomResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@RestController
@RequestMapping
public class ShortenUrlController {

    private static final Logger logger = LoggerFactory.getLogger(ShortenUrlController.class);

    private final ShortenUrlService shortenUrlService;

    @Autowired
    public ShortenUrlController(ShortenUrlService shortenUrlService) {
        this.shortenUrlService = shortenUrlService;
    }

    @PostMapping("/api/v1/shorten-url")
    public ResponseEntity<CustomResponse<ShortenUrlResponseDto>> shortenUrl(@RequestParam(required = false) Integer ttl,
                                                                            @RequestBody @Valid ShortenUrlRequestDto longUrl) {

        ShortenUrlResponseDto shortUrl = shortenUrlService.createShortUrl(longUrl, ttl);

        return ResponseEntity.status(HttpStatus.CREATED.value()).body(
                CustomResponse.successResponse("Short URL created successfully",
                        HttpStatus.CREATED.value(), shortUrl));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> redirectToOriginal(@PathVariable String id) {
        ShortenUrl url = shortenUrlService.getShortUrl(id);

        logger.info("Redirecting to: {}", url.getUrl());
        return ResponseEntity.status(302).location(URI.create(url.getUrl())).build();
    }

    @DeleteMapping("/api/v1/shorten-url/{id}")
    public ResponseEntity<CustomResponse<Void>> deleteShortUrl(@PathVariable String id) {
        shortenUrlService.deleteShortUrl(id);
        return ResponseEntity.ok(CustomResponse.successResponse("Shorten url deleted successfully", HttpStatus.OK.value()));
    }
}
