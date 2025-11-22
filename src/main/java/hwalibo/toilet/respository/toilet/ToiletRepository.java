package hwalibo.toilet.respository.toilet;

import hwalibo.toilet.domain.toilet.Toilet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ToiletRepository extends JpaRepository<Toilet, Long> {

    List<Toilet> findByNameContaining(String name);

    @Query(value = """
        SELECT *
        FROM (
            SELECT 
                t.*,
                (
                    6371e3 * acos(
                        cos(radians(:lat)) * cos(radians(t.latitude)) *
                        cos(radians(t.longitude) - radians(:lng)) +
                        sin(radians(:lat)) * sin(radians(t.latitude))
                    )
                ) AS distance,
                ROW_NUMBER() OVER (
                    PARTITION BY t.name 
                    ORDER BY 
                        (6371e3 * acos(
                            cos(radians(:lat)) * cos(radians(t.latitude)) *
                            cos(radians(t.longitude) - radians(:lng)) +
                            sin(radians(:lat)) * sin(radians(t.latitude))
                        ))
                ) AS rn
            FROM toilet t
        ) ranked
        WHERE rn = 1      -- 역 이름별로 가장 가까운 출구만 선택
        ORDER BY distance -- 거리 기준 정렬
        LIMIT 3;
        """,
            nativeQuery = true)
    List<Toilet> findTop3NearestStations(
            @Param("lat") double lat,
            @Param("lng") double lng
    );
}