package com.evandorou;

import com.evandorou.events.feed.openf1.OpenF1Client;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class WeeApplicationTest {

    @MockBean
    private OpenF1Client openF1Client;

    @Test
    void contextLoads() {
    }
}
