import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Perender aset raster AgriBicara (Fase 8).
 *
 * Jalankan: java tools/StoreAssets.java
 *
 * KENAPA JAVA, dan bukan alat gambar biasa. Diverifikasi di mesin ini
 * 2026-09-02, jangan diulang percobaannya:
 *   - `convert` yang ada di PATH adalah C:\Windows\system32\convert, utilitas
 *     konversi FAT ke NTFS milik Windows. Ia membalas "Invalid drive
 *     specification", BUKAN ImageMagick.
 *   - `python` dan `python3` adalah stub Microsoft Store yang membalas
 *     "Python was not found".
 * JDK sudah wajib ada untuk membangun proyek ini, dan javax.imageio ada di
 * dalamnya. Jadi tidak ada dependency baru sama sekali.
 *
 * Geometri di bawah adalah salinan tepat dari
 * app/src/main/res/drawable/ic_launcher_foreground.xml dalam ruang koordinat
 * 108x108 yang sama. Kalau vector-nya berubah, UBAH JUGA di sini — tidak ada
 * test yang bisa menyadari keduanya berbeda, dan gejalanya adalah ikon toko
 * yang tidak sama dengan ikon di launcher.
 */
public final class StoreAssets {

    /** Sama dengan @color/ic_launcher_background dan palet Color.kt. */
    private static final Color HIJAU = new Color(0x2E7D32);
    private static final Color PUTIH = new Color(0xFFFFFF);
    /** GreenLight di Color.kt. Hanya untuk daun — bentuk dekoratif, bukan teks. */
    private static final Color DAUN = new Color(0xA5D6A7);
    /**
     * SurfaceLight di Color.kt, dipakai untuk teks tagline.
     *
     * DAUN sempat dipakai di sini dan itu keliru: rasionya di atas HIJAU hanya
     * 3,1:1. Angka itu lolos ambang WCAG untuk teks besar, tetapi jatuh di bawah
     * standar yang dipegang proyek ini sendiri sejak Fase 6, yang rasio
     * terendahnya 4,79:1. Warna ini 5,1:1.
     */
    private static final Color TEKS_TERANG = new Color(0xFFFBFF);

    /** Ruang koordinat vector sumber. */
    private static final double KANVAS = 108.0;

    private static final File DIR_TOKO = new File("docs/play/assets");
    private static final File DIR_RES = new File("app/src/main/res");

    public static void main(String[] args) throws IOException {
        if (!new File("app/build.gradle.kts").isFile()) {
            System.err.println("Jalankan dari root proyek: java tools/StoreAssets.java");
            System.exit(1);
        }
        DIR_TOKO.mkdirs();

        // --- Ikon toko Play -------------------------------------------------
        // 512x512, PNG 32-bit BER-ALPHA, maksimum 1024 KB.
        // Digambar sebagai KOTAK PENUH tanpa sudut membulat dan tanpa bayangan:
        // Play memberi mask beradius 30% dan bayangannya sendiri. Menambahkan
        // sudut membulat di sini menghasilkan sudut ganda yang terlihat kotor.
        tulis(ikon(512, 512, false, 1.0), new File(DIR_TOKO, "icon-512.png"));

        // --- Feature graphic ------------------------------------------------
        // 1024x500, TANPA alpha. TYPE_INT_RGB dipakai dengan sengaja; PNG
        // ber-alpha ditolak Play untuk aset ini.
        tulis(featureGraphic(), new File(DIR_TOKO, "feature-graphic.png"));

        // --- Ikon launcher legacy (API 24-25) -------------------------------
        // mipmap-anydpi-v26 tidak dibaca di bawah API 26. Tanpa PNG ini, ikon
        // HILANG persis di perangkat tertua yang didukung proyek (minSdk 24).
        int[][] densitas = {
            {48, 0}, {72, 1}, {96, 2}, {144, 3}, {192, 4},
        };
        String[] namaDir = {
            "mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi",
        };
        for (int[] d : densitas) {
            int px = d[0];
            File dir = new File(DIR_RES, namaDir[d[1]]);
            dir.mkdirs();
            // Sudut membulat 22% meniru bentuk yang diberikan mask sistem pada
            // API 26+, supaya ikon tidak terlihat berbeda saat pengguna
            // memperbarui perangkat.
            tulis(ikon(px, px, true, 1.25), new File(dir, "ic_launcher.png"));
            tulis(ikonBulat(px), new File(dir, "ic_launcher_round.png"));
        }

        System.out.println("Selesai. Aset toko di " + DIR_TOKO.getPath()
            + ", ikon legacy di " + DIR_RES.getPath() + "/mipmap-*dpi/");
    }

