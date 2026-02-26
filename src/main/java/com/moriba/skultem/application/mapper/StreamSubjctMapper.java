package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.StreamDTO;
import com.moriba.skultem.application.dto.StreamSubjectDTO;
import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.application.dto.SubjectGroupDTO;
import com.moriba.skultem.domain.model.StreamSubject;

public class StreamSubjctMapper {
    public static StreamSubjectDTO toDTO(StreamSubject param) {
        StreamDTO stream = StreamMapper.toDTO(param.getStream());
        SubjectDTO subject = SubjectMapper.toDTO(param.getSubject());
        SubjectGroupDTO group = SubjectGroupMapper.toDTO(param.getGroup());

        String streamName = stream.name(), streamId = stream.id();
        String subjectName = subject.name(), subjectId = subject.id();
        String groupName = "N/A", groupId = "";

        if (group != null) {
            groupName = group.name();
            groupId = group.id();
        }

        return new StreamSubjectDTO(param.getId(), param.getSchoolId(), streamName, streamId, subjectName, subjectId,
                groupName, groupId, param.getMandatory());
    }
}
