package com.passly.productoras.internal.negocio;

import com.passly.productoras.NoEsMiembroDeLaProductoraException;
import com.passly.productoras.NombreComercialYaRegistradoException;
import com.passly.productoras.ProductoraNoEncontradaException;
import com.passly.productoras.RolEnProductora;
import com.passly.productoras.RolIncompatibleConLaMembresiaException;
import com.passly.productoras.YaEsMiembroDeLaProductoraException;
import com.passly.productoras.dto.AgregarMiembroRequest;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.productoras.internal.datos.Miembro;
import com.passly.productoras.internal.datos.Productora;
import com.passly.productoras.internal.datos.ProductoraRepository;
import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.UsuarioDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unitario puro de las reglas de membresia (PAS-38): quien puede gestionar eventos, quien puede
 * validar en puerta, y la invariante que cruza los dos ejes de la autorizacion una sola vez, al
 * incorporar a un miembro (CLAUDE.md 4.11). La productora es una entidad real: las reglas viven
 * en el agregado y el servicio solo lo resuelve.
 */
class ProductoraServiceImplTest {

    private static final Long PRODUCTORA = 1L;
    private static final Long DUENIO = 10L;
    private static final Long STAFF = 11L;
    private static final Long VALIDADOR = 12L;
    private static final Long AJENO = 99L;

