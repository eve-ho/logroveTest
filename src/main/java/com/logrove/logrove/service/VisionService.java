package com.logrove.logrove.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class VisionService {

    public List<String> detectLabels(MultipartFile file) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        // 이미지 데이터를 읽어옴
        ByteString imgBytes = ByteString.copyFrom(file.getBytes());
        Image img = Image.newBuilder().setContent(imgBytes).build();

        // 분석 기능 설정 (라벨 감지)
        Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();
        requests.add(request);

        // API 호출 및 결과 반환
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<String> labels = new ArrayList<>();

            for (AnnotateImageResponse res : response.getResponsesList()) {
                if (res.hasError()) return List.of("에러 발생: " + res.getError().getMessage());

                for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
                    labels.add(annotation.getDescription());
                }
            }
            return labels;
        }
    }
}