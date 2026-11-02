# Negocio de Comida – aplicación de escritorio (Java Swing)

## Ejecutar
- Con Maven: `mvn package` y luego `java -jar target/negocio-comida.jar`
- Sin Maven: `./ejecutar.sh` (Linux/Mac) o doble clic en `ejecutar.bat` (Windows). Requiere JDK 17 o superior.

Los datos se guardan automáticamente en `~/.negocio-comida/datos.dat`
(se puede cambiar con `-Dnegocio.datos=/ruta/archivo.dat`).

## Pestañas
- **Principal (día)**: fecha de trabajo, menú del día y **planificación de la semana**
  (lunes a domingo, con opción de copiar la semana anterior), despacho de platos a clientes,
  gastos del día (otros, combustible, gas de cocina y costo de ingredientes), platos
  despachados por cliente y resumen (platos, ventas, gastos, utilidad).
- **Inventario**: ingredientes medidos por Unidad, Libra o Kilo, con existencia y precio.
- **Menús**: platos armados con ingredientes del inventario; calcula costo, ganancia y
  cuántos platos alcanzan con la existencia actual.
- **Clientes**: registro de clientes y reporte de platos despachados por día.

Al registrar un despacho se descuentan los ingredientes del inventario y su costo se anota
automáticamente como gasto del día; al anularlo se devuelven los ingredientes y se quita el gasto.
