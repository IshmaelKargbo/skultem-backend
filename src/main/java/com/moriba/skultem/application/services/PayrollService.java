package com.moriba.skultem.application.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PayrollRunDTO;
import com.moriba.skultem.application.dto.PayrollRunDetailDTO;
import com.moriba.skultem.application.dto.PayrollSummaryDTO;
import com.moriba.skultem.application.dto.PayslipDTO;
import com.moriba.skultem.application.dto.SalaryStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PayrollRunMapper;
import com.moriba.skultem.application.mapper.PayslipMapper;
import com.moriba.skultem.application.mapper.SalaryStructureMapper;
import com.moriba.skultem.application.usecase.CreatePayrollRunUseCase;
import com.moriba.skultem.application.usecase.GeneratePayrollRunUseCase;
import com.moriba.skultem.application.usecase.PublishPayrollRunUseCase;
import com.moriba.skultem.application.usecase.SetSalaryStructureUseCase;
import com.moriba.skultem.application.usecase.TogglePayslipIncludedUseCase;
import com.moriba.skultem.domain.model.PayrollRun;
import com.moriba.skultem.domain.model.SalaryStructure;
import com.moriba.skultem.domain.repository.PayrollRunRepository;
import com.moriba.skultem.domain.repository.PayslipRepository;
import com.moriba.skultem.domain.repository.SalaryStructureRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final SalaryStructureRepository salaryRepo;
    private final PayrollRunRepository runRepo;
    private final PayslipRepository payslipRepo;
    private final TeacherRepository teacherRepo;

    private final SetSalaryStructureUseCase setSalaryStructureUseCase;
    private final CreatePayrollRunUseCase createPayrollRunUseCase;
    private final TogglePayslipIncludedUseCase togglePayslipIncludedUseCase;
    private final GeneratePayrollRunUseCase generatePayrollRunUseCase;
    private final PublishPayrollRunUseCase publishPayrollRunUseCase;

    public SalaryStructureDTO setSalary(String schoolId, String teacherId, BigDecimal basicSalary,
            BigDecimal allowances, BigDecimal deductions) {
        return setSalaryStructureUseCase.execute(schoolId, teacherId, basicSalary, allowances, deductions);
    }

    public Page<SalaryStructureDTO> listSalaries(String schoolId, int page, int size, String search) {
        Pageable pageable = size > 0 ? PageRequest.of(page - 1, size) : Pageable.unpaged();
        return salaryRepo.search(search, schoolId, pageable).map(SalaryStructureMapper::toDTO);
    }

    public SalaryStructureDTO getSalaryByTeacher(String schoolId, String teacherId) {
        var structure = salaryRepo.findByTeacherIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new NotFoundException("No salary structure set up for this teacher yet"));

        return SalaryStructureMapper.toDTO(structure);
    }

    public List<PayslipDTO> getSalaryHistory(String schoolId, String teacherId) {
        return payslipHistory(schoolId, teacherId, false);
    }

    // Self-service: a teacher's own payslip history, resolved from the signed-in user rather
    // than an admin-supplied teacherId (same pattern as the attendance/curriculum "me" endpoints
    // elsewhere) - published runs only, since a draft/generated run's figures aren't final and
    // haven't actually been released to staff yet (see PayrollRun#publish).
    public List<PayslipDTO> getMySalaryHistory(String schoolId, String userId) {
        var teacher = teacherRepo.findByUserIdAndSchoolId(userId, schoolId)
                .orElseThrow(() -> new NotFoundException(
                        "You haven't been added to staff/payroll records yet - ask your admin to include you from your profile"));

        return payslipHistory(schoolId, teacher.getId(), true);
    }

    private List<PayslipDTO> payslipHistory(String schoolId, String teacherId, boolean publishedOnly) {
        var payslips = payslipRepo.findAllByTeacherIdAndSchoolIdOrderByCreatedAtDesc(teacherId, schoolId);

        // Small, school-scoped set in practice (one row per payroll run this teacher was on) -
        // a lookup per distinct run to attach its period is simpler than a joined query here.
        var runsById = payslips.stream()
                .map(com.moriba.skultem.domain.model.Payslip::getPayrollRunId)
                .distinct()
                .map(id -> runRepo.findByIdAndSchoolId(id, schoolId))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(java.util.stream.Collectors.toMap(PayrollRun::getId, r -> r));

        return payslips.stream()
                .filter(p -> {
                    if (!publishedOnly) return true;
                    var run = runsById.get(p.getPayrollRunId());
                    return run != null && run.getStatus() == PayrollRun.Status.PUBLISHED;
                })
                .map(p -> PayslipMapper.toDTO(p, runsById.get(p.getPayrollRunId())))
                .toList();
    }

    public PayrollSummaryDTO summary(String schoolId) {
        var structures = salaryRepo.findAllBySchoolId(schoolId);

        BigDecimal totalGross = structures.stream()
                .map(SalaryStructure::grossSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = structures.isEmpty()
                ? BigDecimal.ZERO
                : totalGross.divide(BigDecimal.valueOf(structures.size()), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal highest = structures.stream().map(SalaryStructure::netSalary).max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal lowest = structures.stream().map(SalaryStructure::netSalary).min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        var latestRun = runRepo.findLatestBySchoolId(schoolId).map(PayrollRunMapper::toDTO).orElse(null);

        return new PayrollSummaryDTO(structures.size(), totalGross, average, highest, lowest, latestRun);
    }

    public PayrollRunDTO createRun(String schoolId, String period, LocalDate payDate) {
        return createPayrollRunUseCase.execute(schoolId, period, payDate);
    }

    public Page<PayrollRunDTO> listRuns(String schoolId, int page, int size) {
        Pageable pageable = size > 0 ? PageRequest.of(page - 1, size) : Pageable.unpaged();
        return runRepo.findAllBySchoolId(schoolId, pageable).map(PayrollRunMapper::toDTO);
    }

    public PayrollRunDetailDTO getRunDetail(String schoolId, String runId) {
        var run = runRepo.findByIdAndSchoolId(runId, schoolId)
                .orElseThrow(() -> new NotFoundException("Payroll run not found"));

        var payslips = payslipRepo.findAllByPayrollRunId(runId);

        var included = payslips.stream().filter(com.moriba.skultem.domain.model.Payslip::isIncluded).toList();

        BigDecimal gross = included.stream().map(com.moriba.skultem.domain.model.Payslip::grossSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deductions = included.stream().map(com.moriba.skultem.domain.model.Payslip::getDeductions)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = included.stream().map(com.moriba.skultem.domain.model.Payslip::netSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var payslipDTOs = payslips.stream().map(p -> PayslipMapper.toDTO(p, run)).toList();

        return new PayrollRunDetailDTO(PayrollRunMapper.toDTO(run), payslipDTOs, gross, deductions, net,
                included.size(), payslips.size());
    }

    public PayslipDTO getPayslip(String schoolId, String runId, String teacherId) {
        var run = runRepo.findByIdAndSchoolId(runId, schoolId)
                .orElseThrow(() -> new NotFoundException("Payroll run not found"));

        var payslip = payslipRepo.findByPayrollRunIdAndTeacherId(runId, teacherId)
                .orElseThrow(() -> new NotFoundException("Payslip not found"));

        return PayslipMapper.toDTO(payslip, run);
    }

    // Self-service version of getPayslip - teacherId resolved from the signed-in user, and only
    // reachable once the run is published (see getMySalaryHistory).
    public PayslipDTO getMyPayslip(String schoolId, String userId, String runId) {
        var teacher = teacherRepo.findByUserIdAndSchoolId(userId, schoolId)
                .orElseThrow(() -> new NotFoundException(
                        "You haven't been added to staff/payroll records yet - ask your admin to include you from your profile"));

        var run = runRepo.findByIdAndSchoolId(runId, schoolId)
                .orElseThrow(() -> new NotFoundException("Payroll run not found"));

        if (run.getStatus() != PayrollRun.Status.PUBLISHED) {
            throw new NotFoundException("Payslip not found");
        }

        var payslip = payslipRepo.findByPayrollRunIdAndTeacherId(runId, teacher.getId())
                .orElseThrow(() -> new NotFoundException("Payslip not found"));

        return PayslipMapper.toDTO(payslip, run);
    }

    public PayslipDTO setIncluded(String schoolId, String runId, String payslipId, boolean included) {
        return togglePayslipIncludedUseCase.execute(schoolId, runId, payslipId, included);
    }

    public PayrollRunDTO generateRun(String schoolId, String runId) {
        return generatePayrollRunUseCase.execute(schoolId, runId);
    }

    public PayrollRunDTO publishRun(String schoolId, String runId) {
        return publishPayrollRunUseCase.execute(schoolId, runId);
    }
}
