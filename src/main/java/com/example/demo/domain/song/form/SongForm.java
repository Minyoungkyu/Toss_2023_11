package com.example.demo.domain.song.form;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SongForm {
    @NotEmpty
    private String subject;
    @NotEmpty
    private String content;
}
