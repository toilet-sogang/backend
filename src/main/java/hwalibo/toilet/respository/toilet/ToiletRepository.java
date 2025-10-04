package hwalibo.toilet.respository.toilet;

import hwalibo.toilet.domain.toilet.Toilet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToiletRepository extends JpaRepository<Toilet, Long> {

}