import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//EntregaSemana5_GrupoB01_Subgrupo12
//@author Eric Andrey Gonzalez Gutierrez
//@author Julian Vacca Sanchez

/**
 * Procesa los archivos de ventas, vendedores y productos y genera dos reportes CSV:
 *
 *   {@code reporte_vendedores.csv}: {@code Nombre Apellidos;TotalRecaudado}, de mayor a menor.
 *   {@code reporte_productos.csv}: {@code NombreProducto;Precio}, del mas vendido al menos vendido.
 * 
 *
 * Extras implementados: (a) varios archivos por vendedor y (c) deteccion de
 * formatos erroneos o informacion incoherente (se reporta una advertencia y se
 * omite la linea o el archivo).
 *
 * No solicita informacion al usuario.
 * 
 * @version 1.0
 */
public class main {

    private static final String ARCHIVO_REPORTE_VENDEDORES = "reporte_vendedores.csv";
    private static final String ARCHIVO_REPORTE_PRODUCTOS = "reporte_productos.csv";

    /** Vendedor cargado desde el archivo de vendedores. */
    private static class Vendedor {
        private final String nombreCompleto;
        private double totalRecaudado;

        Vendedor(String nombreCompleto) {
            this.nombreCompleto = nombreCompleto;
        }
    }

    /** Producto cargado desde el archivo de productos. */
    private static class Producto {
        private final String nombre;
        private final double precio;
        private long cantidadVendida;

        Producto(String nombre, double precio) {
            this.nombre = nombre;
            this.precio = precio;
        }
    }

    /** Cantidad de advertencias por datos erroneos encontrados. */
    private static int advertencias = 0;

    /**
     * Punto de entrada del programa.
     *
     * @param args no se utilizan
     */
    public static void main(String[] args) {
        try {
            Map<String, Producto> productos = cargarProductos(GenerateInfoFiles.ARCHIVO_PRODUCTOS);
            Map<String, Vendedor> vendedores = cargarVendedores(GenerateInfoFiles.ARCHIVO_VENDEDORES);

            procesarArchivosDeVentas(vendedores, productos);

            escribirReporteVendedores(vendedores);
            escribirReporteProductos(productos);

            System.out.println("Reportes generados exitosamente ("
                    + advertencias + " advertencias por datos erroneos).");
        } catch (IOException | RuntimeException e) {
            System.err.println("ERROR al procesar los archivos: " + e.getMessage());
        }
    }

