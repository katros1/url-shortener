package com.abat.urlshortener.service;

import com.abat.urlshortener.dtos.ShortenUrlRequestDto;
import com.abat.urlshortener.entity.ShortenUrl;
import com.abat.urlshortener.exceptions.ConflictException;
import com.abat.urlshortener.exceptions.NotFoundException;
import com.abat.urlshortener.exceptions.UrlExpiredException;
import com.abat.urlshortener.repository.ShortenUrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShortenUrlService {

    private final ShortenUrlRepository shortenUrlRepository;

    private static final Logger logger = LoggerFactory.getLogger(ShortenUrlService.class);

    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String ALPHANUMERIC = LETTERS + DIGITS;

    private static final int SHORT_ID_LENGTH = 6;

    @Autowired
    public ShortenUrlService(ShortenUrlRepository shortenUrlRepository) {
        this.shortenUrlRepository = shortenUrlRepository;
    }

    private String generateShortId() {
        SecureRandom random = new SecureRandom();

        StringBuilder generatedId = new StringBuilder(SHORT_ID_LENGTH);

        generatedId.append(LETTERS.charAt(random.nextInt(LETTERS.length())));

        generatedId.append(DIGITS.charAt(random.nextInt(DIGITS.length())));

        for (int i = 2; i < SHORT_ID_LENGTH; i++) {
            generatedId.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }

        return shuffleString(generatedId.toString(), random);
    }

    private String shuffleString(String input, SecureRandom random) {
        char[] array = input.toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[index];
            array[index] = temp;
        }
        return new String(array);
    }

    public ShortenUrl createShortUrl(ShortenUrlRequestDto longUrl, Integer ttl) {

        String shortId = (longUrl.getCustomId() != null && !longUrl.getCustomId().isEmpty()) ? longUrl.getCustomId() : generateShortId();

        if (shortenUrlRepository.existsById(shortId)) {
            logger.warn("Short ID '{}' already exists", shortId);
            throw new ConflictException("The provided ID already exists. Please choose a different ID.");
        }

        ShortenUrl shortUrl = new ShortenUrl();
        shortUrl.setId(shortId);
        shortUrl.setUrl(longUrl.getLongUrl());
        shortUrl.setTtl(ttl != null ? LocalDateTime.now().plusHours(ttl) : null);

        shortenUrlRepository.save(shortUrl);
        logger.info("Created short URL: {}/{}", "http://localhost:8080", shortId);
        return shortUrl;
    }

    public ShortenUrl getShortUrl(String id) {

        ShortenUrl url = shortenUrlRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("The provided ID could not be found."));

        if (url.getTtl() != null && url.getTtl().isBefore(LocalDateTime.now())) {

            logger.warn("Short URL '{}' expired", id);
            throw new UrlExpiredException("The requested short URL has expired and is no longer accessible.");
        }

        return url;
    }

    public void deleteShortUrl(String id) {

        shortenUrlRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("The provided ID could not be found."));

        shortenUrlRepository.deleteById(id);
        logger.info("Deleted short URL with ID: {}", id);
    }

    @Scheduled(cron = "0 0 * * * ?") // Every hour at minute 0
    public void deleteExpiredShortUrls() {
        LocalDateTime now = LocalDateTime.now();

        List<ShortenUrl> expiredUrls = shortenUrlRepository.findByTtlBefore(now);

        for (ShortenUrl url : expiredUrls) {
            shortenUrlRepository.delete(url);
            logger.info("Deleted expired URL with ID: {}", url.getId());
        }
    }
}
