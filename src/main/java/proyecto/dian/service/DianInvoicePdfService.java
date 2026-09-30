package proyecto.dian.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dian.model.*;
import proyecto.dian.repository.DianElectronicDocumentRepository;
import proyecto.entidades.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DianInvoicePdfService {
    private static final float MM = 72f / 25.4f;
    private static final Font TITLE = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font BOLD = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 7);
    private static final Font SMALL = new Font(Font.HELVETICA, 5.5f);
    private final DianTenantContextService tenantContext;
    private final DianElectronicDocumentRepository documents;

    @Transactional(readOnly = true)
    public PdfResult generate(String authorization, Long saleId, DianEnvironment environment) {
        Empresa company = tenantContext.requireCompanyAdministrator(authorization);
        DianElectronicDocument electronic = documents.findByEmpresaNitAndEnvironmentAndVentaIdAndDocumentType(
                        company.getNit(), environment, saleId, DianDocumentType.INVOICE)
                .orElseThrow(() -> new IllegalArgumentException("Primero debe preparar la factura electrónica"));
        Venta sale = electronic.getVenta();
        try {
            byte[] pdf = render(company, sale, electronic);
            return new PdfResult("factura-" + electronic.getFullNumber() + ".pdf", pdf);
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible generar la representación gráfica", exception);
        }
    }

    byte[] render(Empresa company, Venta sale, DianElectronicDocument electronic) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int itemCount = sale.getDetalles() == null ? 0 : sale.getDetalles().size();
        float dynamicHeightMm = Math.max(188, 180 + itemCount * 8);
        Rectangle page = new Rectangle(80 * MM, dynamicHeightMm * MM);
        Document pdf = new Document(page, 10, 10, 10, 10);
        PdfWriter.getInstance(pdf, output);
        pdf.open();
        addCentered(pdf, "STEELSOFT", TITLE);
        addCentered(pdf, "FACTURA ELECTRÓNICA DE VENTA", BOLD);
        addCentered(pdf, first(company.getRazonSocial(), company.getNombre()), BOLD);
        addCentered(pdf, "NIT " + company.getNit() + suffix(company.getDv()), NORMAL);
        addCentered(pdf, first(company.getDireccionFiscal(), sale.getSede().getDireccionFiscal()), NORMAL);
        addCentered(pdf, company.getCorreoFacturacion(), NORMAL);
        line(pdf);

        table(pdf, new String[][]{
                {"Factura:", electronic.getFullNumber()},
                {"Fecha:", sale.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))},
                {"Forma de pago:", paymentForm(sale.getFormaPagoDian())},
                {"Medio de pago:", paymentMeans(sale.getMedioPagoDian())},
                {"Moneda:", sale.getMonedaCodigo()}
        }, true);
        line(pdf);
        pdf.add(new Paragraph("CLIENTE", BOLD));
        pdf.add(new Paragraph(sale.getCliente().getNombre(), BOLD));
        pdf.add(new Paragraph("Documento: " + sale.getCliente().getDocumento(), NORMAL));
        pdf.add(new Paragraph(sale.getCliente().getCorreo(), NORMAL));
        line(pdf);

        PdfPTable items = new PdfPTable(new float[]{3.2f, 1f});
        items.setWidthPercentage(100);
        addCell(items, "PRODUCTO", BOLD, Element.ALIGN_LEFT);
        addCell(items, "TOTAL", BOLD, Element.ALIGN_RIGHT);
        for (DetalleVenta detail : sale.getDetalles()) {
            String name = detail.getProducto() == null ? detail.getNombreLibre() : detail.getProducto().getNombre();
            BigDecimal total = detail.getSubtotalFiscal() != null ? detail.getSubtotalFiscal()
                    .add(zero(detail.getValorImpuestoFiscal())) : BigDecimal.valueOf(detail.getSubtotal());
            addCell(items, detail.getCantidad() + " x " + name, NORMAL, Element.ALIGN_LEFT);
            addCell(items, money(total), NORMAL, Element.ALIGN_RIGHT);
        }
        pdf.add(items);
        line(pdf);
        table(pdf, new String[][]{
                {"SUBTOTAL", money(sale.getSubtotalFiscal())},
                {"IMPUESTOS", money(sale.getImpuestosFiscal())},
                {"DESCUENTOS", money(sale.getDescuentosFiscal())},
                {"TOTAL", money(sale.getTotalFiscal() != null ? sale.getTotalFiscal() : BigDecimal.valueOf(sale.getTotal()))}
        }, false);
        line(pdf);

        addCentered(pdf, "CUFE", BOLD);
        Paragraph cufe = new Paragraph(electronic.getCufeOrCude(), SMALL);
        cufe.setAlignment(Element.ALIGN_CENTER);
        pdf.add(cufe);
        if (electronic.getCufeOrCude() != null) {
            Image qr = qrImage(qrUrl(electronic));
            qr.scaleAbsolute(74, 74);
            qr.setAlignment(Image.ALIGN_CENTER);
            pdf.add(qr);
        }
        addCentered(pdf, statusText(electronic), BOLD);
        addCentered(pdf, "Representación gráfica de factura electrónica", SMALL);
        addCentered(pdf, "Gracias por su compra", BOLD);
        if (electronic.getStatus() != DianDocumentStatus.ACCEPTED) {
            addCentered(pdf, "MUESTRA - AÚN NO VALIDADA POR LA DIAN", SMALL);
        }
        pdf.close();
        return output.toByteArray();
    }

    private Image qrImage(String value) throws Exception {
        var matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 220, 220);
        BufferedImage image = new BufferedImage(220, 220, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 220; y++) for (int x = 0; x < 220; x++)
            image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
        return Image.getInstance(image, null);
    }

    private void table(Document pdf, String[][] rows, boolean normal) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.35f, 2.1f});
        table.setWidthPercentage(100);
        for (String[] row : rows) {
            addCell(table, row[0], normal ? NORMAL : ("TOTAL".equals(row[0]) ? BOLD : NORMAL), Element.ALIGN_LEFT);
            addCell(table, value(row[1]), "TOTAL".equals(row[0]) ? BOLD : NORMAL, Element.ALIGN_RIGHT);
        }
        pdf.add(table);
    }

    private void addCell(PdfPTable table, String value, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value(value), font));
        cell.setBorder(Rectangle.NO_BORDER); cell.setPadding(1.5f); cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addCentered(Document pdf, String text, Font font) throws DocumentException {
        if (text == null || text.isBlank()) return;
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER); paragraph.setSpacingAfter(2);
        pdf.add(paragraph);
    }

    private void line(Document pdf) throws DocumentException {
        Paragraph line = new Paragraph("________________________________________", SMALL);
        line.setAlignment(Element.ALIGN_CENTER); line.setSpacingBefore(2); line.setSpacingAfter(3); pdf.add(line);
    }

    private String qrUrl(DianElectronicDocument document) {
        String root = document.getEnvironment() == DianEnvironment.PRODUCCION
                ? "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey="
                : "https://catalogo-vpfe-hab.dian.gov.co/document/searchqr?documentkey=";
        return root + document.getCufeOrCude();
    }

    private String statusText(DianElectronicDocument document) {
        return "Estado DIAN: " + document.getStatus().name();
    }

    private String paymentForm(String code) { return "2".equals(code) ? "Crédito" : "Contado"; }
    private String paymentMeans(String code) { return "10".equals(code) ? "Efectivo" : "Código DIAN " + value(code); }
    private String suffix(String dv) { return dv == null || dv.isBlank() ? "" : "-" + dv; }
    private String first(String preferred, String fallback) { return preferred == null || preferred.isBlank() ? fallback : preferred; }
    private String value(String value) { return value == null || value.isBlank() ? "-" : value; }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        format.setMaximumFractionDigits(2); return format.format(zero(value));
    }

    public record PdfResult(String fileName, byte[] content) {}
}
