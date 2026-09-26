package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Notice;
import com.moriba.skultem.domain.repository.NoticeRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.NoticeJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.NoticeMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NoticeAdapter implements NoticeRepository {
    private final NoticeJpaRepository repo;

    @Override
    public void save(Notice domain) {
        var entity = NoticeMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public void delete(Notice domain) {
        repo.deleteById(domain.getId());
    }

    @Override
    public Optional<Notice> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(NoticeMapper::toDomain);
    }

    @Override
    public Page<Notice> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByPinnedDescCreatedAtDesc(schoolId, pageable).map(NoticeMapper::toDomain);
    }

    @Override
    public Page<Notice> findVisibleBySchoolId(String schoolId, Collection<String> sectionIds, Pageable pageable) {
        if (sectionIds == null) {
            return findAllBySchoolId(schoolId, pageable);
        }
        // "in ()" isn't valid - with no sections, only the whole-school rows match the null branch.
        Collection<String> ids = sectionIds.isEmpty() ? List.of("-") : sectionIds;
        return repo.findVisible(schoolId, ids, pageable).map(NoticeMapper::toDomain);
    }
}
