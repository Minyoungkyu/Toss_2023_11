package com.example.demo.domain.song.repository;

import com.example.demo.domain.song.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {
    List<Song> findAllByAuthorId(Long id);
}
