package com.abcde.hdbresale;

import com.abcde.hdbresale.bootstrap.Application;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Checks that the start-up class boots the whole Spring context with every bean wired. */
@SpringBootTest(classes = Application.class)
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
