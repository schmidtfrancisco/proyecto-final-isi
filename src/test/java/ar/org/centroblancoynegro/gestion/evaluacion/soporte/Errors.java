package ar.org.centroblancoynegro.gestion.evaluacion.soporte;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;

/**
 * Verifica errores en el formato de la cabecera de los contratos (Problem Details + code).
 */
public final class Errors {

    private Errors() {
    }

    /** Verifica status HTTP, Content-Type application/problem+json, status del cuerpo y code. */
    public static void verify(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(((Number) JsonPath.read(response.body(), "$.status")).intValue()).isEqualTo(status);
        assertThat((String) JsonPath.read(response.body(), "$.code")).isEqualTo(code);
    }
}
