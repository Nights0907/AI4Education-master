package org.musi.AI4Education.utils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MatrixAction {
    //构建相似性矩阵
    public static Set<String> constructMatrix(String uid, Map<String, Set<String>> userMapList) {
        Set<String> recommendProductList = new HashSet<>();
        // 构造相似度矩阵
        Set<String> currentUserProducts = userMapList.get(uid);
        for (Map.Entry<String, Set<String>> entry : userMapList.entrySet()) {
            String otherUserId = entry.getKey();
            if (!otherUserId.equals(uid)) {
                Set<String> otherUserProducts = entry.getValue();
                double similarity = computeSimilarity(currentUserProducts, otherUserProducts);
                if (similarity > 0) {
                    recommendProductList.addAll(otherUserProducts);
                }
            }
        }
        // 去掉原本就收藏的商品
        recommendProductList.removeAll(currentUserProducts);

        return recommendProductList;
    }

    // 计算两个用户的相似度
    public static double computeSimilarity(Set<String> set1, Set<String> set2) {
        int commonCount = 0;
        for (String item : set1) {
            if (set2.contains(item)) {
                commonCount++;
            }
        }
        double similarity = commonCount / Math.sqrt(set1.size() * set2.size());
        return similarity;
    }
}
