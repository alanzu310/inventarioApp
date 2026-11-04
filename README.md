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
- **Inventario**: ingredientes medidos por Unidad, Libra o Kilo. La existencia y el costo salen
  de las **compras**: cada compra guarda su proveedor, fecha, cantidad y su propio precio (se puede
  escribir el precio por unidad o el total pagado). Comprar el mismo producto más barato a otro
  proveedor no cambia el valor de lo que ya se tenía. Al despachar se usa primero la compra más
  antigua (PEPS). Incluye comparación de precios por proveedor y ajuste por merma o conteo.
- **Proveedores**: registro de proveedores y su historial de compras.
- **Menús**: platos armados con ingredientes del inventario; calcula costo, ganancia y
  cuántos platos alcanzan con la existencia actual.
- **Clientes**: registro de clientes y reporte de platos despachados por día. El botón
  **Reporte de ventas (PDF / Excel)** genera el reporte para el contador: se elige el cliente
  (o todos), el periodo (con atajos: esta semana, quincena, este mes, mes anterior) y el formato.
  El PDF incluye detalle por fecha, subtotales, total a pagar y espacio para firmas; el Excel
  trae una hoja de detalle (con filtros y fórmulas de suma) y una hoja de resumen por cliente.
  No se necesitan librerías externas.

Al registrar un despacho se descuentan los ingredientes de las compras más antiguas y su costo real
se anota automáticamente como gasto del día; al anularlo los ingredientes vuelven a las mismas
compras de donde salieron y se quita el gasto. Los datos de versiones anteriores se convierten solos:
la existencia y el precio de cada ingrediente pasan a ser una compra "Sin proveedor".
