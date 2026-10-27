package com.example.backend.controller;

import com.example.backend.model.Comment;
import com.example.backend.model.Note;
import com.example.backend.repository.CommentRepository;
import com.example.backend.repository.NoteRepository;
import com.example.backend.service.MetricsService;
import com.example.backend.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final NoteRepository noteRepository;
    private final CommentRepository commentRepository;
    private final StorageService storageService;
    private final MetricsService metricsService;

    @Value("${app.aws-enabled:false}")
    private boolean awsEnabled;

    public ApiController(NoteRepository noteRepository,
                         CommentRepository commentRepository,
                         StorageService storageService,
                         MetricsService metricsService) {
        this.noteRepository = noteRepository;
        this.commentRepository = commentRepository;
        this.storageService = storageService;
        this.metricsService = metricsService;
    }

    // 1. GET #1 - Pobiera zgłoszenia wraz z komentarzami i przypisanymi załącznikami S3
    @GetMapping("/notes")
    public List<Note> getNotes(@RequestParam(defaultValue = "0") long delayMs) throws InterruptedException {
        long start = System.currentTimeMillis();

        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }

        List<Note> notes = noteRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        metricsService.recordEndpointLatency("GET_notes", System.currentTimeMillis() - start);
        return notes;
    }

    // 2. GET #2 - Statystyki systemu
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        long start = System.currentTimeMillis();
        Map<String, Object> stats = Map.of(
                "notesCount", noteRepository.count(),
                "commentsCount", commentRepository.count(),
                "attachmentsCount", noteRepository.countByAttachmentFilenameIsNotNull(),
                "mode", awsEnabled ? "AWS Cloud (S3 + RDS + CloudWatch)" : "Local Docker Mode"
        );
        metricsService.recordEndpointLatency("GET_stats", System.currentTimeMillis() - start);
        return stats;
    }

    // 3. POST #1 - Tworzy nowe zgłoszenie w bazie RDS
    @PostMapping("/notes")
    public Note createNote(@RequestBody Note note) {
        long start = System.currentTimeMillis();
        Note saved = noteRepository.save(note);
        metricsService.recordEndpointLatency("POST_notes", System.currentTimeMillis() - start);
        return saved;
    }

    // 4. POST #2 - Dodaje komentarz do zgłoszenia
    @PostMapping("/notes/{id}/comments")
    public Comment addComment(@PathVariable Long id, @RequestBody Comment comment) {
        long start = System.currentTimeMillis();
        Note note = noteRepository.findById(id).orElseThrow();
        comment.setNote(note);
        Comment saved = commentRepository.save(comment);
        metricsService.recordEndpointLatency("POST_comment", System.currentTimeMillis() - start);
        return saved;
    }

    // 5. POST Multimedia — Zapisuje załącznik
    @PostMapping("/media")
    public Note uploadMedia(@RequestParam("noteId") Long noteId,
                            @RequestParam("file") MultipartFile file) throws IOException {
        long start = System.currentTimeMillis();
        Note note = noteRepository.findById(noteId).orElseThrow();

        String filename = storageService.uploadFile(file);
        note.setAttachmentFilename(filename);
        note.setAttachmentContentType(file.getContentType());
        Note updated = noteRepository.save(note);

        metricsService.recordEndpointLatency("POST_media", System.currentTimeMillis() - start);
        return updated;
    }

    // 6. GET Multimedia — Pobiera załącznik z S3 do wyświetlenia przy zgłoszeniu
    @GetMapping("/media/{filename}")
    public ResponseEntity<byte[]> downloadMedia(@PathVariable String filename) throws IOException {
        long start = System.currentTimeMillis();
        byte[] data = storageService.downloadFile(filename);

        String contentType = Files.probeContentType(Paths.get(filename));
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        metricsService.recordEndpointLatency("GET_media", System.currentTimeMillis() - start);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .body(data);
    }
}