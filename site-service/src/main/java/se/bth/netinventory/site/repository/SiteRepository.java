package se.bth.netinventory.site.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.bth.netinventory.site.model.Site;

public interface SiteRepository extends JpaRepository<Site, Long> {

    List<Site> findAllByOrderByCodeAsc();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
