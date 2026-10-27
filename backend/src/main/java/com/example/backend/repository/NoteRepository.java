package com.example.backend.repository;

import com.example.backend.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {
    long countByAttachmentFilenameIsNotNull();
}
