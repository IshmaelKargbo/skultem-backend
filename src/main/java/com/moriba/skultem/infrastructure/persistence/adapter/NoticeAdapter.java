package com.moriba.skultem.infrastructure.persistence.adapter;

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
}
