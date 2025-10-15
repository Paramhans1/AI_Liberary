package com.ai.library;

import com.ai.library.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class FileStorageServiceTest {

    @Test
    public void store_and_load_file() throws Exception {
        // use temp dir
        Path tmp = Files.createTempDirectory("filestorage-test");
        FileStorageService svc = new FileStorageService(tmp.toAbsolutePath().toString());

        byte[] content = "%PDF-1.4 test pdf content%".getBytes();
        MockMultipartFile mf = new MockMultipartFile("file", "sample.pdf", "application/pdf", content);

        String stored = svc.store(mf, null);
        assertThat(stored).isNotNull();
        Path p = Path.of(stored);
        assertThat(Files.exists(p)).isTrue();
        byte[] read = Files.readAllBytes(p);
        assertThat(read).isEqualTo(content);

        // cleanup
        Files.deleteIfExists(p);
        Files.deleteIfExists(tmp);
    }
}
