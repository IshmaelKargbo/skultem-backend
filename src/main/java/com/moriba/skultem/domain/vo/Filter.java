package com.moriba.skultem.domain.vo;

import java.util.List;

public record Filter(String field, FilterOperator operator, String type, String value, String valueTo, List<String> values) {

}
