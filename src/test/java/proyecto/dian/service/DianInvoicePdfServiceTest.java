package proyecto.dian.service;

import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;
import proyecto.dian.model.*;
import proyecto.entidades.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DianInvoicePdfServiceTest {
    @Test
    void rendersSinglePage80mmInvoiceWithCufeAndQr() throws Exception {
        Empresa company = new Empresa();
        company.setNit(902091864L); company.setDv("2"); company.setNombre("JULIAN ESTEBAN NUÑEZ BEJARANO");
        company.setRazonSocial(company.getNombre()); company.setDireccionFiscal("Calle 1 # 2-3");
        company.setCorreoFacturacion("juesnube@gmail.com");
        Sede branch = new Sede(); branch.setEmpresa(company); branch.setDireccionFiscal("Calle 1 # 2-3");
        Cliente customer = new Cliente(); customer.setNombre("Consumidor final");
        customer.setDocumento("222222222222"); customer.setCorreo("cliente@example.com");
        Producto product = new Producto(); product.setNombre("Café y pan especial");
        Venta sale = new Venta(); sale.setId(1L); sale.setSede(branch); sale.setCliente(customer);
        sale.setFecha(LocalDateTime.of(2026, 9, 4, 20, 15)); sale.setMonedaCodigo("COP");
        sale.setFormaPagoDian("1"); sale.setMedioPagoDian("10"); sale.setSubtotalFiscal(new BigDecimal("100000"));
        sale.setImpuestosFiscal(new BigDecimal("19000")); sale.setDescuentosFiscal(BigDecimal.ZERO);
        sale.setTotalFiscal(new BigDecimal("119000"));
        DetalleVenta detail = new DetalleVenta(); detail.setCantidad(1); detail.setProducto(product);
        detail.setSubtotalFiscal(new BigDecimal("100000")); detail.setValorImpuestoFiscal(new BigDecimal("19000"));
        sale.setDetalles(List.of(detail));
        DianElectronicDocument electronic = new DianElectronicDocument();
        electronic.setVenta(sale); electronic.setFullNumber("SETP1001");
        electronic.setEnvironment(DianEnvironment.HABILITACION); electronic.setStatus(DianDocumentStatus.GENERATED);
        electronic.setCufeOrCude("7d583a2c419c813957b6b43887c42d99d3f8c8285e66c31c29a7c047c2a4fbe5edfbcb686f5883170d8e14c2f4e788a3");

        byte[] pdf = new DianInvoicePdfService(null, null).render(company, sale, electronic);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4));
        PdfReader reader = new PdfReader(pdf);
        assertEquals(1, reader.getNumberOfPages());
        assertEquals(80f * 72f / 25.4f, reader.getPageSize(1).getWidth(), 0.2f);
        Path output = Path.of("build", "dian-validation", "invoice-80mm.pdf");
        Files.createDirectories(output.getParent()); Files.write(output, pdf);
    }
}
