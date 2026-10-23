package se.bth.netinventory.equipment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findAllByOrderByHostnameAsc();

    List<Equipment> findBySiteIdOrderByHostnameAsc(Long siteId);

    long countBySiteId(Long siteId);

    boolean existsByHostname(String hostname);

    boolean existsByHostnameAndIdNot(String hostname, Long id);

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndIdNot(String serialNumber, Long id);
}
