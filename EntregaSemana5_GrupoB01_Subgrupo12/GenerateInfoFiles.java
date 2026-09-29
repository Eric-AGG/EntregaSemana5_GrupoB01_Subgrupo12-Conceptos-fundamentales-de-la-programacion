import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

//EntregaSemana5_GrupoB01_Subgrupo12
//@author Eric Andrey Gonzalez Gutierrez
//@author Julian Vacca Sanchez

/**
 * Genera los archivos planos pseudoaleatorios que sirven como entrada
 * para la clase {@code main} (procesador de ventas).
 *
 * Archivos generados (en la carpeta del proyecto):
 *
 *   {@code productos.txt}: {@code IDProducto;NombreProducto;PrecioPorUnidad}
 *   {@code vendedores.txt}: {@code TipoDocumento;NumeroDocumento;Nombres;Apellidos}
 *   {@code ventas_<id>_<nombre>.txt}: uno o mas archivos por vendedor.
 * 
 *
 * Esta clase no solicita informacion al usuario.
 *
 * @version 1.0
 */
public class GenerateInfoFiles {

    /** Tipo de documento usado para todos los vendedores generados. */
    private static final String TIPO_DOCUMENTO = "CC";

    /** Nombre del archivo de productos. */
    public static final String ARCHIVO_PRODUCTOS = "productos.txt";

    /** Nombre del archivo de informacion de vendedores. */
    public static final String ARCHIVO_VENDEDORES = "vendedores.txt";

    /** Prefijo de los archivos de ventas de cada vendedor. */
    public static final String PREFIJO_ARCHIVO_VENTAS = "ventas_";

    private static final String[] NOMBRES = {
        "Juan", "Maria", "Carlos", "Ana", "Luis", "Laura", "Andres", "Camila",
        "Jorge", "Paula", "Felipe", "Daniela", "Santiago", "Valentina",
        "Diego", "Sofia", "Miguel", "Natalia", "Sebastian", "Carolina"
    };

    private static final String[] APELLIDOS = {
        "Garcia", "Rodriguez", "Martinez", "Lopez", "Gonzalez", "Perez",
        "Sanchez", "Ramirez", "Torres", "Diaz", "Vargas", "Castro",
        "Rojas", "Moreno", "Jimenez", "Herrera", "Medina", "Ortiz"
    };

    private static final String[] NOMBRES_PRODUCTO = {
        "Arroz", "Frijol", "Aceite", "Azucar", "Sal", "Cafe", "Leche",
        "Huevos", "Pan", "Mantequilla", "Queso", "Atun", "Pasta",
        "Galletas", "Chocolate", "Jabon", "Detergente", "Papel higienico",
        "Gaseosa", "Jugo"
    };

    private static final Random ALEATORIO = new Random();

    /** Cantidad de productos del ultimo archivo de productos generado. */
    private static int cantidadProductos = 20;

    /**
     * Crea un archivo pseudoaleatorio de ventas para un vendedor.
     * Primera linea: {@code TipoDocumento;NumeroDocumento}. Luego, una venta
     * por linea: {@code IDProducto;Cantidad;}.
     *
     * @param randomSalesCount cantidad de ventas (lineas) a generar
     * @param name             nombre del vendedor (se usa en el nombre del archivo)
     * @param id               numero de documento del vendedor
     * @throws IOException si no se puede escribir el archivo
     */
    public static void createSalesMenFile(int randomSalesCount, String name, long id)
            throws IOException {
        createSalesMenFile(randomSalesCount, name, id, "");
    }

    /**
     * Version con sufijo, que permite crear varios archivos para un mismo vendedor.
     *
     * @param randomSalesCount cantidad de ventas a generar
     * @param name             nombre del vendedor
     * @param id               numero de documento del vendedor
     * @param sufijo           texto agregado al nombre del archivo (por ejemplo "_2")
     * @throws IOException si no se puede escribir el archivo
     */
    private static void createSalesMenFile(int randomSalesCount, String name, long id,
            String sufijo) throws IOException {
        if (randomSalesCount < 0) {
            throw new IllegalArgumentException("La cantidad de ventas no puede ser negativa");
        }
        String nombreSeguro = name.replaceAll("[^A-Za-z0-9]", "");
        String nombreArchivo = PREFIJO_ARCHIVO_VENTAS + id + "_" + nombreSeguro + sufijo + ".txt";

        try (BufferedWriter escritor = Files.newBufferedWriter(
                Paths.get(nombreArchivo), StandardCharsets.UTF_8)) {
            escritor.write(TIPO_DOCUMENTO + ";" + id);
            escritor.newLine();
            for (int i = 0; i < randomSalesCount; i++) {
                int idProducto = 1 + ALEATORIO.nextInt(cantidadProductos);
                int cantidadVendida = 1 + ALEATORIO.nextInt(20);
                escritor.write(formatearIdProducto(idProducto) + ";" + cantidadVendida + ";");
                escritor.newLine();
            }
        }
    }

