package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.model.StreamSubject;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.infrastructure.persistence.entity.StreamEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StreamSubjectEntity;
import com.moriba.skultem.infrastructure.persistence.entity.SubjectEntity;
import com.moriba.skultem.infrastructure.persistence.entity.SubjectGroupEntity;

public class StreamSubjectMapper {
    public static StreamSubject toDomain(StreamSubjectEntity param) {
        if (param == null)
            return null;

        Stream stream = null;
        SubjectGroup group = null;
        Subject subject = null;

        if (param.getStream() != null) {
            stream = StreamMapper.toDomain(param.getStream());
        }

        if (param.getGroup() != null) {
            group = SubjectGroupMapper.toDomain(param.getGroup());
        }

        if (param.getSubject() != null) {
            subject = SubjectMapper.toDomain(param.getSubject());
        }

        return new StreamSubject(param.getId(), param.getSchoolId(), stream, subject,
                group, param.getMandatory(), param.getLocked(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static StreamSubjectEntity toEntity(StreamSubject param) {
        StreamEntity stream = null;
        SubjectGroupEntity group = null;
        SubjectEntity subject = null;

        if (param.getStream() != null) {
            stream = StreamMapper.toEntity(param.getStream());
        }

        if (param.getGroup() != null) {
            group = SubjectGroupMapper.toEntity(param.getGroup());
        }

        if (param.getSubject() != null) {
            subject = SubjectMapper.toEntity(param.getSubject());
        }

        return StreamSubjectEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .stream(stream)
                .subject(subject)
                .locked(param.getLocked())
                .group(group)
                .mandatory(param.getMandatory())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
