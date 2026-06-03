package org.musi.AI4Education.service.impl;

import io.milvus.client.MilvusClient;
import io.milvus.param.R;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.param.collection.FlushParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MilvusAdminService {

    @Autowired
    private MilvusClient milvusClient;

    @Value("${milvus.collection:default}")
    private String collection;

    public long getRowCount() {
        try {
            R<GetCollectionStatisticsResponse> resp = milvusClient.getCollectionStatistics(
                    GetCollectionStatisticsParam.newBuilder()
                            .withCollectionName(collection)
                            .build()
            );
            if (resp == null || resp.getData() == null) return 0L;
            java.util.List<io.milvus.grpc.KeyValuePair> list = resp.getData().getStatsList();
            for (io.milvus.grpc.KeyValuePair kv : list) {
                if ("row_count".equals(kv.getKey())) {
                    try { return Long.parseLong(kv.getValue()); } catch (NumberFormatException ignore) { return 0L; }
                }
            }
            return 0L;

        } catch (Exception e) {
            return 0L;
        }
    }

    public void flushCollection() {
        try {
            milvusClient.flush(
                    FlushParam.newBuilder()
                            .withCollectionNames(java.util.Collections.singletonList(collection))
                            .build()
            );
        } catch (Exception ignore) {
        }
    }
}