    /**
     * Crea el archivo de productos con datos pseudoaleatorios.
     *
     * @param productsCount cantidad de productos a generar (mayor que cero)
     * @throws IOException si no se puede escribir el archivo
     */
    public static void createProductsFile(int productsCount) throws IOException {
        if (productsCount <= 0) {
            throw new IllegalArgumentException("Debe generarse al menos un producto");
        }
        cantidadProductos = productsCount;

        try (BufferedWriter escritor = Files.newBufferedWriter(
                Paths.get(ARCHIVO_PRODUCTOS), StandardCharsets.UTF_8)) {
            for (int i = 1; i <= productsCount; i++) {
                String nombre = NOMBRES_PRODUCTO[(i - 1) % NOMBRES_PRODUCTO.length];
                if (i > NOMBRES_PRODUCTO.length) {
                    nombre = nombre + " " + ((i - 1) / NOMBRES_PRODUCTO.length + 1);
                }
                long precio = (1 + ALEATORIO.nextInt(500)) * 100L;
                escritor.write(formatearIdProducto(i) + ";" + nombre + ";" + precio);
                escritor.newLine();
            }
        }
    }

    /**
     * Crea el archivo de informacion de vendedores con nombres y apellidos
     * tomados de listas de nombres reales, con documentos unicos.
     *
     * @param salesmanCount cantidad de vendedores a generar (mayor que cero)
     * @return lista con los vendedores generados: {tipoDoc, numeroDoc, nombres, apellidos}
     * @throws IOException si no se puede escribir el archivo
     */
    public static List<String[]> createSalesManInfoFile(int salesmanCount) throws IOException {
        if (salesmanCount <= 0) {
            throw new IllegalArgumentException("Debe generarse al menos un vendedor");
        }
        List<String[]> vendedores = new ArrayList<String[]>();
        Set<Long> documentosUsados = new HashSet<Long>();

        try (BufferedWriter escritor = Files.newBufferedWriter(
                Paths.get(ARCHIVO_VENDEDORES), StandardCharsets.UTF_8)) {
            while (vendedores.size() < salesmanCount) {
                long documento = 10000000L + (long) (ALEATORIO.nextDouble() * 1000000000L);
                if (!documentosUsados.add(documento)) {
                    continue;
                }
                String nombres = NOMBRES[ALEATORIO.nextInt(NOMBRES.length)];
                String apellidos = APELLIDOS[ALEATORIO.nextInt(APELLIDOS.length)] + " "
                        + APELLIDOS[ALEATORIO.nextInt(APELLIDOS.length)];
                vendedores.add(new String[] {
                    TIPO_DOCUMENTO, String.valueOf(documento), nombres, apellidos });
                escritor.write(TIPO_DOCUMENTO + ";" + documento + ";" + nombres + ";" + apellidos);
                escritor.newLine();
            }
        }
        return vendedores;
    }

    /**
     * Da formato al identificador de un producto (por ejemplo, 7 -> "P007").
     *
     * @param numero numero consecutivo del producto
     * @return identificador con formato
     */
    private static String formatearIdProducto(int numero) {
        return String.format("P%03d", numero);
    }

    /**
     * Punto de entrada: genera productos, vendedores y sus archivos de ventas.
     *
     * @param args no se utilizan
     */
    public static void main(String[] args) {
        final int totalProductos = 20;
        final int totalVendedores = 10;

        try {
            createProductsFile(totalProductos);
            List<String[]> vendedores = createSalesManInfoFile(totalVendedores);

            for (String[] vendedor : vendedores) {
                String nombreCompleto = vendedor[2] + " " + vendedor[3];
                long id = Long.parseLong(vendedor[1]);
                createSalesMenFile(5 + ALEATORIO.nextInt(26), nombreCompleto, id);
            }
            // Ejemplo de mas de un archivo para el mismo vendedor (extra a).
            String[] primero = vendedores.get(0);
            createSalesMenFile(8, primero[2] + " " + primero[3],
                    Long.parseLong(primero[1]), "_2");

            System.out.println("Archivos de prueba generados exitosamente.");
        } catch (IOException | RuntimeException e) {
            System.err.println("ERROR al generar los archivos: " + e.getMessage());
        }
    }
}
