import fs from "node:fs/promises";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const outputDir = "C:/Users/Julian/Documents/sispos/outputs";
const outputPath = `${outputDir}/productos-importacion-delicias.xlsx`;

const rows = [
  ["Nombre", "Descripcion", "Precio Produccion", "Precio Venta", "Estado"],
  ["Pastel de arequipe", "Hojaldre crujiente con arequipe cremoso en su interior.", 1500, 3500, "Activo"],
  ["Milhojas", "Capas de hojaldre rellenas con crema inglesa y una capa superior de arequipe.", 2200, 6000, "Activo"],
  ["Paquete de pasabocas", "Mini hojaldres rellenos de dulce de guayaba para compartir.", 1500, 3500, "Activo"],
  ["Avena Fria", "Avena Fria", 2000, 5000, "Activo"],
  ["Cafe Americano", "Cafe americano", 1500, 4000, "Activo"],
  ["Capuchino", "capuchino", 2000, 6000, "Activo"],
  ["Milo Frio", "milo frio", 2500, 7000, "Activo"],
  ["Milo Caliente", "milo caliente", 2500, 6000, "Activo"],
  ["Vaso de Leche Fria", "leche fria", 1000, 4000, "Activo"],
  ["Coca-Cola 400ml", "coca cola", 2350, 5500, "Activo"],
  ["Agua en botella 600ml", "Agua en botella", 2000, 3500, "Activo"],
  ["Jugo en Agua de Guanabana", "jugo de guanabana", 2500, 6000, "Activo"],
  ["Jugo de Mora en agua", "jugo de mora", 2500, 6000, "Activo"],
  ["Jugo en Agua de Mango", "jugo de mango", 2500, 6000, "Activo"],
  ["Jugo en Agua de Maracuya", "jugo de maracuya", 2500, 6000, "Activo"],
  ["Cafe en leche", "Cafe en leche", 2000, 5000, "Activo"],
  ["Mora en leche", "Mora en leche", 3000, 7000, "Activo"],
  ["Maracuya en leche", "Maracuya en leche", 3000, 7000, "Activo"],
  ["Mango en leche", "Mango en leche", 3000, 7000, "Activo"],
  ["Guanabana en leche", "Guanabana en leche", 3000, 7000, "Activo"],
  ["Aromatica en agua", "Aromatica en agua", 1000, 3000, "Activo"],
  ["Aromatica en leche", "Aromatica en leche", 1500, 3500, "Activo"],
  ["Chocolate en Agua", "Chocolate en Agua", 1000, 5000, "Activo"],
  ["Chocolate en Leche", "Chocolate en Leche", 1500, 6000, "Activo"],
];

const workbook = Workbook.create();
const sheet = workbook.worksheets.add("Importar productos");

sheet.getRange(`A1:E${rows.length}`).values = rows;

await fs.mkdir(outputDir, { recursive: true });
const file = await SpreadsheetFile.exportXlsx(workbook);
await file.save(outputPath);

console.log(outputPath);
