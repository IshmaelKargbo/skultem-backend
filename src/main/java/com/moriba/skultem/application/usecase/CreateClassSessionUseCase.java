package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.ClassSection;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSectionRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.StreamRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateClassSessionUseCase {

    private final ClassSessionRepository repo;
    private final ClassRepository classRepo;
    private final StreamRepository streamRepo;
    private final ClassSectionRepository classSectionRepo;
    private final AcademicYearRepository academicYearRepo;
    private final ReferenceGeneratorUsecase rg;

    public void execute(String schoolId, String classId, String academicYearId, String streamId,
            String sectionId) {
        var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        ClassSection cs = classSectionRepo.findByIdAndClassIdAndSchoolId(sectionId, classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Section not found"));

        AcademicYear academicYear = academicYearRepo.findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));

        if (!academicYear.isActive()) {
            throw new IllegalStateException("Cannot create class session in a closed academic year");
        }

        if (clazz.getLevel() == Level.SSS && streamId == null) {
            throw new IllegalArgumentException("Stream is required for SSS class");
        }

        if (clazz.getLevel() != Level.SSS && streamId != null) {
            throw new IllegalArgumentException("Only SSS classes can have stream");
        }

        boolean exists;
        Stream stream = null;

        if (streamId == null) {
            stream = streamRepo.findByIdAndSchoolId(streamId, schoolId)
                    .orElseThrow(() -> new NotFoundException("stream not found"));
            exists = repo.existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIsNullAndSchoolId(classId,
                    academicYearId, sectionId, schoolId);
        } else {
            exists = repo.existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(
                    classId, academicYearId, sectionId, streamId, schoolId);
        }

        if (exists) {
            throw new AlreadyExistsException("Class session already exists");
        }

        String id = rg.generate("CLASS_SESSION", "CLS");
        ClassSession record = ClassSession.create(id, schoolId, clazz, stream, cs.getSection(), academicYear);
        repo.save(record);
    }
}
