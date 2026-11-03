package com.taekwondo.examenes.result;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findAllByExamIdOrderByCompletedAtDesc(Long examId);

    List<Result> findAllByStudentUserIdOrderByCompletedAtDesc(Long studentUserId);

    /** De esos usuarios, los que tienen alguna entrega en esos exámenes. */
    @Query("select distinct r.studentUserId from Result r where r.studentUserId in :studentIds and r.examId in :examIds")
    Set<Long> findStudentsWithResultsIn(@Param("studentIds") Collection<Long> studentIds,
                                        @Param("examIds") Collection<Long> examIds);

}
