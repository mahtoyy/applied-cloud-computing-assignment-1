package se.bth.netinventory.equipment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class EquipmentRepositoryTest {
    @Autowired EquipmentRepository repository;

    @Test
    void seedCountsPerSite() {
        assertThat(repository.countBySiteId(1L)).isEqualTo(4);
        assertThat(repository.countBySiteId(2L)).isEqualTo(3);
        assertThat(repository.countBySiteId(3L)).isEqualTo(2);
        assertThat(repository.countBySiteId(4L)).isEqualTo(2);
        assertThat(repository.countBySiteId(99L)).isZero();
    }

    @Test
    void oneSeededItemIsDown() {
        assertThat(repository.findAllByOrderByHostnameAsc())
            .filteredOn(e -> e.getStatus() == EquipmentStatus.DOWN)
            .extracting(Equipment::getHostname)
            .containsExactly("var-rad-01");
    }

    @Test
    void duplicateChecksExcludeSelf() {
        Equipment down = repository.findAllByOrderByHostnameAsc().stream()
            .filter(e -> e.getHostname().equals("var-rad-01")).findFirst().orElseThrow();
        assertThat(repository.existsByHostnameAndIdNot("var-rad-01", down.getId())).isFalse();
        assertThat(repository.existsByHostname("var-rad-01")).isTrue();
        assertThat(repository.existsBySerialNumberAndIdNot(down.getSerialNumber(), down.getId())).isFalse();
    }
}
