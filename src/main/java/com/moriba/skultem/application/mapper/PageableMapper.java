package com.moriba.skultem.application.mapper;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class PageableMapper {

    public static Pageable toPageable(int page, int size) {
        if (size == 0) {
            return Pageable.unpaged();
        }

        if (page < 1) {
            page = 1;
        }
        
        return PageRequest.of(page - 1, size);
    }
}