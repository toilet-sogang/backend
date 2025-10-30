package hwalibo.toilet.respository.toilet;

import hwalibo.toilet.domain.toilet.Toilet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ToiletRepository extends JpaRepository<Toilet, Long> {
    List<Toilet> findByNameContaining(String name);
}