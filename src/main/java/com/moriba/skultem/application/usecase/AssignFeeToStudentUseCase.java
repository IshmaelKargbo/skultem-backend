package com.moriba.skultem.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentFeeDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.StudentFeeMapper;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.utils.Generate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignFeeToStudentUseCase {
    private final AcademicYearRepository academicYearRepo;
    private final FeeStructureRepository feeStructureRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final StudentFeeRepository studentFeeRepo;
    private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    public StudentFeeDTO execute(AssignFeeToStudentRecord param) {
        var academicYear = academicYearRepo.findActiveBySchool(param.schoolId())
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

        var fee = feeStructureRepo.findByIdAndSchoolId(param.feeId(), param.schoolId())
                .orElseThrow(() -> new NotFoundException("Fee structure not found"));

        var enrollment = enrollmentRepo.findByStudentAndAcademicYearAndSchoolId(
                param.studentId(), academicYear.getId(), param.schoolId())
                .orElseThrow(() -> new NotFoundException("Student enrollment not found"));

        if (studentFeeRepo.existsBySchoolAndEnrollmentAndStudentAndFee(
                param.schoolId(), enrollment.getId(), param.studentId(), fee.getId())) {
            throw new AlreadyExistsException("Fee already assigned to this student");
        }

        var studentFee = StudentFee.create(
                rg.generate("STUDENT_FEE", "STF"),
                param.schoolId(),
                enrollment,
                enrollment.getStudent(),
                fee,
                null);

        studentFeeRepo.save(studentFee);

        var description = Generate.generateLedgerDescription(
                TransactionType.FEE_ASSINMENT,
                fee.getTerm().getName(),
                fee.getCategory().getName(),
                enrollment.getStudent().getGivenNames(),
                enrollment.getStudent().getFamilyName(),
                enrollment.getStudent().getAdmissionNumber(),
                fee.getAmount());

        createStudentLedgerUsercase.createEntry(
                param.schoolId(),
                academicYear.getId(),
                enrollment.getStudent().getId(),
                fee.getTerm().getId(),
                TransactionType.FEE_ASSINMENT,
                Direction.DEBIT,
                fee.getAmount(),
                fee.getId(),
                description,
                Instant.now());

        String meta = "studentId=" + enrollment.getStudent().getId()
                + ";feeId=" + fee.getId()
                + ";amount=" + fee.getAmount();
        logActivityUseCase.log(
                param.schoolId(),
                ActivityType.FEES,
                "Fee assigned to student",
                fee.getCategory().getName() + " - " + fee.getTerm().getName(),
                meta,
                fee.getId());

        return StudentFeeMapper.toDTO(studentFee);
    }

    public record AssignFeeToStudentRecord(String schoolId, String studentId, String feeId) {
    }
}
