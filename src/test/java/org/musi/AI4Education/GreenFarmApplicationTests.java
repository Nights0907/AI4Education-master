package org.musi.AI4Education;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.milvus.client.MilvusClient;
import org.junit.jupiter.api.Test;
import org.musi.AI4Education.service.impl.DocumentVectorService;
import org.musi.AI4Education.service.impl.MilvusAdminService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class GreenFarmApplicationTests {

	@MockBean
	private MilvusClient milvusClient;

	@MockBean
	private EmbeddingStore<TextSegment> embeddingStore;

	@MockBean
	private MilvusAdminService milvusAdminService;

	@MockBean
	private DocumentVectorService documentVectorService;

	@Test
	void contextLoads() {
	}

}
