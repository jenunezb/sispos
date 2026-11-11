$ErrorActionPreference = "Stop"

$pgPassword = 'OconjSTwJCCmgKNJKWpxktPlZjbaQtlE'
$connection = 'postgresql://postgres@switchyard.proxy.rlwy.net:49477/railway'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$outputDir = 'C:\temp\migracion-delicias'

New-Item -ItemType Directory -Force $outputDir | Out-Null

$env:PGPASSWORD = $pgPassword

& $psql $connection -c "\copy (SELECT * FROM sede WHERE ubicacion IN ('Av. Bolivar Cra 14# 16n- 61','Cll. 22 # 15-25') ORDER BY id) TO '$outputDir\sede.csv' CSV HEADER"

& $psql $connection -c "\copy (SELECT * FROM cliente WHERE id IN (SELECT DISTINCT cliente_id FROM venta WHERE sede_id IN (SELECT id FROM sede WHERE ubicacion IN ('Av. Bolivar Cra 14# 16n- 61','Cll. 22 # 15-25')) AND cliente_id IS NOT NULL) ORDER BY id) TO '$outputDir\cliente.csv' CSV HEADER"

& $psql $connection -c "\copy (SELECT * FROM venta WHERE sede_id IN (SELECT id FROM sede WHERE ubicacion IN ('Av. Bolivar Cra 14# 16n- 61','Cll. 22 # 15-25')) ORDER BY id) TO '$outputDir\venta.csv' CSV HEADER"

& $psql $connection -c "\copy (SELECT * FROM detalle_venta WHERE venta_id IN (SELECT id FROM venta WHERE sede_id IN (SELECT id FROM sede WHERE ubicacion IN ('Av. Bolivar Cra 14# 16n- 61','Cll. 22 # 15-25'))) ORDER BY id) TO '$outputDir\detalle_venta.csv' CSV HEADER"

Write-Host "Archivos exportados en $outputDir"
