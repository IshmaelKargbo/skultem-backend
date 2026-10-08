package com.moriba.skultem.domain.vo;

// One row of a school's report card remark scale: an average in [minScore, maxScore] gets this remark.
public record RemarkBand(int minScore, int maxScore, String remark) {
}
