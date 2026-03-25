package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.NoteInterne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteInterneRepository extends JpaRepository<NoteInterne, Long> {

    List<NoteInterne> findByTenantIdOrderByDateCreationDesc(Long tenantId);
}
