package com.moriba.skultem.domain.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Behaviour;
import com.moriba.skultem.domain.model.vo.KindCount;

public interface BehaviourRepository {
    void save(Behaviour domain);

    List<KindCount> countByKindForClassOrAll(String academicYearId, String schoolId, String classId);

    Page<Behaviour> findAllAcademicYearAndSchoolId(String academicYearId, String schoolId, Pageable pageable);

    Page<Behaviour> findAllAcademicYearAndClassIdAndSchoolId(String academicYearId, String classId, String schoolId,
            Pageable pageable);
}
