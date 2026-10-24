package se.bth.netinventory.site.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import se.bth.netinventory.site.model.Site;

@DataJpaTest
class SiteRepositoryTest {
    @Autowired SiteRepository repository;

    @Test
    void seedDataIsLoadedInCodeOrder() {
        assertThat(repository.findAllByOrderByCodeAsc()).extracting(Site::getCode)
            .containsExactly("BGS-001", "PDV-001", "SOF-001", "VAR-001");
        assertThat(repository.findById(1L)).get().extracting(Site::getCode).isEqualTo("SOF-001");
    }

    @Test
    void existsByCodeAndIdNotIgnoresTheSiteItself() {
        assertThat(repository.existsByCodeAndIdNot("SOF-001", 1L)).isFalse();
        assertThat(repository.existsByCodeAndIdNot("SOF-001", 2L)).isTrue();
    }
}