    private final ProductoraRepository repositorio = mock(ProductoraRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final ProductoraServiceImpl servicio =
            new ProductoraServiceImpl(repositorio, usuarios, new ProductoraMapper());

    private Productora aurora;

    @BeforeEach
    void productoraConUnMiembroPorRol() {
        aurora = new Productora("Aurora", "20-12345678-9", null, null);
        aurora.incorporar(new Miembro(DUENIO, RolEnProductora.DUENIO));
        aurora.incorporar(new Miembro(STAFF, RolEnProductora.STAFF));
        aurora.incorporar(new Miembro(VALIDADOR, RolEnProductora.VALIDADOR));
        when(repositorio.findById(PRODUCTORA)).thenReturn(Optional.of(aurora));
    }

    @ParameterizedTest(name = "usuario {0}: gestiona eventos={1}, valida entradas={2}")
    @CsvSource({
            "10, true,  true",   // DUENIO
            "11, true,  true",   // STAFF
            "12, false, true",   // VALIDADOR: pertenecer no es poder gestionar
            "99, false, false"   // no es miembro
    })
    void cadaRolInternoTieneSusPermisos(Long usuario, boolean gestiona, boolean valida) {
        assertThat(servicio.puedeGestionarEventos(PRODUCTORA, usuario)).isEqualTo(gestiona);
        assertThat(servicio.puedeValidarEntradas(PRODUCTORA, usuario)).isEqualTo(valida);
    }

    @Test
    void unaProductoraInexistenteNoDaPermisosNiFalla() {
        when(repositorio.findById(404L)).thenReturn(Optional.empty());

        assertThat(servicio.puedeGestionarEventos(404L, DUENIO)).isFalse();
        assertThat(servicio.puedeValidarEntradas(404L, DUENIO)).isFalse();
        assertThat(servicio.esMiembro(404L, DUENIO)).isFalse();
    }

    @Test
    void elDuenioIncorporaUnOrganizadorComoStaff() {
        usuarioCon(20L, "nuevo@passly.test", Rol.ORGANIZADOR);

        var miembro = servicio.agregarMiembro(
                PRODUCTORA, new AgregarMiembroRequest("nuevo@passly.test", RolEnProductora.STAFF), DUENIO);

        assertThat(miembro.idUsuario()).isEqualTo(20L);
        assertThat(aurora.puedeGestionarEventos(20L)).isTrue();
    }

    @ParameterizedTest(name = "{0} como {1} -> rechazado")
    @CsvSource({
            "COMPRADOR,   STAFF",
            "COMPRADOR,   VALIDADOR",
            "VALIDADOR,   STAFF",
            "VALIDADOR,   DUENIO"
    })
    void elRolGlobalTieneQueCorresponderAlRolInterno(Rol rolGlobal, RolEnProductora rolInterno) {
        usuarioCon(20L, "nuevo@passly.test", rolGlobal);

        assertThatThrownBy(() -> servicio.agregarMiembro(
                PRODUCTORA, new AgregarMiembroRequest("nuevo@passly.test", rolInterno), DUENIO))
                .isInstanceOf(RolIncompatibleConLaMembresiaException.class);
        assertThat(aurora.tieneMiembro(20L)).isFalse();
    }

    @ParameterizedTest(name = "{0} puede ser VALIDADOR")
    @CsvSource({"VALIDADOR", "ORGANIZADOR", "ADMIN"})
    void validadorAceptaValidadorOrganizadorYAdmin(Rol rolGlobal) {
        usuarioCon(20L, "nuevo@passly.test", rolGlobal);

        servicio.agregarMiembro(
                PRODUCTORA, new AgregarMiembroRequest("nuevo@passly.test", RolEnProductora.VALIDADOR), DUENIO);

        assertThat(aurora.puedeValidarEntradas(20L)).isTrue();
        assertThat(aurora.puedeGestionarEventos(20L)).isFalse();
    }

    @Test
    void soloElDuenioGestionaElPadron() {
        assertThatThrownBy(() -> servicio.agregarMiembro(
                PRODUCTORA, new AgregarMiembroRequest("nuevo@passly.test", RolEnProductora.STAFF), STAFF))
                .isInstanceOf(NoEsMiembroDeLaProductoraException.class);
        verify(usuarios, never()).consultarUsuarioPorEmail(any());
    }

    @Test
    void noSeIncorporaDosVecesAlMismoUsuario() {
        usuarioCon(STAFF, "staff@passly.test", Rol.ORGANIZADOR);

        assertThatThrownBy(() -> servicio.agregarMiembro(
                PRODUCTORA, new AgregarMiembroRequest("staff@passly.test", RolEnProductora.STAFF), DUENIO))
                .isInstanceOf(YaEsMiembroDeLaProductoraException.class);
    }

    @Test
    void agregarAUnaProductoraInexistenteEsNoEncontrada() {
        when(repositorio.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.agregarMiembro(
                404L, new AgregarMiembroRequest("x@passly.test", RolEnProductora.STAFF), DUENIO))
                .isInstanceOf(ProductoraNoEncontradaException.class);
    }

    @Test
    void crearUnaProductoraHabilitaAlCreadorYLoHaceDuenio() {
        when(repositorio.existsByNombreComercial("Nocturna")).thenReturn(false);
        when(usuarios.consultarUsuario(30L)).thenReturn(new UsuarioDTO(30L, "c@passly.test", "C", Rol.COMPRADOR));
        when(repositorio.save(any(Productora.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var creada = servicio.crearProductora(new CrearProductoraRequest("Nocturna", null, null, null), 30L);

        assertThat(creada.nombreComercial()).isEqualTo("Nocturna");
        verify(usuarios).habilitarCapacidad(30L, Rol.ORGANIZADOR);
    }

    @Test
    void elNombreComercialEsUnico() {
        when(repositorio.existsByNombreComercial("Aurora")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crearProductora(new CrearProductoraRequest("Aurora", null, null, null), 30L))
                .isInstanceOf(NombreComercialYaRegistradoException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void listarMiembrosExigeSerMiembro() {
        assertThatThrownBy(() -> servicio.listarMiembros(PRODUCTORA, AJENO))
                .isInstanceOf(NoEsMiembroDeLaProductoraException.class);
    }

    private void usuarioCon(Long id, String email, Rol rol) {
        when(usuarios.consultarUsuarioPorEmail(email)).thenReturn(new UsuarioDTO(id, email, "Persona", rol));
    }
}
