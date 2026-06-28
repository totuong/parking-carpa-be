package main.twinbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ResponseApi {
    private Object data;
    private Meta meta;

    public ResponseApi() {

    }

    public ResponseApi(Object data) {
        this.data = data;
        this.meta = new Meta();
    }

    public ResponseApi(Object data, Meta meta) {
        this.data = data;
        this.meta = meta;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Meta {
        private Integer page;
        private Integer size;
        private String message = "success";
        private String code;
    }
}