    /**
     * Carga el archivo de productos ({@code ID;Nombre;Precio}), omitiendo
     * lineas con formato erroneo o precios negativos.
     *
     * @param ruta ruta del archivo de productos
     * @return mapa de id de producto a producto
     * @throws IOException si el archivo no puede leerse
     */
    private static Map<String, Producto> cargarProductos(String ruta) throws IOException {
        Map<String, Producto> productos = new HashMap<String, Producto>();
        List<String> lineas = Files.readAllLines(Paths.get(ruta), StandardCharsets.UTF_8);

        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            if (linea.isEmpty()) {
                continue;
            }
            String[] partes = linea.split(";");
            try {
                if (partes.length < 3) {
                    throw new IllegalArgumentException("se esperaban 3 campos");
                }
                double precio = Double.parseDouble(partes[2].trim());
                if (precio < 0) {
                    throw new IllegalArgumentException("precio negativo");
                }
                productos.put(partes[0].trim(), new Producto(partes[1].trim(), precio));
            } catch (IllegalArgumentException e) {
                advertir(ruta, i + 1, e.getMessage());
            }
        }
        return productos;
    }

    /**
     * Carga el archivo de vendedores ({@code Tipo;Numero;Nombres;Apellidos}).
     *
     * @param ruta ruta del archivo de vendedores
     * @return mapa de "tipo;numero" a vendedor
     * @throws IOException si el archivo no puede leerse
     */
    private static Map<String, Vendedor> cargarVendedores(String ruta) throws IOException {
        Map<String, Vendedor> vendedores = new HashMap<String, Vendedor>();
        List<String> lineas = Files.readAllLines(Paths.get(ruta), StandardCharsets.UTF_8);

        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            if (linea.isEmpty()) {
                continue;
            }
            String[] partes = linea.split(";");
            if (partes.length < 4) {
                advertir(ruta, i + 1, "se esperaban 4 campos");
                continue;
            }
            String clave = partes[0].trim() + ";" + partes[1].trim();
            vendedores.put(clave, new Vendedor(partes[2].trim() + " " + partes[3].trim()));
        }
        return vendedores;
    }

    /**
     * Recorre la carpeta del proyecto, procesa todos los archivos de ventas
     * ({@code ventas_*.txt}) y acumula el dinero recaudado por vendedor y las
     * unidades vendidas por producto.
     *
     * @param vendedores vendedores conocidos
     * @param productos  productos conocidos
     * @throws IOException si no se puede explorar la carpeta
     */
    private static void procesarArchivosDeVentas(Map<String, Vendedor> vendedores,
            Map<String, Producto> productos) throws IOException {
        try (DirectoryStream<Path> archivos = Files.newDirectoryStream(
                Paths.get("."), GenerateInfoFiles.PREFIJO_ARCHIVO_VENTAS + "*.txt")) {
            for (Path archivo : archivos) {
                procesarArchivoDeVentas(archivo, vendedores, productos);
            }
        }
    }

    /**
     * Procesa un archivo de ventas. La primera linea identifica al vendedor;
     * las demas son {@code IDProducto;Cantidad;}. Las lineas incoherentes
     * (producto inexistente, cantidad invalida o negativa) se omiten.
     *
     * @param archivo    archivo de ventas
     * @param vendedores vendedores conocidos
     * @param productos  productos conocidos
     * @throws IOException si el archivo no puede leerse
     */
    private static void procesarArchivoDeVentas(Path archivo, Map<String, Vendedor> vendedores,
            Map<String, Producto> productos) throws IOException {
        String nombreArchivo = archivo.getFileName().toString();
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);

        if (lineas.isEmpty()) {
            advertir(nombreArchivo, 1, "archivo vacio");
            return;
        }
        String[] cabecera = lineas.get(0).trim().split(";");
        if (cabecera.length < 2) {
            advertir(nombreArchivo, 1, "cabecera invalida, se omite el archivo");
            return;
        }
        Vendedor vendedor = vendedores.get(cabecera[0].trim() + ";" + cabecera[1].trim());
        if (vendedor == null) {
            advertir(nombreArchivo, 1, "vendedor no existe en el archivo de vendedores");
            return;
        }

        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            if (linea.isEmpty()) {
                continue;
            }
            String[] partes = linea.split(";");
            try {
                if (partes.length < 2) {
                    throw new IllegalArgumentException("se esperaban 2 campos");
                }
                Producto producto = productos.get(partes[0].trim());
                if (producto == null) {
                    throw new IllegalArgumentException("producto inexistente: " + partes[0]);
                }
                int cantidad = Integer.parseInt(partes[1].trim());
                if (cantidad <= 0) {
                    throw new IllegalArgumentException("cantidad no positiva");
                }
                producto.cantidadVendida += cantidad;
                vendedor.totalRecaudado += cantidad * producto.precio;
            } catch (IllegalArgumentException e) {
                advertir(nombreArchivo, i + 1, e.getMessage());
            }
        }
    }

    /**
     * Escribe el reporte de vendedores ordenado por recaudo, de mayor a menor.
     *
     * @param vendedores vendedores con sus totales
     * @throws IOException si no se puede escribir el archivo
     */
    private static void escribirReporteVendedores(Map<String, Vendedor> vendedores)
            throws IOException {
        List<Vendedor> lista = new ArrayList<Vendedor>(vendedores.values());
        Collections.sort(lista, new Comparator<Vendedor>() {
            @Override
            public int compare(Vendedor a, Vendedor b) {
                return Double.compare(b.totalRecaudado, a.totalRecaudado);
            }
        });

        try (BufferedWriter escritor = Files.newBufferedWriter(
                Paths.get(ARCHIVO_REPORTE_VENDEDORES), StandardCharsets.UTF_8)) {
            for (Vendedor vendedor : lista) {
                escritor.write(vendedor.nombreCompleto + ";"
                        + String.format(Locale.US, "%.2f", vendedor.totalRecaudado));
                escritor.newLine();
            }
        }
    }

    /**
     * Escribe el reporte de productos ordenado por cantidad vendida, de mayor a menor.
     *
     * @param productos productos con sus cantidades vendidas
     * @throws IOException si no se puede escribir el archivo
     */
    private static void escribirReporteProductos(Map<String, Producto> productos)
            throws IOException {
        List<Producto> lista = new ArrayList<Producto>(productos.values());
        Collections.sort(lista, new Comparator<Producto>() {
            @Override
            public int compare(Producto a, Producto b) {
                return Long.compare(b.cantidadVendida, a.cantidadVendida);
            }
        });

        try (BufferedWriter escritor = Files.newBufferedWriter(
                Paths.get(ARCHIVO_REPORTE_PRODUCTOS), StandardCharsets.UTF_8)) {
            for (Producto producto : lista) {
                escritor.write(producto.nombre + ";"
                        + String.format(Locale.US, "%.2f", producto.precio));
                escritor.newLine();
            }
        }
    }

    /**
     * Registra una advertencia por un dato erroneo o incoherente.
     *
     * @param archivo archivo donde se encontro el problema
     * @param linea   numero de linea (desde 1)
     * @param motivo  descripcion del problema
     */
    private static void advertir(String archivo, int linea, String motivo) {
        advertencias++;
        System.err.println("ADVERTENCIA [" + archivo + ", linea " + linea + "]: " + motivo);
    }
}
