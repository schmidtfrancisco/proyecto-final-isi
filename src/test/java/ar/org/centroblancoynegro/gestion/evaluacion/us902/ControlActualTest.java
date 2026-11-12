package ar.org.centroblancoynegro.gestion.evaluacion.us902;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

// CASO DE CONTROL: uno pasa y otro falla a proposito. Cuenta como historia actual.
@SpringBootTest
class ControlActualTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void pasa() {
        assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
    }

    @Test
    void fallaAProposito() {
        assertThat(1 + 1).as("suma de control").isEqualTo(3);
    }
}