    /**
     * Ikon persegi.
     *
     * @param sudutMembulat true untuk ikon launcher legacy, false untuk ikon
     *                      toko Play yang wajib kotak penuh.
     * @param zoom          perbesaran motif terhadap ruang 108. Ikon legacy
     *                      memakai 1,25 agar mikrofonnya seukuran tampilan
     *                      adaptive icon setelah dipotong mask.
     */
    private static BufferedImage ikon(int w, int h, boolean sudutMembulat, double zoom) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = siap(img);

        g.setColor(HIJAU);
        if (sudutMembulat) {
            double r = w * 0.22;
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, r, r));
        } else {
            g.fillRect(0, 0, w, h);
        }

        double skala = (w / KANVAS) * zoom;
        double geser = (w - KANVAS * skala) / 2.0;
        g.translate(geser, geser);
        g.scale(skala, skala);
        motif(g);

        g.dispose();
        return img;
    }

    /** Varian bulat untuk android:roundIcon di API 25. */
    private static BufferedImage ikonBulat(int px) {
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = siap(img);

        g.setColor(HIJAU);
        g.fillOval(0, 0, px, px);

        double skala = (px / KANVAS) * 1.25;
        double geser = (px - KANVAS * skala) / 2.0;
        g.translate(geser, geser);
        g.scale(skala, skala);
        motif(g);

        g.dispose();
        return img;
    }

    private static BufferedImage featureGraphic() {
        int w = 1024;
        int h = 500;
        // TYPE_INT_RGB, bukan ARGB: Play menolak feature graphic ber-alpha.
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = siap(img);

        g.setColor(HIJAU);
        g.fillRect(0, 0, w, h);

        // Mikrofon di kiri.
        double skala = 3.0;
        g.translate(90, (h - KANVAS * skala) / 2.0);
        g.scale(skala, skala);
        motif(g);
        g.setTransform(new java.awt.geom.AffineTransform());

        g.setColor(PUTIH);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 92));
        g.drawString("AgriBicara", 470, 230);

        g.setColor(TEKS_TERANG);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 40));
        g.drawString("Tanya cuaca desa Anda", 474, 300);
        g.drawString("cukup dengan berbicara", 474, 352);

        g.dispose();
        return img;
    }

    /**
     * Motif mikrofon dalam ruang 108x108.
     *
     * Salinan tepat ic_launcher_foreground.xml. Seluruh bentuk berada dalam
     * jarak 33 dari titik (54,54) — safe zone adaptive icon.
     */
    private static void motif(Graphics2D g) {
        // Badan mikrofon: kapsul x45..63, y28..60 (radius sudut 9).
        g.setColor(PUTIH);
        g.fill(new RoundRectangle2D.Double(45, 28, 18, 32, 18, 18));

        // Busur "sedang mendengarkan": setengah lingkaran bawah, r=15.
        Path2D.Double busur = new Path2D.Double();
        busur.moveTo(39, 52);
        busur.lineTo(39, 55);
        busur.append(new Arc2D.Double(39, 40, 30, 30, 180, 180, Arc2D.OPEN), true);
        busur.lineTo(69, 52);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(busur);

        // Tiang dan palang alas.
        g.fill(new Rectangle2D.Double(51.5, 70, 5, 9));
        g.fill(new RoundRectangle2D.Double(42, 78, 24, 4, 4, 4));

        // Daun yang tumbuh dari pangkal tiang.
        Path2D.Double daun = new Path2D.Double();
        daun.moveTo(50, 75);
        daun.curveTo(44, 75, 39, 71, 38, 65);
        daun.curveTo(44, 64, 49, 68, 50, 75);
        daun.closePath();
        g.setColor(DAUN);
        g.fill(daun);
    }

    private static Graphics2D siap(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    private static void tulis(BufferedImage img, File tujuan) throws IOException {
        ImageIO.write(img, "png", tujuan);
        System.out.printf("%-52s %dx%d  %d byte%n",
            tujuan.getPath(), img.getWidth(), img.getHeight(), tujuan.length());
    }

    private StoreAssets() {
    }
}
