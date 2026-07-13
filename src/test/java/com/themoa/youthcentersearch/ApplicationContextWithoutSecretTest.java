package com.themoa.youthcentersearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:context-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false",
        "spring.ai.model.chat=none",
        "spring.ai.model.embedding=none",
        "spring.ai.vectorstore.type=none",
        "RAG_ENABLED=false",
        "app.rag.enabled=false",
        "app.region-bootstrap.enabled=false"
})
class ApplicationContextWithoutSecretTest {
    @Test
    void startsWithoutApplicationSecretFile() {
    }
}
